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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ConflictRecurrenceInstance extends ExpandedRecurrenceInstance {

    /**
     * @zm-api-field-description Free/Busy user status
     */
    @XmlElement(name=MailConstants.E_FREEBUSY_USER /* usr */, required=false)
    private List<FreeBusyUserStatus> freebusyUsers = Lists.newArrayList();

    public ConflictRecurrenceInstance() {
    }

    public void setFreebusyUsers(Iterable <FreeBusyUserStatus> freebusyUsers) {
        this.freebusyUsers.clear();
        if (freebusyUsers != null) {
            Iterables.addAll(this.freebusyUsers,freebusyUsers);
        }
    }

    public ConflictRecurrenceInstance addFreebusyUser(FreeBusyUserStatus freebusyUser) {
        this.freebusyUsers.add(freebusyUser);
        return this;
    }

    public List<FreeBusyUserStatus> getFreebusyUsers() {
        return Collections.unmodifiableList(freebusyUsers);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("freebusyUsers", freebusyUsers);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
