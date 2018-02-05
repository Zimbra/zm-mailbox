/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.message;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Objects;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonArrayForWrapper;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Modify Filter rules
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_CONTACT_BACKUP_RESPONSE)
public class ContactBackupResponse {
    /**
     * @zm-api-field-tag mailboxIds
     * @zm-api-field-description List of mailbox ids
     */
    @ZimbraJsonArrayForWrapper
    @XmlElementWrapper(name=AdminConstants.E_DONE_MAILBOXIDS /* doneMailboxIds */, required=false)
    @XmlElement(name=AdminConstants.A_ID /* id */, required=false)
    private List<Integer> doneIds;

    /**
     * @zm-api-field-tag mailboxIds
     * @zm-api-field-description List of mailbox ids
     */
    @ZimbraJsonArrayForWrapper
    @XmlElementWrapper(name=AdminConstants.E_SKIPPED_MAILBOXIDS /* skippedMailboxIds */, required=false)
    @XmlElement(name=AdminConstants.A_ID /* id */, required=false)
    private List<Integer> skippedIds;

    public ContactBackupResponse() {
        this.doneIds = null;
        this.skippedIds = null;
    }

    public ContactBackupResponse(List<Integer> doneIds, List<Integer> skippedIds) {
        this.doneIds = doneIds;
        this.skippedIds = skippedIds;
    }

    /**
     * @return the doneIds
     */
    public List<Integer> getDoneIds() {
        return doneIds;
    }

    /**
     * @param doneIds the list of ids to set as doneIds
     */
    public void setDoneIds(List<Integer> doneIds) {
        this.doneIds = doneIds;
    }

    public void addDoneId(Integer doneId) {
        if (this.doneIds == null) {
            this.doneIds = new ArrayList<Integer>();
        }
        this.doneIds.add(doneId);
    }

    public void addDoneIds(List<Integer> doneIds) {
        if (this.doneIds == null) {
            this.doneIds = new ArrayList<Integer>();
        }
        this.doneIds.addAll(doneIds);
    }

    /**
     * @return the skippedIds
     */
    public List<Integer> getSkippedIds() {
        return skippedIds;
    }

    /**
     * @param skippedIds the list of ids to set as doneIds
     */
    public void setSkippedIds(List<Integer> skippedIds) {
        this.skippedIds = skippedIds;
    }

    public void addSkippedId(Integer skippedId) {
        if (this.skippedIds == null) {
            this.skippedIds = new ArrayList<Integer>();
        }
        this.skippedIds.add(skippedId);
    }

    public void addSkippedIds(List<Integer> skippedIds) {
        if (this.skippedIds == null) {
            this.skippedIds = new ArrayList<Integer>();
        }
        this.skippedIds.addAll(skippedIds);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        if (this.doneIds != null && !this.doneIds.isEmpty()) {
            helper.add("doneIds", "");
            for (Integer doneId : this.doneIds) {
                helper.add("doneId", doneId);
            }
        }
        if (this.skippedIds != null && !this.skippedIds.isEmpty()) {
            helper.add("skippedIds", "");
            for (Integer skippedId : this.skippedIds) {
                helper.add("skippedId", skippedId);
            }
        }
        return helper;
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
