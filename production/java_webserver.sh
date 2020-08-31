#!/bin/bash
#
#	This script starts and stops the Java web server
#
#

case $1 in

start)
	sh /usr/javaModules/run_java_webserver.sh &
;;
stop)
	ps aux | grep run_java_webserver.sh > /tmp/java
	awk '{print $2}' /tmp/java > /tmp/javapid
	for i in `cat /tmp/javapid`
	do
		kill -9 $i
	done
	pkill java

esac
