/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAttribute;

import com.google.common.base.Objects;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ImapDataSource;

public class MailImapDataSource
extends MailDataSource
implements ImapDataSource {
    
    /**
     * @zm-api-field-tag data-source-oauthToken
     * @zm-api-field-description oauthToken for data source
     */
    @XmlAttribute(name=MailConstants.A_DS_OAUTH_TOKEN /* oauthToken */, required=false)
    private String oauthToken;

    public MailImapDataSource() {
    }

    public MailImapDataSource(ImapDataSource data) {
        super(data);
        setOAuthToken(((MailImapDataSource)data).getOAuthToken());
    }
    
    public void setOAuthToken(String oauthToken) { this.oauthToken = oauthToken; }
    public String getOAuthToken() { return oauthToken; }

    @Override
    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("oauthToken", oauthToken);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
