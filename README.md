jnuma
=========

A Java library for accessing NUMA (Non Uniform Memory Access) API. 

## Usage 

(Scala) Add depedency settings to your sbt project file (e.g., `project/build.sbt`) :

    libraryDependencies += "org.xerial" % "jnuma" % "0.1.3"

(Java) Add maven dependency settings for using `org.xerial, jnuma, 0.1.3`

#### Using snapshot versions
Add a resolver setting to your project file:

    resolvers += "Sonatype snapshot repo" at "https://oss.sonatype.org/content/repositories/snapshots/"

## API

Call static methods defined in [xerial.jnuma.Numa](https://oss.sonatype.org/service/local/repositories/releases/archive/org/xerial/jnuma/0.1.3/jnuma-0.1.3-javadoc.jar/!/xerial/jnuma/Numa.html)

## limitation

Currenty jnuma supports 64-bit Linux only. For the other operating systems, standard memory allocation in JVM will be used.


