/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.account;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.ACLUtil;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.RightModifier;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.ZimbraSoapContext;

public class GrantRights extends AccountDocumentHandler {
    
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        if (!canAccessAccount(zsc, account)) {
            throw ServiceException.PERM_DENIED("can not access account");
        }
        
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        for (Element eACE : request.listElements(AccountConstants.E_ACE)) {
            ZimbraACE ace = handleACE(eACE, zsc, true);
            aces.add(ace);
        }

        List<ZimbraACE> granted = ACLUtil.grantRight(Provisioning.getInstance(), account, aces);
        Element response = zsc.createElement(AccountConstants.GRANT_RIGHTS_RESPONSE);
        if (aces != null) {
            for (ZimbraACE ace : granted) {
                ToXML.encodeACE(response, ace);
            }
            AccountUtil.broadcastFlushCache(account);
        }

        return response;
    }
    
    /**
     * @param eACE
     * @param zsc
     * @param granting true if granting, false if revoking
     * @return
     * @throws ServiceException
     */
    static ZimbraACE handleACE(Element eACE, ZimbraSoapContext zsc, boolean granting) 
    throws ServiceException {
        /*
         * Interface and parameter checking style was modeled after FolderAction, 
         * not admin Grant/RevokeRight
         */
        Right right = RightManager.getInstance().getUserRight(eACE.getAttribute(AccountConstants.A_RIGHT));
        GranteeType gtype = GranteeType.fromCode(eACE.getAttribute(AccountConstants.A_GRANT_TYPE));
        String zid = eACE.getAttribute(AccountConstants.A_ZIMBRA_ID, null);
        boolean deny = eACE.getAttributeBool(AccountConstants.A_DENY, false);
        boolean checkGranteeType = eACE.getAttributeBool(AccountConstants.A_CHECK_GRANTEE_TYPE, false);
        String secret = null;
        NamedEntry nentry = null;
        
        if (gtype == GranteeType.GT_AUTHUSER) {
            zid = GuestAccount.GUID_AUTHUSER;
        } else if (gtype == GranteeType.GT_PUBLIC) {
            zid = GuestAccount.GUID_PUBLIC;
        } else if (gtype == GranteeType.GT_GUEST) {
            zid = eACE.getAttribute(AccountConstants.A_DISPLAY);
            if (zid == null || zid.indexOf('@') < 0)
                throw ServiceException.INVALID_REQUEST("invalid guest id or password", null);
            // make sure they didn't accidentally specify "guest" instead of "usr"
            try {
                nentry = lookupGranteeByName(zid, GranteeType.GT_USER, zsc);
                zid = nentry.getId();
                gtype = nentry instanceof DistributionList ? GranteeType.GT_GROUP : GranteeType.GT_USER;
            } catch (ServiceException e) {
                // this is the normal path, where lookupGranteeByName throws account.NO_SUCH_USER
                secret = eACE.getAttribute(AccountConstants.A_PASSWORD);
            }
        } else if (gtype == GranteeType.GT_KEY) {
            zid = eACE.getAttribute(AccountConstants.A_DISPLAY);
            // unlike guest, we do not require the display name to be an email address
            /*
            if (zid == null || zid.indexOf('@') < 0)
                throw ServiceException.INVALID_REQUEST("invalid guest id or key", null);
            */    
            // unlike guest, we do not fixup grantee type for key grantees if they specify an internal user
            
            // get the optional accesskey
            secret = eACE.getAttribute(AccountConstants.A_ACCESSKEY, null);
         
        } else if (zid != null) {
            nentry = lookupGranteeByZimbraId(zid, gtype, granting);
        } else {
            nentry = lookupGranteeByName(eACE.getAttribute(AccountConstants.A_DISPLAY), gtype, zsc);
            zid = nentry.getId();
            // make sure they didn't accidentally specify "usr" instead of "grp"
            if (gtype == GranteeType.GT_USER && nentry instanceof Group) {
                if (checkGranteeType) {
                    throw AccountServiceException.INVALID_REQUEST(eACE.getAttribute(AccountConstants.A_DISPLAY) +
                            " is not a valid grantee for grantee type '" + gtype.getCode() + "'.", null);
                } else {
                    gtype = GranteeType.GT_GROUP;
                }
            }
        }

        RightModifier rightModifier = null;
        if (deny)
            rightModifier = RightModifier.RM_DENY;
        return new ZimbraACE(zid, gtype, right, rightModifier, secret);

    }

    private static NamedEntry lookupEmailAddress(String name) throws ServiceException {
        NamedEntry nentry = null;
        Provisioning prov = Provisioning.getInstance();
        nentry = prov.get(AccountBy.name, name);
        //look for both distribution list and dynamic group
        if (nentry == null) {
            nentry = prov.getGroup(Key.DistributionListBy.name, name);
        }
        return nentry;
    }

    private static NamedEntry lookupGranteeByName(String name, GranteeType type, 
            ZimbraSoapContext zsc) 
    throws ServiceException {
        if (type == GranteeType.GT_AUTHUSER || 
            type == GranteeType.GT_PUBLIC || 
            type == GranteeType.GT_GUEST || 
            type == GranteeType.GT_KEY) {
            return null;
        }
        
        Provisioning prov = Provisioning.getInstance();
        // for addresses, default to the authenticated user's domain
        if ((type == GranteeType.GT_USER || type == GranteeType.GT_GROUP) && name.indexOf('@') == -1) {
            Account authacct = prov.get(AccountBy.id, zsc.getAuthtokenAccountId(), zsc.getAuthToken());
            String authname = (authacct == null ? null : authacct.getName());
            if (authacct != null) {
                name += authname.substring(authname.indexOf('@'));
            }
        }

        NamedEntry nentry = null;
        if (name != null)
            switch (type) {
                case GT_USER:
                    nentry = lookupEmailAddress(name);
                    break;
                case GT_GROUP:
                    nentry = prov.get(Key.DistributionListBy.name, name);
                    break;
                case GT_DOMAIN:
                    nentry = prov.get(Key.DomainBy.name, name);
                    break;
            }

        if (nentry != null) {
            return nentry;
        } switch (type) {
            case GT_USER:
                throw AccountServiceException.NO_SUCH_ACCOUNT(name);
            case GT_GROUP:
                throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(name);
            case GT_DOMAIN:
                throw AccountServiceException.NO_SUCH_DOMAIN(name);
            default:
                throw ServiceException.FAILURE("LDAP entry not found for " + name + " : " + type, null);
        }
    }

    private static NamedEntry lookupGranteeByZimbraId(String zid, GranteeType type, boolean granting) 
    throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        NamedEntry nentry = null;
        try {
            switch (type) {
                case GT_USER:    
                    nentry = prov.get(AccountBy.id, zid); 
                    if (nentry == null && granting) {
                        throw AccountServiceException.NO_SUCH_ACCOUNT(zid);
                    } else {
                        return nentry;
                    }
                case GT_GROUP:   
                    nentry = prov.get(Key.DistributionListBy.id, zid);
                    if (nentry == null && granting) {
                        throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(zid);
                    } else {
                        return nentry;
                    }
                case GT_DOMAIN:   
                    nentry = prov.get(Key.DomainBy.id, zid);
                    if (nentry == null && granting) {
                        throw AccountServiceException.NO_SUCH_DOMAIN(zid);
                    } else {
                        return nentry;
                    }
                case GT_GUEST:
                case GT_KEY:    
                case GT_AUTHUSER:
                case GT_PUBLIC:
                default:
                    return null;
            }
        } catch (ServiceException e) {
            if (granting) {
                throw e;
            } else {
                return null;
            }
        }
    }

}
