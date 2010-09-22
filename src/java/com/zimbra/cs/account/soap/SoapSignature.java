/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.soap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Signature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SoapSignature extends Signature implements SoapEntry {
    
    SoapSignature(Account acct, String name, String id, Map<String, Object> attrs, Provisioning prov) {
        super(acct, name, id, attrs, prov);
    }

    SoapSignature(Account acct, Element e, Provisioning prov) throws ServiceException {
        super(acct, e.getAttribute(AccountConstants.A_NAME), e.getAttribute(AccountConstants.A_ID), fromXML(e), prov);
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
            else if (attr.equals(Provisioning.A_zimbraPrefMailSignatureContactId) && !StringUtil.isNullOrEmpty(value))
                signature.addElement(AccountConstants.E_CONTACT_ID).setText(value);
            else {
                String mimeType = Signature.attrNameToMimeType(attr);
                if (mimeType == null)
                    throw ZClientException.CLIENT_ERROR("unable to determine mime type from attr " + attr, null);
                
                signature.addElement(AccountConstants.E_CONTENT).addAttribute(AccountConstants.A_TYPE, mimeType).addText(value);
            }
        }
    }
    
    private static Map<String, Object> fromXML(Element signature) throws ServiceException {
        List<Element> contents = signature.listElements(AccountConstants.E_CONTENT);
        Map<String,Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraSignatureId, signature.getAttribute(AccountConstants.A_ID));
        attrs.put(Provisioning.A_zimbraSignatureName, signature.getAttribute(AccountConstants.A_NAME));
        
        Element eContactId = signature.getOptionalElement(AccountConstants.E_CONTACT_ID);
        if (eContactId != null)
            attrs.put(Provisioning.A_zimbraPrefMailSignatureContactId, eContactId.getText());
        
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
