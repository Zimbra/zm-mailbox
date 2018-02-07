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
import javax.xml.bind.annotation.XmlAttribute;
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
@XmlRootElement(name=AdminConstants.E_CONTACT_BACKUP_REQUEST)
public class ContactBackupRequest {
    /**
     * @zm-api-field-tag type
     * @zm-api-field-description Type can be either before or after
     */
    @XmlAttribute(name=AdminConstants.A_TASK /* task */, required=false)
    private String task;

    /**
     * @zm-api-field-tag mailboxIds
     * @zm-api-field-description List of mailbox ids
     */
    @ZimbraJsonArrayForWrapper
    @XmlElementWrapper(name=AdminConstants.E_MAILBOXIDS /* mailboxIds */, required=false)
    @XmlElement(name=AdminConstants.E_ID /* id */, required=false)
    private List<Integer> id;

    public ContactBackupRequest() {
        this.task = "start";
        this.id = null;
    }

    public ContactBackupRequest(String task, List<Integer> id) {
        this.task = task;
        this.id = id;
    }

    /**
     * @return the task
     */
    public String getTask() {
        return task;
    }

    /**
     * @param task the task to set
     */
    public void setTask(String task) {
        this.task = task;
    }

    /**
     * @return the id
     */
    public List<Integer> getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(List<Integer> id) {
        this.id = id;
    }

    public void addId(Integer id) {
        if (this.id == null) {
            this.id = new ArrayList<Integer>();
        }
        this.id.add(id);
    }

    public void addIds(List<Integer> ids) {
        if (this.id == null) {
            this.id = new ArrayList<Integer>();
        }
        this.id.addAll(ids);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        if (this.task != null) {
            helper.add("task", this.task);
        }
        if (this.id != null && !this.id.isEmpty()) {
            helper.add("mailboxIds", "");
            for (Integer id : this.id) {
                helper.add("id", id);
            }
        }
        return helper;
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
