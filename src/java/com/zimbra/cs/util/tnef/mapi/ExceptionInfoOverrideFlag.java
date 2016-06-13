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
 * 
 * @author gren
 *
 * See MS-OXOCAL.  Indicates which optional parts of ExceptionInfo
 * are present.
 */
public enum ExceptionInfoOverrideFlag {

    ARO_SUBJECT         (0x0001),
    ARO_MEETINGTYPE     (0x0002),
    ARO_REMINDERDELTA   (0x0004),
    ARO_REMINDER        (0x0008), // Indicates that ReminderSet field is present
    ARO_LOCATION        (0x0010),
    ARO_BUSYSTATUS      (0x0020),
    ARO_ATTACHMENT      (0x0040),
    ARO_SUBTYPE         (0x0080),
    ARO_APPTCOLOR       (0x0100),
    ARO_EXCEPTION_BODY  (0x0200);

    private final int MapiPropValue;

    ExceptionInfoOverrideFlag (int propValue) {
        MapiPropValue = propValue;
    }

    public int mapiPropValue() {
        return MapiPropValue;
    }

}
