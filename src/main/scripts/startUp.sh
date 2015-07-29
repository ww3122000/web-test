/run.sh 
#!/bin/bash
BASE_DIR="/data/web-test"
PID_DIR=$BASE_DIR/pid

MAIN_CLASS="com.ksc.s3.StartUp"

JAVA_OPTS="$JAVA_OPTS -server -Xms2g -Xmx2g -XX:PermSize=156m -XX:MaxPermSize=256m -XX:NewRatio=1 -XX:MaxNewSize=2g"
JAVA_OPTS="$JAVA_OPTS -XX:MaxTenuringThreshold=30 -XX:SurvivorRatio=3 -XX:TargetSurvivorRatio=90"
JAVA_OPTS="$JAVA_OPTS -verbose:gc -XX:+PrintTenuringDistribution -XX:+PrintGCDetails -XX:+PrintGCTimeStamps"
JAVA_OPTS="$JAVA_OPTS -Xloggc:$LOG_DIR/gclog_$1.log -XX:+UseParallelOldGC -XX:PretenureSizeThreshold=20485760"
JAVA_OPTS="$JAVA_OPTS -Djava.library.path=$DEPLOY_DIR/webplib/linux-x64 -Dhttp.proxyHost=picProxy.com -Dhttp.proxyPort=17778"
JAVA_OPTS="$JAVA_OPTS -Dks3LogRoot=$LOG_DIR -Dlogdir="$LOG_DIR/" -Djava.io.tmpdir="/dev/shm/""
#JAVA_OPTS="$JAVA_OPTS -Djava.rmi.server.hostname=10.153.16.13 -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=18999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"

cd $BASE_DIR/bin/

CLASSPATH=".:../config" 

for lib in `ls ../lib/*.jar`
do
    CLASSPATH="$CLASSPATH:$lib"
done

source ~/.bash_profile

#kill `cat $PID_DIR/pid`
#sleep 3
    
exec "java" $JAVA_OPTS -cp $CLASSPATH $MAIN_CLASS

echo $! > $PID_DIR/pid