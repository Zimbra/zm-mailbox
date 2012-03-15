/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.MailCalDataSource;
import com.zimbra.soap.mail.type.MailCaldavDataSource;
import com.zimbra.soap.mail.type.MailGalDataSource;
import com.zimbra.soap.mail.type.MailImapDataSource;
import com.zimbra.soap.mail.type.MailPop3DataSource;
import com.zimbra.soap.mail.type.MailRssDataSource;
import com.zimbra.soap.mail.type.MailUnknownDataSource;
import com.zimbra.soap.mail.type.MailYabDataSource;
import com.zimbra.soap.type.DataSource;

/**
 * @zm-api-command-description Changes attributes of the given data source.  Only the attributes specified in the
 * request are modified.  If the username, host or leaveOnServer settings are modified, the server wipes out saved
 * state for this data source.  As a result, any previously downloaded messages that are still stored on the remote
 * server will be downloaded again.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_MODIFY_DATA_SOURCE_REQUEST)
public class ModifyDataSourceRequest {

    /**
     * @zm-api-field-description Specification of data source changes
     */
    @XmlElements({
        @XmlElement(name=MailConstants.E_DS_IMAP /* imap */, type=MailImapDataSource.class),
        @XmlElement(name=MailConstants.E_DS_POP3 /* pop3 */, type=MailPop3DataSource.class),
        @XmlElement(name=MailConstants.E_DS_CALDAV /* caldav */, type=MailCaldavDataSource.class),
        @XmlElement(name=MailConstants.E_DS_YAB /* yab */, type=MailYabDataSource.class),
        @XmlElement(name=MailConstants.E_DS_RSS /* rss */, type=MailRssDataSource.class),
        @XmlElement(name=MailConstants.E_DS_GAL /* gal */, type=MailGalDataSource.class),
        @XmlElement(name=MailConstants.E_DS_CAL /* cal */, type=MailCalDataSource.class),
        @XmlElement(name=MailConstants.E_DS_UNKNOWN /* unknown */, type=MailUnknownDataSource.class)
    })
    private DataSource dataSource;

    public ModifyDataSourceRequest() {
    }

    public void setDataSource(DataSource dataSource) { this.dataSource = dataSource; }
    public DataSource getDataSource() { return dataSource; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("dataSource", dataSource);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
