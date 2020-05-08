import argparse
import os
import shutil
import tarfile

import sys

import Constants
import Utils
import compile


def make_tarfile():
    with tarfile.open(Constants.ALL_TESTS_OUT_TGZ, "w:gz") as tar:
        tar.add(Constants.ALL_TESTS_OUT_DIR, arcname=os.path.basename(Constants.ALL_TESTS_OUT_DIR))


def remake_dir(name):
    if os.path.exists(name):
        print('{} exists. Deleting and remaking.'.format(name))
        shutil.rmtree(name)
    os.mkdir(name)


def run_test(classname):
    # TODO: add proper logger
    cmd = ['java', '-jar', Constants.JAR_NAME, '-c', classname]
    print('Running {}...'.format(classname), end='')
    Utils.execute_cmd(cmd)
    print('Done.')
    class_dir = os.path.join(Constants.ALL_TESTS_OUT_DIR, classname)
    os.mkdir(class_dir)
    graph_files = [os.path.join(Constants.GRAPH_DIR, el) for el in os.listdir(Constants.GRAPH_DIR)]
    z3_files = [os.path.join(Constants.Z3_DIR, el) for el in os.listdir(Constants.Z3_DIR)]
    all_files = graph_files + z3_files
    print('Copying files for {}...'.format(classname), end='')
    for file in all_files:
        shutil.copy2(file, os.path.join(class_dir, os.path.basename(file)))
    shutil.copy2(Constants.LOG_FILE, os.path.join(class_dir, Constants.LOG_FILE))
    print('Done.')


def main(jar_file):
    compile.main('all')
    if Constants.JAR_NAME not in os.listdir('.'):
        print("{} not found in root dir, copying...".format(Constants.JAR_NAME), end='')
        shutil.copy2(jar_file, os.path.join('.', Constants.JAR_NAME))
        if Constants.JAR_NAME not in os.listdir('.'):
            sys.stderr.write("Error could not copy file.")
            sys.exit(0)
        else:
            print('Done.')
    else:
        print('{} found.'.format(Constants.JAR_NAME))
    remake_dir(Constants.ALL_TESTS_OUT_DIR)
    if os.path.exists(Constants.ALL_TESTS_OUT_TGZ):
        print('Deleting {}.'.format(Constants.ALL_TESTS_OUT_TGZ))
        os.remove(Constants.ALL_TESTS_OUT_TGZ)
    all_classes = Utils.get_all_classes()
    for c in all_classes:
        run_test(c)
    print("Making tar file...", end='')
    make_tarfile()
    print("Done.")


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-j', '--jar',
                        help="jar location", metavar='', default=os.path.join('target', Constants.JAR_NAME),
                        required=False)
    args = parser.parse_args()
    main(args.jar)
