# MPCLoopParallelization

Loop Parallelization for MPC

## Building

This should be pretty strait forward, you will need these reqs:

* maven found [here](https://maven.apache.org/install.html)

If on windows the env variable **M2_HOME**, **MAVEN_HOME** and **JAVA_HOME** need to be set.
I believe that on _*nix_ systems that is done automatically, however I am not positive about OSX 
(I don't have a mac to test on). Once that is done all that is needed is to run the following in the root dir:
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

* the default **-j** and **-r** arguments change depending on operating system.

* Currently the depedencies are:
    *  [tinylog-api 2.0.1](https://mvnrepository.com/artifact/org.tinylog/tinylog-api/2.0.1)
    * [tinylog-impl 2.0.1](https://mvnrepository.com/artifact/org.tinylog/tinylog-impl/2.0.1)
    * [soot 4.2.0](https://mvnrepository.com/artifact/ca.mcgill.sable/soot/4.1.0)
    * [graphviz-java 0.15.1](https://mvnrepository.com/artifact/guru.nidi/graphviz-java/0.15.1)
    * [commons-lang3 3.9](https://mvnrepository.com/artifact/org.apache.commons/commons-lang3/3.9)
    * [commons-cli 1.4](https://mvnrepository.com/artifact/commons-cli/commons-cli/1.4)

Again though, the **mvn package** step should download and set all that up.
