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


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import javax.mail.MessagingException;
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
        Server server;
        String smtpHost = null;
        
        try {
            server = Provisioning.getInstance().getLocalServer();
            smtpHost = getRandomSmtpHost(domain);
        } catch (ServiceException e) {
            throw new MessagingException("Unable initialize JavaMail session", e);
        }
        if (smtpHost == null) {
            throw new MessagingException("Unable to determine SMTP host for " + domain);
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
     * Caches the list of SMTP hosts per domain.  The key is the domain name
     * (or <tt>null</tt> for no domain).
     */
    private static Map<String, List<String>> sSmtpHosts =
        Collections.synchronizedMap(new TimeoutMap<String, List<String>>(LC.smtp_host_retry_millis.intValue()));
    
    public static void resetSmtpHosts() {
        ZimbraLog.smtp.info("Resetting cached SMTP hosts.");
        sSmtpHosts.clear();
    }
    
    /**
     * Returns a random value specified for <tt>zimbraSmtpHostname</tt> on the 
     * server or domain, or <tt>null</tt> if the host name cannot be determined.
     *  
     * @param server the server
     * @param domain the domain, or <tt>null</tt> to use server settings
     */
    private static String getRandomSmtpHost(Domain domain)
    throws ServiceException {
        List<String> hosts = getSmtpHostsInternal(domain);
        if (hosts.size() == 0) {
            return null;
        }
        if (hosts.size() == 1) {
            return hosts.get(0);
        } else {
            return hosts.get(RANDOM.nextInt(hosts.size()));
        }
    }
    
    /**
     * Returns the SMTP hosts.  Domain settings override server settings.
     * 
     * @param domain the domain, or <tt>null</tt> to get the SMTP hosts
     * from the local server
     */
    public static List<String> getSmtpHosts(Domain domain)
    throws ServiceException {
        // Return a copy of the list, so callers don't modify the original.
        List<String> copy = new ArrayList<String>();
        copy.addAll(getSmtpHostsInternal(domain));
        return copy;
    }
    
    private static List<String> getSmtpHostsInternal(Domain domain)
    throws ServiceException {
        String domainName = (domain == null ? null : domain.getName());
        List<String> hosts = sSmtpHosts.get(domainName);
        if (hosts == null) {
            // Load from LDAP and put in the cache.
            String[] hostsFromLdap = getSmtpHostsFromLdap(domain);
            if (hostsFromLdap.length > 0) {
                hosts = Collections.synchronizedList(new ArrayList<String>());
                Collections.addAll(hosts, hostsFromLdap);
                sSmtpHosts.put(domainName, hosts);
            } else {
                return Collections.emptyList();
            }
        }
        return hosts;
    }
    
    /**
     * Remove the specified host from the SMTP hosts list for the given
     * domain.  This host will not be retried for the remainder of the
     * interval specified by {@link LC#smtp_host_retry_millis}.  If the
     * last host is removed, the next call to {@link #getSmtpHost} will
     * reload from LDAP.
     * 
     * @param domain the domain or <tt>null</tt>
     * @param hostName the SMTP server hostname
     * @return the number of remaining SMTP hosts
     */
    public static int removeSmtpHost(Domain domain, String hostName) {
        ZimbraLog.smtp.info("Removing bad smtp host '%s' from the cache.", hostName);
        
        String domainName = (domain == null ? null : domain.getName());
        List<String> hosts = sSmtpHosts.get(domainName);
        if (hosts != null) {
            hosts.remove(hostName);
            if (hosts.size() > 0) { 
                // Reset timeout
                sSmtpHosts.put(domainName, hosts);
            } else {
                // Remove cached hostnames so we reload from LDAP next time.
                sSmtpHosts.remove(domainName);
            }
            return hosts.size();
        } else {
            return 0;
        }
    }
    
    private static final String[] NO_HOSTS = new String[0];

    /**
     * Returns the value of <tt>zimbraSmtpHostname</tt>.  If the value
     * is not set on the domain, returns the value for the local server.
     * 
     * @param domain, or <tt>null</tt> to use the local server
     * @return the SMTP hosts, or an empty array
     */
    private static String[] getSmtpHostsFromLdap(Domain domain)
    throws ServiceException {
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
