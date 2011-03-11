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

@XmlAccessorType(XmlAccessType.FIELD)
public class BufferedCommitInfo {

    @XmlAttribute(name=AdminConstants.A_AID, required=true)
    private final String aid;

    @XmlAttribute(name=AdminConstants.A_CID, required=true)
    private final String cid;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private BufferedCommitInfo() {
        this((String) null, (String) null);
    }

    public BufferedCommitInfo(String aid, String cid) {
        this.aid = aid;
        this.cid = cid;
    }

    public String getAid() { return aid; }
    public String getCid() { return cid; }
}
