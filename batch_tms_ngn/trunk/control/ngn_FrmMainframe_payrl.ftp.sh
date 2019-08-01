#!/bin/sh
/bin/ftp hubble << EOF
	quote site lrecl=91 blk=27937 recfm=FB trk pri=2500 sec=75
	cd ..
	get PPSP.NGN.M1401.PAYROLL /tms/ngn/extracts/PAYROLL
	quit
EOF
