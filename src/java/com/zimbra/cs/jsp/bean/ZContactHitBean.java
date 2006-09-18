package com.zimbra.cs.jsp.bean;

import com.zimbra.cs.zclient.ZContactHit;

public class ZContactHitBean extends ZSearchHitBean {
    
    private ZContactHit mHit;
    
    public ZContactHitBean(ZContactHit hit) {
        super(hit, HitType.contact);
        mHit = hit;
    }
    
    public String getId() { return mHit.getId(); }

    public String getFolderId() { return mHit.getFolderId(); }

    public String getRevision() { return mHit.getRevision(); }
    
    public String getFileAsStr() { return mHit.getFileAsStr(); } 

    public String getEmail() { return mHit.getEmail(); }

    public String getEmail2() { return mHit.getEmail2(); }

    public String getEmail3() { return mHit.getEmail3(); }
    
    /**
     * @return time in msecs
     */
    public long getMetaDataChangedDate() { return mHit.getMetaDataChangedDate(); }
}
