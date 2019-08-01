#!/bin/sh
##############################################################################
#  ngn_2Mainframe.sh               This control file will ftp the output     #
#                                  of the ngn process on Unix to the         #
#                                  Mainframe so that the file can be loaded  #
#                                  to Ifis so process as a ledger input file #
# ( Date value is Myymm )=Payroll Processing month being processed           #
#                                                                            #
# Modification History:                                                      #
# Date:       By:   Description:                                             #
# 11/28/05    Bev   Creation of job                                          # 
# 01/29/07    Bev   Changed date stamped name to generic name for M.F. File  #
#                   date stamping of file will be in M.F. job GAD001ZN       #
##############################################################################

/bin/ftp adcom2 << EOF1
	ascii
	quote site lrecl=250 blksize=23250 trk pri=250 sec=95 recfm=fb
	put /tms/ngn/extracts/GLDOC 'FISP.JVDATA.KWP.NGNCHG.GL.FINAL' (replace
	quit
EOF1
