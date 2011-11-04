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
public class DirPathInfo {

    @XmlAttribute(name=AdminConstants.A_PATH, required=true)
    private final String path;
    @XmlAttribute(name=AdminConstants.A_EXISTS, required=true)
    private final ZmBoolean exists;
    @XmlAttribute(name=AdminConstants.A_IS_DIRECTORY, required=true)
    private final ZmBoolean directory;
    @XmlAttribute(name=AdminConstants.A_READABLE, required=true)
    private final ZmBoolean readable;
    @XmlAttribute(name=AdminConstants.A_WRITABLE, required=true)
    private final ZmBoolean writable;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DirPathInfo() {
        this((String)null, false, false, false, false);
    }

    public DirPathInfo(String path, boolean exists, boolean directory,
            boolean readable, boolean writable) {
        this.path = path;
        this.exists = ZmBoolean.fromBool(exists);
        this.directory = ZmBoolean.fromBool(directory);
        this.readable = ZmBoolean.fromBool(readable);
        this.writable = ZmBoolean.fromBool(writable);
    }

    public String getPath() { return path; }
    public boolean isExists() { return ZmBoolean.toBool(exists); }
    public boolean isDirectory() { return ZmBoolean.toBool(directory); }
    public boolean isReadable() { return ZmBoolean.toBool(readable); }
    public boolean isWritable() { return ZmBoolean.toBool(writable); }
}
