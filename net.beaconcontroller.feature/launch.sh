#!/bin/bash
dir=$(cd $(dirname $0)/../..; pwd)
java -Dosgi.console -Declipse.consoleLog=true -Dosgi.clean=true -Dosgi.debug -Declipse.ignoreApp=true \
  -Dosgi.configuration.area=${dir}/configuration \
  -jar ${dir}/plugins/org.eclipse.osgi_*.jar
