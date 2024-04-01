#!/bin/bash
# Check if the immutable attribute is already set on the directory
if lsattr -d /opt/zimbra/jetty_base/webapps/ | grep -q 'i'; then
	echo "**** remove restrict write access for /opt/zimbra/jetty_base/webapps/ (zimbra-mbox-war)****"
	chattr -i -R /opt/zimbra/jetty_base/webapps/
fi
