#!/bin/sh
echo "Eventually, you should see a directory listing of /tmp on OKI1 if the server is running."
echo ""
set -x
java -cp filingAll.jar CabList http://oki1.mit.edu:1799/tmp
