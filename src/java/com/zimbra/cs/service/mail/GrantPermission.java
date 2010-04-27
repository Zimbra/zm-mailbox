/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.service.mail;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DistributionListBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.ACLUtil;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.RightModifier;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.soap.ZimbraSoapContext;

public class GrantPermission extends MailDocumentHandler {
    
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");
        
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        for (Element eACE : request.listElements(MailConstants.E_ACE)) {
            ZimbraACE ace = handleACE(eACE, zsc, true);
            aces.add(ace);
        }

        List<ZimbraACE> granted = ACLUtil.grantRight(Provisioning.getInstance(), account, aces);
        Element response = zsc.createElement(MailConstants.GRANT_PERMISSION_RESPONSE);
        if (aces != null) {
            for (ZimbraACE ace : granted)
                ToXML.encodeACE(response, ace);
        }
        /*
         * This is done in FolderAction.OP_GRANT, should we do the same?
         * 
        // kinda hacky -- return the zimbra id and name of the grantee in the response
        result.addAttribute(MailConstants.A_ZIMBRA_ID, zid);
        if (nentry != null)
            result.addAttribute(MailConstants.A_DISPLAY, nentry.getName());
        else if (gtype == ACL.GRANTEE_GUEST)
            result.addAttribute(MailConstants.A_DISPLAY, zid);
     
        */ 
        return response;
    }
    
    /**
     * // orig: FolderAction
     * 
     * @param eACE
     * @param zsc
     * @param granting true if granting, false if revoking
     * @return
     * @throws ServiceException
     */
    static ZimbraACE handleACE(Element eACE, ZimbraSoapContext zsc, boolean granting) throws ServiceException {
        Right right = RightManager.getInstance().getUserRight(eACE.getAttribute(MailConstants.A_RIGHT));
        GranteeType gtype = GranteeType.fromCode(eACE.getAttribute(MailConstants.A_GRANT_TYPE));
        String zid = eACE.getAttribute(MailConstants.A_ZIMBRA_ID, null);
        boolean deny = eACE.getAttributeBool(MailConstants.A_DENY, false);
        String secret = null;
        NamedEntry nentry = null;
        
        if (gtype == GranteeType.GT_AUTHUSER) {
            zid = ACL.GUID_AUTHUSER;
        } else if (gtype == GranteeType.GT_PUBLIC) {
            zid = ACL.GUID_PUBLIC;
        } else if (gtype == GranteeType.GT_GUEST) {
            zid = eACE.getAttribute(MailConstants.A_DISPLAY);
            if (zid == null || zid.indexOf('@') < 0)
                throw ServiceException.INVALID_REQUEST("invalid guest id or password", null);
            // make sure they didn't accidentally specify "guest" instead of "usr"
            try {
                nentry = lookupGranteeByName(zid, GranteeType.GT_USER, zsc);
                zid = nentry.getId();
                gtype = nentry instanceof DistributionList ? GranteeType.GT_GROUP : GranteeType.GT_USER;
            } catch (ServiceException e) {
                // this is the normal path, where lookupGranteeByName throws account.NO_SUCH_USER
                secret = eACE.getAttribute(MailConstants.A_PASSWORD);
            }
        } else if (gtype == GranteeType.GT_KEY) {
            zid = eACE.getAttribute(MailConstants.A_DISPLAY);
            // unlike guest, we do not require the display name to be an email address
            /*
            if (zid == null || zid.indexOf('@') < 0)
                throw ServiceException.INVALID_REQUEST("invalid guest id or key", null);
            */    
            // unlike guest, we do not fixup grantee type for key grantees if they specify an internal user
            
            // get the optional accesskey
            secret = eACE.getAttribute(MailConstants.A_ACCESSKEY, null);
         
        } else if (zid != null) {
            nentry = lookupGranteeByZimbraId(zid, gtype, granting);
        } else {
            nentry = lookupGranteeByName(eACE.getAttribute(MailConstants.A_DISPLAY), gtype, zsc);
            zid = nentry.getId();
            // make sure they didn't accidentally specify "usr" instead of "grp"
            if (gtype == GranteeType.GT_USER && nentry instanceof DistributionList)
                gtype = GranteeType.GT_GROUP;
        }
        
        RightModifier rightModifier = null;
        if (deny)
            rightModifier = RightModifier.RM_DENY;
        return new ZimbraACE(zid, gtype, right, rightModifier, secret);

    }
    
    
    /*
     * lookupEmailAddress, lookupGranteeByName, lookupGranteeByZimbraId are borrowed from FolderAction
     * and transplanted to work with ACL in accesscontrol package for usr space account level rights.
     * 
     * The purpose is to match the existing folder grant SOAP interface, which is more flexible/liberal 
     * on identifying grantee and target.
     *   
     * These methods are *not* used for admin space ACL SOAPs. 
     */
    
    // orig: FolderAction.lookupEmailAddress
    private static NamedEntry lookupEmailAddress(String name) throws ServiceException {
        NamedEntry nentry = null;
        Provisioning prov = Provisioning.getInstance();
        nentry = prov.get(AccountBy.name, name);
        if (nentry == null)
            nentry = prov.get(DistributionListBy.name, name);
        return nentry;
    }
    
    // orig: FolderAction.lookupGranteeByName
    private static NamedEntry lookupGranteeByName(String name, GranteeType type, ZimbraSoapContext zsc) throws ServiceException {
        if (type == GranteeType.GT_AUTHUSER || type == GranteeType.GT_PUBLIC || type == GranteeType.GT_GUEST || type == GranteeType.GT_KEY)
            return null;

        Provisioning prov = Provisioning.getInstance();
        // for addresses, default to the authenticated user's domain
        if ((type == GranteeType.GT_USER || type == GranteeType.GT_GROUP) && name.indexOf('@') == -1) {
            Account authacct = prov.get(AccountBy.id, zsc.getAuthtokenAccountId(), zsc.getAuthToken());
            String authname = (authacct == null ? null : authacct.getName());
            if (authacct != null)
                name += authname.substring(authname.indexOf('@'));
        }

        NamedEntry nentry = null;
        if (name != null)
            switch (type) {
                case GT_USER:    nentry = lookupEmailAddress(name);                 break;
                case GT_GROUP:   nentry = prov.get(DistributionListBy.name, name);  break;
                case GT_DOMAIN:  nentry = prov.get(DomainBy.name, name);            break;
            }

        if (nentry != null)
            return nentry;
        switch (type) {
            case GT_USER:    throw AccountServiceException.NO_SUCH_ACCOUNT(name);
            case GT_GROUP:   throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(name);
            case GT_DOMAIN:  throw AccountServiceException.NO_SUCH_DOMAIN(name);
            default:  throw ServiceException.FAILURE("LDAP entry not found for " + name + " : " + type, null);
        }
    }

    // orig: FolderAction.lookupGranteeByZimbraId
    private static NamedEntry lookupGranteeByZimbraId(String zid, GranteeType type, boolean granting) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        NamedEntry nentry = null;
        try {
            switch (type) {
                case GT_USER:    
                    nentry = prov.get(AccountBy.id, zid); 
                    if (nentry == null && granting)
                        throw AccountServiceException.NO_SUCH_ACCOUNT(zid);
                    else
                        return nentry;
                case GT_GROUP:   
                    nentry = prov.get(DistributionListBy.id, zid);
                    if (nentry == null && granting)
                        throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(zid);
                    else
                        return nentry;
                case GT_DOMAIN:   
                    nentry = prov.get(DomainBy.id, zid);
                    if (nentry == null && granting)
                        throw AccountServiceException.NO_SUCH_DOMAIN(zid);
                    else
                        return nentry;
                case GT_GUEST:
                case GT_KEY:    
                case GT_AUTHUSER:
                case GT_PUBLIC:
                default:         return null;
            }
        } catch (ServiceException e) {
            if (granting)
                throw e;
            else
                return null;
        }
    }


}
