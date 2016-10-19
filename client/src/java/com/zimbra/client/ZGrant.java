/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.client;

import java.util.Arrays;

import org.json.JSONException;

import com.zimbra.common.mailbox.ACLGrant;
import com.zimbra.common.mailbox.GrantGranteeType;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.soap.mail.type.Grant;

public class ZGrant implements ACLGrant, ToZJSONObject {

    private String mArgs;
    private final String mGranteeName;
    private final String mGranteeId;
    private final GranteeType mGranteeType;
    private final String mPermissions;

    /** Stolen shamelessly from ACL.java. */
    /** The pseudo-GUID signifying "all authenticated users". */
    public static final String GUID_AUTHUSER = "00000000-0000-0000-0000-000000000000";
    /** The pseudo-GUID signifying "all authenticated and unauthenticated users". */
    public static final String GUID_PUBLIC   = "99999999-9999-9999-9999-999999999999";


    public enum Permission {
        read('r'),
        write('w'),
        insert('i'),
        delete('d'),
        administer('a'),
        workflow('x'),
        freebusy('f');

        private char mPermChar;

        public char getPermissionChar() { return mPermChar; }

        public static String toNameList(String perms) {
            if (perms == null || perms.length() == 0) return "";
            StringBuilder sb = new StringBuilder();
            for (int i=0; i < perms.length(); i++) {
                String v = null;
                for (Permission f : Permission.values()) {
                    if (f.getPermissionChar() == perms.charAt(i)) {
                        v = f.name();
                        break;
                    }
                }
                if (sb.length() > 0) sb.append(", ");
                sb.append(v == null ? perms.substring(i, i+1) : v);
            }
            return sb.toString();
        }

        Permission(char permChar) {
            mPermChar = permChar;
        }
    }

    public enum GranteeType {
        // Need to keep JAXB class com.zimbra.soap.type.GrantGranteeType in sync with this class
        /**
         * access is granted to an authenticated user
         */
        usr(com.zimbra.soap.type.GrantGranteeType.usr, com.zimbra.common.mailbox.GrantGranteeType.usr),
        /**
         * access is granted to a group of users
         */
        grp(com.zimbra.soap.type.GrantGranteeType.grp, com.zimbra.common.mailbox.GrantGranteeType.grp),
        /**
         * access is granted to users on a cos
         */
        cos(com.zimbra.soap.type.GrantGranteeType.cos, com.zimbra.common.mailbox.GrantGranteeType.cos),
        /**
         * access is granted to public. no authentication needed.
         */
        pub(com.zimbra.soap.type.GrantGranteeType.pub, com.zimbra.common.mailbox.GrantGranteeType.pub),
        /**
         * access is granted to all authenticated users
         */
        all(com.zimbra.soap.type.GrantGranteeType.all, com.zimbra.common.mailbox.GrantGranteeType.all),
        /**
         * access is granted to all users in a domain
         */
        dom(com.zimbra.soap.type.GrantGranteeType.dom, com.zimbra.common.mailbox.GrantGranteeType.dom),
        /**
         * access is granted to a non-Zimbra email address and a password
         */
        guest(com.zimbra.soap.type.GrantGranteeType.guest, com.zimbra.common.mailbox.GrantGranteeType.guest),
        /**
         * access is granted to a non-Zimbra email address and an accesskey
         */
        key(com.zimbra.soap.type.GrantGranteeType.key, com.zimbra.common.mailbox.GrantGranteeType.key);

        private com.zimbra.soap.type.GrantGranteeType jaxbGranteeType;
        private com.zimbra.common.mailbox.GrantGranteeType commonGranteeType;

        GranteeType(com.zimbra.soap.type.GrantGranteeType jaxbGT, com.zimbra.common.mailbox.GrantGranteeType commGT) {
            jaxbGranteeType = jaxbGT;
            commonGranteeType = commGT;
        }

        public static GranteeType fromString(String s) throws ServiceException {
            try {
                return GranteeType.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR(
                        "invalid grantee: " + s + ", valid values: " + Arrays.asList(GranteeType.values()), e);
            }
        }

        /* return equivalent JAXB enum */
        public com.zimbra.soap.type.GrantGranteeType toJaxb() {
            return jaxbGranteeType;
        }

        public static GranteeType fromJaxb(com.zimbra.soap.type.GrantGranteeType jaxbGT) {
            for (GranteeType gt :GranteeType.values()) {
                if (gt.toJaxb() == jaxbGT) {
                    return gt;
                }
            }
            throw new IllegalArgumentException("Unrecognised GranteeType:" + jaxbGT);
        }

        /* return equivalent zm-common enum */
        public com.zimbra.common.mailbox.GrantGranteeType toCommon() {
            return commonGranteeType;
        }

