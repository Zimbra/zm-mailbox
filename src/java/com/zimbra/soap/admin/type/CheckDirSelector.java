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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
public class CheckDirSelector {

    @XmlAttribute(name=AdminConstants.A_PATH, required=true)
    private final String path;
    @XmlAttribute(name=AdminConstants.A_CREATE, required=false)
    private final ZmBoolean create;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CheckDirSelector() {
        this((String)null, (Boolean) null);
    }

    public CheckDirSelector(String path) {
        this(path, (Boolean) null);
    }

    public CheckDirSelector(String path, Boolean create) {
        this.path = path;
        this.create = ZmBoolean.fromBool(create);
    }

    public String getPath() { return path; }
    public Boolean isCreate() { return ZmBoolean.toBool(create); }
}
