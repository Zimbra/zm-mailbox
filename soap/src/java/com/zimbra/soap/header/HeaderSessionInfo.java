/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.header;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class HeaderSessionInfo {

    /**
     * @zm-api-field-tag session-proxied
     * @zm-api-field-description Flags whether session has been proxied.  Default value false
     */
    @XmlAttribute(name=HeaderConstants.A_PROXIED /* proxy */, required=false)
    private ZmBoolean sessionProxied;

    /**
     * @zm-api-field-tag session-id
     * @zm-api-field-description Session ID
     */
    @XmlAttribute(name=HeaderConstants.A_ID /* id */, required=false)
    private String sessionId;

    // Default value -1
    /**
     * @zm-api-field-tag sequence-num
     * @zm-api-field-description Sequence number for the highest notification received.
     */
    @XmlAttribute(name=HeaderConstants.A_SEQNO /* seq */, required=false)
    private Integer sequenceNum;

    /**
     * @zm-api-field-tag session-id-from-value
     * @zm-api-field-description Session ID (alternative source for session ID to the 'id' attribute.
     */
    @XmlValue
    private String value;

    public HeaderSessionInfo() {
    }

    public void setSessionProxied(Boolean sessionProxied) { this.sessionProxied = ZmBoolean.fromBool(sessionProxied); }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public void setSequenceNum(Integer sequenceNum) { this.sequenceNum = sequenceNum; }
    public void setValue(String value) { this.value = value; }
    public Boolean getSessionProxied() { return ZmBoolean.toBool(sessionProxied); }
    public String getSessionId() { return sessionId; }
    public Integer getSequenceNum() { return sequenceNum; }
    public String getValue() { return value; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("sessionProxied", sessionProxied)
            .add("sessionId", sessionId)
            .add("sequenceNum", sequenceNum)
            .add("value", value);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
