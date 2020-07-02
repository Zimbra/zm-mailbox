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

package com.zimbra.soap.admin.type;

import com.google.common.base.MoreObjects;
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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("dataSources", dataSources);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
