/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

    @Override
    public EntryType getEntryType() {
        return EntryType.IDENTITY;
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
        String address = getAttr(Provisioning.A_zimbraPrefFromAddress);

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
