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

import com.zimbra.common.util.SystemUtil;
import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.soap.mail.type.DataSourceNameOrId;
import com.zimbra.soap.mail.type.MailDataSource;
import com.zimbra.soap.type.DataSource;
import com.zimbra.soap.type.DataSources;

public class ZDataSource  {
    protected DataSource data;

    public ZDataSource() {
        data = DataSources.newDataSource();
        data.setEnabled(false);
    }

    public ZDataSource(String name, boolean enabled) {
        data = DataSources.newDataSource();
        data.setName(name);
        data.setEnabled(enabled);
    }

    public ZDataSource(String name, boolean enabled, Iterable<String> attributes) {
        data = DataSources.newDataSource();
        data.setAttributes(attributes);
        data.setName(name);
        data.setEnabled(enabled);
    }

    public ZDataSource(DataSource data) {
        this.data = DataSources.newDataSource(data);
    }

    public DataSource toJaxb() {
        MailDataSource jaxbObject = new MailDataSource();
        jaxbObject.setId(data.getId());
        jaxbObject.setName(data.getName());
        jaxbObject.setEnabled(data.isEnabled());
        return jaxbObject;
    }

    public DataSourceNameOrId toJaxbNameOrId() {
        DataSourceNameOrId jaxbObject = DataSourceNameOrId.createForId(data.getId());
        return jaxbObject;
    }

    public String getName() {
        return data.getName();
    }
    public void setName(String name) {
        data.setName(name);
    }
    public DataSourceType getType() {
        return DataSourceType.unknown;
    }
    public String getId() {
        return data.getId();
    }
    public void setId(String id) {
        data.setId(id);
    }
    public String getImportClass() {
        return data.getImportClass();
    }
    public void setImportClass(String importClass) {
        data.setImportClass(importClass);
    }
    public void setEnabled(boolean enabled) { data.setEnabled(enabled); }
    public boolean isEnabled() {
        return SystemUtil.coalesce(data.isEnabled(), Boolean.FALSE);
    }
}
