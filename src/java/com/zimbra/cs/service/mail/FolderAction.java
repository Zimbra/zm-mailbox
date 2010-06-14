/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
 * Created on Aug 30, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.SearchOptions;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Mailbox.FolderNode;
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
        if (OP_GRANT.equalsIgnoreCase(operation) || OP_REVOKE.equalsIgnoreCase(operation) ||
            OP_REVOKEORPHANGRANTS.equalsIgnoreCase(operation))
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
    public static final String OP_REVOKEORPHANGRANTS   = "revokeorphangrants";
    public static final String OP_UNFLAG   = '!' + OP_FLAG;
    public static final String OP_UNTAG    = '!' + OP_TAG;
    public static final String OP_SYNCON   = "syncon";
    public static final String OP_SYNCOFF  = '!' + OP_SYNCON;

    private static final Set<String> FOLDER_OPS = new HashSet<String>(Arrays.asList(new String[] {
        OP_EMPTY, OP_REFRESH, OP_SET_URL, OP_IMPORT, OP_FREEBUSY, OP_CHECK, OP_UNCHECK, OP_GRANT, OP_REVOKE, OP_REVOKEORPHANGRANTS, OP_UPDATE, OP_SYNCON, OP_SYNCOFF
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
            byte gtype   = ACL.stringToType(grant.getAttribute(MailConstants.A_GRANT_TYPE));
            String zid   = grant.getAttribute(MailConstants.A_ZIMBRA_ID, null);
            String secret = null;
            NamedEntry nentry = null;
            if (gtype == ACL.GRANTEE_AUTHUSER) {
                zid = GuestAccount.GUID_AUTHUSER;
            } else if (gtype == ACL.GRANTEE_PUBLIC) {
                zid = GuestAccount.GUID_PUBLIC;
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
                    secret = grant.getAttribute(MailConstants.A_ARGS, null);
                    // bug 30891 for 5.0.x
                    if (secret == null)
                        secret = grant.getAttribute(MailConstants.A_PASSWORD);
                }
            } else if (gtype == ACL.GRANTEE_KEY) {
                zid = grant.getAttribute(MailConstants.A_DISPLAY);
                // unlike guest, we do not require the display name to be an email address
                /*
                if (zid == null || zid.indexOf('@') < 0)
                    throw ServiceException.INVALID_REQUEST("invalid guest id or key", null);
                */    
                // unlike guest, we do not fixup grantee type for key grantees if they specify an internal user
                
                // get the optional accesskey
                secret = grant.getAttribute(MailConstants.A_ACCESSKEY, null);
                
            } else if (zid != null) {
            	nentry = lookupGranteeByZimbraId(zid, gtype);
            } else {
            	nentry = lookupGranteeByName(grant.getAttribute(MailConstants.A_DISPLAY), gtype, zsc);
            	zid = nentry.getId();
                // make sure they didn't accidentally specify "usr" instead of "grp"
            	if (gtype == ACL.GRANTEE_USER && nentry instanceof DistributionList)
            		gtype = ACL.GRANTEE_GROUP;
            }
            
            ACL.Grant g =  mbox.grantAccess(octxt, iid.getId(), zid, gtype, rights, secret);
            
            // kinda hacky -- return the zimbra id and name of the grantee in the response
            result.addAttribute(MailConstants.A_ZIMBRA_ID, zid);
            if (nentry != null)
                result.addAttribute(MailConstants.A_DISPLAY, nentry.getName());
            else if (gtype == ACL.GRANTEE_GUEST || gtype == ACL.GRANTEE_KEY)
                result.addAttribute(MailConstants.A_DISPLAY, zid);
            
            if (gtype == ACL.GRANTEE_KEY)
                result.addAttribute(MailConstants.A_ACCESSKEY, g.getPassword());
       
        } else if (operation.equals(OP_REVOKEORPHANGRANTS)) {
            String zid = action.getAttribute(MailConstants.A_ZIMBRA_ID);
            byte gtype = ACL.stringToType(action.getAttribute(MailConstants.A_GRANT_TYPE));
            
            revokeOrphanGrants(octxt, mbox, iid, zid, gtype);
            
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
            byte gtype   = ACL.stringToType(grant.getAttribute(MailConstants.A_GRANT_TYPE));
            short rights = ACL.stringToRights(grant.getAttribute(MailConstants.A_RIGHTS));
            
            String secret = null;
            if (gtype == ACL.GRANTEE_KEY)
                secret = grant.getAttribute(MailConstants.A_ACCESSKEY, null);
            else if (gtype == ACL.GRANTEE_GUEST) {
                secret = grant.getAttribute(MailConstants.A_ARGS, null);
                // bug 30891 for 5.0.x
                if (secret == null)
                    secret = grant.getAttribute(MailConstants.A_PASSWORD);
            }
            acl.grantAccess(zid, gtype, rights, secret);
        }
        return acl;
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
        if (type == ACL.GRANTEE_AUTHUSER || type == ACL.GRANTEE_PUBLIC || type == ACL.GRANTEE_GUEST || type == ACL.GRANTEE_KEY)
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
                case ACL.GRANTEE_KEY:
                case ACL.GRANTEE_AUTHUSER:
                case ACL.GRANTEE_PUBLIC:
                default:                  return null;
            }
        } catch (ServiceException e) {
            return null;
        }
    }
    
    
    private void revokeOrphanGrants(OperationContext octxt, Mailbox mbox, ItemId iid,
            String granteeId, byte gtype) throws ServiceException {
        
        //
        // check if the grantee still exists
        //
        
        int flags = 0;
        if (gtype == ACL.GRANTEE_USER)
            flags |= (Provisioning.SA_ACCOUNT_FLAG | Provisioning.SA_CALENDAR_RESOURCE_FLAG) ;
        else if (gtype == ACL.GRANTEE_GROUP)
            flags |= Provisioning.SA_DISTRIBUTION_LIST_FLAG;
        else if (gtype == ACL.GRANTEE_COS)
            flags |= Provisioning.SD_COS_FLAG;
        else if (gtype == ACL.GRANTEE_DOMAIN)
            flags |= Provisioning.SA_DOMAIN_FLAG;
        else
            throw ServiceException.INVALID_REQUEST("invalid grantee type for revokeOrphanGrants", null);
            
        String query = "(" + Provisioning.A_zimbraId + "=" + granteeId + ")";    
        
        Provisioning.SearchOptions opts = new SearchOptions();
        opts.setFlags(flags);
        opts.setQuery(query);  
        opts.setOnMaster(true);  // search the grantee on LDAP master
        
        Provisioning prov = Provisioning.getInstance();
        List<NamedEntry> entries = prov.searchDirectory(opts);

        if (entries.size() != 0)
            throw ServiceException.INVALID_REQUEST("grantee " + granteeId + " exists", null);
        
        // the grantee indeed does not exist, revoke all grants granted to the grantee
        // in this folder and all subfolders
        FolderNode rootNode = mbox.getFolderTree(octxt, iid, true);
        revokeOrphanGrants(octxt, mbox, rootNode, granteeId, gtype);
    }
    
    private void revokeOrphanGrants(OperationContext octxt, Mailbox mbox,
            FolderNode node, String granteeId, byte gtype) throws ServiceException {
        if (node.mFolder != null) {
            
            // skip this folder if the authed user does not have admin right
            // we still want to proceed to subfolders because the authed user 
            // may have admin right on subfolders
            //
            // e.g.   folder1 (a)
            //             folder2 (rw)
            //                 folder3 (a)
            //
            //        if there are orphan grants on all folder1, folder2, folder3,
            //        we will revoke the orphan grants on folder1 and folder3 only, not folder2.
            
            boolean canAdmin = (mbox.getEffectivePermissions(octxt, node.mFolder.getId(), MailItem.TYPE_FOLDER) & ACL.RIGHT_ADMIN) != 0;
            
            if (canAdmin) {
                ACL acl = node.mFolder.getACL(); // or getEffectiveACL?
                if (acl != null) {
                    for (ACL.Grant grant : acl.getGrants()) {
                        if (granteeId.equals(grant.getGranteeId()) && gtype == grant.getGranteeType()) {
                            mbox.revokeAccess(octxt, node.mFolder.getId(), granteeId);
                            // break out of the loop since there can be only one grant for the same grantee on a folder
                            break; 
                        }
                    }
                }
            }
        }
        
        for (FolderNode subNode : node.mSubfolders)
            revokeOrphanGrants(octxt, mbox, subNode, granteeId, gtype);
    }
}
