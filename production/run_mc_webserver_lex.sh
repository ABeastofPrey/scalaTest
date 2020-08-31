#!/bin/bash
#
#	This script starts MC's Java web server on LEX
#
#

while true; do
	pkill java
	/usr/java/latest/bin/java -jar /usr/javaModules/TPWebServer.jar 2>&1 | logger
	sleep 1
done
