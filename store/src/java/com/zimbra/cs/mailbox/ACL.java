/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailbox;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.codec.binary.Hex;

import com.zimbra.common.mailbox.ACLGrant;
import com.zimbra.common.mailbox.GrantGranteeType;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;

/**
 * @since Jul 5, 2005
 * @author dkarp
 */
public final class ACL {

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
    /** The right to view a private item. */
    public static final short RIGHT_PRIVATE = 0x0400;
    /** The right to view free/busy on a calendar folder. */
    public static final short RIGHT_FREEBUSY = 0x0800;

    /** The combination of rights that equates to {@link #RIGHT_SUBFOLDER}. */
    private static final short SUBFOLDER_RIGHTS = RIGHT_READ | RIGHT_INSERT;

    /** Bitmask of all rights that can be explicitly granted.  <i>Note:
     *  CAN_CREATE_FOLDER is calculated and hence cannot be granted. */
    private static final short GRANTABLE_RIGHTS = RIGHT_READ    | RIGHT_WRITE    | RIGHT_INSERT |
                                                  RIGHT_DELETE  | RIGHT_ACTION   | RIGHT_ADMIN  |
                                                  RIGHT_PRIVATE | RIGHT_FREEBUSY ;

    public static final short ROLE_VIEW         = ACL.RIGHT_READ;
    public static final short ROLE_MANAGER      = ACL.RIGHT_READ | ACL.RIGHT_WRITE | ACL.RIGHT_INSERT |
                                                  ACL.RIGHT_DELETE | ACL.RIGHT_ACTION;
    public static final short ROLE_ADMIN        = ACL.ROLE_MANAGER | ACL.RIGHT_ADMIN;

    /** The grantee of these rights is the zimbraId for a user. */
    public static final byte GRANTEE_USER     = 1; /* same as GrantGranteeType.usr */
    /** The grantee of these rights is the zimbraId for a distribution list. */
    public static final byte GRANTEE_GROUP    = 2; /* same as GrantGranteeType.grp */
    /** The grantee of these rights is all authenticated users. */
    public static final byte GRANTEE_AUTHUSER = 3; /* same as GrantGranteeType.all */
    /** The grantee of these rights is the zimbraId for a domain. */
    public static final byte GRANTEE_DOMAIN   = 4; /* same as GrantGranteeType.dom */
    /** The grantee of these rights is the zimbraId for a COS. */
    public static final byte GRANTEE_COS      = 5; /* same as GrantGranteeType.cos */
    /** The grantee of these rights is all authenticated and unauthenticated users. */
    public static final byte GRANTEE_PUBLIC   = 6; /* same as GrantGranteeType.pub */
    /** The grantee of these rights is a named non Zimbra user identified by the email address */
    public static final byte GRANTEE_GUEST    = 7; /* same as GrantGranteeType.guest */
    /** The grantee of these rights is a named non Zimbra user identified by the access key */
    public static final byte GRANTEE_KEY      = 8; /* same as GrantGranteeType.key */

    private static final int ACCESSKEY_SIZE_BYTES = 16;

    public static class Grant implements ACLGrant {
        /** The zimbraId of the entry being granted rights. */
        private String mGrantee;
        /** The display name of the grantee, which is often the email address of grantee. */
        private String mName;
        /** The type of object the grantee's ID refers to.
         *  For instance, {@link ACL#GRANTEE_USER}. */
        private final byte mType;
        /** A bitmask of the rights being granted.  For instance,
         *  <tt>{@link ACL#RIGHT_INSERT} | {@link ACL#RIGHT_READ}</tt>. */
        private short mRights;
        /** The password for guest accounts, or hex ascii string version of the accesskey for "key" grantees. */
        private String mSecret;
        /** Time when this grant expires.
         *  Value of 0 indicates that expiry is derived from ACL, except when {@link #mType} is
         *  {@link ACL.GRANTEE_PUBLIC}. */
        private long mExpiry = 0;

        /** Creates a new Grant object granting access to a user or class
         *  of users.  <tt>zimbraId</tt> may be <tt>null</tt>
         *  if the <tt>type</tt> is {@link ACL#GRANTEE_PUBLIC}.
         *
         * @param zimbraId  The zimbraId of the entry being granted rights.
         * @param type      The type of object the grantee's ID refers to.
         * @param rights    A bitmask of the rights being granted.
         * @see ACL */
        Grant(String zimbraId, byte type, short rights) {
            mGrantee = zimbraId;
            mType    = type;
            mRights  = (short) (rights & GRANTABLE_RIGHTS);
        }
        Grant(String zimbraId, byte type, short rights, String secret, long expiry) {
            this(zimbraId, type, rights);
            if (mType == GRANTEE_GUEST || mType == GRANTEE_KEY)
                mSecret = secret;
            mExpiry = expiry;
        }

