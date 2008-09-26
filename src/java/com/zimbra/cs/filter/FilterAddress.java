/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.filter;

import org.apache.jsieve.mail.MailAdapter.Address;

import com.zimbra.common.util.EmailUtil;

/**
 * Used for returning an email address to the jSieve
 * address test. 
 */
class FilterAddress implements Address {
    
    static final Address[] EMPTY_ADDRESS_ARRAY = new Address[0];
    
    private String mLocalPart;
    private String mDomain;
    
    FilterAddress(String address) {
        String[] parts = EmailUtil.getLocalPartAndDomain(address);
        if (parts != null) {
            mLocalPart = parts[0];
            mDomain = parts[1];
        }
    }

    public String getLocalPart() {
        return mLocalPart;
    }

    public String getDomain() {
        return mDomain;
    }
}
