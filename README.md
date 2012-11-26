jnuma
=========

A Java library for accessing NUMA (Non Uniform Memory Access) API. 


## Usage 

(Scala) Add depedency settings to your sbt project file:

    resolvers += "Sonatype snapshot repo" at "https://oss.sonatype.org/content/repositories/snapshots/"

    libraryDependencies += "org.xerial" % "jnuma" % "0.1-SNAPSHOT"



(Java) Add maven dependency settings for using `org.xerial, jnuma,
0.1-SNAPSHOT` and snapshot repository
<https://oss.sonatype.org/content/repositories/snapshots/>

## API

Call static methods defined in [xerial.jnuma.Numa](https://oss.sonatype.org/service/local/repositories/snapshots/archive/org/xerial/jnuma/0.1-SNAPSHOT/jnuma-0.1-SNAPSHOT-javadoc.jar/!/xerial/jnuma/Numa.html)


## limitation

Currenty jnuma supports 64-bit Linux only. 


