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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.im;

import java.util.List;

import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.service.im.IMService;
import com.zimbra.soap.Element;

class IMSubscribedNotification implements IMNotification {
    IMAddr mAddr;
    String mName;
    String[] mGroups;
    boolean mRemove;

    static IMSubscribedNotification create(IMAddr address, String name, List<IMGroup> groups, boolean remove) {
        String[] str = new String[groups.size()];
        int i = 0;
        for (IMGroup grp : groups) 
            str[i++] = grp.getName();

        return new IMSubscribedNotification(address, name, str, remove);
    }
    
    IMSubscribedNotification(IMAddr address, String name, String[] groups, boolean remove) {
        mAddr = address;
        mName = name;
        mGroups = groups;
        mRemove = remove;
        
    }
    
    public Element toXml(Element parent) {
        ZimbraLog.im.info("IMSubscribedNotification " + mAddr + " " + mName + " " +mRemove);
        Element e;
        if (mRemove) { 
            e = parent.addElement(IMService.E_UNSUBSCRIBED);
        } else {
            e = parent.addElement(IMService.E_SUBSCRIBED);
            e.addAttribute(IMService.A_NAME, mName);
        }
        
        if (mGroups != null) {
            e.addAttribute(IMService.A_GROUPS, StringUtil.join(",", mGroups));
        }
        
        e.addAttribute(IMService.A_TO, mAddr.getAddr());

        return e;
    }
}