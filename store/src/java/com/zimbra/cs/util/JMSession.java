/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.util;

import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

import com.google.common.base.Joiner;
import com.zimbra.common.account.ZAttrProvisioning.DataSourceAuthMechanism;
import com.zimbra.common.account.ZAttrProvisioning.ShareNotificationMtaConnectionType;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.net.SocketFactories;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.TimeoutMap;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailclient.auth.OAuth2Provider;
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

        try {
            Security.addProvider(new OAuth2Provider(Provisioning.getInstance().getLocalServer()
                .getServerVersionMajor()));
        } catch (ServiceException e) {
            ZimbraLog.smtp.warn("Exception in getting zimbra server version", e);
            Security.addProvider(new OAuth2Provider(1));
        }

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

    public static Session getSession(DataSource ds) throws ServiceException {
        String smtpHost = ds.getSmtpHost();
        int smtpPort = ds.getSmtpPort();
        boolean isAuthRequired = ds.isSmtpAuthRequired();
        String smtpUser = ds.getSmtpUsername();
        String smtpPass = ds.getDecryptedSmtpPassword();
        if (DataSourceAuthMechanism.XOAUTH2.name().equalsIgnoreCase(ds.getAuthMechanism())) {
            smtpPass = ds.getDecryptedOAuthToken();
        }

        boolean useSSL = ds.isSmtpConnectionSecure();

        if (smtpHost == null || smtpHost.length() == 0) {
            throw ServiceException.FAILURE("null smtp host", null);
        }
        if (smtpPort <= 0) {
            throw ServiceException.FAILURE("invalid smtp port", null);
        }
        if (isAuthRequired && (smtpUser == null || smtpUser.length() == 0 || smtpPass == null || smtpPass.length() == 0)) {
            throw ServiceException.FAILURE("missing smtp username or password", null);
        }

        long timeout = LC.javamail_smtp_timeout.longValue() * Constants.MILLIS_PER_SECOND;
        String localhost = LC.zimbra_server_hostname.value();

        Properties props = new Properties();
        Session session;

        props.put("mail.smtp.socketFactory", SocketFactories.defaultSocketFactory());
        props.setProperty("mail.smtp.socketFactory.fallback", "false");
        props.put("mail.smtp.ssl.socketFactory", SocketFactories.defaultSSLSocketFactory());
        props.setProperty("mail.smtp.ssl.socketFactory.fallback", "false");
        props.put("mail.smtps.ssl.socketFactory", SocketFactories.defaultSSLSocketFactory());
        props.setProperty("mail.smtps.ssl.socketFactory.fallback", "false");

        if (useSSL) {
            props.setProperty("mail.transport.protocol", "smtps");
            props.setProperty("mail.smtps.connectiontimeout", Long.toString(timeout));
            props.setProperty("mail.smtps.timeout", Long.toString(timeout));
            props.setProperty("mail.smtps.localhost", localhost);
            props.setProperty("mail.smtps.sendpartial", "true");
            props.setProperty("mail.smtps.host", smtpHost);
            props.setProperty("mail.smtps.port",  smtpPort + "");
            if (isAuthRequired) {
                props.setProperty("mail.smtps.auth", "true");
                props.setProperty("mail.smtps.user", smtpUser);
                props.setProperty("mail.smtps.password", smtpPass);
                if (DataSourceAuthMechanism.XOAUTH2.name().equalsIgnoreCase(ds.getAuthMechanism())) {
                    addOAuth2Properties(smtpPass, props, "smtps");
                }
                session = Session.getInstance(props, new SmtpAuthenticator(smtpUser, smtpPass));
            } else {
                session = Session.getInstance(props);
            }
            session.setProtocolForAddress("rfc822", "smtps");
        } else {
            props.setProperty("mail.transport.protocol", "smtp");
            props.setProperty("mail.smtp.connectiontimeout", Long.toString(timeout));
            props.setProperty("mail.smtp.timeout", Long.toString(timeout));
            props.setProperty("mail.smtp.localhost", localhost);
            props.setProperty("mail.smtp.sendpartial", "true");
            props.setProperty("mail.smtp.host", smtpHost);
            props.setProperty("mail.smtp.port",  smtpPort + "");
            if (LC.javamail_smtp_enable_starttls.booleanValue()) {
                props.setProperty("mail.smtp.starttls.enable","true");
                // props.put("mail.smtp.socketFactory.class", TlsSocketFactory.getInstance());
            }
            if (isAuthRequired) {
                props.setProperty("mail.smtp.auth", "true");
                props.setProperty("mail.smtp.user", smtpUser);
                props.setProperty("mail.smtp.password", smtpPass);
                if (DataSourceAuthMechanism.XOAUTH2.name().equalsIgnoreCase(ds.getAuthMechanism())) {
                    addOAuth2Properties(smtpPass, props, "smtp");
                }
                session = Session.getInstance(props, new SmtpAuthenticator(smtpUser, smtpPass));
            } else {
                session = Session.getInstance(props);
            }
            session.setProtocolForAddress("rfc822", "smtp");
        }

        if (LC.javamail_smtp_debug.booleanValue()) {
            session.setDebug(true);
        }
        JMSession.setProviders(session);
        return session;
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
        try {
            server = Provisioning.getInstance().getLocalServer();
        } catch (ServiceException e) {
            throw new MessagingException("Unable to initialize JavaMail session", e);
        }

        Properties props = getJavaMailSessionProperties(server, domain);
        configureStartTls(props, server, domain);
        Session session = Session.getInstance(props);
        setProviders(session);
        if (LC.javamail_smtp_debug.booleanValue()) {
            session.setDebug(true);
        }
        return session;
    }


    /**
     * Returns a new JavaMail {@link Session} that is configured to connect to
     * relay MTA.
     */
    public static Session getRelaySession() throws MessagingException {
        Provisioning prov = Provisioning.getInstance();
        Server server;
        String relayHost;
        int relayPort;
        boolean useSmtpAuth;
        boolean useTls;

        try {
            server = prov.getLocalServer();
            relayHost = server.getShareNotificationMtaHostname();
            relayPort = server.getShareNotificationMtaPort();
            useSmtpAuth = server.isShareNotificationMtaAuthRequired();
            useTls = server.getShareNotificationMtaConnectionType() == ShareNotificationMtaConnectionType.STARTTLS;
        } catch (ServiceException e) {
            throw new MessagingException("Unable to identify local server", e);
        }
        if (relayHost == null || relayPort == 0) {
            return getSmtpSession();
        }

        Properties props = getJavaMailSessionProperties(server, null);
        props.setProperty("mail.smtp.host", relayHost);
        props.setProperty("mail.smtp.port", "" + relayPort);
        Authenticator auth = null;

        if (useSmtpAuth) {
            String account = server.getShareNotificationMtaAuthAccount();
            String password = server.getShareNotificationMtaAuthPassword();
            if (account == null || password == null) {
                ZimbraLog.smtp.warn(Provisioning.A_zimbraShareNotificationMtaAuthRequired + " is enabled but account or password is unset");
            } else {
                props.setProperty("mail.smtp.auth", "" + useSmtpAuth);
                props.setProperty("mail.smtp.sasl.enable", "" + useSmtpAuth);
                auth = new SmtpAuthenticator(account, password);
            }
        }

        if (useTls) {
            props.setProperty("mail.smtp.starttls.enable", "" + useTls);
        }

        Session session = (auth == null) ? Session.getInstance(props) : Session.getInstance(props, auth);
        setProviders(session);
        if (LC.javamail_smtp_debug.booleanValue()) {
            session.setDebug(true);
        }
        return session;
    }

    private static class SmtpAuthenticator extends Authenticator {
        private final String username;
        private final String password;
        public SmtpAuthenticator(String username, String password) {
            this.username = username;  this.password = password;
        }
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password);
        }
    }

    private static Properties getJavaMailSessionProperties(Server server, Domain domain) throws MessagingException {
        String smtpHost;

        try {
            smtpHost = getRandomSmtpHost(domain);
        } catch (ServiceException e) {
            throw new MessagingException("Unable to initialize JavaMail session", e);
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

        // indirectly hack up the Message-ID value
        if (domain != null) {
            props.setProperty("mail.host", domain.getName());
        }

        return props;
    }

    private static Properties configureStartTls (Properties props, Server server, Domain domain) {
        String startTlsMode = getValue(server, domain, Provisioning.A_zimbraSmtpStartTlsMode);
        String sslTrustedHosts = getValue(server, domain, Provisioning.A_zimbraSmtpStartTlsTrustedHosts);

        if (startTlsMode != null) {
            if (startTlsMode.equals("off")) {
                props.setProperty("mail.smtp.starttls.enable", "false");
            } else if (startTlsMode.equals("only")) {
                props.setProperty("mail.smtp.starttls.enable", "true");
                props.setProperty("mail.smtp.starttls.required", "true");
            } else {
                if (!startTlsMode.equals("on")) {
                    ZimbraLog.smtp.warn("Invalid value for %s. Defaulting to 'on'.", Provisioning.A_zimbraSmtpStartTlsMode);
                }
                props.setProperty("mail.smtp.starttls.enable", "true");
                props.setProperty("mail.smtp.starttls.required", "false");
            }
        }
        if (sslTrustedHosts != null) {
            props.setProperty("mail.smtp.ssl.trust", sslTrustedHosts);
        }

        return props;
    }
    
    /**
     * Add OAuth2 properties to a JAVA Mail Session
     * @param oauthToken
     * @param props
     */
    public static void addOAuth2Properties(String oauthToken, Properties props, String protocol) {
        Map<String, String> map = new HashMap<String, String>();
        addOAuth2Properties(oauthToken, map, protocol);
        props.putAll(map);
    }

    public static void addOAuth2Properties(String oauthToken, Map<String, String> map,
        String protocol) {
        map.put("mail." + protocol + ".ssl.enable", "true");
        map.put("mail." + protocol + ".sasl.enable", "true");
        map.put("mail." + protocol + ".sasl.mechanisms", "XOAUTH2");
        map.put("mail." + protocol + ".auth.login.disable", "true");
        map.put("mail." + protocol + ".auth.plain.disable", "true");
        map.put("mail." + protocol + ".sasl.mechanisms.oauth2.oauthToken", oauthToken);
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
     * @param domain the domain, or <tt>null</tt> to use server settings
     */
    private static String getRandomSmtpHost(Domain domain) throws ServiceException {
        List<String> hostList = getSmtpHosts(domain);
        if (hostList.size() > 0) {
            return hostList.get(0);
        }
        return null;
    }

    private static boolean isHostBad(String hostname) {
        if (hostname != null) {
            hostname = hostname.toLowerCase();
        }
        boolean isBad = sBadSmtpHosts.containsKey(hostname);
        if (isBad) {
            ZimbraLog.smtp.debug("List of bad SMTP hosts contains '%s'", hostname);
        }
        return isBad;
    }

    /**
     * Returns a new set that contains all SMTP hosts, not including
     * hosts that were marked as bad with {@link #markSmtpHostBad}.
     */
    public static List<String> getSmtpHosts(Domain domain) throws ServiceException {
        List<String> hosts = new ArrayList<String>();
        for (String host : lookupSmtpHosts(domain)) {
            if (!isHostBad(host)) {
                hosts.add(host);
            }
        }
        Collections.shuffle(hosts);
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
    private static String[] lookupSmtpHosts(Domain domain) throws ServiceException {
        String[] hosts = NO_HOSTS;
        if (domain != null) {
            hosts = domain.getSmtpHostname();
        }
        if (hosts.length > 0) {
            if (ZimbraLog.smtp.isDebugEnabled()) {
                ZimbraLog.smtp.debug("lookupSmtpHosts domain=%s has %s SMTP hostnames configured - %s",
                        domain == null ? "<null>" : domain.getName(), hosts.length, Joiner.on(',').join(hosts));
            }
            return hosts;
        }
        Server server = Provisioning.getInstance().getLocalServer();
        hosts = server.getSmtpHostname();
        if (ZimbraLog.smtp.isDebugEnabled()) {
            ZimbraLog.smtp.debug("lookupSmtpHosts domain=%s has %s SMTP hostnames configured - %s. [via server %s]",
                        domain == null ? "<null>" : domain.getName(), hosts.length, Joiner.on(',').join(hosts),
                        server.getName());
        }
        return hosts;
    }
}
