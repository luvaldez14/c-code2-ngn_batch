#!/bin/sh
/bin/ftp hubble << EOF
	quote site lrecl=46 blk=27968 recfm=FB trk pri=2500 sec=75
	cd ..
	get PPSP.NGN.M1401.EMPLOYEE /tms/ngn/extracts/EMPLOYEE
	quit
EOF
