#!/bin/sh
##############################################################################
#  mc_2Mainframe.sh               This control file will ftp the output      #
#                                  of the ngn process on Unix to the         #
#                                  Mainframe so that the file can be loaded  #
#                                  to Ifis so process as a ledger input file #
#                                                                            #
# ( Date value is Myymm )=Payroll Monthend that is currently being processed #
#                                                                            #
# Modification History:                                                      #
# Date:       By:   Description:                                             #
# 06/28/06    ege                                                            # 
# 01/29/07    Bev   Changed date stamped file to generic M.F. file-date      #
#                   stamping will take place in M.F. job GAD001ZM            # 
##############################################################################

/bin/ftp adcom2 << EOF1
	ascii
	quote site lrecl=250 blksize=23250 trk pri=250 sec=95 recfm=fb
	put /tms/ngn/extracts/MCGLDOC 'FISP.JVDATA.KWP.NGNMC.GL.FINAL' (replace
	quit
EOF1
