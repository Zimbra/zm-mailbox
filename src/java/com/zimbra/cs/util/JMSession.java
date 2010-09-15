/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.TimeoutMap;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailclient.smtp.SmtpTransport;
import com.zimbra.cs.mailclient.smtp.SmtpsTransport;

/**
 * Factory for JavaMail {@link Session}.
 *
 * @author schemers
 */
public final class JMSession {

    public static final String SMTP_SEND_PARTIAL_PROPERTY = "mail.smtp.sendpartial";
    public static final String SMTPS_SEND_PARTIAL_PROPERTY = "mail.smtps.sendpartial";

    private static final Session sSession;
    static {
        // Assume that most malformed base64 errors occur due to incorrect delimiters,
        // as opposed to errors in the data itself.  See bug 11213 for more details.
        System.setProperty("mail.mime.base64.ignoreerrors", "true");

        Properties props = new Properties();
        props.setProperty("mail.mime.address.strict", "false");
        sSession = Session.getInstance(props);
        setProviders(sSession);
    }

    /**
     * Registers custom JavaMail providers to the {@link Session}.
     *
     * @param session JavaMail {@link Session}
     */
    public static void setProviders(Session session) {
        if (LC.javamail_zsmtp.booleanValue()) {
            try {
                session.setProvider(SmtpTransport.PROVIDER);
                session.setProvider(SmtpsTransport.PROVIDER);
            } catch (NoSuchProviderException e) {
                assert(false);
            }
        }
    }

    /**
     * Returns the shared JavaMail {@link Session} that has the latest SMTP
     * settings from LDAP.
     */
    public static Session getSession() {
        return sSession;
    }

    /**
     * Returns a new JavaMail {@link Session} that has the latest SMTP settings
     * from the local server.
     */
    public static Session getSmtpSession() throws MessagingException {
        return getSmtpSession((Domain) null);
    }

    /**
     * Returns a new JavaMail {@link Session} that has the latest SMTP settings
     * from LDAP. Settings are retrieved from the local server and overridden by
     * the domain.
     *
     * @param domain the domain, or {@code null} to use server settings
     */
    private static Session getSmtpSession(Domain domain) throws MessagingException {
        Server server;
        String smtpHost = null;

        try {
            server = Provisioning.getInstance().getLocalServer();
            smtpHost = getRandomSmtpHost(domain);
        } catch (ServiceException e) {
            throw new MessagingException("Unable initialize JavaMail session", e);
        }
        if (smtpHost == null) {
            String msg = "No SMTP hosts available";
            if (domain != null) {
                msg += " for domain " + domain.getName();
            }
            throw new MessagingException(msg);
        }

        Properties props = new Properties(sSession.getProperties());
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
        props.setProperty(SMTP_SEND_PARTIAL_PROPERTY, sendPartial.toString());
        props.setProperty(SMTPS_SEND_PARTIAL_PROPERTY, sendPartial.toString());

        Session session = Session.getInstance(props);
        setProviders(session);
        if (LC.javamail_smtp_debug.booleanValue()) {
            session.setDebug(true);
        }
        return session;
    }

    /**
     * Returns the JavaMail SMTP {@link Session} with settings from the given
     * account and its domain.
     */
    public static Session getSmtpSession(Account account) throws MessagingException {
        Domain domain = null;
        if (account != null) {
            try {
                domain = Provisioning.getInstance().getDomain(account);
            } catch (ServiceException e) {
                ZimbraLog.smtp.warn("Unable to look up domain for account %s.", account.getName(), e);
            }
        }
        Session session = getSmtpSession(domain);
        if (account != null && account.isSmtpEnableTrace()) {
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

    /**
     * Caches the set of SMTP hosts that we've failed to connect to.  Only
     * the key is used.  The value is ignored.
     */
    private static Map<String, Object> sBadSmtpHosts =
        Collections.synchronizedMap(new TimeoutMap<String, Object>(LC.smtp_host_retry_millis.intValue()));

    public static void resetSmtpHosts() {
        ZimbraLog.smtp.debug("Resetting bad SMTP hosts.");
        sBadSmtpHosts.clear();
    }

    /**
     * Returns a random value specified for <tt>zimbraSmtpHostname</tt> on the
     * server or domain, or <tt>null</tt> if the host name cannot be determined.
     *
     * @param server the server
     * @param domain the domain, or <tt>null</tt> to use server settings
     */
    private static String getRandomSmtpHost(Domain domain) throws ServiceException {
        String[] hosts = getSmtpHostsFromLdap(domain);
        if (hosts.length == 0) {
            return null;
        }

        if (hosts.length == 1) {
            if (isHostBad(hosts[0])) {
                return null;
            } else {
                return hosts[0];
            }
        }


        List<String> hostList = Arrays.asList(hosts);
        Collections.shuffle(hostList);
        for (String currentHost : hostList) {
            if (!isHostBad(currentHost)) {
                return currentHost;
            }
        }
        return null;
    }

    private static boolean isHostBad(String hostname) {
        if (hostname != null) {
            hostname = hostname.toLowerCase();
        }
        return sBadSmtpHosts.containsKey(hostname);
    }

    /**
     * Returns a new set that contains all SMTP hosts, not including
     * hosts that were marked as bad with {@link #markSmtpHostBad}.
     */
    public static Set<String> getSmtpHosts(Domain domain) throws ServiceException {
        Set<String> hosts = new HashSet<String>();
        for (String host : getSmtpHostsFromLdap(domain)) {
            if (!isHostBad(host)) {
                hosts.add(host);
            }
        }
        return hosts;
    }

    /**
     * Mark the given SMTP host as bad.  We will not attempt to
     * connect to this host for the
     * interval specified by {@link LC#smtp_host_retry_millis}.
     *
     * @param hostName the SMTP server hostname
     */
    public static void markSmtpHostBad(String hostName) {
        if (hostName == null) {
            return;
        }
        ZimbraLog.smtp.info(
            "Disallowing connections to %s for %d milliseconds.", hostName, LC.smtp_host_retry_millis.intValue());
        sBadSmtpHosts.put(hostName.toLowerCase(), null);
    }

    private static final String[] NO_HOSTS = new String[0];

    /**
     * Returns the value of <tt>zimbraSmtpHostname</tt>.  If the value
     * is not set on the domain, returns the value for the local server.
     *
     * @param domain, or <tt>null</tt> to use the local server
     * @return the SMTP hosts, or an empty array
     */
    private static String[] getSmtpHostsFromLdap(Domain domain) throws ServiceException {
        String[] hosts = NO_HOSTS;
        if (domain != null) {
            hosts = domain.getSmtpHostname();
        }
        if (hosts.length == 0) {
            Server server = Provisioning.getInstance().getLocalServer();
            hosts = server.getSmtpHostname();
        }
        return hosts;
    }
}
