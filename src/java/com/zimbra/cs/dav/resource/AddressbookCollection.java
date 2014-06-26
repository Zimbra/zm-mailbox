/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.resource;

import java.util.ArrayList;
import java.util.Locale;

import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.calendar.cache.CtagInfo;

public class AddressbookCollection extends Collection {

    public AddressbookCollection(DavContext ctxt, Folder f) throws DavException, ServiceException {
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
        Element vcard = rp.addChild(DavElements.CardDav.E_ADDRESS_DATA);
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
        if (f.getDefaultView() == MailItem.Type.CONTACT) {
            addResourceType(DavElements.CardDav.E_ADDRESSBOOK);
        }
        mCtag = CtagInfo.makeCtag(f);
        setProperty(DavElements.E_GETCTAG, mCtag);
    }

    private final String mCtag;

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

    /**
     * Filter out non-contact related children.  Apple Mac OS X Mavericks Contacts doesn't cope well with them.
     */
    @Override
    public java.util.Collection<DavResource> getChildren(DavContext ctxt) throws DavException {
        ArrayList<DavResource> children = new ArrayList<DavResource>();
        java.util.Collection<DavResource> allChildren = super.getChildren(ctxt);
        for (DavResource child: allChildren) {
            if (child.isCollection() || child instanceof AddressObject) {
                children.add(child);
            } else {
                ZimbraLog.dav.debug("Ignoring non-address resource %s (class %s) AddressbookCollection child",
                        child.getUri(), child.getClass().getName());
            }
        }
        return children;
    }

}
