#!/usr/local/bin/bash
DTSTR=`date +'%y%m%d'`
LOGBASE=./log
LOGFILE=$LOGBASE/log.$DTSTR
LOGHIS=$LOGBASE/loghis
if [ -n "$QUERY_STRING" ];then
    LOGFILE=$LOGBASE/log.$QUERY_STRING.$DTSTR
fi
if [ -z "`grep $DTSTR $LOGHIS`" ];then
    echo $DTSTR >> $LOGHIS
fi

if [ -n "$CONTENT_LENGTH" ];then
  while true;do
    read -n $CONTENT_LENGTH QUERY_STRING_POST
    if [ -z "$QUERY_STRING_POST" ];then
      break
    fi
    echo $QUERY_STRING_POST >> $LOGFILE
  done
fi
echo Content-type: text/plain
echo ""
echo "ok"