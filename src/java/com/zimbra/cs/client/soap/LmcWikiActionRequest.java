package com.zimbra.cs.client.soap;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.cs.service.mail.MailService;
import com.zimbra.soap.DomUtil;

public class LmcWikiActionRequest extends LmcItemActionRequest {
    private String mName;

    public void setName(String n) { mName = n; }
    public String getName() { return mName; }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailService.WIKI_ACTION_REQUEST);
        Element a = DomUtil.add(request, MailService.E_ACTION, "");
        DomUtil.addAttr(a, MailService.A_ID, mIDList);
        DomUtil.addAttr(a, MailService.A_OPERATION, mOp);
        DomUtil.addAttr(a, MailService.A_TAG, mTag);
        DomUtil.addAttr(a, MailService.A_FOLDER, mFolder);
        DomUtil.addAttr(a, MailService.A_NAME, mName);
        return request;
    }
}
