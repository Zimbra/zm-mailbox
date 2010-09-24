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
package com.zimbra.common.mime;

import java.util.ArrayList;
import java.util.List;

public class MimeAddressHeader extends MimeHeader {
    private List<InternetAddress> mAddresses;

    public MimeAddressHeader(final String name, final List<InternetAddress> iaddrs) {
        super(name, null, -1);
        mAddresses = new ArrayList<InternetAddress>(iaddrs);
    }

    public MimeAddressHeader(final String name, final String value) {
        super(name, value);
        if (mValueStart > 0) {
            mAddresses = InternetAddress.parseHeader(mContent, mValueStart, mContent.length - mValueStart);
        } else {
            mAddresses = new ArrayList<InternetAddress>();
        }
    }

    public MimeAddressHeader(final String name, final byte[] bvalue) {
        super(name, bvalue);
        if (mValueStart > 0) {
            mAddresses = InternetAddress.parseHeader(mContent, mValueStart, mContent.length - mValueStart);
        } else {
            mAddresses = new ArrayList<InternetAddress>();
        }
    }

    public List<InternetAddress> getAddresses() {
        return new ArrayList<InternetAddress>(mAddresses);
    }

    public MimeAddressHeader addAddress(InternetAddress iaddr) {
        mAddresses.add(iaddr);
        markDirty();
        return this;
    }

    @Override protected void reserialize() {
        if (isDirty()) {
            StringBuilder value = new StringBuilder();
            for (int i = 0; i < mAddresses.size(); i++) {
                value.append(i == 0 ? "" : ", ").append(mAddresses.get(i));
            }
            // FIXME: need to fold every 75 bytes
            updateContent(value.toString().getBytes());
        }
    }
}