        /** Creates a new Grant object from a decoded {@link Metadata} hash.
         *
         * @param meta  The Metadata object containing ACL data.
         * @throws ServiceException if any required fields are missing. */
        public Grant(Metadata meta) throws ServiceException {
            mType    = (byte) meta.getLong(FN_TYPE);
            mRights  = (short) (meta.getLong(FN_RIGHTS) & GRANTABLE_RIGHTS);
            mName    = meta.get(FN_NAME, null);
            if (hasGrantee())
                mGrantee = meta.get(FN_GRANTEE);
            if (mType == ACL.GRANTEE_GUEST)
                mSecret = meta.get(FN_PASSWORD, null);
            else if (mType == ACL.GRANTEE_KEY)
                mSecret = meta.get(FN_ACCESSKEY);
            mExpiry = meta.getLong(FN_EXPIRY, 0);
        }

        /** Returns true if there is an explicit grantee. */
        public boolean hasGrantee() { return mType != ACL.GRANTEE_AUTHUSER && mType != ACL.GRANTEE_PUBLIC; }
        /** Returns the zimbraId of the entry granted rights. */
        @Override
        public String getGranteeId() { return hasGrantee() ? mGrantee : null; }
        /** Returns type of object the grantee's ID refers to. */
        public byte getGranteeType() { return mType; }

        @Override
        public GrantGranteeType getGrantGranteeType() {
            return GrantGranteeType.fromByte(mType);
        }
        /** Returns the bitmask of the rights granted. */
        public short getGrantedRights() { return mRights; }

        @Override
        public String getPermissions() {
            return ACL.rightsToString(getGrantedRights());
        }

        /** Returns the rights granted to the given {@link Account} by this
         *  <tt>Grant</tt>.  If the grant does not apply to the Account,
         *  returns <tt>0</tt>. */
        public short getGrantedRights(Account acct, ACL acl) throws ServiceException {
            if (isExpired(acl)) {
                if (ZimbraLog.acl.isTraceEnabled()) {
                    ZimbraLog.acl.trace("ACL.GrantedRights 0 for acl=%s (expired)", acl);
                }
                return 0;
            }
            if (matches(acct)) {
                if (ZimbraLog.acl.isTraceEnabled()) {
                    ZimbraLog.acl.trace("ACL.GrantedRights %s for acl=%s", mRights, acl);
                }
                return mRights;
            }
            if (ZimbraLog.acl.isTraceEnabled()) {
                ZimbraLog.acl.trace("ACL.GrantedRights 0 for acl=%s (does not match %s)",
                        acl, acct == null ? "'null acct'" : acct.getName());
            }
            return 0;
        }

        private boolean isExpired(ACL acl) {
            long expiry = getEffectiveExpiry(acl);
            return expiry != 0 && System.currentTimeMillis() > expiry;
        }

        public long getEffectiveExpiry(ACL acl) {
            long expiry = mExpiry;
            if (expiry == 0) {
                if (mType == ACL.GRANTEE_GUEST || mType == ACL.GRANTEE_KEY) {
                    expiry = acl.getGuestGrantExpiry();
                } else if (mType != ACL.GRANTEE_PUBLIC) {
                    expiry = acl.getInternalGrantExpiry();
                }
            }
            return expiry;
        }

        /** Returns the display name of grantee. */
        @Override
        public String getGranteeName() { return mName; }
        /** Sets the display name of grantee. */
        public void setGranteeName(String name) { mName = name; }

