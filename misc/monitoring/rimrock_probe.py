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


def read_proxy(file_path):
    f = open(file_path)
    a = f.read()
    return base64.encodestring(a.strip()).replace("\n", "")


# testing scenarios

def process_sequence():
    payload = {"host": ui_url, "command": "pwd"}
    headers = {
        "Content-type": "application/json",
        "Accept": "*/*",
        "PROXY": proxy
    }
    desired_response = {
        "status": "OK",
        "exit_code": 0,
        "error_output": "",
        "error_message": None
    }

    conn = httplib.HTTPSConnection(rimrock_url, timeout=timeout)
    try:
        conn.request("POST", "/api/process", body=json.dumps(payload), headers=headers)
        resp = conn.getresponse()
    except Exception, e:
        return_critical("Unable to do a post request for process_sequence", e)

    response = resp.read()

    try:
        parsed_response = json.loads(response)
    except Exception, e:
        return_critical("Unable to parse response of process_sequence", "response:" + response + "\n" + str(e))

    for k, v in desired_response.items():
        if parsed_response[k] != v:
            return_critical("Response for process_sequence contains errors", response)

# main function

if __name__ == "__main__":
    arguments = docopt(__doc__, version="Rimrock probe 1.0")
    if arguments['-d']:
        debug = True
    debug_log("arguments: " + str(arguments))

    if "X509_USER_PROXY" in os.environ.keys():
        proxy_location = os.environ["X509_USER_PROXY"]
        try:
            proxy = read_proxy(os.path.expanduser(proxy_location))
            debug_log("got proxy: " + proxy)
        except Exception, e:
            return_unknown("Unable to read proxy file", e)
    else:
        return_unknown("Unable to find proxy, please set X509_USER_PROXY environment variable!")

    rimrock_url = arguments['-H']

    if not unicode(arguments['-t']).isnumeric() or int(arguments['-t']) <= 0:
        return_unknown("Timeout is not a number, please supply a positive, non zero numerical value")

    timeout = int(arguments['-t'])

    process_sequence()

    return_ok("OK", "Test plan completed without errors")