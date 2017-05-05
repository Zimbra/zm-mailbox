package com.zimbra.client;

import org.json.JSONException;

import com.zimbra.client.event.ZModifyEvent;
import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.mailbox.MailItemType;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;

public class ZIdHit implements ZImapSearchHit {

    private String id;
    private String sortField;

    public ZIdHit(Element e) throws ServiceException {
        id = e.getAttribute(MailConstants.A_ID);
        sortField = e.getAttribute(MailConstants.A_SORT_FIELD, null);
    }

    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("id", id);
        zjo.put("sortField", sortField);
        return zjo;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getSortField() {
        return sortField;
    }

    @Override
    public void modifyNotification(ZModifyEvent event) throws ServiceException {}

    @Override
    public int getItemId() throws ServiceException {
        return new ItemIdentifier(id, null).id;
    }

    @Override
    public int getParentId() throws ServiceException {
        return -1;
    }

    @Override
    public int getModifiedSequence() throws ServiceException {
        return -1;
    }

    @Override
    public MailItemType getMailItemType() throws ServiceException {
        return null;
    }

    @Override
    public int getImapUid() throws ServiceException {
        return -1;
    }

    @Override
    public int getFlagBitmask() throws ServiceException {
        return -1;
    }

    @Override
    public String[] getTags() throws ServiceException {
        return null;
    }
}
