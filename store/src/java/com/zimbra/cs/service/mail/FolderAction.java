/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016, 2022 Synacor, Inc.
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
 * Created on Aug 30, 2004
 */
package com.zimbra.cs.service.mail;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.FolderConstants;
import com.zimbra.common.mime.InternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.MailTarget;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SearchDirectoryOptions;
import com.zimbra.cs.fb.FreeBusyProvider;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.FolderNode;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.type.RetentionPolicy;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class FolderAction extends ItemAction {

    @Override protected String[] getProxiedIdPath(Element request) {
        String operation = getXPath(request, OPERATION_PATH);
        if (operation != null && FOLDER_OPS.contains(operation.toLowerCase()))
            return TARGET_ITEM_PATH;
        return super.getProxiedIdPath(request);
    }
    @Override protected boolean checkMountpointProxy(Element request) {
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
    public static final String OP_RETENTIONPOLICY = "retentionpolicy";
    public static final String OP_DISABLE_ACTIVESYNC = "disableactivesync";
    public static final String OP_ENABLE_ACTIVESYNC = '!' + OP_DISABLE_ACTIVESYNC;
    public static final String OP_WEBOFFLINESYNCDAYS = "webofflinesyncdays";


    private static final Set<String> FOLDER_OPS = ImmutableSet.of(
        OP_EMPTY, OP_REFRESH, OP_SET_URL, OP_IMPORT, OP_FREEBUSY, OP_CHECK, OP_UNCHECK, OP_GRANT,
        OP_REVOKE, OP_REVOKEORPHANGRANTS, OP_UPDATE, OP_SYNCON, OP_SYNCOFF, OP_RETENTIONPOLICY,
        OP_DISABLE_ACTIVESYNC, OP_ENABLE_ACTIVESYNC, OP_WEBOFFLINESYNCDAYS
    );

    @Override public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        Element action = request.getElement(MailConstants.E_ACTION);
        String operation = action.getAttribute(MailConstants.A_OPERATION).toLowerCase();

        Element response = zsc.createElement(MailConstants.FOLDER_ACTION_RESPONSE);
        Element result = response.addUniqueElement(MailConstants.E_ACTION);

        if (operation.equals(OP_TAG) || operation.equals(OP_FLAG) || operation.equals(OP_UNTAG) || operation.equals(OP_UNFLAG)) {
            throw ServiceException.INVALID_REQUEST("cannot tag/flag a folder", null);
        } else if (operation.endsWith(OP_COPY) || operation.endsWith(OP_SPAM)) {
            throw ServiceException.INVALID_REQUEST("invalid operation on folder: " + operation, null);
        }

        String successes;
        if (FOLDER_OPS.contains(operation)) {
            successes = handleFolder(context, request, operation, result);
        } else {
            successes = Joiner.on(",").join(handleCommon(context, request, MailItem.Type.FOLDER).getSuccessIds());
        }
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
            mbox.alterTag(octxt, iid.getId(), MailItem.Type.FOLDER, Flag.FlagInfo.EXCLUDE_FREEBUSY, fb, null);
            FreeBusyProvider.mailboxChanged(zsc.getRequestedAccountId());
        } else if (operation.equals(OP_CHECK) || operation.equals(OP_UNCHECK)) {
            mbox.alterTag(octxt, iid.getId(), MailItem.Type.FOLDER, Flag.FlagInfo.CHECKED, operation.equals(OP_CHECK), null);
        } else if (operation.equals(OP_SET_URL)) {
            String url = action.getAttribute(MailConstants.A_URL, "");
            mbox.setFolderUrl(octxt, iid.getId(), url);
            if (!url.equals("")) {
                mbox.synchronizeFolder(octxt, iid.getId());
            }
            if (action.getAttribute(MailConstants.A_EXCLUDE_FREEBUSY, null) != null) {
                boolean fb = action.getAttributeBool(MailConstants.A_EXCLUDE_FREEBUSY, false);
                mbox.alterTag(octxt, iid.getId(), MailItem.Type.FOLDER, Flag.FlagInfo.EXCLUDE_FREEBUSY, fb, null);
            }
        } else if (operation.equals(OP_REVOKE)) {
            String zid = action.getAttribute(MailConstants.A_ZIMBRA_ID);
            mbox.revokeAccess(octxt, iid.getId(), zid);
        } else if (operation.equals(OP_GRANT)) {
            if (!LC.zimbra_root_folder_sharing_enabled.booleanValue() && iid != null
                    && (iid.getId() == FolderConstants.ID_FOLDER_USER_ROOT || iid.getId() == FolderConstants.ID_FOLDER_ROOT)) {
                throw ServiceException.INVALID_REQUEST("root folder sharing disabled", null);
            }
            Element grant = action.getElement(MailConstants.E_GRANT);
            short rights = ACL.stringToRights(grant.getAttribute(MailConstants.A_RIGHTS));
            byte gtype   = ACL.stringToType(grant.getAttribute(MailConstants.A_GRANT_TYPE));
            String zid   = grant.getAttribute(MailConstants.A_ZIMBRA_ID, null);
            long expiry  = grant.getAttributeLong(MailConstants.A_EXPIRY, 0);
            String secret = null;
            NamedEntry nentry = null;
            if (gtype == ACL.GRANTEE_AUTHUSER) {
                zid = GuestAccount.GUID_AUTHUSER;
            } else if (gtype == ACL.GRANTEE_PUBLIC) {
                zid = GuestAccount.GUID_PUBLIC;
                expiry = validateGrantExpiry(grant.getAttribute(MailConstants.A_EXPIRY, null),
                        AccountUtil.getMaxPublicShareLifetime(mbox.getAccount(), mbox.getFolderById(octxt, iid.getId()).getDefaultView()));
            } else if (gtype == ACL.GRANTEE_GUEST) {
                zid = grant.getAttribute(MailConstants.A_DISPLAY);
                if (zid == null || zid.indexOf('@') < 0)
                    throw ServiceException.INVALID_REQUEST("invalid guest id or password", null);
                // first make sure they didn't accidentally specify "guest" instead of "usr"
                boolean guestGrantee = true;
                try {
                    nentry = lookupGranteeByName(zid, ACL.GRANTEE_USER, zsc);
                    if (nentry instanceof MailTarget) {
                        Domain domain = Provisioning.getInstance().getDomain(mbox.getAccount());
                        String granteeDomainName = ((MailTarget) nentry).getDomainName();
                        if (domain.isInternalSharingCrossDomainEnabled() ||
                                domain.getName().equals(granteeDomainName) ||
                                Sets.newHashSet(domain.getInternalSharingDomain()).contains(granteeDomainName)) {
                            guestGrantee = false;
                            zid = nentry.getId();
                            gtype = nentry instanceof Group ? ACL.GRANTEE_GROUP : ACL.GRANTEE_USER;
                        }
                    }
                } catch (ServiceException e) {
                    // this is the normal path, where lookupGranteeByName throws account.NO_SUCH_USER
                }
                if (guestGrantee) {
                    secret = grant.getAttribute(MailConstants.A_ARGS, null);
                    // password is no longer required for external sharing
                    if (secret == null) {
                        secret = grant.getAttribute(MailConstants.A_PASSWORD, null);
                    }
                }
            } else if (gtype == ACL.GRANTEE_KEY) {
                zid = grant.getAttribute(MailConstants.A_DISPLAY);
                // unlike guest, we do not require the display name to be an email address
                // unlike guest, we do not fixup grantee type for key grantees if they specify an internal user
                // get the optional accesskey
                secret = grant.getAttribute(MailConstants.A_ACCESSKEY, null);
            } else if (zid != null) {
                nentry = lookupGranteeByZimbraId(zid, gtype);
            } else {
                try {
                    nentry = lookupGranteeByName(grant.getAttribute(MailConstants.A_DISPLAY), gtype, zsc);
                    zid = nentry.getId();
                    // make sure they didn't accidentally specify "usr" instead of "grp"
                    if (gtype == ACL.GRANTEE_USER && nentry instanceof Group) {
                        gtype = ACL.GRANTEE_GROUP;
                    }
                } catch (ServiceException e) {
                    if (AccountServiceException.NO_SUCH_ACCOUNT.equals(e.getCode())) {
                        // looks like the case of an internal user not provisioned yet
                        // we'll treat it as external sharing
                        gtype = ACL.GRANTEE_GUEST;
                        zid = grant.getAttribute(MailConstants.A_DISPLAY);
                    } else {
                        throw e;
                    }
                }
            }

            ACL.Grant g =  mbox.grantAccess(octxt, iid.getId(), zid, gtype, rights, secret, expiry);

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
            if (!iidFolder.belongsTo(mbox)) {
                throw ServiceException.INVALID_REQUEST("cannot move folder between mailboxes", null);
            } else if (folderId != null && iidFolder.getId() <= 0) {
                throw MailServiceException.NO_SUCH_FOLDER(iidFolder.getId());
            }
            String flags = action.getAttribute(MailConstants.A_FLAGS, null);
            byte color = (byte) action.getAttributeLong(MailConstants.A_COLOR, -1);
            String view = action.getAttribute(MailConstants.A_DEFAULT_VIEW, null);
            Element eAcl = action.getOptionalElement(MailConstants.E_ACL);
            ACL acl = null;
            if (eAcl != null) {
                acl = parseACL(eAcl,
                        view == null ? mbox.getFolderById(octxt, iid.getId()).getDefaultView() : MailItem.Type.of(view),
                        mbox.getAccount());
            }
            if (color >= 0) {
                mbox.setColor(octxt, iid.getId(), MailItem.Type.FOLDER, color);
            }
            if (acl != null) {
                mbox.setPermissions(octxt, iid.getId(), acl);
            }
            if (flags != null) {
                mbox.setTags(octxt, iid.getId(), MailItem.Type.FOLDER, Flag.toBitmask(flags), null, null);
            }
            if (view != null) {
                mbox.setFolderDefaultView(octxt, iid.getId(), MailItem.Type.of(view));
            }
            if (newName != null) {
                mbox.rename(octxt, iid.getId(), MailItem.Type.FOLDER, newName, iidFolder.getId());
            } else if (iidFolder.getId() > 0) {
                mbox.move(octxt, iid.getId(), MailItem.Type.FOLDER, iidFolder.getId(), null);
            }
        } else if (operation.equals(OP_SYNCON) || operation.equals(OP_SYNCOFF)) {
            mbox.alterTag(octxt, iid.getId(), MailItem.Type.FOLDER, Flag.FlagInfo.SYNC, operation.equals(OP_SYNCON), null);
        } else if (operation.equals(OP_RETENTIONPOLICY)) {
            mbox.setRetentionPolicy(octxt, iid.getId(), MailItem.Type.FOLDER,
                new RetentionPolicy(action.getElement(MailConstants.E_RETENTION_POLICY)));
        } else if (operation.equals(OP_DISABLE_ACTIVESYNC) || operation.equals(OP_ENABLE_ACTIVESYNC)) {
            mbox.setActiveSyncDisabled(octxt, iid.getId(), operation.equals(OP_DISABLE_ACTIVESYNC));
        } else if (operation.equals(OP_WEBOFFLINESYNCDAYS)) {
            mbox.setFolderWebOfflineSyncDays(octxt, iid.getId(),
                    action.getAttributeInt(MailConstants.A_NUM_DAYS));
        } else {
            throw ServiceException.INVALID_REQUEST("unknown operation: " + operation, null);
        }

        return ifmt.formatItemId(iid);
    }

    static ACL parseACL(Element eAcl, MailItem.Type folderType, Account account) throws ServiceException {
        if (eAcl == null)
            return null;

        long internalGrantExpiry = validateGrantExpiry(eAcl.getAttribute(MailConstants.A_INTERNAL_GRANT_EXPIRY, null),
                AccountUtil.getMaxInternalShareLifetime(account, folderType));
        long guestGrantExpiry = validateGrantExpiry(eAcl.getAttribute(MailConstants.A_GUEST_GRANT_EXPIRY, null),
                AccountUtil.getMaxExternalShareLifetime(account, folderType));
        ACL acl = new ACL(internalGrantExpiry, guestGrantExpiry);

        for (Element grant : eAcl.listElements(MailConstants.E_GRANT)) {
            String zid   = grant.getAttribute(MailConstants.A_ZIMBRA_ID);
            byte gtype   = ACL.stringToType(grant.getAttribute(MailConstants.A_GRANT_TYPE));
            short rights = ACL.stringToRights(grant.getAttribute(MailConstants.A_RIGHTS));
            long expiry = gtype == ACL.GRANTEE_PUBLIC ?
                    validateGrantExpiry(grant.getAttribute(MailConstants.A_EXPIRY, null),
                            AccountUtil.getMaxPublicShareLifetime(account, folderType)) :
                    grant.getAttributeLong(MailConstants.A_EXPIRY, 0);

            String secret = null;
            if (gtype == ACL.GRANTEE_KEY) {
                secret = grant.getAttribute(MailConstants.A_ACCESSKEY, null);
            } else if (gtype == ACL.GRANTEE_GUEST) {
                secret = grant.getAttribute(MailConstants.A_ARGS, null);
                // bug 30891 for 5.0.x
                if (secret == null) {
                    secret = grant.getAttribute(MailConstants.A_PASSWORD, null);
                }
            }
            acl.grantAccess(zid, gtype, rights, secret, expiry);
        }
        return acl;
    }

    public static NamedEntry lookupEmailAddress(String name) throws ServiceException {
        if (name.indexOf('<') > 0) {
            InternetAddress addr = new InternetAddress(name);
            name = addr.getAddress();
        }
        Provisioning prov = Provisioning.getInstance();
        NamedEntry nentry = prov.get(AccountBy.name, name);
        if (nentry == null) {
            nentry = prov.getGroup(Key.DistributionListBy.name, name);
        }
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
                case ACL.GRANTEE_COS:     nentry = prov.get(Key.CosBy.name, name);               break;
                case ACL.GRANTEE_DOMAIN:  nentry = prov.get(Key.DomainBy.name, name);            break;
                case ACL.GRANTEE_USER:    nentry = lookupEmailAddress(name);                 break;
                case ACL.GRANTEE_GROUP:   nentry = prov.getGroup(Key.DistributionListBy.name, name);  break;
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
                case ACL.GRANTEE_COS:     return prov.get(Key.CosBy.id, zid);
                case ACL.GRANTEE_DOMAIN:  return prov.get(Key.DomainBy.id, zid);
                case ACL.GRANTEE_USER:    return prov.get(AccountBy.id, zid);
                case ACL.GRANTEE_GROUP:   return prov.getGroup(Key.DistributionListBy.id, zid);
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

    private void revokeOrphanGrants(OperationContext octxt, Mailbox mbox, ItemId iid, String granteeId, byte gtype)
    throws ServiceException {
        // check if the grantee still exists
        SearchDirectoryOptions opts = new SearchDirectoryOptions();
        if (gtype == ACL.GRANTEE_USER) {
            opts.addType(SearchDirectoryOptions.ObjectType.accounts);
            opts.addType(SearchDirectoryOptions.ObjectType.resources);
        } else if (gtype == ACL.GRANTEE_GROUP) {
            opts.addType(SearchDirectoryOptions.ObjectType.distributionlists);
        } else if (gtype == ACL.GRANTEE_COS) {
            opts.addType(SearchDirectoryOptions.ObjectType.coses);
        } else if (gtype == ACL.GRANTEE_DOMAIN) {
            opts.addType(SearchDirectoryOptions.ObjectType.domains);
        } else {
            throw ServiceException.INVALID_REQUEST("invalid grantee type for revokeOrphanGrants", null);
        }

        String query = "(" + Provisioning.A_zimbraId + "=" + granteeId + ")";
        opts.setFilterString(FilterId.SEARCH_GRANTEE, query);
        opts.setOnMaster(true);  // search the grantee on LDAP master
        List<NamedEntry> entries = Provisioning.getInstance().searchDirectory(opts);

        if (entries.size() != 0) {
            throw ServiceException.INVALID_REQUEST("grantee " + granteeId + " exists", null);
        }

        // the grantee indeed does not exist, revoke all grants granted to the grantee
        // in this folder and all subfolders
        FolderNode rootNode = mbox.getFolderTree(octxt, iid, true);
        revokeOrphanGrants(octxt, mbox, rootNode, granteeId, gtype);
    }

    private void revokeOrphanGrants(OperationContext octxt, Mailbox mbox, FolderNode node, String granteeId, byte gtype)
    throws ServiceException {
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

            boolean canAdmin = (mbox.getEffectivePermissions(octxt, node.mFolder.getId(), MailItem.Type.FOLDER) & ACL.RIGHT_ADMIN) != 0;

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
