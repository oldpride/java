https://medium.com/javarevisited/back-to-the-basics-of-java-part-1-classpath-47cf3f834ff
learn how java package works without maven.

The CLASSPATH is simply a list of
   - directories,
   - JAR files,
   - ZIP archives
CLASSPATH default to current dir.

This example will use the dir feature of CLASSPATH.

$ javaenv 11

file structure
    $ find .
    .
    ./bin
    ./readme.txt
    ./readme.txt.orig
    ./src
    ./src/myprogram
    ./src/myprogram/Main.java
    ./src/myprogram/utils
    ./src/myprogram/utils/Util.java

compile
    $ javac src/myprogram/Main.java src/myprogram/utils/Util.java -d bin/

compiled files were automatically follow the structure
    $ find bin/
        bin/
        bin/myprogram
        bin/myprogram/Main.class
        bin/myprogram/utils
        bin/myprogram/utils/Util.class

run the compiled files
    $ echo $CLASSPATH
    (CLASSPATH was not set. default to current dir)

    $ cd bin

    bin$ java myprogram.Main
    Here is 1337

the above command tried to find
    myprogram/Main.class
    myprogram/utils/Util.class
from CLASSPATH, which was defaulted to ".". 
They both were found.

As long as the two files can be found relative to a
dir in CLASSPATH, the above command will work !!!

https://medium.com/javarevisited/back-to-the-basics-of-java-part-2-the-jar-6e923685d571
created jar file   
    $ jar -c -e myprogram.Main -f project.jar -C bin/ myprogram/Main.class -C bin/ myprogram/utils/Util.class
    (
        -c|--create          create
        -e myprogram.Main    specify the executable
        -f|--file            archive name. (output)
        -C bin/ xxx.class    2-arg flag, change dir, include file.
    )
or
    $ jar -cfe project.jar myprogram.Main -C bin/ .
    (specified the directory and a wildcard using the dot "." 
        which takes everything inside the bin directory
    )

run with the jar
    $ java -cp project.jar myprogram.Main
or
    $ java -jar project.jar

to check the one component in the jar file, 
    $ jar -tvf project.jar
        0 Sun May 28 22:44:42 EDT 2023 META-INF/
       97 Sun May 28 22:44:42 EDT 2023 META-INF/MANIFEST.MF
      476 Sun May 28 21:21:52 EDT 2023 myprogram/Main.class
      687 Sun May 28 21:21:52 EDT 2023 myprogram/utils/Util.class

    $ unzip -q -c project.jar META-INF/MANIFEST.MF
        Manifest-Version: 1.0
        Created-By: 11.0.16.1 (Oracle Corporation)
        Main-Class: myprogram.Main

        -q    quite mode
        -c    extract to stdout

Error:
    if jar without -C bin/, i couldn't run it
        $ jar -c -e myprogram.Main -f project.jar bin/myprogram/Main.class bin/myprogram/utils/Util.class
    
    ran it
        $ java -cp project.jar myprogram.Main
        Error: Could not find or load main class myprogram.Main
        Caused by: java.lang.ClassNotFoundException: 
            myprogram.Main

    this was because the jar file had extra bin/ folder
        $ jar -tf project.jar
        META-INF/
        META-INF/MANIFEST.MF
        bin/myprogram/Main.class
        bin/myprogram/utils/Util.class

    running it with bin didn't help
        $ java -cp project.jar bin.myprogram.Main
        Error: Could not find or load main class bin.myprogram.Main
        Caused by: java.lang.NoClassDefFoundError: myprogram/Main 
        (wrong name: bin/myprogram/Main)
fix:
    use the -C bin/ ... command. (see above)