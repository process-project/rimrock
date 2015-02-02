#!/usr/bin/env python
"""Rimrock probe.

Usage:
    rimrock_probe.py -H <hostname> -t <timeout> [-x <proxy_path>] [-d]

Options:
    -H <hostname>       Target host for submitting jobs
    -t <timeout>        Time limit for individual operations.
    -x <proxy_path>     Proxy file location, also can be supplied as X509_USER_PROXY environment variable
    -d                  Print verbose output, not suitable for Nagios operation.

"""
import sys
import os
import base64
import httplib
import inspect
import time
from optparse import OptionParser

import simplejson


# global variables

debug = False
proxy = None
rimrock_url = None
ui_url = "zeus.cyfronet.pl"
timeout = 200

# helper functions

def return_with(code, msg, details=None):
    print msg
    if details is not None:
        print details
    sys.exit(code)


def return_ok(msg, details=None):
    return_with(0, msg, details)


def return_warning(msg, details=None):
    return_with(1, msg, details)


def return_critical(msg, details=None):
    return_with(2, msg, details)


def return_unknown(msg, details=None):
    return_with(3, msg, details)


def debug_log(text):
    if debug:
        sys.stderr.write(text + '\n')


def load_proxy(file_path):
    f = open(file_path)
    a = f.read()
    return base64.encodestring(a.strip()).replace("\n", "")


def check_response(desired_result, result):
    # this returns name of function calling check_response()
    test_name = inspect.stack()[2][4][0].strip()
    if result is None and desired_result != result:
        return_critical("Empty response for " + test_name + "\ndesired response:\n" + str(desired_result))
    for k, v in desired_result.items():
        if result[k] != v:
            return_critical("Response for " + test_name + " contains errors",
                            "actual result:\n" + str(result) + "\ndesired response:\n" + str(desired_result))


def make_request(path, payload=None, method="POST", add_headers=None):
    headers = {
        "Content-type": "application/json",
        "PROXY": proxy
    }

    if add_headers is not None:
        headers.update(add_headers)

    debug_log("Request: " + path + ", method: " + method + ", with payload: " + str(
        payload) + ", with additional headers: " + str(add_headers))

    conn = httplib.HTTPSConnection(rimrock_url)
    try:
        body = None
        if payload is not None:
            body = simplejson.dumps(payload)
        conn.request(method, path, body=body, headers=headers)
        # conn.set_debuglevel(1)
        resp = conn.getresponse()
    except Exception, e1:
        return_critical("Unable to do a post request", e1)

    response = resp.read()

    if response == "":
        return (None, resp.status)
    else:
        try:
            parsed_response = simplejson.loads(response)
        except Exception, e2:
            return_critical("Unable to parse response", "response:" + response + "\n" + str(e2))

        debug_log("Response: " + str(parsed_response) + ", code:" + str(resp.status))
        return (parsed_response, resp.status)


# testing scenarios

def process_sequence():
    payload = {"host": ui_url, "command": "pwd"}
    desired_response = {
        "status": "OK",
        "exit_code": 0,
        "error_output": "",
        "error_message": None
    }

    response, code = make_request("/api/process", payload)
    check_response(desired_response, response)


def iprocesses_sequence():
    response, code = make_request("/api/iprocesses", {"host": ui_url, "command": "bash"})
    check_response({"status": "OK"}, response)

    process_id = response["process_id"]
    debug_log("process_id: " + process_id)

    response, code = make_request("/api/iprocesses/" + process_id, method="GET")
    check_response({"status": "OK"}, response)

    response, code = make_request("/api/iprocesses", method="GET")
    if len(response) == 0:
        return_critical("Listing user jobs didn't return anything", response)

    response, code = make_request("/api/iprocesses/" + process_id, {"standard_input": "exit"}, method="PUT")
    check_response({"status": "OK"}, response)

    finished = response["finished"]
    count = 0

    while not finished:
        time.sleep(1)
        response, code = make_request("/api/iprocesses/" + process_id, method="GET")
        check_response({"status": "OK"}, response)
        finished = response["finished"]
        count += 1
        if count > 20:
            return_critical("Process did not finish in 20 seconds, after issuing exit command!")


def job_sequence():
    response, code = make_request("/api/jobs", {"host": ui_url, "script": "#!/bin/bash\necho hello\nexit 0"})
    status = response["status"]
    job_id = response["job_id"]

    response, code = make_request("/api/jobs", method="GET")

    if len(response) == 0:
        return_critical("Listing user jobs didn't return anything", response)

    time.sleep(3)
    response, code = make_request("/api/jobs/" + job_id, method="GET")
    status = response["status"]
    if status == "ERROR":
        return_critical("Job in ERROR state!", response)
    if status != "QUEUED" and status != "FINISHED":
        return_critical("Unknown job state!", response)


def job_cancel_sequence():
    response, code = make_request("/api/jobs", {"host": ui_url, "script": "#!/bin/bash\necho hello\nexit 0"})
    status = response["status"]
    job_id = response["job_id"]
    err_msg = None

    time.sleep(3)

    response, code = make_request("/api/jobs/" + job_id, method="DELETE")
    if code != 204:
        return_critical("Unable to abort job! Got code: " + str(code), response)

    count = 0

    while status != "ABORTED" and not (status == "ERROR" and "does not exist" in err_msg):
        time.sleep(1)
        response, code = make_request("/api/jobs/" + job_id, method="GET")
        status = response["status"]
        err_msg = response["error_message"]
        count += 1
        if count > 20:
            return_critical("Job cancel had no effect in 20 seconds!")


# main function

if __name__ == "__main__":
    parser = OptionParser(version="Rimrock probe v1.0")

    parser.add_option("-H", dest="hostname", metavar="hostname", help="Target host for submitting jobs")
    parser.add_option("-t", dest="timeout", metavar="timeout", type="int", default=200, help="Time limit for individual operations")
    parser.add_option("-x", dest="proxy_path", metavar="proxy_path", help="Proxy file location, also can be supplied as X509_USER_PROXY environment variable")
    parser.add_option("-d", action="store_true", dest="debug", help="Print verbose output, not suitable for Nagios operation", default=False)

    (options, args) = parser.parse_args()

    if len(args) != 0:
        return_unknown("Unkown arguments: " + args)

    if options.debug:
        debug = True
    debug_log("options: " + str(options) + ", arguments: " + str(args))

    proxy_location = None

    if "X509_USER_PROXY" in os.environ.keys():
        debug_log("X509_USER_PROXY is set")
        proxy_location = os.environ["X509_USER_PROXY"]
    if options.proxy_path:
        debug_log("-x argument is given")
        proxy_location = options.proxy_path

    if proxy_location is not None:
        debug_log("proxy location: " + proxy_location)
        try:
            proxy = load_proxy(os.path.expanduser(proxy_location))
            debug_log("got proxy: " + proxy[:40] + "...")
        except Exception, e:
            return_unknown("Unable to read proxy file", e)
    else:
        return_unknown("Unable to find proxy, please provide -x parameter or set X509_USER_PROXY environment variable!")

    if not options.hostname:
        return_unknown("Please provide a hostname with -H option")

    rimrock_url = options.hostname

    if options.timeout <= 0:
        return_unknown("Timeout is negative, it needs to be a positive value")

    timeout = options.timeout

    process_sequence()
    iprocesses_sequence()
    job_sequence()
    job_cancel_sequence()

    return_ok("OK", "Test plan completed without errors")
