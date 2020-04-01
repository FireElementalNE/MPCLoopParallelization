import argparse
import os
import shutil
import subprocess

BASE_DIR = 'test_programs'
OUT_DIR = os.path.join(BASE_DIR, 'out')
SRC_DIR = os.path.join(BASE_DIR, 'src')


def execute_cmd(cmd):
    (outs, errs) = execute_cmd_helper(cmd)
    if errs:
        print('ERROR.')
        print(errs)
    else:
        print('Done.')


def execute_cmd_helper(cmd):
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True)
    try:
        outs, errs = proc.communicate()
        return outs, errs
    except subprocess.TimeoutExpired:
        proc.kill()
        outs, errs = proc.communicate()
        return outs, errs


def get_all_files():
    global SRC_DIR
    return [os.path.join(SRC_DIR, filename) for filename in os.listdir(SRC_DIR)]


def main(main_args):
    global BASE_DIR
    global OUT_DIR
    global SRC_DIR

    if main_args.classname == "all":
        if os.path.exists(OUT_DIR):
            print(OUT_DIR + " exists, removing and remaking.")
            shutil.rmtree(OUT_DIR)
        os.mkdir(OUT_DIR)
        src_files = get_all_files()
        print("Found {} source files.".format(len(src_files)))
    else:
        src_files = [os.path.join(SRC_DIR, main_args.classname) + ".java"]

    for file in src_files:
        print('Compiling {}...'.format(file), end='')
        cmd = ['javac', file, '-g', '-d', OUT_DIR, '-cp', OUT_DIR]
        execute_cmd(cmd)


if __name__ == '__main__':
    choice_lst = [os.path.basename(el).split('.')[0] for el in get_all_files()]
    choice_lst.append('all')

    parser = argparse.ArgumentParser()
    parser.add_argument("-c", "--classname", choices=choice_lst,
                        help="what class to compile (pass 'all' to compile everything) Allowed values are: " +
                             ", ".join(choice_lst), metavar='', default="all", required=False)
    args = parser.parse_args()

    main(args)
