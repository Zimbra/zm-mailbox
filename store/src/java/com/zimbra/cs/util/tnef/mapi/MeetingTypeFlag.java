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
