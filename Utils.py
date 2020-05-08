import os
import subprocess

import sys

import Constants


def execute_cmd_error_catch(cmd):
    (outs, errs) = execute_cmd(cmd)
    if errs:
        print('ERROR.')
        print(errs, file=sys.stderr)
        sys.exit(0)
    else:
        print('Done.')


def execute_cmd(cmd):
    # TODO: sort out how to deal with errors in run_all_tests.py
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True)
    try:
        outs, errs = proc.communicate()
        return outs, errs
    except subprocess.TimeoutExpired:
        proc.kill()
        outs, errs = proc.communicate()
        return outs, errs


def get_all_files():
    return [os.path.join(Constants.SRC_DIR, filename) for filename in os.listdir(Constants.SRC_DIR)]


def get_all_classes():
    return [os.path.basename(el).split('.')[0] for el in get_all_files()]
