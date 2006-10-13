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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

import java.util.Arrays;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.soap.Element;

public class ZGrant {

    private String mArgs;
    private String mGranteeName;
    private String mGranteeId;
    private GranteeType mGranteeType;
    private boolean mInherit;
    private String mPermissions;
    
    public enum Permission {
        read('r'),
        write('w'),
        insert('i'),        
        delete('d'),
        administer('a'),        
        workflow('x');                

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
        guest;

        public static GranteeType fromString(String s) throws ServiceException {
            try {
                return GranteeType.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid grantee: "+s+", valid values: "+Arrays.asList(GranteeType.values()), e);
            }
        }
    }


    public ZGrant(Element e) throws ServiceException {
        mArgs = e.getAttribute(MailService.A_ARGS, null);
        mPermissions = e.getAttribute(MailService.A_RIGHTS);
        mGranteeName = e.getAttribute(MailService.A_DISPLAY, null);
        mGranteeId = e.getAttribute(MailService.A_ZIMBRA_ID, null);        
        mGranteeType = GranteeType.fromString(e.getAttribute(MailService.A_GRANT_TYPE));
        mInherit = e.getAttributeBool(MailService.A_INHERIT);
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
    
    /**
     * the type of grantee: "usr", "grp", "dom" (domain),
     * "all" (all authenticated users), "pub" (public authenticated and unauthenticated access), 
     * "guest" (non-Zimbra email address and password)
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
     * whether rights granted on this folder are also granted on all subfolders
     */
    public boolean getInherit() {
        return mInherit;
    }
    
    /**
     *  optional argument.  password when {grantee-type} is "guest"
     */
    public String getArgs() {
        return mArgs;
    }
    
    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add("type", mGranteeType.name());
        sb.add("name", mGranteeName);
        sb.add("id", mGranteeId);
        sb.add("permissions", mPermissions);
        sb.add("inherit", mInherit);
        sb.add("args", mArgs);
        sb.endStruct();
        return sb.toString();
    }

}
