#!/bin/bash

# You must have exported ECLIPSE_HOME for this to work, ie:
# export ECLIPSE_HOME=/home/username/eclipse
BASEDIR=`pwd`

rm -rfd temp
mkdir -p temp/plugins
mv plugins/* temp/plugins/
java -jar $ECLIPSE_HOME/plugins/org.eclipse.equinox.launcher_*.jar \
   -application org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher \
   -metadataRepository file://$BASEDIR \
   -artifactRepository file://$BASEDIR \
   -source $BASEDIR/temp \
   -publishArtifacts
rm -rfd temp