        /** Returns whether this grant applies to the given {@link Account}.
         *  If <tt>acct</tt> is <tt>null</tt>, only return
         *  <tt>true</tt> if the grantee is {@link ACL#GRANTEE_PUBLIC}. */
        public boolean matches(Account acct) throws ServiceException {
            Provisioning prov = Provisioning.getInstance();
            if (acct == null)
                return mType == ACL.GRANTEE_PUBLIC;
            switch (mType) {
                case ACL.GRANTEE_PUBLIC:   return true;
                case ACL.GRANTEE_AUTHUSER: return isInternalAccount(acct);
                case ACL.GRANTEE_COS:      return mGrantee.equals(getId(prov.getCOS(acct)));
                case ACL.GRANTEE_DOMAIN:   return matchesDomainGrantee(acct, prov);
                case ACL.GRANTEE_GROUP:    return prov.inACLGroup(acct, mGrantee);
                case ACL.GRANTEE_USER:     return mGrantee.equals(acct.getId());
                case ACL.GRANTEE_GUEST:    return matchesGuestAccount(acct);
                case ACL.GRANTEE_KEY:      return matchesAccessKey(acct);
                default:  throw ServiceException.FAILURE("unknown ACL grantee type: " + mType, null);
            }
        }

        private boolean matchesDomainGrantee(Account acct, Provisioning prov) throws ServiceException {
            return !acct.isIsExternalVirtualAccount() && mGrantee.equals(getId(prov.getDomain(acct)));
        }

        private boolean isInternalAccount(Account acct) {
            return !acct.getId().equals(GuestAccount.GUID_PUBLIC) && !acct.isIsExternalVirtualAccount();
        }

        private boolean matchesGuestAccount(Account acct) {
            if (acct instanceof GuestAccount) {
                // Now that we can have virtual accounts, the secret is null/empty in virtual account
                // sharing, and so we need to block access to GuestAccount(s)
                if (!StringUtil.isNullOrEmpty(mSecret)) {
                    return ((GuestAccount) acct).matches(mGrantee, mSecret);
                }
            } else if (acct.isIsExternalVirtualAccount()) {
                return mGrantee.equalsIgnoreCase(acct.getExternalUserMailAddress());
            }
            return false;
        }

        private boolean matchesAccessKey(Account acct) {
            if (!(acct instanceof GuestAccount))
                return false;
            return ((GuestAccount) acct).matchesAccessKey(mGrantee, mSecret);
        }

        /** Utility function: Returns the zimbraId for a null-checked LDAP
         *  entry. */
        private static final String getId(NamedEntry entry) {
            return (entry == null ? null : entry.getId());
        }

        /** Returns whether the principal id exactly matches the grantee.
         *  <tt>zimbraId</tt> must be {@link GuestAccount#GUID_PUBLIC} (<tt>null</tt>
         *  is also OK) if the actual grantee is {@link ACL#GRANTEE_PUBLIC}.
         *  <tt>zimbraId</tt> must be {@link GuestAccount#GUID_AUTHUSER} if the actual
         *  grantee is {@link ACL#GRANTEE_AUTHUSER}.
         *
         * @param zimbraId  The zimbraId of the principal. */
        public boolean isGrantee(String zimbraId) {
            if (zimbraId == null || zimbraId.equals(GuestAccount.GUID_PUBLIC))
                return (mType == GRANTEE_PUBLIC);
            else if (zimbraId.equals(GuestAccount.GUID_AUTHUSER))
                return (mType == GRANTEE_AUTHUSER);
            return mType == GRANTEE_GUEST || mType == GRANTEE_KEY ?
                    zimbraId.equalsIgnoreCase(mGrantee) : zimbraId.equals(mGrantee);
        }

        /** Updates the granted rights in the <tt>Grant</tt>.  The old
         *  set of rights is discarded.
         *
         * @param rights   A bitmask of the rights being granted.
         * @param inherit  Whether subfolders inherit these same rights.
         * @see ACL */
        void setRights(short rights) {
            mRights = rights;
        }

        /** For grants to external users, sets the password/accesskey required to
         *  access the resource. */
        void setPassword(String password) {
            if ((mType == GRANTEE_GUEST || mType == GRANTEE_KEY) && password != null)
                mSecret = password;
        }

        /**
         * Only for grants to external users
         */
        public String getPassword() {
            return mSecret;
        }

        /** Updates the expiry time for the grant.
         *
         * @param expiry
         */
        public void setExpiry(long expiry) {
            mExpiry = expiry;
        }

        public long getExpiry() {
            return mExpiry;
        }


        private static final String FN_GRANTEE   = "g";
        private static final String FN_NAME      = "n";
        private static final String FN_TYPE      = "t";
        private static final String FN_RIGHTS    = "r";
        private static final String FN_PASSWORD  = "a";
        private static final String FN_ACCESSKEY = "k";
        private static final String FN_EXPIRY    = "e";

