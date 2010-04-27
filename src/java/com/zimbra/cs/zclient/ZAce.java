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
package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.zclient.ZClientException;

import org.json.JSONException;

import java.util.Arrays;

public class ZAce implements ToZJSONObject {

    private String mGranteeName;
    private String mGranteeId;
    private ZAce.GranteeType mGranteeType;
    private String mRight;
    private boolean mDeny;
    private String mSecret; // password for guest grantee, accesskey for key grantee

    /** Stolen shamelessly from ACL.java. */
    /** The pseudo-GUID signifying "all authenticated users". */
    public static final String GUID_AUTHUSER = "00000000-0000-0000-0000-000000000000";
    /** The pseudo-GUID signifying "all authenticated and unauthenticated users". */
    public static final String GUID_PUBLIC   = "99999999-9999-9999-9999-999999999999";

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
         * access is granted to all users in a domain
         */
        dom,
        /**
         * accesss is granted to public. no authentication needed.
         */
        pub,
        /**
         * access is granted to all authenticated users
         */
        all, 
        /**
         * access is granted to a non-Zimbra email address and a password 
         */
        gst,
        /**
         * access is granted to a non-Zimbra name and an access key
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

    public ZAce(Element e) throws ServiceException {
        mRight = e.getAttribute(MailConstants.A_RIGHT);
        mDeny = e.getAttributeBool(MailConstants.A_DENY, false);
        mGranteeName = e.getAttribute(MailConstants.A_DISPLAY, null);
        mGranteeId = e.getAttribute(MailConstants.A_ZIMBRA_ID, null);
        mGranteeType = GranteeType.fromString(e.getAttribute(MailConstants.A_GRANT_TYPE));
        mSecret = e.getAttribute(MailConstants.A_PASSWORD, null);
    }
    
    public ZAce(ZAce.GranteeType granteeType, String granteeId, String granteeName, String right, boolean deny, String secret) throws ServiceException {
        mRight = right;
        mDeny = deny;
        mGranteeName = granteeName;
        mGranteeId = granteeId;
        mGranteeType = granteeType;
        mSecret = secret;
    }

    public void toElement(Element parent) {
        Element ace = parent.addElement(MailConstants.E_ACE);
        ace.addAttribute(MailConstants.A_RIGHT, mRight);
        ace.addAttribute(MailConstants.A_GRANT_TYPE, mGranteeType.name());
        ace.addAttribute(MailConstants.A_ZIMBRA_ID, mGranteeId);
        ace.addAttribute(MailConstants.A_DISPLAY, mGranteeName);
        if (mGranteeType == GranteeType.gst)
            ace.addAttribute(MailConstants.A_PASSWORD, mSecret);
        else if (mGranteeType == GranteeType.key)
            ace.addAttribute(MailConstants.A_ACCESSKEY, mSecret);
        
        if (mDeny)
            ace.addAttribute(MailConstants.A_DENY, mDeny);
    }
    
    public String getRight() {
        return mRight;
    }
    
    public boolean getDeny() {
        return mDeny;
    }
    
    public String getRightDisplay() {
        return (mDeny? "-" : "") + mRight;
    }
    
    /**
     * the type of grantee: 
     * "usr", 
     * "grp",
     * "all" (all authenticated users),
     * "guest" (non-Zimbra email address and password)
     * "pub" (public authenticated and unauthenticated access), 
     */
    public ZAce.GranteeType getGranteeType() {
        return mGranteeType;
    }
    
    /***
     * the display name (*not* the zimbra id) of the principal being granted rights;
     * optional if {grantee-type} is "all"
     */
    public String getGranteeName() {
        if (mGranteeName == null)
            return "";
        else
            return mGranteeName;
    }

    /***
     * the zimbraId of the granteee
     */
    public String getGranteeId() {
        return mGranteeId;                                                                          
    }
    
    public void setGranteeId(String granteeId) {
        mGranteeId = granteeId;
    }
    
    public String getPassword() {
        return mSecret;
    }
    
    public String getAccessKey() {
        return mSecret;
    }

    /**
     * Is this grant a public grant?
     *
     */
    public boolean isPublic() {
        return this.getGranteeType().equals(ZAce.GranteeType.pub);
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject jo = new ZJSONObject();
        jo.put("type", mGranteeType.name());
        jo.put("name", mGranteeName);
        jo.put("id", mGranteeId);
        jo.put("right", mRight);
        jo.put("deny", mDeny);
        return jo;
    }

    public String toString() {
        return String.format("[ZGrant %s %s]", mGranteeName, mRight);
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

    public String getGranteeTypeDisplay() {
        switch (mGranteeType) {
        case usr: return "account";
        case grp: return "group";
        case dom: return "domain";
        case pub: return "public";
        case all: return "all";
        case gst: return "guest";
        case key: return "key";
        default: return "unknown";
        }
    }
    
    public int  getGranteeTypeSortOrder() {
        switch (mGranteeType) {
        case usr: return 0;
        case grp: return 3;
        case dom: return 4;
        case pub: return 6;
        case all: return 5;
        case gst: return 1;
        case key: return 2;
        default: return 7; // ??
        }
    }
    
    public static ZAce.GranteeType getGranteeTypeFromDisplay(String name) throws ServiceException {
        if (name.equalsIgnoreCase("account")) return GranteeType.usr;
        else if (name.equalsIgnoreCase("group")) return GranteeType.grp; 
        else if (name.equalsIgnoreCase("domain")) return GranteeType.dom; 
        else if (name.equalsIgnoreCase("public")) return GranteeType.pub;
        else if (name.equalsIgnoreCase("all")) return GranteeType.all;
        else if (name.equalsIgnoreCase("guest")) return GranteeType.gst;
        else if (name.equalsIgnoreCase("key")) return GranteeType.key;
        else throw ZClientException.CLIENT_ERROR("unknown grantee type: "+name, null);
    }
}
