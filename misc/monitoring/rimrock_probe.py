"""Rimrock probe.

Usage:
    rimrock_probe.py -H <hostname> -t <timeout> [-d]

Options:
    -H <hostname>   Target host for submitting jobs
    -t <timeout>    Time limit for individual operations.
    -d              Print verbose output, not suitable for Nagios operation.

"""
import sys
import os
import base64
import json
import httplib
import inspect
import time
from docopt import docopt

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
    for k, v in desired_result.items():
        if result[k] != v:
            return_critical("Response for " + test_name + " contains errors",
                            "actual result:\n" + str(result) + "\ndesired response:\n" + str(desired_result))


def make_request(path, payload=None, method="POST"):
    headers = {
        "Content-type": "application/json",
        "PROXY": proxy
    }

    debug_log("Request: " + path + ", method: " + method + ", with payload: " + str(payload))

    conn = httplib.HTTPSConnection(rimrock_url, timeout=timeout)
    try:
        body = json.dumps(payload) if payload is not None else None
        conn.request(method, path, body=body, headers=headers)
        # conn.set_debuglevel(1)
        resp = conn.getresponse()
    except Exception, e1:
        return_critical("Unable to do a post request for process_sequence", e1)

    response = resp.read()

    if response == "":
        return (None, resp.status)
    else:
        try:
            parsed_response = json.loads(response)
        except Exception, e2:
            return_critical("Unable to parse response of process_sequence", "response:" + response + "\n" + str(e2))

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


def iprocess_sequence():
    response, code = make_request("/api/iprocess", {"host": ui_url, "command": "bash"})
    check_response({"status": "OK"}, response)

    process_id = response["process_id"]
    debug_log("process_id: " + process_id)

    response, code = make_request("/api/iprocess/" + process_id, method="GET")
    check_response({"status": "OK"}, response)

    response, code = make_request("/api/iprocess", method="GET")
    if len(response) == 0:
        return_critical("Listing user jobs didn't return anything", response)

    response, code = make_request("/api/iprocess/" + process_id, {"standard_input": "exit"}, method="PUT")
    check_response({"status": "OK"}, response)

    finished = response["finished"]
    count = 0

    while not finished:
        time.sleep(1)
        response, code = make_request("/api/iprocess/" + process_id, method="GET")
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
    arguments = docopt(__doc__, version="Rimrock probe 1.0")
    if arguments['-d']:
        debug = True
    debug_log("arguments: " + str(arguments))

    if "X509_USER_PROXY" in os.environ.keys():
        proxy_location = os.environ["X509_USER_PROXY"]
        try:
            proxy = load_proxy(os.path.expanduser(proxy_location))
            debug_log("got proxy: " + proxy[:40] + "...")
        except Exception, e:
            return_unknown("Unable to read proxy file", e)
    else:
        return_unknown("Unable to find proxy, please set X509_USER_PROXY environment variable!")

    rimrock_url = arguments['-H']

    if not unicode(arguments['-t']).isnumeric() or int(arguments['-t']) <= 0:
        return_unknown("Timeout is not a number, please supply a positive, non zero numerical value")

    timeout = int(arguments['-t'])

    process_sequence()
    iprocess_sequence()
    job_sequence()
    job_cancel_sequence()

    return_ok("OK", "Test plan completed without errors")