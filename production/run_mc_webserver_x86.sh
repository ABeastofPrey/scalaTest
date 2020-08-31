#!/bin/bash
#
#	This script starts MC's Java web server on X86 systems
#
#

while true; do
	pkill java
	/var/jre1.8.0_112/bin/java -jar /usr/javaModules/TPWebServer.jar 2>&1 | logger
	sleep 1
done
