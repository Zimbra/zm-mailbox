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
