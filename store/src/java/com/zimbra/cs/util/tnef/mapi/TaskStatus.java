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
