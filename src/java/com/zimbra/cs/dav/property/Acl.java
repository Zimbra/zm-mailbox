/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.dav.property;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.ACL.Grant;

/*
 * The following are the required properties for supporting DAV access-control.
 *
 * DAV:owner - RFC3744 section 5.1
 * DAV:group - RFC3744 section 5.2
 * DAV:supported-privilege-set - RFC3744 section 5.3
 * DAV:current-user-privilege-set - RFC3744 section 5.4
 * DAV:acl - RFC3744 section 5.5
 * DAV:acl-restrictions - RFC3744 section 5.6
 * DAV:inherited-acl-set- RFC3744 section 5.7
 * DAV:principal-collection-set - RFC3744 section 5.8
 */
public class Acl extends ResourceProperty {
    public static Set<ResourceProperty> getAclProperties(DavResource rs, Folder folder) throws ServiceException, DavException {
        HashSet<ResourceProperty> props = new HashSet<ResourceProperty>();
        if (folder == null)
            return props;

        String owner = rs.getOwner();
        ACL acl = folder.getEffectiveACL();
        props.add(getSupportedPrivilegeSet());
        if (folder != null) {
            // calendar feeds are read-only.
            if (folder.getDefaultView() != MailItem.Type.APPOINTMENT || folder.getUrl() == null || folder.getUrl().equals("")) {
                props.add(getCurrentUserPrivilegeSet(acl, folder.getAccount()));
            } else {
                props.add(getCurrentUserPrivilegeSet(ACL.RIGHT_READ));
            }
            props.add(getPrincipalCollectionSet());
        }
        props.add(getAcl(acl, owner));
        props.add(getAclRestrictions());

        ResourceProperty p = new ResourceProperty(DavElements.E_OWNER);
        p.setProtected(true);
        Element href = p.addChild(DavElements.E_HREF);
        href.setText(UrlNamespace.getPrincipalUrl(owner));
        props.add(p);

        // empty properties
        p = new ResourceProperty(DavElements.E_GROUP);
        p.setProtected(true);
        props.add(p);
        p = new ResourceProperty(DavElements.E_INHERITED_ACL_SET);
        p.setProtected(true);
        props.add(p);
        return props;
    }

    public static ResourceProperty getPrincipalUrl(DavResource rs) {
        return new PrincipalUrl(rs);
    }
    public static ResourceProperty getAcl(ACL acl, String owner) {
        return new Acl(acl, owner);
    }
    public static ResourceProperty getSupportedPrivilegeSet() {
        return new SupportedPrivilegeSet();
    }

    public static ResourceProperty getCurrentUserPrivilegeSet(ACL acl, Account owner) {
        return new CurrentUserPrivilegeSet(acl, owner);
    }

    public static ResourceProperty getCurrentUserPrivilegeSet(short rights) {
        return new CurrentUserPrivilegeSet(rights);
    }

    public static ResourceProperty getMountpointTargetPrivilegeSet(short rights) {
        return new MountpointTargetPrivilegeSet(rights);
    }

    public static ResourceProperty getAclRestrictions() {
        return new AclRestrictions();
    }

    public static ResourceProperty getPrincipalCollectionSet() {
        return new PrincipalCollectionSet();
    }

    public static ResourceProperty getCurrentUserPrincipal() {
        return new CurrentUserPrincipal();
    }

    protected ACL mAcl;
    protected String mOwner;

    private Acl(ACL acl, String owner) {
        this(DavElements.E_ACL, acl, owner);
    }

    private Acl(QName name, ACL acl, String owner) {
        super(name);
        setProtected(true);
        mAcl = acl;
        mOwner = owner;
    }

    @Override
    public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
        Element acl = super.toElement(ctxt, parent, true);

        if (mAcl == null)
            return acl;

