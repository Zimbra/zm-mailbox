/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.common.util.UUIDUtil;
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
import com.zimbra.cs.service.formatter.VCard;
import com.zimbra.cs.servlet.ETagHeaderFilter;

public class AddressbookCollection extends Collection {

    private static QName[] SUPPORTED_REPORTS = {
            DavElements.CardDav.E_ADDRESSBOOK_MULTIGET,
            DavElements.CardDav.E_ADDRESSBOOK_QUERY,
            DavElements.E_ACL_PRINCIPAL_PROP_SET,
            DavElements.E_PRINCIPAL_MATCH,
            DavElements.E_PRINCIPAL_PROPERTY_SEARCH,
            DavElements.E_PRINCIPAL_SEARCH_PROPERTY_SET,
            DavElements.E_EXPAND_PROPERTY
    };

    public AddressbookCollection(DavContext ctxt, Folder f) throws DavException, ServiceException {
        super(ctxt, f);
        setupAddressbookCollection(this, ctxt, f);
    }

    protected static void setupAddressbookCollection(Collection coll, DavContext ctxt, Folder f)
            throws ServiceException {
        Account acct = f.getAccount();
        Locale lc = acct.getLocale();
        String description = L10nUtil.getMessage(MsgKey.carddavAddressbookDescription,
                lc, acct.getAttr(Provisioning.A_displayName), f.getName());
        ResourceProperty rp = new ResourceProperty(DavElements.CardDav.E_ADDRESSBOOK_DESCRIPTION);
        rp.setMessageLocale(lc);
        rp.setStringValue(description);
        rp.setProtected(false);
        coll.addProperty(rp);
        coll.addProperty(ResourceProperty.AddMember.create(UrlNamespace.getFolderUrl(ctxt.getUser(),
                f.getName())));
        rp = new ResourceProperty(DavElements.CardDav.E_SUPPORTED_ADDRESS_DATA);
        Element vcard = rp.addChild(DavElements.CardDav.E_ADDRESS_DATA);
        vcard.addAttribute(DavElements.P_CONTENT_TYPE, DavProtocol.VCARD_CONTENT_TYPE);
        vcard.addAttribute(DavElements.P_VERSION, DavProtocol.VCARD_VERSION);
        rp.setProtected(true);
        coll.addProperty(rp);
        long maxSize = Provisioning.getInstance().getLocalServer().getLongAttr(
                Provisioning.A_zimbraFileUploadMaxSize, -1);
        if (maxSize > 0) {
            rp = new ResourceProperty(DavElements.CardDav.E_MAX_RESOURCE_SIZE_ADDRESSBOOK);
            rp.setStringValue(Long.toString(maxSize));
            rp.setProtected(true);
            coll.addProperty(rp);
        }
        if (f.getDefaultView() == MailItem.Type.CONTACT) {
            coll.addResourceType(DavElements.CardDav.E_ADDRESSBOOK);
        }
        coll.setProperty(DavElements.E_GETCTAG, CtagInfo.makeCtag(f));
    }

    @Override
    protected QName[] getSupportedReports() {
        return SUPPORTED_REPORTS;
    }

    @Override
    public DavResource createItem(DavContext ctxt, String name) throws DavException, IOException {
        return createVCard(ctxt, name);
    }

    @Override
    public void handlePost(DavContext ctxt) throws DavException, IOException, ServiceException {
        Provisioning prov = Provisioning.getInstance();
        DavResource rs = null;
        try {
            String user = ctxt.getUser();
            Account account = prov.get(AccountBy.name, user);
            if (account == null) {
                // Anti-account name harvesting.
                ZimbraLog.dav.info("Failing POST to Addressbook - no such account '%s'", user);
                throw new DavException("Request denied", HttpServletResponse.SC_NOT_FOUND, null);
            }

            VCard vcard = AddressObject.uploadToVCard(ctxt);
            String baseName = new StringBuilder(vcard.uid).append(AddressObject.VCARD_EXTENSION).toString();
            rs = UrlNamespace.getResourceAt(ctxt, ctxt.getUser(), relativeUrlForChild(ctxt.getUser(), baseName));
            if (rs != null) {
                // name based on uid already taken - choose another.
                baseName = new StringBuilder(UUIDUtil.generateUUID()).append(AddressObject.VCARD_EXTENSION).toString();
            }
            rs = AddressObject.create(ctxt, baseName, this, vcard, false);
            if (rs.isNewlyCreated()) {
                ctxt.getResponse().setHeader("Location", rs.getHref());
                ctxt.setStatus(HttpServletResponse.SC_CREATED);
            } else {
                ctxt.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
            if (rs.hasEtag()) {
                ctxt.getResponse().setHeader(DavProtocol.HEADER_ETAG, rs.getEtag());
                ctxt.getResponse().setHeader(ETagHeaderFilter.ZIMBRA_ETAG_HEADER, rs.getEtag());
            }
        } catch (ServiceException e) {
            if (e.getCode().equals(ServiceException.FORBIDDEN)) {
                throw new DavException(e.getMessage(), HttpServletResponse.SC_FORBIDDEN, e);
            } else {
                throw new DavException("cannot create vcard item", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
            }
        }
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
