/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.im;

import java.util.Collection;
import java.util.List;

import org.xmpp.packet.Roster;

import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.IMConstants;
import com.zimbra.common.soap.Element;

class IMSubscribedNotification extends IMNotification {
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
    
    static IMSubscribedNotification create(IMAddr address, String name, Collection<String> groups, boolean subscribed, Roster.Ask ask) {
        String[] str = new String[groups.size()];
        int i = 0;
        for (String s : groups) 
            str[i++] = s;

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
            e = create(parent, IMConstants.E_SUBSCRIBED);
        } else {
            e = create(parent, IMConstants.E_UNSUBSCRIBED);
        }
        e.addAttribute(IMConstants.A_NAME, mName);
        
        if (mAsk != null) {
            e.addAttribute(IMConstants.A_ASK, mAsk.name());
        }
        
        if (mGroups != null) {
            e.addAttribute(IMConstants.A_GROUPS, StringUtil.join(",", mGroups));
        }
        
        e.addAttribute(IMConstants.A_TO, mAddr.getAddr());

        return e;
    }
}