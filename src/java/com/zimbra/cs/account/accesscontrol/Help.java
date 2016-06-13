/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.account.accesscontrol;

import java.util.List;

import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;

public class Help {
    private String name;
    private String desc;
    private List<String> items = Lists.newArrayList();
    
    Help(String name) {
        this.name = new String(name);
    }
    
    String getName() {
        return name;
    }
    
    void setDesc(String desc) {
        this.desc = new String(desc);
    }
    
    public String getDesc() {
        return desc;
    }

    void addItem(String item) {
        items.add(new String(item));
    }
    
    public List<String> getItems() {
        return items;
    }
    
    void validate() throws ServiceException {
        if (desc == null) {
            throw ServiceException.PARSE_ERROR("missing desc", null);
        }
    }

}
