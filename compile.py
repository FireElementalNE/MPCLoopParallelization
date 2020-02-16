import os
import subprocess

def execute_cmd(cmd):
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True)
    try:
        outs, errs = proc.communicate()
        return (outs, errs)
    except subprocess.TimeoutExpired:
        proc.kill()
        outs, errs = proc.communicate()
        return (outs, errs)

cmd = 'javac ./resources/src/{} -d ./resources/out'

lst = os.listdir('./resources/src')
for el in lst:
	if el.endswith('.java'):
		print('Compiling {}...'.format(el), end='')
		(outs, errs) = execute_cmd(cmd.format(el))
		if errs:
			print('ERROR.')
			print(errs)
		else:
			print('Done.')
	