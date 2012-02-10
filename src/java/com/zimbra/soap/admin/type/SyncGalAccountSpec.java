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

package com.zimbra.soap.admin.type;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class SyncGalAccountSpec {

    /**
     * @zm-api-field-tag account-id
     * @zm-api-field-description Account ID
     */
    @XmlAttribute(name=AdminConstants.A_ID /* id */, required=true)
    private String id;

    /**
     * @zm-api-field-description SyncGalAccount data source specifications
     */
    @XmlElement(name=AdminConstants.E_DATASOURCE /* datasource */, required=false)
    private List<SyncGalAccountDataSourceSpec> dataSources = Lists.newArrayList();

    public SyncGalAccountSpec() {
    }

    private SyncGalAccountSpec(String id) {
        setId(id);
    }

    public static SyncGalAccountSpec createForId(String id) {
        return new SyncGalAccountSpec(id);
    }

    public void setId(String id) { this.id = id; }
    public void setDataSources(Iterable <SyncGalAccountDataSourceSpec> dataSources) {
        this.dataSources.clear();
        if (dataSources != null) {
            Iterables.addAll(this.dataSources,dataSources);
        }
    }

    public void addDataSource(SyncGalAccountDataSourceSpec dataSource) {
        this.dataSources.add(dataSource);
    }

    public String getId() { return id; }
    public List<SyncGalAccountDataSourceSpec> getDataSources() {
        return dataSources;
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("dataSources", dataSources);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
