# MPCLoopParallelization

Loop Parallelization for MPC

## Building

This should be pretty strait forward, you will need these reqs:

* maven found [here](https://maven.apache.org/install.html)

You will also need to set the **M2_HOME**, **MAVEN_HOME** and **JAVA_HOME** env variables.
Once that is done all that is needed is to run the following in the root dir:
```bash
mvn package
```

## Usage

The usage is as follows:
```bash
usage: utility-name
 -c,--class <arg>        name of the class to analyze
 -cp,--classpath <arg>   path to the class to analyze
 -j,--jcepath <arg>      complete path to jce.jar, default: C:\Program
                         Files\Java\jdk1.8.0_221\jre\lib\jce.jar
 -r,--rtpath <arg>       complete path to rt.jar, default: C:\Program
                         Files\Java\jdk1.8.0_221\jre\lib\rt.jar
```

## Notes

* You can compile the test programs (assuming that javac is in your PATH) by running
compile.py

* There are two loggers, one is a file set to INFO the other is a console logger set to DEBUG both can be configured in tinylog.properties.
