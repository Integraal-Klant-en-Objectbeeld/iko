#!/bin/sh

OPTS="-XX:InitialHeapSize=1024M -XX:MinRAMPercentage=70 -XX:MaxRAMPercentage=80 -Djava.security.egd=file:/dev/./urandom"
JWDP="-agentlib:jdwp=transport=dt_socket,server=y,address=*:8000,suspend=n"
java $OPTS $(if [ "$JDWP_ENABLED" = "true" ]; then echo -n "$JWDP "; else echo -n " "; fi)-jar /app/iko.jar