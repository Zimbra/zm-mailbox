/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on Aug 30, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author dkarp
 */
public class FolderAction extends ItemAction {

    protected String[] getProxiedIdPath(Element request) {
        String operation = getXPath(request, OPERATION_PATH);
        if (operation != null && FOLDER_OPS.contains(operation.toLowerCase()))
            return TARGET_ITEM_PATH;
        return super.getProxiedIdPath(request);
    }
    protected boolean checkMountpointProxy(Element request) {
        String operation = getXPath(request, OPERATION_PATH);
        // grant/revoke ops get passed through to the referenced resource
        if (OP_GRANT.equalsIgnoreCase(operation) || OP_REVOKE.equalsIgnoreCase(operation))
            return true;
		return super.checkMountpointProxy(request);
	}

    public static final String OP_EMPTY    = "empty";
    public static final String OP_REFRESH  = "sync";
    public static final String OP_FREEBUSY = "fb";
    public static final String OP_CHECK    = "check";
    public static final String OP_UNCHECK  = '!' + OP_CHECK;
    public static final String OP_SET_URL  = "url";
    public static final String OP_IMPORT   = "import";
    public static final String OP_GRANT    = "grant";
    public static final String OP_REVOKE   = '!' + OP_GRANT;
    public static final String OP_UNFLAG   = '!' + OP_FLAG;
    public static final String OP_UNTAG    = '!' + OP_TAG;
    public static final String OP_SYNCON   = "syncon";
    public static final String OP_SYNCOFF  = '!' + OP_SYNCON;

    private static final Set<String> FOLDER_OPS = new HashSet<String>(Arrays.asList(new String[] {
        OP_EMPTY, OP_REFRESH, OP_SET_URL, OP_IMPORT, OP_FREEBUSY, OP_CHECK, OP_UNCHECK, OP_GRANT, OP_REVOKE, OP_UPDATE, OP_SYNCON, OP_SYNCOFF
    }));

	public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        Element action = request.getElement(MailConstants.E_ACTION);
        String operation = action.getAttribute(MailConstants.A_OPERATION).toLowerCase();

        Element response = zsc.createElement(MailConstants.FOLDER_ACTION_RESPONSE);
        Element result = response.addUniqueElement(MailConstants.E_ACTION);

        if (operation.equals(OP_TAG) || operation.equals(OP_FLAG) || operation.equals(OP_UNTAG) || operation.equals(OP_UNFLAG))
            throw ServiceException.INVALID_REQUEST("cannot tag/flag a folder", null);
        if (operation.endsWith(OP_COPY) || operation.endsWith(OP_SPAM))
            throw ServiceException.INVALID_REQUEST("invalid operation on folder: " + operation, null);
        String successes;
        if (FOLDER_OPS.contains(operation))
            successes = handleFolder(context, request, operation, result);
        else
            successes = handleCommon(context, request, operation, MailItem.TYPE_FOLDER);

