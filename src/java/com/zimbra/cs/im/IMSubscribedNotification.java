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
package com.zimbra.cs.im;

import java.util.List;

import org.xmpp.packet.Roster;

import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.service.im.IMService;
import com.zimbra.soap.Element;

class IMSubscribedNotification implements IMNotification {
    IMAddr mAddr;
    String mName;
    String[] mGroups;
    boolean mSubscribed;
    Roster.Ask mAsk;

    static IMSubscribedNotification create(IMAddr address, String name, List<IMGroup> groups, boolean subscribed, Roster.Ask ask) {
        String[] str = new String[groups.size()];
        int i = 0;
        for (IMGroup grp : groups) 
            str[i++] = grp.getName();

        return new IMSubscribedNotification(address, name, str, subscribed, ask);
    }
    
    static IMSubscribedNotification create(IMAddr address, String name, String[] groups, boolean subscribed, Roster.Ask ask) {
        return new IMSubscribedNotification(address, name, groups, subscribed, ask);
    }
    
    static IMSubscribedNotification create(IMAddr address, String name, boolean subscribed, Roster.Ask ask) {
        return new IMSubscribedNotification(address, name, null, subscribed, ask);
    }
    
    private IMSubscribedNotification(IMAddr address, String name, String[] groups, boolean subscribed, Roster.Ask ask) {
        mAddr = address;
        mName = name;
        mGroups = groups;
        mSubscribed = subscribed;
        mAsk = ask;
    }
    
    public Element toXml(Element parent) {
        ZimbraLog.im.info("IMSubscribedNotification " + mAddr + " " + mName + " Subscribed=" +mSubscribed
                    + (mAsk != null ? " Ask="+mAsk.toString() : ""));
        
        Element e;
        if (mSubscribed) { 
            e = parent.addElement(IMService.E_SUBSCRIBED);
            e.addAttribute(IMService.A_NAME, mName);
        } else {
            e = parent.addElement(IMService.E_UNSUBSCRIBED);
        }
        
        if (mAsk != null) {
            e.addAttribute("ask", mAsk.name());
        }
        
        if (mGroups != null) {
            e.addAttribute(IMService.A_GROUPS, StringUtil.join(",", mGroups));
        }
        
        e.addAttribute(IMService.A_TO, mAddr.getAddr());

        return e;
    }
}