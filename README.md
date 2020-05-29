# MPCLoopParallelization

Loop Parallelization for MPC

HYCC examples are [here](https://gitlab.com/securityengineering/HyCC/-/tree/master/examples).

Currently working:

* BFS algorithm
* Finding SCC for intra-loop dependencies
* Phi variable parsing
* Maven packaging system (see below)
* Graph creation
* Added tons of HYCC tests.

In progress:

* Designing the algorithm for multiple loops

## Building

This should be pretty strait forward, you will need these reqs:

* maven found [here](https://maven.apache.org/install.html)

If on windows the env variable **M2_HOME**, **MAVEN_HOME** and **JAVA_HOME** need to be set.
I believe that on _*nix_ systems that is done automatically, however I am not positive about OSX 
(I don't have a mac to test on). Once that is done all that is needed is to run the following in the root dir:
```bash
mvn package
```
Pyhon must also be installed an available in the PATH.
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
[compile.py](https://github.com/FireElementalNE/MPCLoopParallelization/blob/master/compile.py)

* There are two loggers, one is a file set to DEBUG the other is a console logger set to DEBUG both can be configured in [tinylog.properties](https://github.com/FireElementalNE/MPCLoopParallelization/blob/master/src/main/resources/tinylog.properties).

* a folder that holds the graphs. This is deleted (if it exists) and recreated at runtime.

* the default **-j** and **-r** arguments change depending on operating system.

* Currently, the Java dependencies are (the **mvn package** step should download and set these up):
    * [tinylog-api 2.0.1](https://mvnrepository.com/artifact/org.tinylog/tinylog-api/2.0.1)
    * [tinylog-impl 2.0.1](https://mvnrepository.com/artifact/org.tinylog/tinylog-impl/2.0.1)
    * [soot 4.2.0](https://mvnrepository.com/artifact/ca.mcgill.sable/soot/4.1.0)
    * [graphviz-java 0.15.1](https://mvnrepository.com/artifact/guru.nidi/graphviz-java/0.15.1)
    * [commons-lang3 3.9](https://mvnrepository.com/artifact/org.apache.commons/commons-lang3/3.9)
    * [commons-cli 1.4](https://mvnrepository.com/artifact/commons-cli/commons-cli/1.4)
    * [exp4j 0.4.8](https://mvnrepository.com/artifact/net.objecthunter/exp4j/0.4.8)
    * [graphviz-rough 0.16.2](https://mvnrepository.com/artifact/guru.nidi/graphviz-rough)

* THe Python dependencies:

    * [z3-solver 4.8.7.0](https://pypi.org/project/z3-solver/)

* Creates graph pngs in the graphs/ directory

* Createds python files in the z3_python/ directory

* Creates JavaDoc jar file

