#!/bin/bash

SCHEDULE_NUM=`ps -ef | grep com.lenovo.tv.schedule.ShiyunMain | grep -v grep | awk '{print $2}'`
CHECK_NUM=`ps -ef | grep process.sh | grep -v grep | awk '{print $2}'`

[ -n "$SCHEDULE_NUM" ] && kill -9 $SCHEDULE_NUM
[ -n "$CHECK_NUM" ] && kill -9 $CHECK_NUM