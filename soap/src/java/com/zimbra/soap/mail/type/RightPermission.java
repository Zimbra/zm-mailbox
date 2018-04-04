/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.mail.type;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class RightPermission {

    /**
     * @zm-api-field-tag has-right-on-target
     * @zm-api-field-description If set then the authed user has the right <b>{right-name}</b> on the target.
     */
    @XmlAttribute(name=MailConstants.A_ALLOW /* allow */, required=true)
    private final ZmBoolean allow;

    /**
     * @zm-api-field-tag right-name
     * @zm-api-field-description Right name
     */
    @XmlValue
    private String rightName;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private RightPermission() {
        this((Boolean) null);
    }

    public RightPermission(Boolean allow) {
        this.allow = ZmBoolean.fromBool(allow);
    }

    public void setRightName(String rightName) { this.rightName = rightName; }
    public ZmBoolean getAllow() { return allow; }
    public String getRightName() { return rightName; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("allow", allow)
            .add("rightName", rightName);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
