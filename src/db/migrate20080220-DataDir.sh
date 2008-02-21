#!/bin/bash
source "/opt/zimbra/bin/zmshutil" || exit 1

zmsetvars \
        zimbra_home

mkdir -p ${zimbra_home}/data/postfix
mkdir -p ${zimbra_home}/data/amavisd

chown zimbra:zimbra ${zimbra_home}/data

${zimbra_home}/bin/zmlocalconfig -e postfix_queue_directory=${zimbra_home}/data/postfix/spool

if [ -d "${zimbra_home}/postfix-2.4.3.4z/spool" ]; then
	if [ -d "${zimbra_home}/data/postfix/spool" ]; then
		rmdir spool 2>/dev/null
		if [ $? == 1 ]; then
  			echo "Failed to remove ${zimbra_home}/data/postfix/spool."
			echo "Manually migrate existing postfix spool after upgrade."
		else
			mv -f ${zimbra_home}/postfix-2.4.3.4z/spool ${zimbra_home}/data/postfix/
		fi
	else
		mv -f ${zimbra_home}/postfix-2.4.3.4z/spool ${zimbra_home}/data/postfix/
	fi
fi

if [ -d "${zimbra_home}/amavisd/.spamassassin" ]; then
	mv -f ${zimbra_home}/amavisd/.spamassassin ${zimbra_home}/data/amavisd/
fi

if [ -d "${zimbra_home}/amavisd/db" ]; then
	mv -f ${zimbra_home}/amavisd/db ${zimbra_home}/data/amavisd/
fi

if [ -d "${zimbra_home}/amavisd/quarantine" ]; then
	mv -f ${zimbra_home}/amavisd/quarantine ${zimbra_home}/data/amavisd/
fi

if [ -d "${zimbra_home}/amavisd/tmp" ]; then
	mv -f ${zimbra_home}/amavisd/tmp ${zimbra_home}/data/amavisd/
fi

if [ -d "${zimbra_home}/amavisd/var" ]; then
	mv -f ${zimbra_home}/amavisd/var ${zimbra_home}/data/amavisd/
fi

${zimbra_home}/libexec/zmfixperms
