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
package com.zimbra.cs.mailbox.im;

import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;

public class IMPresence {
    public enum Show {
        AWAY, CHAT, DND, XA, ONLINE, OFFLINE;  
    }
    
    private Show mShow;
    private byte mPriority;
    private String mStatus;
    
    public String toString() {
        return mShow.toString() + " pri="+mPriority+" st="+mStatus; 
    }
    
    public IMPresence(Show show, byte prio, String status) {
        assert(show != null);
        mShow = show;
        mPriority = prio;
        mStatus = status;
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
    
    public Element toXml(Element parent) {
        Element e = parent.addUniqueElement("presence");
            
        IMPresence.Show show = mShow;
        if (show != null)
            e.addAttribute("show", show.toString());
                
        if (mStatus != null) {
            Element se = e.addElement("status");
            se.setText(mStatus);
        }
        return e;
    }
    
    public Show getShow() { return mShow; }
    public byte getPriority() { return mPriority; }
    public String getStatus() { return mStatus; }
}