        /** Encapsulates this <tt>Grant</tt> as a {@link Metadata} object
         *  for serialization. */
        public Metadata encode() {
            Metadata meta = new Metadata();
            meta.put(FN_GRANTEE,  hasGrantee() ? mGrantee : null);
            meta.put(FN_NAME,     mName);
            meta.put(FN_TYPE,     mType);
            // FIXME: use "rwidxsca" instead of numeric value
            meta.put(FN_RIGHTS,   mRights);

            if (mType == GRANTEE_KEY)
                meta.put(FN_ACCESSKEY, mSecret);
            else
                meta.put(FN_PASSWORD, mSecret);
            meta.put(FN_EXPIRY, mExpiry);

            return meta;
        }
    }

    /** The <tt>List</tt> of all {@link ACL.Grant}s set on an item. */
    private final List<Grant> mGrants = new CopyOnWriteArrayList<Grant>();
    /** Time when all grants to internal users or groups expire. Value of 0 indicates that they never expire. */
    private long mInternalGrantExpiry = 0;
    /** Time when all grants to guest/external users expire. Value of 0 indicates that they never expire. */
    private long mGuestGrantExpiry = 0;

    public ACL()  { }

    public ACL(long internalGrantExpiry, long guestGrantExpiry) {
        mInternalGrantExpiry = internalGrantExpiry;
        mGuestGrantExpiry = guestGrantExpiry;
    }

    public ACL(MetadataList mlist) {
        decodeGrants(mlist);
    }

    public ACL(Metadata meta) {
        MetadataList mlist = null;
        try {
            mlist = meta.getList(FN_GRANTS, true);
            mInternalGrantExpiry = meta.getLong(FN_INT_GRANT_EXPIRY, 0);
            mGuestGrantExpiry = meta.getLong(FN_GST_GRANT_EXPIRY, 0);
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("malformed ACL: " + meta, e);
        }
        if (mlist != null) {
            decodeGrants(mlist);
        }
    }

    private void decodeGrants(MetadataList mlist) {
        for (int i = 0; i < mlist.size(); i++) {
            try {
                mGrants.add(new Grant(mlist.getMap(i)));
            } catch (ServiceException e) {
                ZimbraLog.mailbox.warn("malformed permission grant: " + mlist, e);
            }
        }
    }

    public long getInternalGrantExpiry() {
        return mInternalGrantExpiry;
    }

    public long getGuestGrantExpiry() {
        return mGuestGrantExpiry;
    }

    /** Returns the bitmask of rights granted to the user by the ACL, or
     *  <tt>null</tt> if there are no rights granted to anyone.  (Note that
     *  if rights are granted to <i>other</i> accounts but not to the
     *  specified user, returns <tt>0</tt>.)
     *
     * @param authuser   The user to gather rights for.
     * @return A <tt>Short</tt> containing the OR'ed-together rights
     *         granted to the user, or <tt>null</tt>. */
    public Short getGrantedRights(Account authuser) throws ServiceException {
        if (mGrants.isEmpty()) {
            if (ZimbraLog.acl.isTraceEnabled()) {
                ZimbraLog.acl.trace("ACL.GrantedRights NULL (no grants)");
            }
            return null;
        }

        short rightsGranted = 0;
        for (Grant grant : mGrants) {
            rightsGranted |= grant.getGrantedRights(authuser, this);
        }
        if ((rightsGranted & SUBFOLDER_RIGHTS) == SUBFOLDER_RIGHTS)
            rightsGranted |= RIGHT_SUBFOLDER;

        if (ZimbraLog.acl.isTraceEnabled()) {
            ZimbraLog.acl.trace("ACL.GrantedRights %s from %s grants", rightsGranted, mGrants.size());
        }
        return Short.valueOf(rightsGranted);
    }

    /** Returns whether there are any grants encapsulated by this ACL. */
    public boolean isEmpty() {
        return mGrants.isEmpty();
    }

    public ACL.Grant grantAccess(String zimbraId, byte type, short rights, String secret)
    throws ServiceException {
        return grantAccess(zimbraId, type, rights, secret, 0);
    }

