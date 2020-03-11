import os
import subprocess
import shutil
import platform

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
        return (outs, errs)
    except subprocess.TimeoutExpired:
        proc.kill()
        outs, errs = proc.communicate()
        return (outs, errs)

test_base_dir = 'test_programs'
out_dir = os.path.join(test_base_dir, 'out')

print(out_dir + " exists, removing and remaking.")

if os.path.exists(out_dir):
	shutil.rmtree(out_dir)
os.mkdir(out_dir)

src_dir = os.path.join(test_base_dir, 'src')
src_files = [os.path.join(src_dir, filename) for filename in os.listdir(src_dir)]

print("Found {} source files.".format(len(src_files)))

for file in src_files:
    print('Compiling {}...'.format(file), end='')
    cmd = ['javac', file, '-d', out_dir, '-cp', out_dir]
    execute_cmd(cmd)



	