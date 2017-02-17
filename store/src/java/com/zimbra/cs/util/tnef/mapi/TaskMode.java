/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.util.tnef.mapi;

/**
 * The <code>TaskMode</code> class relates to the PidLidTaskMode MAPI Property
 * which is documented in MS-OXOTASK as specifying the assignment status of a
 * task object.
 *
 * @author Gren Elliot
 */
public enum TaskMode {
    TASK_NOT_ASSIGNED         (0x00000000),   // The Task object is not assigned.
    TASK_REQUEST              (0x00000001),   // The Task object is embedded in a task request.
    TASK_ACCEPTED_BY_ASSIGNEE (0x00000002),   // The Task object has been accepted by the task assignee.
    TASK_REJECTED_BY_ASSIGNEE (0x00000003),   // The Task object was rejected by the task assignee.
    TASK_UPDATE               (0x00000004),   // The Task object is embedded in a task update.
    TASK_SELF_DELEGATED       (0x00000005);   // The Task object was assigned to the task assigner (self-delegation).

    private final int MapiPropValue;

    TaskMode(int propValue) {
        MapiPropValue = propValue;
    }

    public int mapiPropValue() {
        return MapiPropValue;
    }

}
