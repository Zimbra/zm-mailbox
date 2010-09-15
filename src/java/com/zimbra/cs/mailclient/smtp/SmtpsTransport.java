/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
