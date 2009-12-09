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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import javax.mail.Session;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.TimeoutMap;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailclient.smtp.SmtpConfig;
import com.zimbra.cs.mailclient.smtp.SmtpConnection;

/**
 * @author schemers
 */
public class JMSession {

    private static Session sSession;
    
    static {
        // Assume that most malformed base64 errors occur due to incorrect delimiters,
        // as opposed to errors in the data itself.  See bug 11213 for more details.
        System.setProperty("mail.mime.base64.ignoreerrors", "true");
        
        Properties props = new Properties();
        props.setProperty("mail.mime.address.strict", "false");
        sSession = Session.getInstance(props);
    }
    
    /**
     * Returns a new JavaMail session.
     */
    public synchronized static Session getSession() {
        return sSession;
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
    public static String getRandomSmtpHost(Domain domain)
    throws ServiceException {
        String[] hosts = getSmtpHostsFromLdap(domain);
        if (hosts.length == 0) {
            return null;
        }

        String host = null;
        if (hosts.length == 1) {
            host = hosts[0];
        } else {
            host = hosts[RANDOM.nextInt(hosts.length)];
        }
        if (!sBadSmtpHosts.containsKey(host)) {
            return host;
        }
        if (hosts.length == 1) {
            // No other hosts to try.
            return null;
        }
        
        // Current host is bad.  Find another one.
        List<String> hostList = new ArrayList<String>();
        Collections.addAll(hostList, hosts);
        Collections.shuffle(hostList);
        for (String currentHost : hostList) {
            if (!sBadSmtpHosts.containsKey(currentHost)) {
                return currentHost;
            }
        }
        return null;
    }
    
    /**
     * Returns a new set that contains all SMTP hosts, not including
     * hosts that were marked as bad with {@link #markSmtpHostBad}.
     * 
     * @param account the account, or <tt>null</tt> if the message is
     * not being sent from a mailbox
     */
    public static Set<String> getSmtpHosts(Account account)
    throws ServiceException {
        Domain domain = Provisioning.getInstance().getDomain(account);
        Set<String> hosts = new HashSet<String>();
        for (String host : getSmtpHostsFromLdap(domain)) {
            if (!sBadSmtpHosts.containsKey(host)) {
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
        ZimbraLog.smtp.info(
            "Disallowing connections to %s for %d milliseconds.", hostName, LC.smtp_host_retry_millis.intValue());
        sBadSmtpHosts.put(hostName, null);
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
    
    public static SmtpConfig getSmtpConfig()
    throws ServiceException {
        return getSmtpConfig(null, getRandomSmtpHost(null));
    }
    
    public static SmtpConfig getSmtpConfig(Account account, String host)
    throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Domain domain = null;
        if (account != null) {
            domain = prov.getDomain(account);
        }
        
        // Set host, port, etc.
        SmtpConfig config = new SmtpConfig(host);
        Server server = prov.getLocalServer();
        config.setPort(Integer.parseInt(getValue(server, domain, Provisioning.A_zimbraSmtpPort)));
        config.setAllowPartialSend(Boolean.parseBoolean(getValue(server, domain, Provisioning.A_zimbraSmtpSendPartial)));
        config.setDomain(LC.zimbra_server_hostname.value());
        
        // Set timeout.
        String sTimeout = getValue(server, domain, Provisioning.A_zimbraSmtpTimeout);
        int timeout = (sTimeout == null ? 60 : Integer.parseInt(sTimeout));
        config.setConnectTimeout(timeout);
        config.setReadTimeout(timeout);
        
        return config;
    }
    
    public static SmtpConnection getSmtpConnection()
    throws ServiceException {
        return new SmtpConnection(getSmtpConfig());
    }
}
