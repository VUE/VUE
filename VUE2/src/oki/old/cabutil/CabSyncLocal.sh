#!/bin/sh
if [ ! -d local ]; then
    mkdir local
fi
if [ ! -d remote ]; then
    mkdir remote
fi
set -x
java -cp filingAll.jar CabSyncViewer -poll 5 local remote
