package com.zimbra.client;

import org.json.JSONException;

import com.zimbra.client.event.ZModifyEvent;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;

public class ZIdHit implements ZSearchHit {

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

}
