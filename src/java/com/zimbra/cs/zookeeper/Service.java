/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zookeeper;

import org.codehaus.jackson.map.annotate.JsonRootName;

@JsonRootName("details")
public class Service {
    private String id;

    public Service() {
        this("");
    }

    public Service(String id) {
        this.id = id;
    }

    public void setService(String id) {
        this.id = id;
    }

    public String getService() {
        return id;
    }
}
