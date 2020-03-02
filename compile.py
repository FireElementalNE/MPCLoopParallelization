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

def cp_sep():
	if platform.system() == 'Windows':
		return ";"
	else:
		return ":"

def make_cp():
	jars = os.listdir('project_jars')
	cp = "\""
	for jar in jars:
		full_path = os.path.join('project_jars', jar)
		cp += "{}{}".format(full_path, cp_sep())
	cp += "\""
	return cp

def make_manifest():
	fh = open('manifest.txt', 'w+')
	fh.write('Main-Class: Main\n')
	tmp = make_cp()
	tmp = tmp.replace(cp_sep(), " ")
	tmp = tmp.replace("\"", '')
	fh.write("Class-Path: {}\n".format(tmp))
	fh.close()

# if os.path.exists('build'):
# 	shutil.rmtree('build')
# os.mkdir('build')

os.chdir('build')

make_manifest()

all_lib_jars = os.path.join('project_jars', '*.jar')
all_class_files = os.path.join('*.class')
all_src_files = os.path.join('..', 'src', '*.java')

cmd = 'javac -cp {} {} -d .'.format(make_cp(), all_src_files)
execute_cmd(cmd)
cmd = 'jar cvfm MPCLoopParallelization.jar manifest.txt *.class {}'.format(all_lib_jars)
execute_cmd(cmd)



	