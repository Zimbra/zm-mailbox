/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.soap.ZimbraSoapContext;
import org.dom4j.QName;

public class GetFilterRules extends MailDocumentHandler {

    public Element handle(Element document, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("cannot access account");

        Element response = zsc.createElement(getResponseElementName());
        Element rules = getRulesAsXML(account, response.getFactory());
        response.addElement(rules);
        return response;
    }

    protected QName getResponseElementName() {
        return MailConstants.GET_FILTER_RULES_RESPONSE;
    }

    protected Element getRulesAsXML(Account account, Element.ElementFactory elementFactory) throws ServiceException {
        return RuleManager.getIncomingRulesAsXML(elementFactory, account, true);
    }
}
