/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
