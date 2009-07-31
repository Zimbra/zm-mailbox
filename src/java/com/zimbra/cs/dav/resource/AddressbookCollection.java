/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.resource;

import java.util.Locale;

import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.mailbox.Folder;

public class AddressbookCollection extends MailItemResource {

    public AddressbookCollection(DavContext ctxt, Folder f) throws ServiceException {
        super(ctxt, f);
        Account acct = f.getAccount();
        Locale lc = acct.getLocale();
        String description = L10nUtil.getMessage(MsgKey.carddavAddressbookDescription, lc, acct.getAttr(Provisioning.A_displayName), f.getName());
        ResourceProperty rp = new ResourceProperty(DavElements.CardDav.E_ADDRESSBOOK_DESCRIPTION);
        rp.setMessageLocale(lc);
        rp.setStringValue(description);
        rp.setProtected(false);
        addProperty(rp);
        rp = new ResourceProperty(DavElements.CardDav.E_SUPPORTED_ADDRESS_DATA);
        Element vcard = rp.addChild(DavElements.CardDav.E_ADDRESSBOOK_DATA);
        vcard.addAttribute(DavElements.P_CONTENT_TYPE, DavProtocol.VCARD_CONTENT_TYPE);
        vcard.addAttribute(DavElements.P_VERSION, DavProtocol.VCARD_VERSION);
        rp.setProtected(true);
        addProperty(rp);
        long maxSize = Provisioning.getInstance().getLocalServer().getLongAttr(Provisioning.A_zimbraFileUploadMaxSize, -1);
        if (maxSize > 0) {
            rp = new ResourceProperty(DavElements.CardDav.E_MAX_RESOURCE_SIZE_ADDRESSBOOK);
            rp.setStringValue(Long.toString(maxSize));
            rp.setProtected(true);
            addProperty(rp);
        }
    }

    @Override
    public boolean isCollection() {
        return true;
    }
    
    private static QName[] SUPPORTED_REPORTS = {
            DavElements.CardDav.E_ADDRESSBOOK_MULTIGET,
            DavElements.CardDav.E_ADDRESSBOOK_QUERY,
            DavElements.E_ACL_PRINCIPAL_PROP_SET,
            DavElements.E_PRINCIPAL_MATCH,
            DavElements.E_PRINCIPAL_PROPERTY_SEARCH,
            DavElements.E_PRINCIPAL_SEARCH_PROPERTY_SET,
            DavElements.E_EXPAND_PROPERTY
    };

    @Override
    protected QName[] getSupportedReports() {
        return SUPPORTED_REPORTS;
    }
}
