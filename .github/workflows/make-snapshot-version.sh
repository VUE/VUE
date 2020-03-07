#!/bin/sh
set -e

NEWVER=+CI-$(date +%Y%m%d)
echo "Appending suffix snapshot version $NEWVER"
sed -i "s/vue.version=\(.*\)/\1$NEWVER/" VUE2/src/main/resources/tufts/vue/VueResources.properties
