#!/bin/sh
# -----------------------------------------------------------------------------
#

. ./setEnv.sh
export CLASSPATH=$CLASSPATH
$JAVA_HOME/bin/SoundETL -Xms256m -Xmx1024m  -Djava.awt.headless=true -Dfile.encoding=UTF-8 -DFlumeProbe.home=`pwd` "cluster.startup.Main"  "$@"