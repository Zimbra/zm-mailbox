/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * 
 */
public class SoapLogger extends AdminDocumentHandler {
    
    static enum Op {
        ADD, REMOVE, CLEAR, LIST; 
    }

    /* @see com.zimbra.soap.DocumentHandler#handle(com.zimbra.common.soap.Element, java.util.Map) */
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Element response = lc.createElement(AdminConstants.SOAP_LOGGER_RESPONSE);
        
        String opStr = request.getAttribute("op");
        Op op = Op.valueOf(opStr.toUpperCase());
        
        String acctStr = request.getAttribute(AdminConstants.A_ACCOUNTID, null);
        String nameStr = request.getAttribute(AdminConstants.A_NAME, null);
        
        switch (op) {
            case ADD:
                if (acctStr == null && nameStr == null) 
                    throw ServiceException.INVALID_REQUEST("Missing required argument: "+AdminConstants.A_ACCOUNTID, null);
                if (nameStr != null)
                    acctStr = Provisioning.getInstance().get(AccountBy.name, nameStr).getId();
                SoapEngine.acctsToLog().add(acctStr);
                break;
            case REMOVE:
                if (acctStr == null && nameStr == null) 
                    throw ServiceException.INVALID_REQUEST("Missing required argument: "+AdminConstants.A_ACCOUNTID, null);
                if (nameStr != null)
                    acctStr = Provisioning.getInstance().get(AccountBy.name, nameStr).getId();
                SoapEngine.acctsToLog().remove(acctStr);
                break;
            case CLEAR:
                SoapEngine.acctsToLog().clear();
                break;
            case LIST:
                for (String acct : SoapEngine.acctsToLog()) {
                    Element e = response.addElement(AdminConstants.E_ACCOUNT);
                    e.addAttribute(AdminConstants.A_ACCOUNTID, acct);
                    String name = Provisioning.getInstance().get(AccountBy.id, acct).getName();
                    e.addAttribute(AdminConstants.A_NAME, name);
                }
                break;
        }
        
        return response;
    }

}
