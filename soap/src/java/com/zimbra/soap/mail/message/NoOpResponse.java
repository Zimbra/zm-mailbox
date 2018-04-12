/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.mail.message;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_NO_OP_RESPONSE)
public final class NoOpResponse {

    /**
     * @zm-api-field-tag wait-disallowed
     * @zm-api-field-description Set if wait was disallowed
     */
    @XmlAttribute(name=MailConstants.A_WAIT_DISALLOWED /* waitDisallowed */, required=false)
    private ZmBoolean waitDisallowed;

    public NoOpResponse() {
    }

    public NoOpResponse(Boolean waitDisallowed) {
        setWaitDisallowed(waitDisallowed);
    }

    public static NoOpResponse create(Boolean waitDisallowed) {
        return new NoOpResponse(waitDisallowed);
    }

    public void setWaitDisallowed(Boolean waitDisallowed) { this.waitDisallowed = ZmBoolean.fromBool(waitDisallowed); }
    public Boolean getWaitDisallowed() { return ZmBoolean.toBool(waitDisallowed); }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("waitDisallowed", waitDisallowed);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
