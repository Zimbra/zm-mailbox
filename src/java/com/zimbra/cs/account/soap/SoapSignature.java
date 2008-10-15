/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.soap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Signature;
import com.zimbra.cs.zclient.ZClientException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SoapSignature extends Signature implements SoapEntry {
    
    SoapSignature(Account acct, String name, String id, Map<String, Object> attrs) {
        super(acct, name, id, attrs);
    }

    SoapSignature(Account acct, Element e) throws ServiceException {
        super(acct, e.getAttribute(AccountConstants.A_NAME), e.getAttribute(AccountConstants.A_ID), fromXML(e));
    }
    
    public void modifyAttrs(SoapProvisioning prov, Map<String, ? extends Object> attrs, boolean checkImmutable) throws ServiceException {
    }

    public void reload(SoapProvisioning prov) throws ServiceException {
    }
    
    public static void toXML(Element signature, Map<String, Object> attrs) throws ServiceException {
        for (Map.Entry entry : attrs.entrySet()) {
            String attr = (String)entry.getKey();
            String value = (String)entry.getValue();
            
            if (attr.equals(Provisioning.A_zimbraSignatureId) && !StringUtil.isNullOrEmpty(value))
                signature.addAttribute(AccountConstants.A_ID, value);
            else if (attr.equals(Provisioning.A_zimbraSignatureName) && !StringUtil.isNullOrEmpty(value))
                signature.addAttribute(AccountConstants.A_NAME, value);
            else {
                String mimeType = Signature.attrNameToMimeType(attr);
                if (mimeType == null)
                    throw ZClientException.CLIENT_ERROR("invalid attr: "+attr, null);
                
                signature.addElement(AccountConstants.E_CONTENT).addAttribute(AccountConstants.A_TYPE, mimeType).addText(value);
            }
        }
    }
    
    private static Map<String, Object> fromXML(Element signature) throws ServiceException {
        List<Element> contents = signature.listElements(AccountConstants.E_CONTENT);
        Map<String,Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraSignatureId, signature.getAttribute(AccountConstants.A_ID));
        attrs.put(Provisioning.A_zimbraSignatureName, signature.getAttribute(AccountConstants.A_NAME));
        
        for (Element eContent : contents) {
            String type = eContent.getAttribute(AccountConstants.A_TYPE);
            String attr = Signature.mimeTypeToAttrName(type);
            if (attr != null) {
                attrs.put(attr, eContent.getText());
            }
        }

        return attrs;
    }
    
    public Account getAccount() throws ServiceException {
        throw ServiceException.INVALID_REQUEST("unsupported, use getAccount(Provisioning)", null);
    }
}
