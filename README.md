scala-min
=========

A minimal project template to start programming with Scala.

### Contents

scala-min project includes:
- Sample Scala codes.
- [ScalaTest](http://www.scalatest.org/) examples (writing specs, logging, tagging tests, measuring code performances, etc.)
- Pre-configured settings for developing with IntelliJ IDE
- A command for packaging projects with [sbt-pack](http://github.com/xerial/sbt-pack) plugin.
  - `sbt-pack` also generates installation scripts for you programs.

### Usage

Download [tar.gz archive](https://github.com/xerial/scala-min/archive/0.1.1.tar.gz) or [.zip](https://github.com/xerial/scala-min/archive/0.1.1.zip) of this project, then extract the contents.
Alternatively, you can run the following commands to extract the scala-min project:

    $ mkdir myproject
    $ cd myproject
    $ curl -L https://github.com/xerial/scala-min/archive/0.1.tar.gz | tar xvz --strip-components=1


**Run tests**

    $ bin/sbt test

**Run tests when updates of the source codes are detected**
   
    $ bin/sbt "~test"

**Run specific tests matching a pattern**

    $ bin/sbt "~test-only *HelloTest"

**Run tagged test only**

    $ bin/sbt "~test -- include(test1)"

**Create a runnable package**
  
    $ bin/sbt pack
    $ target/pack/bin/hello
    Hello World!!

**Install your program**

    $ bin/sbt pack
    $ cd target/pack; make install
    $ ~/local/bin/hello
    Hello World!!

**Create IntelliJ project files**

    $ bin/sbt gen-idea

Then open the project folder from IntelliJ.

**Add dependent libraries**

Edit `project/Build.scala`, then add libraries to `libraryDependences` variable.

### Customize

Rename the project name defined in `project/Build.scala` as you like.

### ToDo

- Add examples using command line parser

