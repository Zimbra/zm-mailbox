/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 VMware, Inc.
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

import com.zimbra.common.service.ServiceException;

public class UI implements Comparable<UI> {
    
    private String name;
    private String desc;
    
    public UI(String name) {
        this.name = name;
    }
    
    /*
     * for sorting for RightManager.genAdminDocs()
     */
    @Override
    public int compareTo(UI other) {
        return name.compareTo(other.name);
    }
    
    String getName() {
        return name;
    }
    
    void setDesc(String desc) {
        this.desc = new String(desc);
    }
    
    String getDesc() {
        return desc;
    }
    
    void validate() throws ServiceException {
        if (desc == null) {
            throw ServiceException.PARSE_ERROR("missing desc", null);
        }
    }


    
}
