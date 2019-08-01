#!/bin/sh
. /env/db2.sh

LOGIN=`cat $HOME/.db2user`
PASS=`cat $HOME/.db2pwd`

EXHOME="/tms/ngn/extracts"
ASHOME="/tms/ngn/control"

CLASSPATH=$ASHOME/lib

/usr/java1.8/bin/java -cp $CLASSPATH FormatMainframeToDB2

db2 connect to tms_db user $LOGIN using $PASS
db2 set schema ngn
db2 import from '/tms/ngn/extracts/employee.txt' of del "insert into ngn.ngn_employee"
db2 import from '/tms/ngn/extracts/payroll.txt' of del "insert into ngn.ngn_payroll"
db2 terminate

ES=$?

rm $EXHOME/employee.txt
rm $EXHOME/payroll.txt

exit $ES
