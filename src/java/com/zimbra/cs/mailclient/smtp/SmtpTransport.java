/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.cs.mailclient.smtp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.sun.mail.util.PropUtil;
import com.zimbra.cs.util.BuildInfo;

/**
 * A custom SMTP {@link Transport} implementation using {@link SmtpConnection}.
 * <p>
 * <table border="1">
 *  <caption><b>Supported {@link Session} properties</b><caption>
 *  <tr><th>Name</th><th>Type</th><th>Description</th></tr>
 *  <tr><td>mail.smtp(s).auth</td><td>boolean</td>
 *      <td>If true, attempt to authenticate the user using the AUTH command.
 *      Defaults to false.</td></tr>
 *  <tr><td>mail.smtp(s).port</td><td>int</td>
 *      <td>The SMTP server port to connect to, if the connect() method doesn't
 *      explicitly specify one. Defaults to {@link SmtpConfig#DEFAULT_PORT} for
 *      SMTP, {@link SmtpConfig#DEFAULT_SSL_PORT} for SMTPS.</td></tr>
 *  <tr><td>mail.smtp(s).localhost</td><td>String</td>
 *      <td>Local host name used in the SMTP HELO or EHLO command.</td></tr>
 *  <tr><td>mail.smtp(s).sendpartial</td><td>boolean</td>
 *      <td>If set to true, and a message has some valid and some invalid
 *      addresses, send the message anyway, reporting the partial failure with
 *      a {@link SendFailedException}. If set to false (the default),
 *      the message is not sent to any of the recipients if there is an invalid
 *      recipient address.</td></tr>
 * </table>
 *
 * @author ysasaki
 */
public class SmtpTransport extends Transport {

    public static final Provider PROVIDER = new Provider(
            Provider.Type.TRANSPORT, "smtp", SmtpTransport.class.getName(),
            "Zimbra", BuildInfo.VERSION);

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
    protected boolean protocolConnect(String host, int port, String user,
            String passwd) throws MessagingException {

        boolean auth = PropUtil.getBooleanSessionProperty(session,
                "mail." + protocol + ".auth", false);
        if (auth && (user == null || passwd == null)) {
            return false;
        }

        if (port < 0) {
            port =  PropUtil.getIntSessionProperty(session,
                    "mail." + protocol + ".port",
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
        config.setDebug(session.getDebug());
        config.setSecurity(ssl ? SmtpConfig.Security.SSL : SmtpConfig.Security.NONE);
        config.setAllowPartialSend(PropUtil.getBooleanSessionProperty(session,
                "mail." + protocol + ".sendpartial", false));
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
                connection.login(passwd);
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

        try {
            connection.sendMessage((MimeMessage) msg, rcpts);
        } catch (MessagingException e) {
            notify(e, msg, rcpts);
        } catch (IOException e) {
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
            invalid.length > 0 ? TransportEvent.MESSAGE_PARTIALLY_DELIVERED :
                TransportEvent.MESSAGE_DELIVERED;

        notifyTransportListeners(notify, validSent, validUnsent, invalid, msg);
        switch (notify) {
            case TransportEvent.MESSAGE_NOT_DELIVERED:
                throw new SendFailedException("MESSAGE_NOT_DELIVERED", ex,
                        validSent, validUnsent, invalid);
            case TransportEvent.MESSAGE_PARTIALLY_DELIVERED:
                throw new SendFailedException("MESSAGE_PARTIALLY_DELIVERED", ex,
                        validSent, validUnsent, invalid);
        }
    }

}
