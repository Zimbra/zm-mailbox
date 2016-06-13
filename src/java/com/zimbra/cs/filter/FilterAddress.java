/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
