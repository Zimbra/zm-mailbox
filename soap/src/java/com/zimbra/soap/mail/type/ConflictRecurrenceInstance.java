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

package com.zimbra.soap.mail.type;

import com.google.common.base.MoreObjects;
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("freebusyUsers", freebusyUsers);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