        result.addAttribute(MailConstants.A_ID, successes);
        result.addAttribute(MailConstants.A_OPERATION, operation);
        return response;
	}

    private String handleFolder(Map<String,Object> context, Element request, String operation, Element result)
    throws ServiceException {
        Element action = request.getElement(MailConstants.E_ACTION);

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);
        ItemId iid = new ItemId(action.getAttribute(MailConstants.A_ID), zsc);

        if (operation.equals(OP_EMPTY)) {
            boolean subfolders = action.getAttributeBool(MailConstants.A_RECURSIVE, true);
            mbox.emptyFolder(octxt, iid.getId(), subfolders);
            // empty trash means also to purge all IMAP \Deleted messages
            if (iid.getId() == Mailbox.ID_FOLDER_TRASH)
                mbox.purgeImapDeleted(octxt);
        } else if (operation.equals(OP_REFRESH)) {
            mbox.synchronizeFolder(octxt, iid.getId());
        } else if (operation.equals(OP_IMPORT)) {
            String url = action.getAttribute(MailConstants.A_URL);
            mbox.importFeed(octxt, iid.getId(), url, false);
        } else if (operation.equals(OP_FREEBUSY)) {
            boolean fb = action.getAttributeBool(MailConstants.A_EXCLUDE_FREEBUSY, false);
            mbox.alterTag(octxt, iid.getId(), MailItem.TYPE_FOLDER, Flag.ID_FLAG_EXCLUDE_FREEBUSY, fb);
        } else if (operation.equals(OP_CHECK) || operation.equals(OP_UNCHECK)) {
            mbox.alterTag(octxt, iid.getId(), MailItem.TYPE_FOLDER, Flag.ID_FLAG_CHECKED, operation.equals(OP_CHECK));
        } else if (operation.equals(OP_SET_URL)) {
            String url = action.getAttribute(MailConstants.A_URL, "");
            mbox.setFolderUrl(octxt, iid.getId(), url);
            if (!url.equals(""))
                mbox.synchronizeFolder(octxt, iid.getId());

            if (action.getAttribute(MailConstants.A_EXCLUDE_FREEBUSY, null) != null) {
                boolean fb = action.getAttributeBool(MailConstants.A_EXCLUDE_FREEBUSY, false);
                mbox.alterTag(octxt, iid.getId(), MailItem.TYPE_FOLDER, Flag.ID_FLAG_EXCLUDE_FREEBUSY, fb);
            }
        } else if (operation.equals(OP_REVOKE)) {
        	String zid = action.getAttribute(MailConstants.A_ZIMBRA_ID);
            mbox.revokeAccess(octxt, iid.getId(), zid);
        } else if (operation.equals(OP_GRANT)) {
            Element grant = action.getElement(MailConstants.E_GRANT);
            short rights = ACL.stringToRights(grant.getAttribute(MailConstants.A_RIGHTS));
            byte gtype   = stringToType(grant.getAttribute(MailConstants.A_GRANT_TYPE));
            String zid   = grant.getAttribute(MailConstants.A_ZIMBRA_ID, null);
            String password = null;
            NamedEntry nentry = null;
            if (gtype == ACL.GRANTEE_AUTHUSER) {
                zid = ACL.GUID_AUTHUSER;
            } else if (gtype == ACL.GRANTEE_PUBLIC) {
                zid = ACL.GUID_PUBLIC;
            } else if (gtype == ACL.GRANTEE_GUEST) {
                zid = grant.getAttribute(MailConstants.A_DISPLAY);
                if (zid == null || zid.indexOf('@') < 0)
                	throw ServiceException.INVALID_REQUEST("invalid guest id or password", null);
                // make sure they didn't accidentally specify "guest" instead of "usr"
                try {
                    nentry = lookupGranteeByName(zid, ACL.GRANTEE_USER, zsc);
                    zid = nentry.getId();
                    gtype = nentry instanceof DistributionList ? ACL.GRANTEE_GROUP : ACL.GRANTEE_USER;
                } catch (ServiceException e) {
                    // this is the normal path, where lookupGranteeByName throws account.NO_SUCH_USER
                    password = grant.getAttribute(MailConstants.A_ARGS);
                }
            } else if (zid != null) {
            	nentry = lookupGranteeByZimbraId(zid, gtype);
            } else {
            	nentry = lookupGranteeByName(grant.getAttribute(MailConstants.A_DISPLAY), gtype, zsc);
            	zid = nentry.getId();
                // make sure they didn't accidentally specify "usr" instead of "grp"
            	if (gtype == ACL.GRANTEE_USER && nentry instanceof DistributionList)
            		gtype = ACL.GRANTEE_GROUP;
            }
            
            mbox.grantAccess(octxt, iid.getId(), zid, gtype, rights, password);
            // kinda hacky -- return the zimbra id and name of the grantee in the response
            result.addAttribute(MailConstants.A_ZIMBRA_ID, zid);
            if (nentry != null)
                result.addAttribute(MailConstants.A_DISPLAY, nentry.getName());
            else if (gtype == ACL.GRANTEE_GUEST)
                result.addAttribute(MailConstants.A_DISPLAY, zid);
        } else if (operation.equals(OP_UPDATE)) {
            // duplicating code from ItemAction.java for now...
            String newName = action.getAttribute(MailConstants.A_NAME, null);
            String folderId = action.getAttribute(MailConstants.A_FOLDER, null);
            ItemId iidFolder = new ItemId(folderId == null ? "-1" : folderId, zsc);
            if (!iidFolder.belongsTo(mbox))
                throw ServiceException.INVALID_REQUEST("cannot move folder between mailboxes", null);
            else if (folderId != null && iidFolder.getId() <= 0)
                throw MailServiceException.NO_SUCH_FOLDER(iidFolder.getId());
            String flags = action.getAttribute(MailConstants.A_FLAGS, null);
            byte color = (byte) action.getAttributeLong(MailConstants.A_COLOR, -1);
            ACL acl = parseACL(action.getOptionalElement(MailConstants.E_ACL));

            if (color >= 0)
                mbox.setColor(octxt, iid.getId(), MailItem.TYPE_FOLDER, color);
            if (acl != null)
                mbox.setPermissions(octxt, iid.getId(), acl);
            if (flags != null)
                mbox.setTags(octxt, iid.getId(), MailItem.TYPE_FOLDER, flags, null, null);
            if (newName != null)
                mbox.rename(octxt, iid.getId(), MailItem.TYPE_FOLDER, newName, iidFolder.getId());
            else if (iidFolder.getId() > 0)
                mbox.move(octxt, iid.getId(), MailItem.TYPE_FOLDER, iidFolder.getId(), null);
        } else if (operation.equals(OP_SYNCON) || operation.equals(OP_SYNCOFF)) {
            mbox.alterTag(octxt, iid.getId(), MailItem.TYPE_FOLDER, Flag.ID_FLAG_SYNC, operation.equals(OP_SYNCON));
        } else {
            throw ServiceException.INVALID_REQUEST("unknown operation: " + operation, null);
        }

        return ifmt.formatItemId(iid);
    }

    static ACL parseACL(Element eAcl) throws ServiceException {
        if (eAcl == null)
            return null;

        ACL acl = new ACL();
        for (Element grant : eAcl.listElements(MailConstants.E_GRANT)) {
            String zid   = grant.getAttribute(MailConstants.A_ZIMBRA_ID);
            byte gtype   = stringToType(grant.getAttribute(MailConstants.A_GRANT_TYPE));
            short rights = ACL.stringToRights(grant.getAttribute(MailConstants.A_RIGHTS));
            String args   = grant.getAttribute(MailConstants.A_ARGS, null);
            acl.grantAccess(zid, gtype, rights, args);
        }
        return acl;
    }

    public static byte stringToType(String typeStr) throws ServiceException {
        if (typeStr.equalsIgnoreCase("usr"))  return ACL.GRANTEE_USER;
        if (typeStr.equalsIgnoreCase("grp"))  return ACL.GRANTEE_GROUP;
        if (typeStr.equalsIgnoreCase("cos"))  return ACL.GRANTEE_COS;
        if (typeStr.equalsIgnoreCase("dom"))  return ACL.GRANTEE_DOMAIN;
        if (typeStr.equalsIgnoreCase("all"))  return ACL.GRANTEE_AUTHUSER;
        if (typeStr.equalsIgnoreCase("pub"))  return ACL.GRANTEE_PUBLIC;
        if (typeStr.equalsIgnoreCase("guest")) return ACL.GRANTEE_GUEST;
        throw ServiceException.INVALID_REQUEST("unknown grantee type: " + typeStr, null);
    }

    static String typeToString(byte type) {
        if (type == ACL.GRANTEE_USER)      return "usr";
        if (type == ACL.GRANTEE_GROUP)     return "grp";
        if (type == ACL.GRANTEE_PUBLIC)    return "pub";
        if (type == ACL.GRANTEE_AUTHUSER)  return "all";
        if (type == ACL.GRANTEE_COS)       return "cos";
        if (type == ACL.GRANTEE_DOMAIN)    return "dom";
        if (type == ACL.GRANTEE_GUEST)     return "guest";
        return null;
    }

    public static NamedEntry lookupEmailAddress(String name) throws ServiceException {
    	NamedEntry nentry = null;
        Provisioning prov = Provisioning.getInstance();
        nentry = prov.get(AccountBy.name, name);
        if (nentry == null)
        	nentry = prov.get(DistributionListBy.name, name);
        return nentry;
    }
    
    static NamedEntry lookupGranteeByName(String name, byte type, ZimbraSoapContext zsc) throws ServiceException {
        if (type == ACL.GRANTEE_AUTHUSER || type == ACL.GRANTEE_PUBLIC || type == ACL.GRANTEE_GUEST)
            return null;

        Provisioning prov = Provisioning.getInstance();
        // for addresses, default to the authenticated user's domain
        if ((type == ACL.GRANTEE_USER || type == ACL.GRANTEE_GROUP) && name.indexOf('@') == -1) {
            Account authacct = prov.get(AccountBy.id, zsc.getAuthtokenAccountId(), zsc.getAuthToken());
            String authname = (authacct == null ? null : authacct.getName());
            if (authacct != null)
                name += authname.substring(authname.indexOf('@'));
        }

        NamedEntry nentry = null;
        if (name != null)
            switch (type) {
                case ACL.GRANTEE_COS:     nentry = prov.get(CosBy.name, name);               break;
                case ACL.GRANTEE_DOMAIN:  nentry = prov.get(DomainBy.name, name);            break;
                case ACL.GRANTEE_USER:    nentry = lookupEmailAddress(name);                 break;
                case ACL.GRANTEE_GROUP:   nentry = prov.get(DistributionListBy.name, name);  break;
            }

        if (nentry != null)
            return nentry;
        switch (type) {
            case ACL.GRANTEE_COS:     throw AccountServiceException.NO_SUCH_COS(name);
            case ACL.GRANTEE_DOMAIN:  throw AccountServiceException.NO_SUCH_DOMAIN(name);
            case ACL.GRANTEE_USER:    throw AccountServiceException.NO_SUCH_ACCOUNT(name);
            case ACL.GRANTEE_GROUP:   throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(name);
            default:  throw ServiceException.FAILURE("LDAP entry not found for " + name + " : " + type, null);
        }
    }

    public static NamedEntry lookupGranteeByZimbraId(String zid, byte type) {
        Provisioning prov = Provisioning.getInstance();
        try {
            switch (type) {
                case ACL.GRANTEE_COS:     return prov.get(CosBy.id, zid);
                case ACL.GRANTEE_DOMAIN:  return prov.get(DomainBy.id, zid);
                case ACL.GRANTEE_USER:    return prov.get(AccountBy.id, zid);
                case ACL.GRANTEE_GROUP:   return prov.get(DistributionListBy.id, zid);
                case ACL.GRANTEE_GUEST:
                case ACL.GRANTEE_AUTHUSER:
                case ACL.GRANTEE_PUBLIC:
                default:                  return null;
            }
        } catch (ServiceException e) {
            return null;
        }
    }
}