        Account ownerAccount = null;
        Account authAccount = ctxt.getAuthAccount();
        try {
            ownerAccount = Provisioning.getInstance().getAccountByName(mOwner);
        } catch (ServiceException se) {
        }
        for (Grant g : mAcl.getGrants()) {
            try {
                if (ownerAccount != null && authAccount.compareTo(ownerAccount) != 0 && g.getGrantedRights(authAccount, mAcl) == 0)
                    continue;
                Element ace = acl.addElement(DavElements.E_ACE);
                Element principal = ace.addElement(DavElements.E_PRINCIPAL);
                Element e;
                switch (g.getGranteeType()) {
                case ACL.GRANTEE_USER:
                    e = principal.addElement(DavElements.E_HREF);
                    e.setText(UrlNamespace.getAclUrl(g.getGranteeId(), UrlNamespace.ACL_USER));
                    break;
                case ACL.GRANTEE_GUEST:
                    e = principal.addElement(DavElements.E_HREF);
                    e.setText(UrlNamespace.getAclUrl(g.getGranteeId(), UrlNamespace.ACL_GUEST));
                    break;
                case ACL.GRANTEE_KEY:
                    // 30049 TODO
                    break;
                case ACL.GRANTEE_AUTHUSER:
                    principal.addElement(DavElements.E_AUTHENTICATED);
                    break;
                case ACL.GRANTEE_COS:
                    e = principal.addElement(DavElements.E_HREF);
                    e.setText(UrlNamespace.getAclUrl(g.getGranteeId(), UrlNamespace.ACL_COS));
                    break;
                case ACL.GRANTEE_DOMAIN:
                    e = principal.addElement(DavElements.E_HREF);
                    e.setText(UrlNamespace.getAclUrl(g.getGranteeId(), UrlNamespace.ACL_DOMAIN));
                    break;
                case ACL.GRANTEE_GROUP:
                    e = principal.addElement(DavElements.E_HREF);
                    e.setText(UrlNamespace.getAclUrl(g.getGranteeId(), UrlNamespace.ACL_GROUP));
                    break;
                case ACL.GRANTEE_PUBLIC:
                    principal.addElement(DavElements.E_UNAUTHENTICATED);
                    break;
                }

                addGrantDeny(ace, g, true);
            } catch (DavException e) {
                ZimbraLog.dav.error("can't add principal: grantee="+g.getGranteeId(), e);
            } catch (ServiceException e) {
                ZimbraLog.dav.error("can't add principal: grantee="+g.getGranteeId(), e);
            }
        }
        return acl;
    }

    protected Element addGrantDeny(Element ace, Grant g, boolean isGrant) {
        Element grant = isGrant ? ace.addElement(DavElements.E_GRANT) : ace.addElement(DavElements.E_DENY);
        addPrivileges(grant, g.getGrantedRights());
        return grant;
    }

    protected Element addPrivileges(Element grant, short rights) {
        grant.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_READ_CURRENT_USER_PRIVILEGE_SET);
        if ((rights & ACL.RIGHT_READ) > 0) {
            grant.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_READ);
        }
        if ((rights & ACL.RIGHT_WRITE) > 0) {
            grant.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_WRITE);
            grant.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_WRITE_CONTENT);
            grant.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_WRITE_PROPERTIES);
        }
        if ((rights & ACL.RIGHT_INSERT) > 0) {
            grant.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_BIND);
        }
        if ((rights & ACL.RIGHT_DELETE) > 0) {
            grant.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_UNBIND);
        }
        if ((rights & ACL.RIGHT_ADMIN) > 0) {
            grant.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_UNLOCK);
            grant.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_WRITE_ACL);
        }
        /*
        if ((rights & ACL.RIGHT_FREEBUSY) > 0)
            grant.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_READ_FREE_BUSY);
         */
        return grant;
    }

    static protected HashMap<String, Short> sRightsMap;
    private static final short RIGHT_UNSUPPORTED = 0;

    static {
        sRightsMap = new HashMap<String, Short>();
        sRightsMap.put(DavElements.P_READ, ACL.RIGHT_READ);
        sRightsMap.put(DavElements.P_READ_CURRENT_USER_PRIVILEGE_SET, ACL.RIGHT_READ);
        sRightsMap.put(DavElements.P_READ_FREE_BUSY, RIGHT_UNSUPPORTED);
        sRightsMap.put(DavElements.P_BIND, ACL.RIGHT_WRITE);
        sRightsMap.put(DavElements.P_UNBIND, ACL.RIGHT_WRITE);
        sRightsMap.put(DavElements.P_WRITE, ACL.RIGHT_WRITE);
        sRightsMap.put(DavElements.P_WRITE_ACL, ACL.RIGHT_ADMIN);
        sRightsMap.put(DavElements.P_WRITE_CONTENT, ACL.RIGHT_WRITE);
        sRightsMap.put(DavElements.P_WRITE_PROPERTIES, ACL.RIGHT_WRITE);
        sRightsMap.put(DavElements.P_UNLOCK, ACL.RIGHT_ADMIN);
    }

    private static class PrincipalCollectionSet extends ResourceProperty {
        public PrincipalCollectionSet() {
            super(DavElements.E_PRINCIPAL_COLLECTION_SET);
            setProtected(true);
        }

        @Override
        public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
            Element pcs = parent.addElement(getName());
            Element e = pcs.addElement(DavElements.E_HREF);
            try {
                e.setText(UrlNamespace.getPrincipalCollectionUrl(ctxt.getAuthAccount()));
            } catch (ServiceException ex) {
                ZimbraLog.dav.warn("can't generate principal-collection-url", ex);
            }
            return pcs;
        }
    }
    private static class SupportedPrivilegeSet extends Acl {

        public SupportedPrivilegeSet() {
            super(DavElements.E_SUPPORTED_PRIVILEGE_SET, null, null);
        }

        public Element addPrivilege(Element parent, QName name, String description) {
            Element priv = parent.addElement(DavElements.E_SUPPORTED_PRIVILEGE);
            priv.addElement(DavElements.E_PRIVILEGE).addElement(name);
            Element desc = priv.addElement(DavElements.E_DESCRIPTION);
            desc.addAttribute(DavElements.E_LANG, DavElements.LANG_EN_US);
            desc.setText(description);

            return priv;
        }

        @Override
        public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
            Element sps = parent.addElement(getName());
            if (nameOnly)
                return sps;

            if (mAcl == null)
                return sps;

            Element all = addPrivilege(sps, DavElements.E_ALL, "any operation");
            addPrivilege(all, DavElements.E_READ, "read calendar, attachment, notebook");
            addPrivilege(all, DavElements.E_WRITE, "add calendar appointment, upload attachment");
            addPrivilege(all, DavElements.E_UNLOCK, "unlock your own resources locked by someone else");

            return sps;
        }
    }

    private static class CurrentUserPrivilegeSet extends Acl {
        private String mOwnerId;
        private short mRights;
        public CurrentUserPrivilegeSet(ACL acl, Account owner) {
            super(DavElements.E_CURRENT_USER_PRIVILEGE_SET, acl, owner.getName());
            mOwnerId = owner.getId();
            mRights = -1;
        }
        public CurrentUserPrivilegeSet(short rights) {
            super(DavElements.E_CURRENT_USER_PRIVILEGE_SET, null, null);
            mRights = rights;
        }

        @Override
        public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
            Element cups = parent.addElement(getName());
            if (nameOnly)
                return cups;

            if (mRights > 0) {
                // this is for the mountpoint.  all the privileges except for write-properties
                // come from the remote folder.  write-properties is always enabled.
                addPrivileges(cups, mRights);
                if ((mRights & ACL.RIGHT_WRITE) == 0)
                    cups.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_WRITE_PROPERTIES);
                return cups;
            }

            // the requestor still has full permission if owner.
            if (ctxt.getAuthAccount().getId().equals(mOwnerId)) {
                addPrivileges(cups, (short)(ACL.RIGHT_READ | ACL.RIGHT_WRITE | ACL.RIGHT_DELETE | ACL.RIGHT_INSERT));
                return cups;
            }

            if (mAcl == null) {
                return cups;
            }

            for (Grant g : mAcl.getGrants()) {
                try {
                    short rights = g.getGrantedRights(ctxt.getAuthAccount(), mAcl);
                    if (rights > 0) {
                        addPrivileges(cups, rights);
                        break;
                    }
                } catch (ServiceException e) {
                    ZimbraLog.dav.error("can't add principal: grantee="+g.getGranteeId(), e);
                }
            }

            return cups;
        }
    }

    private static class MountpointTargetPrivilegeSet extends Acl {
        private short mRights;
        public MountpointTargetPrivilegeSet(short rights) {
            super(DavElements.E_MOUNTPOINT_TARGET_PRIVILEGE_SET, null, null);
            mRights = rights;
        }

        @Override
        public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
            Element mtps = parent.addElement(getName());
            if (nameOnly)
                return mtps;

            addPrivileges(mtps, mRights);
            return mtps;
        }
    }

    /*
     * DAV:principal-URL
     * RFC3744 section 4.2
     */
    private static class PrincipalUrl extends Acl {
        private Account mAccount;
        public PrincipalUrl(DavResource rs) {
            super(DavElements.E_PRINCIPAL_URL, null, null);
            try {
                mAccount = Provisioning.getInstance().get(AccountBy.name, rs.getOwner());
            } catch (ServiceException e) {
                ZimbraLog.dav.warn("can't get account "+rs.getOwner(), e);
            }
        }

        @Override
        public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
            Element pu = parent.addElement(getName());
            if (mAccount != null)
                pu.addElement(DavElements.E_HREF).setText(UrlNamespace.getPrincipalUrl(mAccount));
            return pu;
        }
    }

    /*
     * DAV:current-user-principal
     * RFC5397
     */
    private static class CurrentUserPrincipal extends Acl {
        public CurrentUserPrincipal() {
            super(DavElements.E_CURRENT_USER_PRINCIPAL, null, null);
        }

        @Override
        public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
            Element cup = parent.addElement(getName());
            cup.addElement(DavElements.E_HREF).setText(UrlNamespace.getPrincipalUrl(ctxt.getAuthAccount()));
            return cup;
        }
    }

    /*
     * DAV:acl-restrictions
     * RFC3744 section 5.6
     */
    private static class AclRestrictions extends Acl {
        public AclRestrictions() {
            super(DavElements.E_ACL_RESTRICTIONS, null, null);
        }

        @Override
        public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
            Element ar = parent.addElement(getName());
            ar.addElement(DavElements.E_GRANT_ONLY);
            ar.addElement(DavElements.E_NO_INVERT);
            return ar;
        }
    }

    public static class Ace {
        private String mPrincipalUrl;
        private String mId;
        private short mRights;
        private byte mGranteeType;

        public Ace(Element a) throws DavException {
            Element elem = a.element(DavElements.E_PRINCIPAL);
            if (elem == null)
                throw new DavException("missing principal element", HttpServletResponse.SC_BAD_REQUEST);

            List<Element> principal = elem.elements();
            if (principal.size() != 1)
                throw new DavException("invalid principal element", HttpServletResponse.SC_BAD_REQUEST);
            for (Element p : principal) {
                QName name = p.getQName();
                if (name.equals(DavElements.E_HREF)) {
                    mPrincipalUrl = elem.getText();
                    mGranteeType = ACL.GRANTEE_USER;
                    try {
                        Account acc = UrlNamespace.getPrincipal(mPrincipalUrl);
                        if (acc == null)
                            throw new DavException("invalid principal: "+mPrincipalUrl, HttpServletResponse.SC_BAD_REQUEST);
                        mId = acc.getId();
                    } catch (ServiceException se) {
                        throw new DavException("can't find principal: "+mPrincipalUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se);
                    }
                } else if (name.equals(DavElements.E_ALL)) {
                    mGranteeType = ACL.GRANTEE_PUBLIC;
                } else if (name.equals(DavElements.E_UNAUTHENTICATED)) {
                    // XXX we don't yet support "only for unauthenticated users"
                    mGranteeType = ACL.GRANTEE_PUBLIC;
                } else if (name.equals(DavElements.E_AUTHENTICATED)) {
                    mGranteeType = ACL.GRANTEE_AUTHUSER;
                } else {
                    throw new DavException("unsupported principal: "+name.getName(), HttpServletResponse.SC_NOT_IMPLEMENTED);
                }
            }
            if (elem != null)
                elem = elem.element(DavElements.E_HREF);


            mRights = (short)0;
            elem = a.element(DavElements.E_GRANT);
            if (elem == null)
                throw new DavException("missing grant element", HttpServletResponse.SC_BAD_REQUEST);
            List<Element> priv = elem.elements(DavElements.E_PRIVILEGE);
            for (Element p : priv) {
                List<Element> right = p.elements();
                if (right.size() != 1)
                    throw new DavException("number of right elements contained in privilege element is not one", HttpServletResponse.SC_BAD_REQUEST);
                mRights |= sRightsMap.get(right.get(0).getName());
            }
        }

        public Ace(String id, short rights, byte type) {
            mId = id;
            mRights = rights;
            mGranteeType = type;
        }

        public String getPrincipalUrl() {
            if (mPrincipalUrl != null)
                return mPrincipalUrl;

            switch (mGranteeType) {
            case ACL.GRANTEE_USER:
                try {
                    Account acct = Provisioning.getInstance().get(Key.AccountBy.id, mId);
                    mPrincipalUrl = UrlNamespace.getPrincipalCollectionUrl(acct);
                } catch (ServiceException se) {
                    ZimbraLog.dav.warn("can't lookup account "+mId, se);
                }
                break;
            }
            return mPrincipalUrl;
        }

        public String getZimbraId() {
            return mId;
        }

        public byte getGranteeType() {
            return mGranteeType;
        }

        public short getRights() {
            return mRights;
        }

        public boolean hasHref() {
            return mGranteeType == ACL.GRANTEE_USER;
        }
    }
}
