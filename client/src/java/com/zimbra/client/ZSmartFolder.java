package com.zimbra.client;

import org.json.JSONException;

import com.zimbra.client.event.ZModifySmartFolderEvent;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.RetentionPolicy;
import com.zimbra.soap.mail.type.TagInfo;

public class ZSmartFolder implements Comparable<ZSmartFolder>, ZItem, ToZJSONObject {

    private String id;
    private String name;
    private int unreadCount;
    private RetentionPolicy mRetentionPolicy = new RetentionPolicy();

    public ZSmartFolder(TagInfo tagInfo) {
        this.id = tagInfo.getId();
        this.name = tagInfo.getName();
        this.unreadCount = tagInfo.getUnread() == null ? 0 : tagInfo.getUnread();
    }

    public ZSmartFolder(Element e) throws ServiceException {
        id = e.getAttribute(MailConstants.A_ID);
        name = e.getAttribute(MailConstants.A_NAME);
        unreadCount = (int) e.getAttributeLong(MailConstants.A_UNREAD, 0);
        Element rpEl = e.getOptionalElement(MailConstants.E_RETENTION_POLICY);
        if (rpEl != null) {
            mRetentionPolicy = new RetentionPolicy(rpEl);
        }
    }

    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("id", id);
        zjo.put("name", name);
        zjo.put("unreadCount", unreadCount);
        return zjo;
    }

    public void modifyNotification(ZModifySmartFolderEvent event) throws ServiceException {
        unreadCount = event.getUnreadCount(unreadCount);
        mRetentionPolicy = event.getRetentionPolicy(mRetentionPolicy);
    }

    @Override
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getUuid() {
        return null;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

    @Override
    public int compareTo(ZSmartFolder other) {
        return getName().compareToIgnoreCase(other.getName());
    }

}
