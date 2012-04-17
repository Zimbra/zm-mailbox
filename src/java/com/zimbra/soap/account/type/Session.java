/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010,2012 Zimbra, Inc.
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

package com.zimbra.soap.account.type;

import com.google.common.base.Objects;
import com.zimbra.common.soap.HeaderConstants;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

 // See ZimbraSoapContext.encodeSession:
 // eSession.addAttribute(HeaderConstants.A_TYPE, typeStr).addAttribute(HeaderConstants.A_ID, sessionId).setText(sessionId);

@XmlAccessorType(XmlAccessType.NONE)
public class Session {

    /**
     * @zm-api-field-tag session-type
     * @zm-api-field-description Session type - currently only set if value is "admin"
     */
    @XmlAttribute(name=HeaderConstants.A_TYPE, required=false)
    private String type;

    /**
     * @zm-api-field-tag id
     * @zm-api-field-description Session ID
     */
    @XmlAttribute(name=HeaderConstants.A_ID, required=true)
    private String id;

    public Session() {
    }

    public void setType(String type) { this.type = type; }
    public void setId(String id) { this.id = id; }
    public void setSessionId(String sessionId) { this.id = sessionId; }
    public String getType() { return type; }
    public String getId() { return id; }
    /**
     * @zm-api-field-tag session-id
     * @zm-api-field-description Session ID  (same as <b>id</b>)
     */
    @XmlValue
    public String getSessionId() { return id; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("type", type)
            .add("id", id);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
