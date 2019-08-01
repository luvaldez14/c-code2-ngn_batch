#!/bin/sh
# establish environment
. /etc/server_env.sh
echo $JLINK_BATCH_ENVIRONMENT
echo $ACT_SERVER_ENV

DB2LIB="/usr/db2_9.1"
DB2CLASSPATH=:\
$DB2LIB/db2jcc.jar:\
$DB2LIB/db2jcc_license_cisuz.jar

ASHOME="/tms/ngn/control"
CLASSPATH=$ASHOME/lib:\
$ASHOME/lib/javax.jar:\
$ASHOME/lib/COM.jar:\
$ASHOME/lib/mail.jar:\
$ASHOME/lib/com.jar:\
$ASHOME/lib/jlink2.jar:\


OUTPUT_FILE='/tms/ngn/msgs/health_sciences_reports'`date '+%m%d%Y'`'.log'

/usr/java1.8/bin/java -Xmx450M -cp $DB2CLASSPATH:$CLASSPATH \
-Dbatch.env=$ACT_SERVER_ENV \
-Dtms.batch.environment=$JLINK_BATCH_ENVIRONMENT \
-Djlink.app.context.path=$ASHOME \
-Djlink.batch=true \
-Djlink.log.common=false \
-Djlink.log.exceptions=false \
-Djlink.log.sql=false \
HsReports

ES=$?
 
cp $OUTPUT_FILE /tms/ngn/histmsg

exit $ES 