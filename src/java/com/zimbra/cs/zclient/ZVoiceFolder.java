/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra Collaboration Suite Server.
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

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

    public ZVoiceFolder(Element e, ZFolder parent) throws ServiceException {
        super(e, parent);
    }

    protected ZFolder createSubFolder(Element element) throws ServiceException {
        return new ZVoiceFolder(element, this);
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
