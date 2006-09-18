package com.zimbra.cs.jsp.bean;

import java.util.Date;
import java.util.List;

import com.zimbra.cs.zclient.ZConversationHit;
import com.zimbra.cs.zclient.ZEmailAddress;

public class ZConversationHitBean extends ZSearchHitBean {

    private ZConversationHit mHit;
    
    public ZConversationHitBean(ZConversationHit hit) {
        super(hit, HitType.conversation);
        mHit = hit;
    }

    /**
     * @return conversation's id
     */
    public String getId() { return mHit.getId(); }
    
    /** change to a method that returns tag names? public String getTagIds(); */
    
    public Date getDate() { return new Date(mHit.getDate()); }
    
    public boolean getHasFlags() { return mHit.hasFlags(); }
    
    public boolean getHasTags() { return mHit.hasTags(); }
    
    public boolean getIsUnread() { return mHit.isUnread(); }

    public boolean getIsFlagged() { return mHit.isFlagged(); }

    public boolean getIsSentByMe() { return mHit.isSentByMe(); }

    public boolean getHasAttachment() { return mHit.hasAttachment(); }

    public String getSubject() { return mHit.getSubject(); }
    
    public String getFragment() { return mHit.getFragment(); }
    
    public int getMessageCount() { return mHit.getMessageCount(); }
    
    public List<String> getMatchedMessageIds() { return mHit.getMatchedMessageIds(); }
    
    public List<ZEmailAddress> getRawRecipients() { return mHit.getRecipients(); }
    
    public String getRecipients() { return BeanUtils.getAddrs(mHit.getRecipients()); }    
}
