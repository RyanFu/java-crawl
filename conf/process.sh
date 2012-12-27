#!/bin/bash

[ -z "$SCHEDULE_HOME" ] && SCHEDULE_HOME=`pwd`

[ ! -z "$SCHEDULE_HOME/conf" ] && CLASSPATH=$CLASSPATH:$SCHEDULE_HOME/conf

for file in $SCHEDULE_HOME/lib/*.jar
do
    [ -f $file ] && CLASSPATH=$CLASSPATH:$file
done

# Uncomment the following line if you have OutOfMemoryError errors
#JAVA_OPTS="-Xms128m -Xmx1024m -Xdebug -Xint -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8001"
#JAVA_OPTS="-Xms2048m -Xmx4096m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./dump"

CLASSNAME=com.lenovo.tv.schedule.ShiyunMain

RUNJAVA="java $JAVA_OPTS -cp $CLASSPATH $CLASSNAME"

while [ 1 ] ; do
    PROCESS_NUM=`ps -ef | grep $CLASSNAME | grep -v grep | wc -l`
    [ $PROCESS_NUM -eq 0 ] && nohup $RUNJAVA > /dev/null &
    sleep 1d
done