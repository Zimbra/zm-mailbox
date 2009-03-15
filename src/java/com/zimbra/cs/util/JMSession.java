/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.util;


import java.util.Properties;
import java.util.Random;

import javax.mail.MessagingException;
import javax.mail.Session;


import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

/**
 * @author schemers
 */
public class JMSession {

    static {
        // Assume that most malformed base64 errors occur due to incorrect delimiters,
        // as opposed to errors in the data itself.  See bug 11213 for more details.
        System.setProperty("mail.mime.base64.ignoreerrors", "true");
    }
    
    /**
     * Returns a new JavaMail session that has the latest SMTP settings from LDAP.
     */
    public static Session getSession()
    throws MessagingException {
        return getSession(null);
    }
    
    /**
     * Returns a new JavaMail session that has the latest SMTP settings from LDAP.
     * Settings are retrieved from the local server and overridden by the domain.
     * 
     * @param domain the domain, or <tt>null</tt> to use server settings
     */
    public static Session getSession(Domain domain)
    throws MessagingException {
        Server server = null;
        String smtpHost = null;
        
        try {
            server = Provisioning.getInstance().getLocalServer();
            smtpHost = getSmtpHost(server, domain);
        } catch (ServiceException e) {
            throw new MessagingException("Unable initialize JavaMail session", e);
        }
        
        Properties props = new Properties();
        props.setProperty("mail.mime.address.strict", "false");
        props.setProperty("mail.smtp.host", smtpHost);
        props.setProperty("mail.smtp.port", getValue(server, domain, Provisioning.A_zimbraSmtpPort));
        props.setProperty("mail.smtp.localhost", LC.zimbra_server_hostname.value());

        // Get timeout value in seconds from LDAP, convert to millis, and set on the session.
        String sTimeout = getValue(server, domain, Provisioning.A_zimbraSmtpTimeout);
        long timeout = (sTimeout == null ? 60 : Long.parseLong(sTimeout));
        sTimeout = Long.toString(timeout * Constants.MILLIS_PER_SECOND);
        props.setProperty("mail.smtp.connectiontimeout", sTimeout);
        props.setProperty("mail.smtp.timeout", sTimeout);
        
        Boolean sendPartial = Boolean.parseBoolean(getValue(server, domain, Provisioning.A_zimbraSmtpSendPartial));
        props.setProperty("mail.smtp.sendpartial", sendPartial.toString());
        
        Session session = Session.getInstance(props);
        return session;
    }
    
    /**
     * Returns the JavaMail SMTP session with settings from the given
     * account and its domain.
     */
    public static Session getSmtpSession(Account account)
    throws MessagingException {
        Domain domain = null;
        if (account != null) {
            try {
                domain = Provisioning.getInstance().getDomain(account);
            } catch (ServiceException e) {
                ZimbraLog.smtp.warn("Unable to look up domain for account %s.", account.getName(), e);
            }
        }
        Session session = getSession(domain);
        if (LC.javamail_smtp_debug.booleanValue() ||
            (account != null && account.isSmtpEnableTrace())) {
            session.setDebug(true);
        }
        return session;
    }
    
    /**
     * Returns the attr value from the server or domain.
     */
    private static String getValue(Server server, Domain domain, String attrName) {
        String value = null;
        if (domain != null) {
            value = domain.getAttr(attrName);
        }
        if (StringUtil.isNullOrEmpty(value)) {
            return server.getAttr(attrName);
        }
        return value;
    }
    
    private static final Random RANDOM = new Random();
    
    /**
     * Returns a random value specified for <tt>zimbraSmtpHostname</tt> on the 
     * server or domain.
     *  
     * @param server the server
     * @param domain the domain, or <tt>null</tt> to use server settings
     */
    private static String getSmtpHost(Server server, Domain domain)
    throws ServiceException {
        String[] hosts = null;
        if (domain != null) {
            hosts = domain.getSmtpHostname();
        }
        if (hosts == null || hosts.length == 0) {
            hosts = server.getSmtpHostname();
        }
        if (hosts == null || hosts.length == 0) {
            throw ServiceException.FAILURE("Could not determine SMTP hostname.", null);
        }
        if (hosts.length == 1) {
            return hosts[0];
        } else {
            return hosts[RANDOM.nextInt(hosts.length)];
        }
    }
}
