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
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
public class CalendarAttendeeWithGroupInfo extends CalendarAttendee {

    @XmlAttribute(name=MailConstants.A_IS_GROUP /* isGroup */, required=false)
    private ZmBoolean group;

    @XmlAttribute(name=MailConstants.A_EXP /* exp */, required=false)
    private ZmBoolean canExpandGroupMembers;

    public CalendarAttendeeWithGroupInfo() {
    }

    public void setGroup(Boolean group) { this.group = ZmBoolean.fromBool(group); }
    public void setCanExpandGroupMembers(Boolean canExpandGroupMembers) {
        this.canExpandGroupMembers = ZmBoolean.fromBool(canExpandGroupMembers);
    }
    public Boolean getGroup() { return ZmBoolean.toBool(group); }
    public Boolean getCanExpandGroupMembers() { return ZmBoolean.toBool(canExpandGroupMembers); }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("group", group)
            .add("canExpandGroupMembers", canExpandGroupMembers);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