    /** Grants the specified set of rights to the target.  If another set
     *  of rights has already been granted to the exact given (id, type)
     *  pair, the previous set is revoked and the new set is granted.
     *
     * @param zimbraId  The zimbraId of the entry being granted rights.
     * @param type      The type of object the grantee's ID refers to.
     * @param rights    A bitmask of the rights being granted.
     * @param secret    password or accesskey
     * @param expiry    time when grant expires
     * @return          the grant object
     */
    public ACL.Grant grantAccess(String zimbraId, byte type, short rights, String secret, long expiry)
    throws ServiceException {

        if (expiry != 0) {
            if (type == ACL.GRANTEE_GUEST || type == ACL.GRANTEE_KEY) {
                if (mGuestGrantExpiry != 0 && expiry > mGuestGrantExpiry) {
                   throw ServiceException.PERM_DENIED("share expiration policy conflict");
                }
            } else if (type != ACL.GRANTEE_PUBLIC) {
                // internal grantee
                if (mInternalGrantExpiry != 0 && expiry > mInternalGrantExpiry) {
                    throw ServiceException.PERM_DENIED("share expiration policy conflict");
                }
            }
        }

        if (type == GRANTEE_AUTHUSER)
            zimbraId = GuestAccount.GUID_AUTHUSER;
        else if (type == GRANTEE_PUBLIC)
            zimbraId = GuestAccount.GUID_PUBLIC;
        else if (zimbraId == null)
            throw ServiceException.INVALID_REQUEST("missing grantee id", null);

        // always generate a new key (if not provided) for updating or new key grants
        if (type == GRANTEE_KEY && secret == null)
            secret = generateAccessKey();

        if (!mGrants.isEmpty()) {
            for (Grant grant : mGrants) {
                if (grant.isGrantee(zimbraId)) {
                    if (grant.getGrantedRights() == rights &&
                            ((type != GRANTEE_GUEST && type != GRANTEE_KEY) ||
                                    StringUtil.equal(grant.getPassword(), secret)) &&
                            (grant.getExpiry() == expiry)) {
                        // same grant is already in the ACL
                        throw MailServiceException.GRANTEE_EXISTS(zimbraId, null);

                    }
                    grant.setRights(rights);
                    if (type == GRANTEE_GUEST || type == GRANTEE_KEY)
                        grant.setPassword(secret);
                    grant.setExpiry(expiry);
                    return grant;
                }
            }
        }

        Grant grant = new Grant(zimbraId, type, rights, secret, expiry);
        mGrants.add(grant);
        return grant;
    }

    /** Removes the set of rights granted to the specified id.  If no rights
     *  were previously granted to the target, no error is thrown and
     *  <tt>false</tt> is returned.
     *
     * @param zimbraId  The zimbraId of the entry being revoked rights.
     * @return whether an {@link Grant} was actually removed from the set. */
    public boolean revokeAccess(String zimbraId) {
        if (mGrants == null || mGrants.isEmpty())
            return false;
        int count = mGrants.size();
        for (Grant grant : mGrants) {
            if (grant.isGrantee(zimbraId)) {
                mGrants.remove(grant);
            }
        }
        return (mGrants.size() != count);
    }

    public void  setGuestGrantExpiry(long expiry) {
        mGuestGrantExpiry = expiry;
    }

    public void  setInternalGrantExpiry(long expiry) {
        mInternalGrantExpiry = expiry;
    }

    private static final String FN_GRANTS           = "g";
    private static final String FN_INT_GRANT_EXPIRY = "ie";
    private static final String FN_GST_GRANT_EXPIRY = "ge";

    public Metadata encode() {
        Metadata meta = new Metadata();
        MetadataList mlist = new MetadataList();
        for (Grant grant : mGrants)
            mlist.add(grant.encode());
        meta.put(FN_GRANTS, mlist);
        meta.put(FN_INT_GRANT_EXPIRY, mInternalGrantExpiry);
        meta.put(FN_GST_GRANT_EXPIRY, mGuestGrantExpiry);
        return meta;
    }

    @Override public String toString() {
        return encode().toString();
    }

    /** Returns a different <tt>ACL</tt> with the same contents. */
    public ACL duplicate()  { return new ACL(encode()); }

    /** Returns the list of this <tt>ACL</tt>'s set of encapsulated
     *  {@link ACL.Grant} objects. */
    public List<Grant> getGrants() {
        return Collections.unmodifiableList(mGrants);
    }

    public int getNumberOfGrantsByType(byte granteeType) {
        int i = 0;
        if (mGrants != null) {
            for (Grant grant : mGrants) {
                if (grant.getGranteeType() == granteeType) {
                    i++;
                }
            }
        }
        return i;
    }

