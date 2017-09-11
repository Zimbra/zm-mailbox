/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.client;

import java.util.List;

import com.zimbra.soap.account.type.AccountDataSource;
import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.soap.mail.type.DataSourceNameOrId;
import com.zimbra.soap.type.DataSource;
import com.zimbra.soap.type.DataSources;

public class ZDataSource  {
    private AccountDataSource data;

    public ZDataSource() {
    }

    public ZDataSource(String name) {
        data = DataSources.newDataSource();
        data.setName(name);
    }

    public ZDataSource(String name, String importClass) {
        data = DataSources.newDataSource();
        data.setImportClass(importClass);
        data.setName(name);
    }

    public ZDataSource(String name, String importClass, Iterable<String> attributes) {
        data = DataSources.newDataSource();
        data.setImportClass(importClass);
        data.setAttributes(attributes);
        data.setName(name);
    }

    public ZDataSource(DataSource data) {
        this.data = DataSources.newDataSource(data);
    }

    public DataSourceType getType() {
        return DataSourceType.custom;
    }
    public String getName() {
        return data.getName();
    }
    public String getId() {
        return data.getId();
    }
    public String getImportClass() {
        return data.getImportClass();
    }
    public List<String> getAttributes() {
        return data.getAttributes();
    }
    public DataSource toJaxb() {
        AccountDataSource jaxbObject = new AccountDataSource();
        jaxbObject.setId(data.getId());
        jaxbObject.setName(data.getName());
        jaxbObject.setHost(data.getHost());
        jaxbObject.setPort(data.getPort());
        jaxbObject.setUsername(data.getUsername());
        jaxbObject.setPassword(data.getPassword());
        jaxbObject.setFolderId(data.getFolderId());
        jaxbObject.setConnectionType(data.getConnectionType());
        jaxbObject.setImportOnly(data.isImportOnly());
        return jaxbObject;
    }

    public DataSourceNameOrId toJaxbNameOrId() {
        DataSourceNameOrId jaxbObject = DataSourceNameOrId.createForId(data.getId());
        return jaxbObject;
    }

    public ZDataSource setAttributes(Iterable<String> attrs) {
        if(data != null) {
            data.setAttributes(attrs);
        }
        return this;
    }

    public ZDataSource setRefreshToken(String val) {
        if(data != null) {
            data.setRefreshToken(val);
        }
        return this;
    }

    public ZDataSource setRefreshTokenURL(String val) {
        if(data != null) {
            data.setRefreshTokenUrl(val);
        }
        return this;
    }

    public String getRefreshToken() {
        return data == null ? null : data.getRefreshToken();
    }

    public String getRefreshTokenUrl() {
        return data == null ? null : data.getRefreshTokenUrl();
    }
}
