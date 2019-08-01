#!/usr/local/bin/perl
#
#====================================================================================#
#  ngn_FrmMainframe_empl.sh      This control file will bring the 'EMPLOYEE' file -  #
#                                the first of two ngn Payroll files                  #
#                                to Unix from the Mainframe.                         #
#                                                                                    #
#  ( Date value is: Myymm=Monthend Payroll that is currently being processed         #
#                                                                                    #
# Modification History:                                                              #
# Date:      By:    Description:                                                     #
# 1/29/07    Bev    Script Created.                                                  #
#                                                                                    #
#====================================================================================#
#

$FISPLD = $ENV{FISPLD};

#constants
$MAINFRAME_DATASET="PPSP.NGN.M$FISPLD.EMPLOYEE";
$LAWLESS_DATASET="EMPLOYEE";

$LRECL=46;
$BLKSZ=27968;


($mainframe_dataset_final = $MAINFRAME_DATASET);
($lawless_dataset_final = $LAWLESS_DATASET);

open(FTP,">/tms/ngn/control/ngn_FrmMainframe_empl.ftp.sh");
print FTP "#!/bin/sh\n";
print FTP "/bin/ftp hubble << EOF\n";
##print FTP "\tquote site trailingblanks\n";
print FTP "\tquote site lrecl=$LRECL blk=$BLKSZ recfm=FB trk pri=2500 sec=75\n";
print FTP "\tcd ..\n";
print FTP "\tget $mainframe_dataset_final /tms/ngn/extracts/$lawless_dataset_final\n";
print FTP "\tquit\n";
print FTP "EOF\n";
close(FTP);
`/usr/local/bin/ssh2 -xq lawless /bin/sh /tms/ngn/control/ngn_FrmMainframe_empl.ftp.sh`;