    public static final char ABBR_READ = 'r';
    public static final char ABBR_WRITE = 'w';
    public static final char ABBR_INSERT = 'i';
    public static final char ABBR_DELETE = 'd';
    public static final char ABBR_ACTION = 'x';
    public static final char ABBR_ADMIN = 'a';
    public static final char ABBR_PRIVATE = 'p';
    public static final char ABBR_FREEBUSY = 'f';
    public static final char ABBR_CREATE_FOLDER = 'c';

    public static short stringToRights(String encoded) throws ServiceException {
        short rights = 0;
        if (encoded != null && encoded.length() != 0) {
            for (int i = 0; i < encoded.length(); i++) {
                switch (encoded.charAt(i)) {
                    case ABBR_READ:     rights |= RIGHT_READ;     break;
                    case ABBR_WRITE:    rights |= RIGHT_WRITE;    break;
                    case ABBR_INSERT:   rights |= RIGHT_INSERT;   break;
                    case ABBR_DELETE:   rights |= RIGHT_DELETE;   break;
                    case ABBR_ACTION:   rights |= RIGHT_ACTION;   break;
                    case ABBR_ADMIN:    rights |= RIGHT_ADMIN;    break;
                    case ABBR_PRIVATE:  rights |= RIGHT_PRIVATE;  break;
                    case ABBR_FREEBUSY: rights |= RIGHT_FREEBUSY; break;
                    case ABBR_CREATE_FOLDER:                      break;
                    default:  throw ServiceException.INVALID_REQUEST("unknown right: " + encoded.charAt(i), null);
                }
            }
        }
        return rights;
    }

    public static String rightsToString(short rights) {
        if (rights == 0)
            return "";
        StringBuffer sb = new StringBuffer();
        if ((rights & RIGHT_READ) != 0)     sb.append(ABBR_READ);
        if ((rights & RIGHT_WRITE) != 0)    sb.append(ABBR_WRITE);
        if ((rights & RIGHT_INSERT) != 0)   sb.append(ABBR_INSERT);
        if ((rights & RIGHT_DELETE) != 0)   sb.append(ABBR_DELETE);
        if ((rights & RIGHT_ACTION) != 0)   sb.append(ABBR_ACTION);
        if ((rights & RIGHT_ADMIN) != 0)    sb.append(ABBR_ADMIN);
        if ((rights & RIGHT_PRIVATE) != 0)  sb.append(ABBR_PRIVATE);
        if ((rights & RIGHT_FREEBUSY) != 0) sb.append(ABBR_FREEBUSY);
        if ((rights & RIGHT_SUBFOLDER) != 0)  sb.append(ABBR_CREATE_FOLDER);
        return sb.toString();
    }

    public static byte stringToType(String typeStr) throws ServiceException {
        if (typeStr.equalsIgnoreCase("usr"))  return ACL.GRANTEE_USER;
        if (typeStr.equalsIgnoreCase("grp"))  return ACL.GRANTEE_GROUP;
        if (typeStr.equalsIgnoreCase("cos"))  return ACL.GRANTEE_COS;
        if (typeStr.equalsIgnoreCase("dom"))  return ACL.GRANTEE_DOMAIN;
        if (typeStr.equalsIgnoreCase("all"))  return ACL.GRANTEE_AUTHUSER;
        if (typeStr.equalsIgnoreCase("pub"))  return ACL.GRANTEE_PUBLIC;
        if (typeStr.equalsIgnoreCase("guest")) return ACL.GRANTEE_GUEST;
        if (typeStr.equalsIgnoreCase("key"))  return ACL.GRANTEE_KEY;
        throw ServiceException.INVALID_REQUEST("unknown grantee type: " + typeStr, null);
    }

    public static String typeToString(byte type) {
        if (type == ACL.GRANTEE_USER)      return "usr";
        if (type == ACL.GRANTEE_GROUP)     return "grp";
        if (type == ACL.GRANTEE_PUBLIC)    return "pub";
        if (type == ACL.GRANTEE_AUTHUSER)  return "all";
        if (type == ACL.GRANTEE_COS)       return "cos";
        if (type == ACL.GRANTEE_DOMAIN)    return "dom";
        if (type == ACL.GRANTEE_GUEST)     return "guest";
        if (type == ACL.GRANTEE_KEY)       return "key";
        return null;
    }

    public static String generateAccessKey() {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[ACCESSKEY_SIZE_BYTES];
        random.nextBytes(key);

        // in the form of e.g. 8d159aed5fb9431d8ac52db5e20baafb
        return new String(Hex.encodeHex(key));
    }
}
