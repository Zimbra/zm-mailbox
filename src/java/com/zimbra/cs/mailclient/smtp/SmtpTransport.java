/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailclient.smtp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.mail.Address;
import javax.mail.AuthenticationFailedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Provider;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.event.TransportEvent;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.security.auth.login.LoginException;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.sun.mail.smtp.SMTPMessage;
import com.sun.mail.util.PropUtil;
import com.zimbra.common.account.ZAttrProvisioning.DataSourceAuthMechanism;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailclient.auth.SaslAuthenticator;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.JMSession;

/**
 * A custom SMTP {@link Transport} implementation using {@link SmtpConnection}.
 * <p>
 * <table border="1">
 *  <caption><b>Supported {@link Session} properties</b><caption>
 *  <tr><th>Name</th><th>Type</th><th>Description</th></tr>
 *  <tr>
 *   <td>mail.smtp[s].auth</td><td>boolean</td>
 *   <td>If true, attempt to authenticate the user using the AUTH command.
 *   Defaults to false.</td>
 *  </tr>
 *  <tr>
 *   <td>mail.smtp[s].from</td><td>String</td>
 *   <td>Email address to use for SMTP MAIL command. This sets the envelope
 *   return address. Defaults to {@link MimeMessage#getFrom()}.
 *   </td>
 *  </tr>
 *  <tr>
 *   <td>mail.smtp[s].localhost</td><td>String</td>
 *   <td>Local host name used in the SMTP HELO or EHLO command.</td>
 *  </tr>
 *  <tr>
 *   <td>mail.smtp[s].sendpartial</td><td>boolean</td>
 *   <td>If set to true, and a message has some valid and some invalid addresses,
 *   send the message anyway, reporting the partial failure with a
 *   {@link SendFailedException}. If set to false (the default), the message is
 *   not sent to any of the recipients if there is an invalid recipient address.</td>
 *  </tr>
 *  <tr>
 *   <td>mail.smtp[s].socketFactory</td><td>SocketFactory</td>
 *   <td>If set to a class that implements {@link SocketFactory}, this class
 *   will be used to create SMTP sockets. Note that this is an instance of a
 *   class, not a name, and must be set using {@link Properties#put(Object, Object)},
 *   not {@link Properties#setProperty(String, String)}.</td></tr>
 *  <tr>
 *   <td>mail.smtp[s].ssl.socketFactory</td><td>SSLSocketFactory</td>
 *   <td>If set to a class that extends the {@link SSLSocketFactory}, this class
 *   will be used to create SMTP SSL sockets. Note that this is an instance of a
 *   class, not a name, and must be set using {@link Properties#put(Object, Object)},
 *   not {@link Properties#setProperty(String, String)}.</td>
 *  </tr>
 *  <tr>
 *   <td>mail.smtp[s].connectiontimeout</td><td>int</td>
 *   <td>Socket connection timeout value in milliseconds.
 *   Default is infinite timeout.</td>
 *  </tr>
 *  <tr>
 *   <td>mail.smtp[s].timeout</td><td>int</td>
 *   <td>Socket I/O timeout value in milliseconds.
 *   Default is infinite timeout.</td>
 *  </tr>
 * </table>
 *
 * @author ysasaki
 */
public class SmtpTransport extends Transport {

    public static final Provider PROVIDER = new Provider(
            Provider.Type.TRANSPORT, "smtp", SmtpTransport.class.getName(), "Zimbra", BuildInfo.VERSION);

    private SmtpConnection connection;
    private final boolean ssl;
    private final String protocol;

    /**
     * Constructs a new {@link SmtpTransport}.
     *
     * @param session JavaMail session
     * @param urlname URL name
     */
    public SmtpTransport(Session session, URLName urlname) {
        this(session, urlname, false);
    }

    /**
     * Constructs a new {@link SmtpTransport}.
     *
     * @param session JavaMail session
     * @param urlname URL name
     * @param ssl true to enable SSL, otherwise false
     */
    SmtpTransport(Session session, URLName urlname, boolean ssl) {
        super(session, urlname);
        this.ssl = ssl;
        protocol = ssl ? "smtps" : "smtp";
    }

