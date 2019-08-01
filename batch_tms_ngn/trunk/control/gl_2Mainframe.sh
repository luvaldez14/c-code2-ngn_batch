#!/bin/sh
##############################################################################
#  gl_2Mainframe.sh                This control file will ftp the output     #
#                                  of the gl process on Unix to the          #
#                                  Mainframe so that the file can be loaded  #
#                                  to Ifis so process as a ledger input file #
# ( Date value is Myymm )=         GL  month being processed                 #
#                                                                            #
# Modification History:                                                      #
# Date:       By:   Description:                                             #
# 11/28/05    Bev   Creation of job                                          # 
# 07/06/10    EGE   Changed date stamped name to generic name for M.F. File  #
#                   date stamping of file will be in M.F. job                #
##############################################################################

$FISP = $ENV{FISP};

#constants
$MAINFRAME_DATASET="FISP.JVDATA.$FISPLD.CAMPTELC";

/bin/ftp adcom2 << EOF1
	ascii
	put /tms/fis/extracts/glintf.dat 'FISP.JVDATA.$FISPLD01.CAMPTELC' (replace
	quit
EOF1

/bin/ftp adcom2 << EOF1
        ascii
        put /tms/fis/extracts/glintl.dat 'FISP.JVDATA.$FISPLD02.CAMPTELC' (replace
        quit
EOF1

