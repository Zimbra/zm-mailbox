/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

package com.zimbra.cs.client.soap;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.client.*;
import com.zimbra.soap.DomUtil;

public class LmcGetContactsRequest extends LmcSoapRequest {

    private LmcContactAttr mAttrs[];
    private String mIDList[];
    
    public void setContacts(String c[]) { mIDList = c; }
    public void setAttrs(LmcContactAttr attrs[]) { mAttrs = attrs; }
    
    public String[] getContacts() { return mIDList; }
    public LmcContactAttr[] getAttrs() { return mAttrs; }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailService.GET_CONTACTS_REQUEST);
        
        // emit contact attributes if any
        for (int i = 0; mAttrs != null && i < mAttrs.length; i++)
            addContactAttr(request, mAttrs[i]);
        
        // emit specified contacts if any
        for (int i = 0; mIDList != null && i < mIDList.length; i++) {
            Element newCN = DomUtil.add(request, MailService.E_CONTACT, "");
            DomUtil.addAttr(newCN, MailService.A_ID, mIDList[i]);
        }

        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException, LmcSoapClientException 
    {
        LmcGetContactsResponse response = new LmcGetContactsResponse();
        LmcContact cons[] = parseContactArray(responseXML);
        response.setContacts(cons);
        return response;
    }
}
