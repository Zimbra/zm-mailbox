/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.mime;

import java.util.ArrayList;
import java.util.List;

public class MimeAddressHeader extends MimeHeader {
    private List<InternetAddress> mAddresses;

    public MimeAddressHeader(final String name, final List<InternetAddress> iaddrs) {
        super(name, null, -1);
        mAddresses = new ArrayList<InternetAddress>(iaddrs);
    }

    @Deprecated public MimeAddressHeader(final String name, final String value) {
        this(name, value.getBytes());
    }

    public MimeAddressHeader(final String name, final byte[] bvalue) {
        super(name, bvalue);
        parseAddresses();
    }

    MimeAddressHeader(MimeHeader header) {
        super(header);
        if (header instanceof MimeAddressHeader) {
            mAddresses = new ArrayList<InternetAddress>(((MimeAddressHeader) header).getAddresses());
        } else {
            parseAddresses();
        }
    }


    private void parseAddresses() {
        if (valueStart > 0) {
            mAddresses = InternetAddress.parseHeader(content, valueStart, content.length - valueStart);
        } else {
            mAddresses = new ArrayList<InternetAddress>(3);
        }
    }

    /** Returns copies of all the addresses from the header.  RFC 5322 groups
     *  are included as individual unexpended members of this list.
     * @see #expandAddresses() */
    public List<InternetAddress> getAddresses() {
        List<InternetAddress> addresses = new ArrayList<InternetAddress>(mAddresses.size());
        for (InternetAddress addr : mAddresses) {
            addresses.add(addr.clone());
        }
        return addresses;
    }

    /** Returns copies of all the addresses from the header, replacing RFC 5322
     *  groups with their component addresses. */
    public List<InternetAddress> expandAddresses() {
        List<InternetAddress> addresses = new ArrayList<InternetAddress>(mAddresses.size());
        for (InternetAddress addr : mAddresses) {
            if (addr instanceof InternetAddress.Group) {
                for (InternetAddress member : ((InternetAddress.Group) addr).getMembers()) {
                    addresses.add(member.clone());
                }
            } else {
                addresses.add(addr.clone());
            }
        }
        return addresses;
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