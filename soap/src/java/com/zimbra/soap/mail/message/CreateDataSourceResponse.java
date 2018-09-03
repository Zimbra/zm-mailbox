/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.mail.message;

import com.google.common.base.MoreObjects;

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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("dataSource", dataSource);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
