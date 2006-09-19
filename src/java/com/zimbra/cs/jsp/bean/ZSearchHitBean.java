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
package com.zimbra.cs.jsp.bean;

import com.zimbra.cs.zclient.ZSearchHit;

public abstract class ZSearchHitBean {
    
    public enum HitType { conversation, contact, message };
    
    private HitType mHitType;
    private ZSearchHit mHit;
    
    protected ZSearchHitBean(ZSearchHit hit, HitType hitType) {
        mHit = hit;
        mHitType = hitType;
    }
    
    public String getId() { return mHit.getId(); }
    
    public String getSortField() { return mHit.getSortFied(); }
    
    public float getScore() { return mHit.getScore(); }
    
    public String getType() { return mHitType.name(); }
    
    public boolean getIsConversation() { return mHitType == HitType.conversation; }
    
    public boolean getIsMessage() { return mHitType == HitType.message; }
    
    public boolean getIsContact() { return mHitType == HitType.contact; }
}
