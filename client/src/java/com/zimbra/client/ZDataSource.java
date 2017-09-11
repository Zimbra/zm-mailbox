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

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.soap.type.DataSource;
import com.zimbra.soap.type.DataSources;

public class ZDataSource  {
    private DataSource data;

    public ZDataSource() {
    }

    public ZDataSource(DataSource data) {
        this.data = DataSources.newDataSource(data);
    }
    
    public Element toElement(Element parent) {
        Element src = parent.addNonUniqueElement(MailConstants.E_DS);
        src.addAttribute(MailConstants.A_ID, data.getId());
        src.addAttribute(MailConstants.A_NAME, data.getName());
        src.addAttribute(MailConstants.A_DS_IS_ENABLED, data.isEnabled());
        src.addAttribute(MailConstants.A_DS_HOST, data.getHost());
        src.addAttribute(MailConstants.A_DS_PORT, data.getPort());
        src.addAttribute(MailConstants.A_DS_USERNAME, data.getUsername());
        src.addAttribute(MailConstants.A_DS_PASSWORD, data.getPassword());
        src.addAttribute(MailConstants.A_FOLDER, data.getFolderId());
        src.addAttribute(MailConstants.A_DS_CONNECTION_TYPE, data.getConnectionType().name());
        return src;
    }

    public Element toIdElement(Element parent) {
        Element src = parent.addElement(MailConstants.E_DS);
        src.addAttribute(MailConstants.A_ID, getId());
        return src;
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
}
