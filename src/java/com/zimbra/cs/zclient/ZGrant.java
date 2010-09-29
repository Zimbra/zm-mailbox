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

package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.soap.mail.type.Grant;

import org.json.JSONException;

import java.util.Arrays;

public class ZGrant implements ToZJSONObject {

    private String mArgs;
    private String mGranteeName;
    private String mGranteeId;
    private GranteeType mGranteeType;
    private String mPermissions;

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
        /**
         * access is granted to an authenticated user
         */
        usr, 
        /**
         * access is granted to a group of users
         */
        grp,
        /**
         * accesss is granted to public. no authentication needed.
         */
        pub,
        /**
         * access is granted to all authenticated users
         */
        all, 
        /**
         * access is granted to all users in a domain
         */
        dom, 
        /**
         * access is granted to a non-Zimbra email address and a password 
         */
        guest,
        /**
         * access is granted to a non-Zimbra email address and an accesskey
         */
        key;

        public static GranteeType fromString(String s) throws ServiceException {
            try {
                return GranteeType.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid grantee: "+s+", valid values: "+Arrays.asList(GranteeType.values()), e);
            }
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

    /***
     * the display name (*not* the zimbra id) of the principal being granted rights;
     * optional if {grantee-type} is "all"
     */
    public String getGranteeName() {
        return mGranteeName;
    }

    /***
     * the zimbraId of the granteee
     */
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
    
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject jo = new ZJSONObject();
        jo.put("type", mGranteeType.name());
        jo.put("name", mGranteeName);
        jo.put("id", mGranteeId);
        jo.put("permissions", mPermissions);
        jo.put("args", mArgs);
        return jo;
    }

    public String toString() {
        return String.format("[ZGrant %s %s %s]", mGranteeType.name(), mGranteeName, mPermissions);
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }
}
