
# Building jnuma

## Requirements

* libnuma (install by `yum numactl-devel`)
* GCC (glibc 2.5 or higher)
* JDK (1.5 or higher)

## Building a native library

    $ make native

## Create a JAR package

    $ bin/sbt package


## Install jnuma to local package directory

    $ bin/sbt publish-local



