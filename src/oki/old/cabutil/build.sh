#!/bin/sh

# A shell script to compile all the filing demo source,
# as well as build a COMPLETE jar file that includes
# ALL the core OKI code, plus the filing code -- everything
# you need to run.  This has only been tested on a unix
# box (MACOSX).

# This must be run in the current directory within the
# tech build tree: tech/contrib/mit/apps/examples/util/filing/CabUtil

if [ ! -d build ]; then
    mkdir build
fi
TECH=../../../../../../..
JARFILE=filingAll.jar
LIBDIR=$TECH/build/lib
function assertFile()
{
    if [ ! -r $1 ]; then
        echo "Can't find $1 -- run top level build in tech (ant).";
        exit 1;
    fi
}
assertFile $LIBDIR/mitServiceImpl.jar
assertFile $LIBDIR/okiServiceApi.jar
assertFile $LIBDIR/okiServiceImpl.jar
export CLASSPATH=$LIBDIR/mitServiceImpl.jar:$LIBDIR/okiServiceApi.jar:$LIBDIR/okiServiceImpl.jar
set -x
javac -deprecation -d build *.java 
#jar cf $JARFILE -C build build/*
# Some bug in jar is causing the first file specified this way to
# appear missing even if it's there.  So we add a dummy file to
# take the hit.
jar cf $JARFILE -C build intentionally-missing build/*
jar uf $JARFILE -C $TECH/build/classes org
jar uf $JARFILE -C $TECH/contrib/mit/impl/build/classes edu
jar uf $JARFILE -C $TECH/contrib/mit/impl/build/classes krb4
ls -l $JARFILE
# Copy it to somewhere not so buried in the path in case
# you want to use it manually on the command line with "java -cp"
cp $JARFILE $LIBDIR
