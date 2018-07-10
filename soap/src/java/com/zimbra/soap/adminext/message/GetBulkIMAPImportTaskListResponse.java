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

package com.zimbra.soap.adminext.message;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminExtConstants;
import com.zimbra.soap.adminext.type.BulkIMAPImportTaskInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminExtConstants.E_GET_BULK_IMAP_IMPORT_TASKLIST_RESPONSE)
public class GetBulkIMAPImportTaskListResponse {

    /**
     * @zm-api-field-description Information on inport tasks
     */
    @XmlElement(name=AdminExtConstants.E_Task /* task */, required=false)
    private List<BulkIMAPImportTaskInfo> tasks = Lists.newArrayList();

    public GetBulkIMAPImportTaskListResponse() {
    }

    public void setTasks(Iterable <BulkIMAPImportTaskInfo> tasks) {
        this.tasks.clear();
        if (tasks != null) {
            Iterables.addAll(this.tasks,tasks);
        }
    }

    public void addTask(BulkIMAPImportTaskInfo task) {
        this.tasks.add(task);
    }

    public List<BulkIMAPImportTaskInfo> getTasks() {
        return tasks;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("tasks", tasks);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
