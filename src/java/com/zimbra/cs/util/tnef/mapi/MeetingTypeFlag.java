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
 * The <code>MeetingTypeFlag</code> enum is used to represent flags
 * in the PidLidMeetingType MAPI property.
 * 
 * @author Gren Elliot
 *
 */
public enum MeetingTypeFlag {
    MTG_EMPTY                (0x00000000), // Unspecified.
    MTG_REQUEST              (0x00000001), // Initial meeting request.
    MTG_FULL                 (0x00010000), // Full update.
    MTG_INFO                 (0x00020000), // Informational update.
    MTG_OUTOFDATE            (0x00080000), // A newer Meeting Request object or Meeting Update object was
                                           // received after this one. For more details, see section 3.1.5.2.
    MTG_DELEGATORCOPY        (0x00100000); // This is set on the delegator's copy when a delegate will handle
                                           // meeting-related objects.

    private final int mapiFlagBit;

    MeetingTypeFlag(int flagPos) {
        mapiFlagBit = flagPos;
    }

    public int mapiFlagBit() {
        return mapiFlagBit;
    }


}
