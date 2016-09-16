#!/usr/bin/env bash
# -----------------------------------------------------------------------------
#  Set CLASSPATH and Java options
#
# -----------------------------------------------------------------------------

#set JAVA_HOME & UC_HOME here
JAVA_HOME=/usr/jdk6/jdk1.6.0_32;export JAVA_HOME
UC_HOME=..;export UC_HOME

# Make sure prerequisite environment variables are set
if [ -z "$JAVA_HOME" ]; then
  echo "The JAVA_HOME environment variable is not defined"
  echo "This environment variable is needed to run this program"
  exit 1
fi
if [ -z "$UC_HOME" ]; then
  echo "The UC_HOME environment variable is not defined"
  echo "This environment variable is needed to run this program"
  exit 1
fi
if [ ! -r "$UC_HOME"/bin/setEnv.sh ]; then
  echo "The UC_HOME environment variable is not defined correctly"
  echo "This environment variable is needed to run this program"
  exit 1
fi

# Set standard CLASSPATH
CLASSPATH="$JAVA_HOME"/jre/lib:"$UC_HOME"/bin:"$UC_HOME"/conf:"$UC_HOME"/lib:"$UC_HOME"/classes:"$UC_HOME"/images:"$UC_HOME"/src

# Append jars to CLASSPATH
if [ -d "$UC_HOME"/lib ]; then
  for i in "$UC_HOME"/lib/*.jar; do
    CLASSPATH="$CLASSPATH":"$i"
  done
fi
