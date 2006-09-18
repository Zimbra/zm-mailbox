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