    @Override
    protected boolean protocolConnect(String host, int port, String user, String passwd) throws MessagingException {

        boolean auth = PropUtil.getBooleanSessionProperty(session, "mail." + protocol + ".auth", false);
        String authMechanism = session.getProperty("mail." + protocol + ".sasl.mechanisms");
        if (authMechanism != null && SaslAuthenticator.XOAUTH2.equalsIgnoreCase(authMechanism)) {
            passwd = session.getProperty("mail." + protocol + ".sasl.mechanisms.oauth2.oauthToken");
        }

        if (auth && (user == null || passwd == null)) {
            return false;
        }

        if (port < 0) {
            port =  PropUtil.getIntSessionProperty(session, "mail." + protocol + ".port",
                    ssl ? SmtpConfig.DEFAULT_SSL_PORT : SmtpConfig.DEFAULT_PORT);
        }

        // mail.protocol.host is examined in Service class

        if (Strings.isNullOrEmpty(host)) {
            host = "localhost";
        }

        SmtpConfig config = new SmtpConfig();
        config.setHost(host);
        config.setPort(port);
        config.setDomain(session.getProperty("mail." + protocol + ".localhost"));
        config.setSecurity(ssl ? SmtpConfig.Security.SSL : 
            PropUtil.getBooleanSessionProperty(session, "mail.smtp.starttls.enable", false) ? SmtpConfig.Security.TLS_IF_AVAILABLE : SmtpConfig.Security.NONE);
        config.setAllowPartialSend(PropUtil.getBooleanSessionProperty(session,
                "mail." + protocol + ".sendpartial", false));
        config.setConnectTimeout(PropUtil.getIntSessionProperty(session,
                "mail." + protocol + ".connectiontimeout", 0) / 1000); // msec to sec
        config.setReadTimeout(PropUtil.getIntSessionProperty(session,
                "mail." + protocol + ".timeout", 0) / 1000); // msec to sec
        config.setDsn(session.getProperty("mail." + protocol + ".dsn.notify"));

        Properties props = session.getProperties();
        Object socketFactory = props.get("mail." + protocol + ".socketFactory");
        if (socketFactory instanceof SocketFactory) {
            config.setSocketFactory((SocketFactory) socketFactory);
        }
        Object sslSocketFactory = props.get("mail." + protocol + ".ssl.socketFactory");
        if (sslSocketFactory instanceof SSLSocketFactory) {
            config.setSSLSocketFactory((SSLSocketFactory) sslSocketFactory);
        }

        if (authMechanism != null && SaslAuthenticator.XOAUTH2.equalsIgnoreCase(authMechanism)) {
            config.setMechanism(SaslAuthenticator.XOAUTH2);
            HashMap<String, String> map = new HashMap<String, String>();
            JMSession.addOAuth2Properties(passwd, map, config.getProtocol());
            config.setSaslProperties(map);
        }

        if (user != null && passwd != null) {
            config.setAuthenticationId(user);
        }

        connection = new SmtpConnection(config);
        try {
            connection.connect();
        } catch (IOException e) {
            throw new MessagingException(e.getMessage(), e);
        }
        if (auth || (user != null && passwd != null)) {
            try {
                connection.authenticate(passwd);
            } catch (LoginException e) {
                throw new AuthenticationFailedException(e.getMessage());
            } catch (IOException e) {
                throw new AuthenticationFailedException(e.getMessage());
            }
        }
        return true;
    }

    /**
     * Sends the message to the recipients. This implementation immediately
     * closes the SMTP connection after sending a message, which might be
     * incompatible with JavaMail.
     *
     * @param msg message to send
     * @param rcpts recipients, may be different from ones in the MIME header
     */
    @Override
    public void sendMessage(Message msg, Address[] rcpts) throws MessagingException {
        Preconditions.checkArgument(msg instanceof MimeMessage);
        Preconditions.checkState(connection != null);

        String sender = session.getProperty("mail." + protocol + ".from");
        if (msg instanceof SMTPMessage) {
            SMTPMessage smtpMsg = (SMTPMessage) msg;
            if (smtpMsg.getEnvelopeFrom() != null) {
                sender = smtpMsg.getEnvelopeFrom();
            }
        }
        try {
            if (sender != null) {
                connection.sendMessage(sender, rcpts, (MimeMessage) msg);
            } else {
                connection.sendMessage(rcpts, (MimeMessage) msg);
            }
        } catch (MessagingException e) {
            ZimbraLog.smtp.warn("Failed to send message", e);
            notify(e, msg, rcpts);
        } catch (IOException e) {
            ZimbraLog.smtp.warn("Failed to send message", e);
            notify(e, msg, rcpts);
        }
        notify(null, msg, rcpts);
    }

    @Override
    public void close() throws MessagingException {
        if (connection != null) {
            connection.close();
        }
        if (isConnected()) {
            super.close();
        }
    }

    /**
     * Sends {@code MAIL FROM} command to the server.
     *
     * @param from sender address
     * @throws MessagingException error
     */
    public void mail(String from) throws MessagingException {
        try {
            connection.mail(from);
        } catch (IOException e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    /**
     * Sends {@code RSET} command to the server.
     *
     * @throws MessagingException error
     */
    public void rset() throws MessagingException {
        try {
            connection.rset();
        } catch (IOException e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    private void notify(Exception ex, Message msg, Address[] addrs) throws SendFailedException {
        Set<String> validRcpts = connection.getValidRecipients();
        Set<String> invalidRcpts = connection.getInvalidRecipients();

        List<Address> validAddrs = new ArrayList<Address>(validRcpts.size());
        List<Address> invalidAddrs = new ArrayList<Address>(invalidRcpts.size());
        for (Address addr : addrs) {
            if (addr instanceof InternetAddress) {
                InternetAddress iaddr = (InternetAddress) addr;
                if (validRcpts.contains(iaddr.getAddress())) {
                    validAddrs.add(iaddr);
                } else if (invalidRcpts.contains(iaddr.getAddress())) {
                    invalidAddrs.add(iaddr);
                }
            }
        }
        Address[] validSent = Iterables.toArray(validAddrs, Address.class);
        Address[] validUnsent = new Address[0];
        Address[] invalid = Iterables.toArray(invalidAddrs, Address.class);

        int notify = ex != null ? TransportEvent.MESSAGE_NOT_DELIVERED :
            invalid.length > 0 ? TransportEvent.MESSAGE_PARTIALLY_DELIVERED : TransportEvent.MESSAGE_DELIVERED;

        notifyTransportListeners(notify, validSent, validUnsent, invalid, msg);
        switch (notify) {
            case TransportEvent.MESSAGE_NOT_DELIVERED:
                throw new SendFailedException("MESSAGE_NOT_DELIVERED", ex, validSent, validUnsent, invalid);
            case TransportEvent.MESSAGE_PARTIALLY_DELIVERED:
                throw new SendFailedException("MESSAGE_PARTIALLY_DELIVERED", ex, validSent, validUnsent, invalid);
        }
    }

}
