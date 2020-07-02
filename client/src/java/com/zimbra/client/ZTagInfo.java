package com.zimbra.client;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.RetentionPolicy;
import com.zimbra.soap.mail.type.TagInfo;

public abstract class ZTagInfo implements  ZItem, ToZJSONObject {
    protected String id;
    protected String name;
    protected int unreadCount;
    protected RetentionPolicy retentionPolicy = new RetentionPolicy();
    protected ZMailbox mailbox;

    public ZTagInfo(TagInfo tagInfo, ZMailbox mbox) throws ServiceException {
        this.id = tagInfo.getId();
        this.name = tagInfo.getName();
        this.unreadCount = tagInfo.getUnread() == null ? 0 : tagInfo.getUnread();
        this.retentionPolicy = tagInfo.getRetentionPolicy();
        this.mailbox = mbox;
    }

    public ZTagInfo(Element e, ZMailbox mbox) throws ServiceException {
        id = e.getAttribute(MailConstants.A_ID);
        name = e.getAttribute(MailConstants.A_NAME);
        unreadCount = (int) e.getAttributeLong(MailConstants.A_UNREAD, 0);
        Element rpEl = e.getOptionalElement(MailConstants.E_RETENTION_POLICY);
        if (rpEl != null) {
            retentionPolicy = new RetentionPolicy(rpEl);
        }
        this.mailbox = mbox;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return null;
    }

    public ZMailbox getMailbox() {
        return mailbox;
    }

    public String getName() {
        return name;
    }

    /**
     * @return number of unread items in folder
     */
    public int getUnreadCount() {
        return unreadCount;
    }

    public RetentionPolicy getRetentionPolicy() {
        return retentionPolicy;
    }

    public void setRetentionPolicy(RetentionPolicy rp) {
        retentionPolicy = rp;
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }
}


