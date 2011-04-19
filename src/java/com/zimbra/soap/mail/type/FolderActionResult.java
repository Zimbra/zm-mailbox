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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class FolderActionResult extends IdAndOperation {

    @XmlAttribute(name=MailConstants.A_ZIMBRA_ID, required=false)
    private String zimbraId;

    @XmlAttribute(name=MailConstants.A_DISPLAY, required=false)
    private String displayName;

    @XmlAttribute(name=MailConstants.A_ACCESSKEY, required=false)
    private String accessKey;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private FolderActionResult() {
        this((String) null, (String) null);
    }

    public FolderActionResult(String id, String operation) {
        super(id, operation);
    }


    public void setZimbraId(String zimbraId) { this.zimbraId = zimbraId; }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public String getZimbraId() { return zimbraId; }
    public String getDisplayName() { return displayName; }
    public String getAccessKey() { return accessKey; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("zimbraId", zimbraId)
            .add("displayName", displayName)
            .add("accessKey", accessKey)
            .toString();
    }
}
