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

package com.zimbra.soap.mail.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.collect.Iterables;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.MailImapDataSource;
import com.zimbra.soap.mail.type.MailPop3DataSource;
import com.zimbra.soap.type.DataSource;

@XmlRootElement(name="GetDataSourcesResponse")
@XmlType(propOrder = {})
public class GetDataSourcesResponse {
    
    @XmlElements({
        @XmlElement(name=MailConstants.E_DS_POP3, type=MailPop3DataSource.class),
        @XmlElement(name=MailConstants.E_DS_IMAP, type=MailImapDataSource.class)
    })
    private List<DataSource> dataSources = new ArrayList<DataSource>();
    
    public List<DataSource> getDataSources() { return Collections.unmodifiableList(dataSources); }
    
    public void setDataSources(Iterable<DataSource> dataSources) {
        this.dataSources.clear();
        if (dataSources != null) {
            Iterables.addAll(this.dataSources, dataSources);
        }
    }

    /*
    @SuppressWarnings("unchecked")
    public List<DataSource> getAllDataSources() {
        List<DataSource> list = new ArrayList<DataSource>();
        list.addAll(pop3DataSources);
        return list;
    }
    */
}
