/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import com.google.common.base.MoreObjects;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailclient.MailConfig;

/**
 * SMTP client configuration.
 */
public final class SmtpConfig extends MailConfig {
    public static final String PROTOCOL = "smtp";
    public static final int DEFAULT_PORT = 25;
    public static final int DEFAULT_SSL_PORT = 465;

    private String domain;
    private boolean allowPartialSend;
    private String dsn;

    public SmtpConfig(String host, int port, String domain) {
        super(ZimbraLog.smtp, host);
        setPort(port);
        setDomain(domain);
    }

    public SmtpConfig(String host) {
        super(ZimbraLog.smtp, host);
        setPort(DEFAULT_PORT);
    }

    public SmtpConfig() {
        super(ZimbraLog.smtp);
    }

    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return MoreObjects.firstNonNull(domain, "localhost");
    }

    public void setAllowPartialSend(boolean allow) {
        this.allowPartialSend = allow;
    }

    public boolean isPartialSendAllowed() {
        return allowPartialSend;
    }

    public void setDsn(String dsn) {
        this.dsn = dsn;
    }

    public String getDsn() {
        return dsn;
    }

    @Override
    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        helper
            .add("domain", domain)
            .add("allowPartialSend", allowPartialSend);
        if (null != dsn) {
            helper.add("dsn", dsn);
        }
        return helper;
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
