#!/bin/sh
if [ ! -d local ]; then
    mkdir local
fi
set -x
java -cp filingAll.jar CabSyncViewer local http://oki1.mit.edu:1799/tmp
