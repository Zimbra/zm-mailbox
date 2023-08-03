/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.ContactGroup;
import com.zimbra.cs.mailbox.ContactMemberOfMap;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.GetContactsRequest;
import com.zimbra.soap.type.AttributeName;
import com.zimbra.soap.type.Id;

/**
 * @since May 26, 2004
 */
public final class GetContacts extends MailDocumentHandler  {

    private static final int ALL_FOLDERS = -1;

    // bug 65324
    // default max number of members to return in the response for a gal group
    static final long NO_LIMIT_MAX_MEMBERS = 0;
    private static final long DEFAULT_MAX_MEMBERS = NO_LIMIT_MAX_MEMBERS;

    protected static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.A_FOLDER };

    @Override
    protected String[] getProxiedIdPath(Element request) {
        return TARGET_FOLDER_PATH;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);
        Account mRequestedAcct = getRequestedAccount(zsc);
        Account mAuthedAcct = getAuthenticatedAccount(zsc);
        GetContactsRequest req = zsc.elementToJaxb(request);
        boolean sync = req.getSync() == null ? false : req.getSync();
        boolean derefContactGroupMember = req.getDerefGroupMember();

        String folderIdStr = req.getFolderId();
        int folderId = ALL_FOLDERS;
        if (folderIdStr != null) {
            ItemId iidFolder = new ItemId(folderIdStr, zsc);
            if (iidFolder.belongsTo(mbox))
                folderId = iidFolder.getId();
            else
                throw ServiceException.FAILURE("Got remote folderId: " + folderIdStr + " but did not proxy", null);
        }

        SortBy sort = SortBy.of(req.getSortBy());
        if (sort == null) {
            sort = SortBy.NONE;
        }
        ArrayList<String> attrs = null;
        ArrayList<String> memberAttrs = null;
        ArrayList<ItemId> ids = null;

        //MailConstants.A_ATTRIBUTE_NAME
        List<AttributeName> reqAttrs = req.getAttributes();
        if(reqAttrs != null && reqAttrs.size() > 0) {
            attrs = new ArrayList<String>();
            for(AttributeName attrName : reqAttrs) {
                attrs.add(attrName.getName());
            }
        }

        //MailConstants.E_CONTACT_GROUP_MEMBER_ATTRIBUTE
        reqAttrs = req.getMemberAttributes();
        if(reqAttrs != null && reqAttrs.size() > 0) {
            memberAttrs = new ArrayList<String>();
            for(AttributeName attrName : reqAttrs) {
                memberAttrs.add(attrName.getName());
            }
        }

        //MailConstants.E_CONTACT
        List<Id> contactIds = req.getContacts();
        if(contactIds != null && contactIds.size() > 0) {
            ids = new ArrayList<ItemId>();
            for(Id target : contactIds) {
                String idStr = target.getId();
                if(idStr.indexOf(",") > 0) {
                    //comma-separated IDs. TODO: deprecate this use-case
                    String[] toks = idStr.split(",");
                    for(int i=0; i < toks.length; i++) {
                        ids.add(new ItemId(toks[i], zsc));
                    }
                } else {
                    ids.add(new ItemId(idStr, zsc));
                }
            }
        }

        long maxMembers = DEFAULT_MAX_MEMBERS;
        boolean returnHiddenAttrs = false;
        if (attrs == null) {
            returnHiddenAttrs = req.getReturnHiddenAttrs();
            maxMembers = (req.getMaxMembers() == null) ? DEFAULT_MAX_MEMBERS : req.getMaxMembers();
        }

        boolean returnCertInfo = req.getReturnCertInfo();
        Map<String,Set<String>> memberOfMap = null;
        if (req.getIncludeMemberOf()) {
            memberOfMap = ContactMemberOfMap.getMemberOfMap(mbox, octxt);
        }

        Element response = zsc.createElement(MailConstants.GET_CONTACTS_RESPONSE);

        // want to return modified date only on sync-related requests
        int fields = ToXML.NOTIFY_FIELDS;
        if (sync) {
            fields |= Change.CONFLICT;
        }
        if (req.getWantImapUid()) {
            fields |= Change.IMAP_UID;
        }

        if (ids != null) {
            ArrayList<Integer> local = new ArrayList<Integer>();
            HashMap<String, StringBuffer> remote = new HashMap<String, StringBuffer>();
            partitionItems(zsc, ids, local, remote);

            if (remote.size() > 0) {
                if (folderId > 0)
                    throw ServiceException.INVALID_REQUEST("Cannot specify a folder with mixed-mailbox items", null);

                List<Element> responses = proxyRemote(request, remote, context);
                for (Element e : responses)
                    response.addNonUniqueElement(e);
            }

            if (local.size() > 0) {
                boolean migrateDlist = CreateContact.needToMigrateDlist(zsc);
                for (int id : local) {
                    Contact con = mbox.getContactById(octxt, id);
                    if (con != null && (folderId == ALL_FOLDERS || folderId == con.getFolderId())) {
                        ContactGroup contactGroup = null;
                        String migratedDlist = null;
                        if (migrateDlist) {
                            ContactGroup cg = ContactGroup.init(con, false);
                            if (cg != null) {
                                migratedDlist = cg.migrateToDlist(con.getMailbox(), octxt);
                            }
                        } else if (derefContactGroupMember) {
                            contactGroup = ContactGroup.init(con, false);
                            if (contactGroup != null) {
                                contactGroup.derefAllMembers(con.getMailbox(), octxt,
                                        zsc.getResponseProtocol());
                            }
                        }
                        ToXML.encodeContact(response, ifmt, octxt, con, contactGroup,
                                memberAttrs, false /* summary */, attrs, fields, migratedDlist,
                                returnHiddenAttrs, maxMembers, returnCertInfo,
                                ContactMemberOfMap.setOfMemberOf(zsc.getRequestedAccountId(), id, memberOfMap), mRequestedAcct, mAuthedAcct);
                    }
                }
            }
        } else {
            for (Contact con : mbox.getContactList(octxt, folderId, sort)) {
                if (con != null) {
                    ContactGroup contactGroup = null;
                    if (derefContactGroupMember) {
                        contactGroup = ContactGroup.init(con, false);
                        if (contactGroup != null) {
                            contactGroup.derefAllMembers(con.getMailbox(), octxt,
                                    zsc.getResponseProtocol());
                        }
                    }
                    ToXML.encodeContact(response, ifmt, octxt, con, contactGroup, null,
                            false /* summary */, attrs, fields, null, returnHiddenAttrs, maxMembers, returnCertInfo,
                            ContactMemberOfMap.setOfMemberOf(zsc.getRequestedAccountId(), con.getId(), memberOfMap));
                }
            }
        }
        return response;
    }


    static void partitionItems(ZimbraSoapContext lc, ArrayList<ItemId> ids, ArrayList<Integer> local,
            Map<String, StringBuffer> remote) throws ServiceException {
        Account acct = getRequestedAccount(lc);
        for (ItemId iid : ids) {
            if (iid.belongsTo(acct))
                local.add(iid.getId());
            else {
                StringBuffer sb = remote.get(iid.getAccountId());
                if (sb == null)
                    remote.put(iid.getAccountId(), new StringBuffer(iid.toString()));
                else
                    sb.append(',').append(iid.toString());
            }
        }
    }

    List<Element> proxyRemote(Element request, Map<String, StringBuffer> remote, Map<String,Object> context)
    throws ServiceException {
        List<Element> responses = new ArrayList<Element>();

        //remove all 'contact' elements from original request
        for (Element e : request.listElements(MailConstants.E_CONTACT)) {
            e.detach();
        }

        //add 'contact' elements with IDs of remote contacts
        Element cn = request.addNonUniqueElement(MailConstants.E_CONTACT);
        for (Map.Entry<String, StringBuffer> entry : remote.entrySet()) {
            cn.addAttribute(MailConstants.A_ID, entry.getValue().toString());

            Element response = proxyRequest(request, context, entry.getKey());
            for (Element e : response.listElements()) {
                responses.add(e.detach());
            }
        }

        return responses;
    }
}
