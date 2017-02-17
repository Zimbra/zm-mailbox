/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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

import javax.mail.Provider;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;

import com.zimbra.cs.util.BuildInfo;

/**
 * A custom SMTPS (SMTP over SSL) {@link Transport} implementation using
 * {@link SmtpConnection}.
 *
 * @see SmtpTransport
 * @author ysasaki
 */
public final class SmtpsTransport extends SmtpTransport {

    public static final Provider PROVIDER = new Provider(
            Provider.Type.TRANSPORT, "smtps", SmtpsTransport.class.getName(),
            "Zimbra", BuildInfo.VERSION);

    public SmtpsTransport(Session session, URLName urlname) {
        super(session, urlname, true);
    }

}