        public static GranteeType fromCommon(com.zimbra.common.mailbox.GrantGranteeType commGT) {
            for (GranteeType gt :GranteeType.values()) {
                if (gt.toCommon() == commGT) {
                    return gt;
                }
            }
            throw new IllegalArgumentException("Unrecognised GranteeType:" + commGT);
        }
    }


    public ZGrant(Element e) throws ServiceException {
        mPermissions = e.getAttribute(MailConstants.A_RIGHTS);
        mGranteeName = e.getAttribute(MailConstants.A_DISPLAY, null);
        mGranteeId = e.getAttribute(MailConstants.A_ZIMBRA_ID, null);
        mGranteeType = GranteeType.fromString(e.getAttribute(MailConstants.A_GRANT_TYPE));

        if (mGranteeType == GranteeType.key)
            mArgs = e.getAttribute(MailConstants.A_ACCESSKEY, null);
        else
            mArgs = e.getAttribute(MailConstants.A_PASSWORD, null);
    }

    public ZGrant(Grant grant) throws ServiceException {
        mPermissions = grant.getPerm();
        mGranteeName = grant.getGranteeName();
        mGranteeId = grant.getGranteeId();
        mGranteeType = GranteeType.fromString(grant.getGranteeType().toString());
        if (mGranteeType == GranteeType.key)
            mArgs = grant.getAccessKey();
        else
            mArgs = grant.getGuestPassword();
    }

    public void toElement(Element parent) {
        Element grant = parent.addElement(MailConstants.E_GRANT);
        if (mPermissions != null)
            grant.addAttribute(MailConstants.A_RIGHTS, mPermissions);

        grant.addAttribute(MailConstants.A_GRANT_TYPE, mGranteeType.name());

        if (mGranteeId != null)
            grant.addAttribute(MailConstants.A_ZIMBRA_ID, mGranteeId);

        if (mGranteeName != null)
            grant.addAttribute(MailConstants.A_DISPLAY, mGranteeName);

        if (mArgs != null && mArgs.length() > 0) {
            if (mGranteeType == GranteeType.key)
                grant.addAttribute(MailConstants.A_ACCESSKEY, mArgs);
            else
                grant.addAttribute(MailConstants.A_ARGS, mArgs);
        }
    }

    /**
     *  some combination of (r)ead, (w)rite, (i)nsert, (d)elete, (a)dminister, workflow action (x)
     */
    @Override
    public String getPermissions() {
        return mPermissions;
    }

    private boolean hasPerm(Permission p) {
        return (mPermissions != null) && mPermissions.indexOf(p.getPermissionChar()) != -1;
    }

    public boolean canAdminister() {
        return hasPerm(Permission.administer);
    }

    public boolean canDelete() {
        return hasPerm(Permission.delete);
    }

    public boolean canInsert() {
        return hasPerm(Permission.insert);
    }

    public boolean canRead() {
        return hasPerm(Permission.read);
    }

    public boolean canWorkflow() {
        return hasPerm(Permission.workflow);
    }

    public boolean canWrite() {
        return hasPerm(Permission.write);
    }

    public boolean canViewFreeBusy() {
        return hasPerm(Permission.freebusy);
    }

    /**
     * the type of grantee: "usr", "grp", "dom" (domain),
     * "all" (all authenticated users), "pub" (public authenticated and unauthenticated access),
     * "guest" (non-Zimbra email address and password)
     * "key" (access key)
     */
    public GranteeType getGranteeType() {
        return mGranteeType;
    }

    @Override
    public GrantGranteeType getGrantGranteeType() {
        return (mGranteeType == null) ? null : mGranteeType.commonGranteeType;
    }

    /***
     * the display name (*not* the zimbra id) of the principal being granted rights;
     * optional if {grantee-type} is "all"
     */
    @Override
    public String getGranteeName() {
        return mGranteeName;
    }

    /***
     * the zimbraId of the granteee
     */
    @Override
    public String getGranteeId() {
        return mGranteeId;
    }

    /**
     *  optional argument.  password when {grantee-type} is "guest"
     */
    public String getArgs() {
        return mArgs;
    }

    void setAccessKey(String accesskey) {
        mArgs = accesskey;
    }

    /**
     * Is this grant a public grant?
     *
     */
    public boolean isPublic() {
        return this.getGranteeType().equals(GranteeType.pub);
    }

    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject jo = new ZJSONObject();
        jo.put("type", mGranteeType.name());
        jo.put("name", mGranteeName);
        jo.put("id", mGranteeId);
        jo.put("permissions", mPermissions);
        jo.put("args", mArgs);
        return jo;
    }

    @Override
    public String toString() {
        return String.format("[ZGrant %s %s %s]", mGranteeType.name(), mGranteeName, mPermissions);
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

}
