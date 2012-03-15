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

package com.zimbra.soap.admin.message;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DataSourceInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_DATA_SOURCES_RESPONSE)
public class GetDataSourcesResponse {

    /**
     * @zm-api-field-description Information on data sources
     */
    @XmlElement(name=AccountConstants.E_DATA_SOURCE, required=false)
    private List<DataSourceInfo> dataSources = Lists.newArrayList();

    public GetDataSourcesResponse() {
    }

    public void setDataSources(Iterable <DataSourceInfo> dataSources) {
        this.dataSources.clear();
        if (dataSources != null) {
            Iterables.addAll(this.dataSources,dataSources);
        }
    }

    public GetDataSourcesResponse addDataSource(DataSourceInfo dataSource) {
        this.dataSources.add(dataSource);
        return this;
    }

    public List<DataSourceInfo> getDataSources() {
        return Collections.unmodifiableList(dataSources);
    }
}
