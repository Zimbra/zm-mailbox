/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.client;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.VoiceConstants;
import com.zimbra.common.service.ServiceException;

import java.util.Map;
import java.util.HashMap;

public class ZVoiceFolder extends ZFolder {

    private static Map<String, Integer> mSortMap;
    static {
        mSortMap = new HashMap<String, Integer>();
        mSortMap.put(VoiceConstants.FNAME_PLACEDCALLS, 5);
        mSortMap.put(VoiceConstants.FNAME_ANSWEREDCALLS, 4);
        mSortMap.put(VoiceConstants.FNAME_MISSEDCALLS, 3);
        mSortMap.put(VoiceConstants.FNAME_VOICEMAILINBOX, 1);
        mSortMap.put(VoiceConstants.FNAME_TRASH, 2);
    }

    public ZVoiceFolder(Element e, ZFolder parent, ZMailbox mailbox) throws ServiceException {
        super(e, parent, mailbox);
    }

    protected ZFolder createSubFolder(Element element) throws ServiceException {
        return new ZVoiceFolder(element, this, getMailbox());
    }

    public int compareTo(Object obj) {
        if (!(obj instanceof ZVoiceFolder))
            return 0;
        ZFolder other = (ZFolder) obj;
        int valueA = mSortMap.get(getName());
        int valueB = mSortMap.get(other.getName());
        return valueA - valueB;
    }
}
