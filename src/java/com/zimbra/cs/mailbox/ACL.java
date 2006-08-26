/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
/*
 * Created on Jul 5, 2005
 */
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;

/**
 * @author dkarp */
public class ACL {

    /** The right to read a message, list a folder's contents, etc. */
	public static final short RIGHT_READ    = 0x0001;
    /** The right to edit an item, change its flags, etc.. */
	public static final short RIGHT_WRITE   = 0x0002;
    /** The right to add or move an item to a folder */
	public static final short RIGHT_INSERT  = 0x0004;
    /** The right to hard-delete an item. */
    public static final short RIGHT_DELETE  = 0x0008;
    /** The right to take a workflow action on an item (e.g. accept a meeting). */
    public static final short RIGHT_ACTION  = 0x0010;
    /** The right to grant permissions on the item. */
    public static final short RIGHT_ADMIN   = 0x0100;
    /** The calculated right to create subfolders in a folder. */
    public static final short RIGHT_SUBFOLDER = 0x0200;

    /** Bitmask of all rights that can be explicitly granted.  <i>Note:
     *  CAN_CREATE_FOLDER is calculated and hence cannot be granted. */
    private static final short GRANTABLE_RIGHTS = RIGHT_READ   | RIGHT_WRITE  | RIGHT_INSERT |
                                                  RIGHT_DELETE | RIGHT_ACTION | RIGHT_ADMIN;

    /** The grantee of these rights is the zimbraId for a user. */
	public static final byte GRANTEE_USER     = 1;
    /** The grantee of these rights is the zimbraId for a distribution list. */
	public static final byte GRANTEE_GROUP    = 2;
    /** The grantee of these rights is all authenticated users. */
	public static final byte GRANTEE_AUTHUSER = 3;
    /** The grantee of these rights is the zimbraId for a domain. */
	public static final byte GRANTEE_DOMAIN   = 4;
    /** The grantee of these rights is the zimbraId for a COS. */
	public static final byte GRANTEE_COS      = 5;
    /** The grantee of these rights is all authenticated and unauthenticated users. */
	public static final byte GRANTEE_PUBLIC   = 6;
	/** The grantee of these rights is a named non Zimbra user identified by the email address */
	public static final byte GRANTEE_GUEST    = 7;
	

    /** The pseudo-GUID signifying "all authenticated users". */
    public static final String GUID_AUTHUSER = "00000000-0000-0000-0000-000000000000";
    /** The pseudo-GUID signifying "all authenticated and unauthenticated users". */
    public static final String GUID_PUBLIC   = "99999999-9999-9999-9999-999999999999";

    public static final Account ANONYMOUS_ACCT = new Account("public", GUID_PUBLIC, getAnonAttrs(), null);

