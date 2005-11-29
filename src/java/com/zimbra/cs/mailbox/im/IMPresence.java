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

public class IMPresence {
    public enum Show {
        AWAY, CHAT, DND, XA;  
    }
    
    private Show mShow;
    private byte mPriority;
    private String mStatus;
    
    public IMPresence(Show show, byte prio, String status) {
        mShow = show;
        mPriority = prio;
        mStatus = status;
    }
    
    public Show getShow() { return mShow; }
    public byte getPriority() { return mPriority; }
    public String getStatus() { return mStatus; }
}
