#!/bin/sh

 mailx -s "Monthly NGN Recharge Batch Process Message File" tms-support-l@ucsd.edu < /tms/ngn/msgs/ngn_process.msg
 mailx -s "Monthly NGN Recharge Batch Process Message File" its-finance@ucsd.edu < /tms/ngn/msgs/ngn_process.msg