    private static Map<String, Object> getAnonAttrs() {
        Map<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_uid, "public");
        return attrs;
    }
    
    public static class GuestAccount extends Account {
    	public GuestAccount(String emailAddress, String password) {
            super(emailAddress, GUID_PUBLIC, getAnonAttrs(), null);
            mPassword = password;
    	}
    	public String getPassword()              { return mPassword; }
    	private String mPassword;
    }

    public static class Grant {
        /** The zimbraId of the entry being granted rights. */
        private String mGrantee;
        /** The type of object the grantee's ID refers to.
         *  For instance, {@link ACL#GRANTEE_USER}. */
        private byte mType;
        /** A bitmask of the rights being granted.  For instance, 
         *  <code>{@link ACL#RIGHT_INSERT} | {@link ACL#RIGHT_READ}</code>. */
        private short mRights;
        /** Whether subfolders inherit these same rights. */
        private boolean mInherit;
        /** The password for guest accounts. */
        private String mPassword;

        /** Creates a new Grant object granting access to a user or class
         *  of users.  <code>zimbraId</code> may be <code>null</code>
         *  if the <code>type</code> is {@link ACL#GRANTEE_PUBLIC}.
         * 
         * @param zimbraId  The zimbraId of the entry being granted rights.
         * @param type      The type of object the grantee's ID refers to.
         * @param rights    A bitmask of the rights being granted.
         * @param inherit   Whether subfolders inherit these same rights.
         * @see ACL */
        Grant(String zimbraId, byte type, short rights, boolean inherit) {
            mGrantee = zimbraId;
            mType    = type;
            mRights  = (short) (rights & GRANTABLE_RIGHTS);
            mInherit = inherit;
        }
        Grant(String zimbraId, byte type, short rights, boolean inherit, String password) {
        	this(zimbraId, type, rights, inherit);
            if (mType == GRANTEE_GUEST)
                mPassword = password;
        }

        /** Creates a new Grant object from a decoded {@link Metadata} hash.
         * 
         * @param meta  The Metadata object containing ACL data.
         * @throws ServiceException if any required fields are missing. */
        Grant(Metadata meta) throws ServiceException {
            mType    = (byte) meta.getLong(FN_TYPE);
            mRights  = (short) (meta.getLong(FN_RIGHTS) & GRANTABLE_RIGHTS);
            mInherit = meta.getBool(FN_INHERIT, false);
            if (hasGrantee())
                mGrantee = meta.get(FN_GRANTEE);
            if (mType == ACL.GRANTEE_GUEST)
            	mPassword = meta.get(FN_PASSWORD);
        }

        /** Returns true if there is an explicit grantee. */
        public boolean hasGrantee() { return mType != ACL.GRANTEE_AUTHUSER && mType != ACL.GRANTEE_PUBLIC; }
        /** Returns the zimbraId of the entry granted rights. */
        public String getGranteeId() { return hasGrantee() ? mGrantee : null; }
        /** Returns type of object the grantee's ID refers to. */
        public byte getGranteeType() { return mType; }
        /** Returns the bitmask of the rights granted. */
        public short getGrantedRights() { return mRights; }
        /** Returns whether subfolders inherit these same rights. */
        public boolean isGrantInherited() { return mInherit; }
        
        /** Returns the rights granted to the given {@link Account} by this
         *  <code>Grant</code>.  If the grant does not apply to the Account,
         *  returns <code>0</code>. */
        public short getGrantedRights(Account acct) throws ServiceException {
            return matches(acct) ? mRights : 0;
        }

        /** Returns whether this grant applies to the given {@link Account}.
         *  If <code>acct</code> is <code>null</code>, only return
         *  <code>true</code> if the grantee is {@link ACL#GRANTEE_PUBLIC}. */
        private boolean matches(Account acct) throws ServiceException {
            Provisioning prov = Provisioning.getInstance();
            if (acct == null)
                return mType == ACL.GRANTEE_PUBLIC;
            switch (mType) {
                case ACL.GRANTEE_PUBLIC:   return true;
                case ACL.GRANTEE_AUTHUSER: return !acct.equals(ANONYMOUS_ACCT);
                case ACL.GRANTEE_COS:      return mGrantee.equals(getId(prov.getCOS(acct)));
                case ACL.GRANTEE_DOMAIN:   return mGrantee.equals(getId(prov.getDomain(acct)));
                case ACL.GRANTEE_GROUP:    return prov.inDistributionList(acct, mGrantee);
                case ACL.GRANTEE_USER:     return mGrantee.equals(acct.getId());
                case ACL.GRANTEE_GUEST:    return matchesGuestAccount(acct);
                default:  throw ServiceException.FAILURE("unknown ACL grantee type: " + mType, null);
            }
        }

        private boolean matchesGuestAccount(Account acct) {
        	if (!(acct instanceof GuestAccount))
        		return false;
        	GuestAccount ga = (GuestAccount) acct;
        	return ga.getName().equals(mGrantee) && ga.getPassword().equals(mPassword);
        }
        
        /** Utility function: Returns the zimbraId for a null-checked LDAP
         *  entry. */
        private static final String getId(NamedEntry entry) {
            return (entry == null ? null : entry.getId());
        }

        /** Returns whether the id exactly matches the grantee.
         *  <code>zimbraId</code> must be {@link ACL#GUID_AUTHUSER} if the
         *  actual grantee is {@link ACL#GRANTEE_AUTHUSER}.
         *  <code>zimbraId</code> may be {@link ACL#GUID_PUBLIC} if the
         *  actual grantee is {@link ACL#GRANTEE_PUBLIC}.
         * 
         * @param zimbraId  The zimbraId of the entry being granted rights.*/
        public boolean isGrantee(String zimbraId) {
        	if (zimbraId == null || zimbraId.equals(GUID_PUBLIC))
                return (mType == GRANTEE_PUBLIC);
        	else if (zimbraId.equals(GUID_AUTHUSER))
                return (mType == GRANTEE_AUTHUSER);
            return zimbraId.equals(mGrantee);
        }

        /** Updates the granted rights in the <code>Grant</code>.  The old
         *  set of rights is discarded.
         * 
         * @param rights   A bitmask of the rights being granted.
         * @param inherit  Whether subfolders inherit these same rights.
         * @see ACL */
        void setRights(short rights, boolean inherit) {
            mRights = rights;  mInherit = inherit;
        }

        /** For grants to external users, sets the password required to
         *  access the resource. */
        void setPassword(String password) {
        	if (mType == GRANTEE_GUEST && password != null)
                mPassword = password;
        }


        private static final String FN_GRANTEE  = "g";
        private static final String FN_TYPE     = "t";
        private static final String FN_RIGHTS   = "r";
        private static final String FN_INHERIT  = "i";
        private static final String FN_PASSWORD = "a";

        /** Encapsulates this <code>Grant</code> as a {@link Metadata} object
         *  for serialization. */
        public Metadata encode() {
            Metadata meta = new Metadata();
            meta.put(FN_GRANTEE,  hasGrantee() ? mGrantee : null);
            meta.put(FN_TYPE,     mType);
            // FIXME: use "rwidxsca" instead of numeric value
            meta.put(FN_RIGHTS,   mRights);
            meta.put(FN_INHERIT,  mInherit);
            meta.put(FN_PASSWORD, mPassword);
            return meta;
        }
    }

    /** The <code>List</code> of all {@link ACL.Grant}s set on an item. */
    private List<Grant> mGrants = new ArrayList<Grant>();


    public ACL()  { }
    public ACL(MetadataList mlist) {
        for (int i = 0; i < mlist.size(); i++)
            try {
                mGrants.add(new Grant(mlist.getMap(i)));
            } catch (ServiceException e) {
                ZimbraLog.mailbox.warn("malformed permission grant: " + mlist, e);
            }
    }

    /** Returns the bitmask of rights granted to the user by the ACL, or
     *  <code>null</code> if there are no rights granted to anyone.  (Note
     *  that if rights are granted to <i>other</i> accounts but not to the
     *  specified user, returns <code>0</code>.)
     * 
     * @param authuser       The user to gather rights for.
     * @param inheritedOnly  Whether to consider only inherited rights.
     * @return A <code>Short</code> containing the OR'ed-together rights
     *         granted to the user, or <code>null</code>. */
    Short getGrantedRights(Account authuser, boolean inheritedOnly) throws ServiceException {
        if (mGrants.isEmpty())
            return null;

        short rightsGranted = 0;
        boolean inheritedRead = false;
        for (Grant grant : mGrants) {
            boolean inherited = grant.isGrantInherited();
            if (!inheritedOnly || inherited) {
                short rights = grant.getGrantedRights(authuser);
                rightsGranted |= rights;
                if (inherited && (rights & RIGHT_READ) != 0)
                    inheritedRead = true;
            }
        }
        if (inheritedRead && (rightsGranted & RIGHT_INSERT) != 0)
            rightsGranted |= RIGHT_SUBFOLDER;

        return new Short(rightsGranted);
    }

    /** Returns whether there are any grants encapsulated by this ACL. */
    boolean isEmpty() {
        return mGrants.isEmpty();
    }

    /** Grants the specified set of rights to the target.  If another set
     *  of rights has already been granted to the exact given (id, type)
     *  pair, the previous set is revoked and the new set is granted.
     * 
     * @param zimbraId  The zimbraId of the entry being granted rights.
     * @param type      The type of object the grantee's ID refers to.
     * @param rights    A bitmask of the rights being granted.
     * @param inherit   Whether subfolders inherit these same rights. */
    public void grantAccess(String zimbraId, byte type, short rights, boolean inherit, String password)
    throws ServiceException {
        if (type == GRANTEE_AUTHUSER)
            zimbraId = GUID_AUTHUSER;
        else if (type == GRANTEE_PUBLIC)
        	zimbraId = GUID_PUBLIC;
        else if (zimbraId == null)
            throw ServiceException.INVALID_REQUEST("missing grantee id", null);

        if (!mGrants.isEmpty()) {
            for (Grant grant : mGrants)
                if (grant.isGrantee(zimbraId)) {
                    grant.setRights(rights, inherit);
                    if (type == GRANTEE_GUEST)
                        grant.setPassword(password);
                    return;
                }
        }
        mGrants.add(new Grant(zimbraId, type, rights, inherit, password));
    }

    /** Removes the set of rights granted to the specified id.  If no rights
     *  were previously granted to the target, no error is  thrown and 
     *  <code>false</code> is returned.
     * 
     * @param zimbraId  The zimbraId of the entry being revoked rights.
     * @return whether an {@link Grant} was actually removed from the set. */
    boolean revokeAccess(String zimbraId) {
        if (mGrants == null || mGrants.isEmpty())
            return false;
        int count = mGrants.size();
        for (Iterator<Grant> it = mGrants.iterator(); it.hasNext(); ) {
            Grant grant = it.next();
            if (grant.isGrantee(zimbraId))
                it.remove();
        }
        return (mGrants.size() != count);
    }

    /** Encapsulates this set of {@link ACL.Grant}s as a
     *  {@link MetadataList} for serialization. */
    MetadataList encode() {
        MetadataList mlist = new MetadataList();
        for (Grant grant : mGrants)
            mlist.add(grant.encode());
        return mlist;
    }

    public String toString() {
        return encode().toString();
    }

    /** Returns a different <code>ACL</code> with the same contents. */
    public ACL duplicate()  { return new ACL(encode()); }

    /** Returns an <code>Iterator</code> over this <code>ACL</code>'s set of
     *  encapsulated {@link ACL.Grant} objects. */
    public Iterator<Grant> grantIterator() {
        return mGrants.iterator();
    }

    private static final char ABBR_READ = 'r';
    private static final char ABBR_WRITE = 'w';
    private static final char ABBR_INSERT = 'i';
    private static final char ABBR_DELETE = 'd';
    private static final char ABBR_ACTION = 'x';
    private static final char ABBR_ADMIN = 'a';
    private static final char ABBR_CREATE_FOLDER = 'c';

    public static short stringToRights(String encoded) throws ServiceException {
        short rights = 0;
        if (encoded != null && encoded.length() != 0)
            for (int i = 0; i < encoded.length(); i++)
                switch (encoded.charAt(i)) {
                    case ABBR_READ:    rights |= RIGHT_READ;    break;
                    case ABBR_WRITE:   rights |= RIGHT_WRITE;   break;
                    case ABBR_INSERT:  rights |= RIGHT_INSERT;  break;
                    case ABBR_DELETE:  rights |= RIGHT_DELETE;  break;
                    case ABBR_ACTION:  rights |= RIGHT_ACTION;  break;
                    case ABBR_ADMIN:   rights |= RIGHT_ADMIN;   break;
                    default:  throw ServiceException.INVALID_REQUEST("unknown right: " + encoded.charAt(i), null);
                }
        return rights;
    }

    public static String rightsToString(short rights) {
        if (rights == 0)
            return "";
        StringBuffer sb = new StringBuffer();
        if ((rights & RIGHT_READ) != 0)    sb.append(ABBR_READ);
        if ((rights & RIGHT_WRITE) != 0)   sb.append(ABBR_WRITE);
        if ((rights & RIGHT_INSERT) != 0)  sb.append(ABBR_INSERT);
        if ((rights & RIGHT_DELETE) != 0)  sb.append(ABBR_DELETE);
        if ((rights & RIGHT_ACTION) != 0)  sb.append(ABBR_ACTION);
        if ((rights & RIGHT_ADMIN) != 0)   sb.append(ABBR_ADMIN);
        if ((rights & RIGHT_SUBFOLDER) != 0)  sb.append(ABBR_CREATE_FOLDER);
        return sb.toString();
    }
}
