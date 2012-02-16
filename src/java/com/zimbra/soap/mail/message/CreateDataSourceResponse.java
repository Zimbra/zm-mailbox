/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.CalDataSourceId;
import com.zimbra.soap.mail.type.CaldavDataSourceId;
import com.zimbra.soap.mail.type.GalDataSourceId;
import com.zimbra.soap.mail.type.ImapDataSourceId;
import com.zimbra.soap.mail.type.Pop3DataSourceId;
import com.zimbra.soap.mail.type.RssDataSourceId;
import com.zimbra.soap.mail.type.UnknownDataSourceId;
import com.zimbra.soap.mail.type.YabDataSourceId;
import com.zimbra.soap.type.Id;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_CREATE_DATA_SOURCE_RESPONSE)
@XmlType(propOrder = {})
public class CreateDataSourceResponse {

    /**
     * @zm-api-field-description ID information for the created data source
     */
    @XmlElements({
        @XmlElement(name=MailConstants.E_DS_IMAP /* imap */, type=ImapDataSourceId.class),
        @XmlElement(name=MailConstants.E_DS_POP3 /* pop3 */, type=Pop3DataSourceId.class),
        @XmlElement(name=MailConstants.E_DS_CALDAV /* caldav */, type=CaldavDataSourceId.class),
        @XmlElement(name=MailConstants.E_DS_YAB /* yab */, type=YabDataSourceId.class),
        @XmlElement(name=MailConstants.E_DS_RSS /* rss */, type=RssDataSourceId.class),
        @XmlElement(name=MailConstants.E_DS_GAL /* gal */, type=GalDataSourceId.class),
        @XmlElement(name=MailConstants.E_DS_CAL /* cal */, type=CalDataSourceId.class),
        @XmlElement(name=MailConstants.E_DS_UNKNOWN /* unknown */, type=UnknownDataSourceId.class)
    })
    private Id dataSource;

    public CreateDataSourceResponse() {
    }

    public void setDataSource(Id dataSource) { this.dataSource = dataSource; }
    public Id getDataSource() { return dataSource; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("dataSource", dataSource);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
