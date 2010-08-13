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
 * The <code>TaskStatus</code> class relates to the PidLidTaskStatus MAPI Property
 * which is documented in MS-OXOTASK as specifying the status of the user's progress
 * on the task.
 *
 * @author Gren Elliot
 */
public enum TaskStatus {
    NOT_STARTED       (0x00000000),   // User has not started work on the task (PidLidPercentComplete == 0)
    IN_PROGRESS       (0x00000001),   // User's work is in progress (0 < PidLidPercentComplete < 1.0)
    COMPLETE          (0x00000002),   // User's work on task is complete (PidLidPercentComplete == 1.0)
    WAITING_ON_OTHER  (0x00000003),   // User is waiting on somebody else.
    DEFERRED          (0x00000004);   // User has deferred work on the task.

    private final int MapiPropValue;

    TaskStatus(int propValue) {
        MapiPropValue = propValue;
    }

    public int mapiPropValue() {
        return MapiPropValue;
    }

}
