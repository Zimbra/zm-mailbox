package com.zimbra.cs.jsp.bean;

import java.util.Date;
import java.util.List;

import com.zimbra.cs.zclient.ZMessageHit;
import com.zimbra.cs.zclient.ZEmailAddress;

public class ZMessageHitBean extends ZSearchHitBean {

    private ZMessageHit mHit;
    
    public ZMessageHitBean(ZMessageHit hit) {
        super(hit, HitType.message);
        mHit = hit;
    }

    public String getId() { return mHit.getId(); }

    public String getFlags() { return mHit.getFlags(); } 
    
    public String getFolderId() { return mHit.getFolderId(); }

    public long getSize() { return mHit.getSize(); }

    public Date getDate() { return new Date(mHit.getDate()); }
    
    public String getConversationId() { return mHit.getConversationId(); }
    
    public boolean getIsUnread() { return mHit.isUnread(); }

    public boolean getIsFlagged() { return mHit.isFlagged(); }

    public boolean getHasAttachment() { return mHit.hasAttachment(); }

    public boolean getIsRepliedTo() { return mHit.isRepliedTo(); }

    public boolean getIsSentByMe() { return mHit.isSentByMe(); }

    public boolean getIsForwarded() { return mHit.isForwarded(); } 

    public boolean getIsDraft() { return mHit.isDraft(); }

    public boolean getIsDeleted() { return mHit.isDeleted(); }

    public boolean getIsNotificationSent() { return mHit.isNotificationSent(); }
    
    /**
     * @return comma-separated list of tag ids
     */
    public String getTagIds() { return mHit.getTagIds(); }

    public String getSubject() { return mHit.getSubject(); }
    
    public boolean getHasFlags() { return mHit.hasFlags(); }
    
    public boolean getHasTags() { return mHit.hasTags(); }
    
    public String getFragment() { return mHit.getFragment(); }
    
    public ZEmailAddress getSender() { return mHit.getSender(); }

    public String getDisplaySender() { return BeanUtils.getAddr(mHit.getSender()); }

    public boolean getContentMatched() { return mHit.getContentMatched(); }
    
    /**
     *  @return names (1.2.3...) of mime part(s) that matched, or empty list.
     */
    public List<String> getMimePartHits() { return mHit.getMimePartHits(); }
}
