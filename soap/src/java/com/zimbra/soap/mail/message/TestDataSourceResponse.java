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

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.TestDataSource;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_TEST_DATA_SOURCE_RESPONSE)
public class TestDataSourceResponse {
    /**
     * @zm-api-field-description Data source information
     */
    @XmlElements({
        @XmlElement(name=MailConstants.E_DS_IMAP /* imap */, type=TestDataSource.class),
        @XmlElement(name=MailConstants.E_DS_POP3 /* pop3 */, type=TestDataSource.class),
        @XmlElement(name=MailConstants.E_DS_CALDAV /* caldav */, type=TestDataSource.class),
        @XmlElement(name=MailConstants.E_DS_YAB /* yab */, type=TestDataSource.class),
        @XmlElement(name=MailConstants.E_DS_RSS /* rss */, type=TestDataSource.class),
        @XmlElement(name=MailConstants.E_DS_GAL /* gal */, type=TestDataSource.class),
        @XmlElement(name=MailConstants.E_DS_CAL /* cal */, type=TestDataSource.class),
        @XmlElement(name=MailConstants.E_DS_UNKNOWN /* unknown */, type=TestDataSource.class)
    })
    private List<TestDataSource> dataSources = Lists.newArrayList();

    public void setDataSources(Iterable <TestDataSource> dataSources) {
        this.dataSources.clear();
        if (dataSources != null) {
            Iterables.addAll(this.dataSources,dataSources);
        }
    }

    private TestDataSourceResponse() {
    }

    public TestDataSourceResponse addDataSource(TestDataSource dataSource) {
        this.dataSources.add(dataSource);
        return this;
    }

    public List<TestDataSource> getDataSources() {
        return Collections.unmodifiableList(dataSources);
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("dataSources", dataSources);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
