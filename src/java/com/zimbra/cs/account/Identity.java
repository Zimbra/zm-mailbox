/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

package com.zimbra.cs.account;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;

/**
 * @author schemers
 */
public class Identity extends AccountProperty implements Comparable {

    public Identity(Account acct, String name, String id, Map<String, Object> attrs, Provisioning prov) {
        super(acct, name, id, attrs, null, prov);
    }
    
    /**
     * this should only be used internally by the server. it doesn't modify the real id, just
     * the cached one.
     * @param id
     */
    public void setId(String id) {
        mId = id;
        getRawAttrs().put(Provisioning.A_zimbraPrefIdentityId, id);
    }

    public InternetAddress getFriendlyEmailAddress() {
        String personalPart = getAttr(Provisioning.A_zimbraPrefFromDisplay);
        if (personalPart == null || personalPart.trim().equals(""))
            personalPart = null;
        String address = getAttr(Provisioning.A_zimbraPrefFromDisplay);

        try {
            return new JavaMailInternetAddress(address, personalPart, MimeConstants.P_CHARSET_UTF8);
        } catch (UnsupportedEncodingException e) { }

        // UTF-8 should *always* be supported (i.e. this is actually unreachable)
        try {
            // fall back to using the system's default charset (also pretty much guaranteed not to be "unsupported")
            return new JavaMailInternetAddress(address, personalPart);
        } catch (UnsupportedEncodingException e) { }

        // if we ever reached this point (which we won't), just return an address with no personal part
        InternetAddress ia = new JavaMailInternetAddress();
        ia.setAddress(address);
        return ia;
    }
}
