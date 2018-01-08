package com.zimbra.client;

import org.json.JSONException;

import com.zimbra.client.event.ZModifySmartFolderEvent;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.mail.type.TagInfo;

public class ZSmartFolder extends ZTagInfo implements Comparable<ZSmartFolder> {

    public ZSmartFolder(TagInfo tagInfo, ZMailbox mbox) throws ServiceException {
        super(tagInfo, mbox);
    }

    public ZSmartFolder(Element e, ZMailbox mbox) throws ServiceException {
        super(e, mbox);
    }

    public void modifyNotification(ZModifySmartFolderEvent event) throws ServiceException {
        unreadCount = event.getUnreadCount(unreadCount);
        retentionPolicy = event.getRetentionPolicy(retentionPolicy);
    }

    @Override
    public int compareTo(ZSmartFolder other) {
        return getName().compareToIgnoreCase(other.getName());
    }


    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("id", id);
        zjo.put("name", name);
        zjo.put("unreadCount", unreadCount);
        return zjo;
    }

}
