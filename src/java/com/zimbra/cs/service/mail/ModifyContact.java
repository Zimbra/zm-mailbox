/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011 Zimbra, Inc.
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

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Contact.Attachment;
import com.zimbra.cs.mailbox.util.TagUtil;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class ModifyContact extends MailDocumentHandler  {

    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.E_CONTACT, MailConstants.A_ID };
    @Override
    protected String[] getProxiedIdPath(Element request)     { return TARGET_FOLDER_PATH; }
    @Override
    protected boolean checkMountpointProxy(Element request)  { return false; }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

        boolean replace = request.getAttributeBool(MailConstants.A_REPLACE, false);
        boolean verbose = request.getAttributeBool(MailConstants.A_VERBOSE, true);

        Element cn = request.getElement(MailConstants.E_CONTACT);
        ItemId iid = new ItemId(cn.getAttribute(MailConstants.A_ID), zsc);
        String tagsAttr = cn.getAttribute(MailConstants.A_TAG_NAMES, null);
        String[] tags = (tagsAttr == null) ? null : TagUtil.decodeTags(tagsAttr);

        Contact contact = mbox.getContactById(octxt, iid.getId());

        ParsedContact pc;
        if (replace) {
            Pair<Map<String,Object>, List<Attachment>> cdata = CreateContact.parseContact(cn, zsc, octxt, contact);
            pc = new ParsedContact(cdata.getFirst(), cdata.getSecond());
        } else {
            Pair<ParsedContact.FieldDeltaList, List<Attachment>> cdata = 
                CreateContact.parseContactMergeMode(cn, zsc, octxt, contact);
            pc = new ParsedContact(contact).modify(cdata.getFirst(), cdata.getSecond());
        }

        if (CreateContact.needToMigrateDlist(zsc)) {
            CreateContact.migrateFromDlist(pc);
        }

        mbox.modifyContact(octxt, iid.getId(), pc);
        if (tags != null) {
            mbox.setTags(octxt, iid.getId(), MailItem.Type.CONTACT, MailItem.FLAG_UNCHANGED, tags);
        }

        Contact con = mbox.getContactById(octxt, iid.getId());
        Element response = zsc.createElement(MailConstants.MODIFY_CONTACT_RESPONSE);
        if (con != null) {
            if (verbose)
                ToXML.encodeContact(response, ifmt, octxt, con, true, null);
            else
                response.addElement(MailConstants.E_CONTACT).addAttribute(MailConstants.A_ID, con.getId());
        }
        return response;
    }

    static Map<String, String> parseFields(List<Element> elist) throws ServiceException {
        if (elist == null || elist.isEmpty())
            return null;

        HashMap<String, String> attrs = new HashMap<String, String>();
        for (Element e : elist) {
            String name = e.getAttribute(MailConstants.A_ATTRIBUTE_NAME);
            attrs.put(name, e.getText());
        }
        return attrs;
    }
}
