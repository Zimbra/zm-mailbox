/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

import org.xmpp.packet.Presence;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.mailbox.Metadata;

public class IMPresence {
    public enum Show {
        AWAY, CHAT, DND, XA, ONLINE, OFFLINE;  
    }
    
    private Show mShow;
    private byte mPriority;
    private String mStatus;
    
    public static final IMPresence UNAVAILABLE = new IMPresence(Show.OFFLINE, (byte)0, "");
    
    public String toString() {
        return mShow.toString() + " pri="+mPriority+" st="+mStatus; 
    }
    
    public IMPresence(Show show, byte prio, String status) {
        assert(show != null);
        mShow = show;
        mPriority = prio;
        mStatus = status;
    }
    
    public IMPresence(Presence pres) {
        mPriority = (byte)(pres.getPriority());
        mStatus = pres.getStatus();
        
        Presence.Type ptype = pres.getType();
        if (ptype == null) {
            Presence.Show pShow = pres.getShow();
            mShow = IMPresence.Show.ONLINE;
            if (pShow != null) {
                switch (pShow) {
                    case chat:
                        mShow = Show.CHAT;
                        break;
                    case away:
                        mShow = Show.AWAY;
                    break;
                    case xa:
                        mShow = Show.XA;
                    break;
                    case dnd:
                        mShow = Show.DND;
                    break;
                }
            }
        } else {
            mShow = Show.OFFLINE;
        }
    }
    
    Presence getXMPPPresence() {
        Presence xmppPresence;

        if (getShow() == Show.OFFLINE) {
            xmppPresence = new Presence(Presence.Type.unavailable);
        } else {
            xmppPresence = new Presence();
            if (getShow() == Show.CHAT)
                xmppPresence.setShow(Presence.Show.chat);
            else if (getShow() == Show.AWAY)
                xmppPresence.setShow(Presence.Show.away);
            else if (getShow() == Show.XA)
                xmppPresence.setShow(Presence.Show.xa);
            else if (getShow() == Show.DND)
                xmppPresence.setShow(Presence.Show.dnd);
        }
        if (getStatus() != null && getStatus().length() > 0)
            xmppPresence.setStatus(getStatus());
        
        if (this.getPriority() != 0)
            xmppPresence.setPriority(this.getPriority());
        
        return xmppPresence;
    }
    
    private static final String FN_SHOW = "h";
    private static final String FN_PRIORITY = "p";
    private static final String FN_STATUS = "t";
    
    Metadata encodeAsMetadata()
    {
        Metadata meta = new Metadata();
        
        meta.put(FN_SHOW, mShow.toString());
        meta.put(FN_PRIORITY, Byte.toString(mPriority));
        if (mStatus != null && mStatus.length() > 0) 
            meta.put(FN_STATUS, mStatus);

        return meta;
    }
    
    static IMPresence decodeMetadata(Metadata meta) throws ServiceException
    {
        Show show = Show.valueOf(meta.get(FN_SHOW));
        byte priority = Byte.parseByte(meta.get(FN_PRIORITY));
        String status = null;
        if (meta.containsKey(FN_STATUS))
            status = meta.get(FN_STATUS);
     
        return new IMPresence(show, priority, status);
    }
    
    public void toXml(Element parent) {
        IMPresence.Show show = mShow;
        if (show != null)
            parent.addAttribute("show", show.toString());
                
        if (mStatus != null) {
            parent.addAttribute("status", mStatus);
        }
    }
    
    public Show getShow() { return mShow; }
    public byte getPriority() { return mPriority; }
    public String getStatus() { return mStatus; }
}
