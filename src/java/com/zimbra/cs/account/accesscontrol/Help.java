/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011 VMware, Inc.
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
