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
package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.SyncConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class SyncStateInfo {
    /**
     * @zm-api-field-tag syncState
     * @zm-api-field-description SyncState
     */
    @XmlAttribute(name = SyncConstants.E_SYNCSTATE /* syncState */, required = true)
    private final String syncState;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private SyncStateInfo() {
        this((String) null);
    }

    public SyncStateInfo(String syncState) {
        this.syncState = syncState;
    }

    public String getSyncState() {
        return syncState;
    }
}
