#!/bin/bash
#
#	This script starts MC's Java web server on ARM systems
#
#
PORT=$(netstat -nat 2>/dev/null | grep ':5001' | grep LISTEN)
while [ -z "$PORT" ]
do
	sleep 1
	PORT=$(netstat -nat 2>/dev/null | grep ':5001' | grep LISTEN)
done

while true; do
	pkill java
	/var/jre1.8.0_111/bin/java -jar /usr/javaModules/TPWebServer.jar 2>&1 | logger
	sleep 1
done
