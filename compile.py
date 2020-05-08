import argparse
import os
import shutil

import Constants
import Utils


def compile_program(file):
    print('Compiling {}...'.format(file), end='')
    cmd = ['javac', file, '-g', '-d', Constants.OUT_DIR, '-cp', Constants.OUT_DIR]
    Utils.execute_cmd_error_catch(cmd)


def main(classname):
    if classname == "all":
        if os.path.exists(Constants.OUT_DIR):
            print(Constants.OUT_DIR + " exists, removing and remaking.")
            shutil.rmtree(Constants.OUT_DIR)
        os.mkdir(Constants.OUT_DIR)
        src_files = Utils.get_all_files()
        print("Found {} source files.".format(len(src_files)))
    else:
        src_files = [os.path.join(Constants.SRC_DIR, classname) + ".java"]

    for file in src_files:
        compile_program(file)


if __name__ == '__main__':
    choice_lst = Utils.get_all_classes()
    choice_lst.append('all')

    parser = argparse.ArgumentParser()
    parser.add_argument("-c", "--classname", choices=choice_lst,
                        help="what class to compile (pass 'all' to compile everything) Allowed values are: " +
                             ", ".join(choice_lst), metavar='', default="all", required=False)
    args = parser.parse_args()

    main(args.classname)
