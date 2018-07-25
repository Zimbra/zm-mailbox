/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.account.type;

import com.google.common.base.MoreObjects;
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("type", type)
            .add("id", id);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
