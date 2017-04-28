/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Collection;
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
import com.zimbra.cs.mailbox.ContactGroup;
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
        boolean wantImapUid = request.getAttributeBool(MailConstants.A_WANT_IMAP_UID, true);

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
            pc = CreateContact.parseContactMergeMode(cn, zsc, octxt, contact);
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
            if (verbose) {
                ToXML.encodeContact(response, ifmt, octxt, con,
                        (ContactGroup)null, (Collection<String>)null /* memberAttrFilter */, true /* summary */,
                        (Collection<String>)null /* attrFilter */, ToXML.NOTIFY_FIELDS, null,
                        false /* returnHiddenAttrs */,
                        GetContacts.NO_LIMIT_MAX_MEMBERS, true /* returnCertInfo */, wantImapUid);
            } else {
                Element contct = response.addNonUniqueElement(MailConstants.E_CONTACT);
                contct.addAttribute(MailConstants.A_ID, con.getId());
                if (wantImapUid) {
                    contct.addAttribute(MailConstants.A_IMAP_UID, con.getImapUid());
                }
            }
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
