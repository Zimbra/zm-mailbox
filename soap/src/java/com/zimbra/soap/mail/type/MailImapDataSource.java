/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAttribute;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ImapDataSource;
import com.zimbra.soap.type.ZmBoolean;

public class MailImapDataSource
extends MailDataSource
implements ImapDataSource {
    
    /**
     * @zm-api-field-tag data-source-oauthToken
     * @zm-api-field-description oauthToken for data source
     */
    @XmlAttribute(name=MailConstants.A_DS_OAUTH_TOKEN /* oauthToken */, required=false)
    private String oauthToken;

    /**
     * @zm-api-field-tag data-source-clientId
     * @zm-api-field-description client Id for refreshing data source oauth token
     */
    @XmlAttribute(name = MailConstants.A_DS_CLIENT_ID /* clientId */, required = false)
    private String clientId;

    /**
     * @zm-api-field-tag data-source-clientSecret
     * @zm-api-field-description client secret for refreshing data source oauth token
     */
    @XmlAttribute(name = MailConstants.A_DS_CLIENT_SECRET /* clientSecret */, required = false)
    private String clientSecret;

    /**
     * @zm-api-field-tag test-data-source
     * @zm-api-field-description boolean field for client to denote if it wants
     *                           to test the data source before creating
     */
    @XmlAttribute(name = MailConstants.A_DS_TEST , required = false)
    private ZmBoolean test;

    public MailImapDataSource() {
    }

    public MailImapDataSource(ImapDataSource data) {
        super(data);
        setOAuthToken(((MailImapDataSource)data).getOAuthToken());
        setClientId(((MailImapDataSource)data).getClientId());
        setClientSecret(((MailImapDataSource)data).getClientSecret());
    }

    public void setOAuthToken(String oauthToken) { this.oauthToken = oauthToken; }
    public String getOAuthToken() { return oauthToken; }

    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientId() { return clientId; }

    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public String getClientSecret() { return clientSecret; }

    public void setTest(boolean test) { this.test = ZmBoolean.fromBool(test, false); }
    public boolean isTest() { return ZmBoolean.toBool(test, false); }

    @Override
    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("oauthToken", oauthToken)
            .add("clientId", clientId)
            .add("clientSecret", clientSecret)
            .add("refreshToken", this.getRefreshToken())
            .add("refreshTokenUrl", this.getRefreshTokenUrl());
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
