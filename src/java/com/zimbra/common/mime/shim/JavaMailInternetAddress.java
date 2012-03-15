/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.common.mime.shim;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

public class JavaMailInternetAddress extends InternetAddress implements JavaMailShim {
    private static final long serialVersionUID = -8715292468770012173L;
    private static final boolean ZPARSER = JavaMailMimeMessage.ZPARSER;

    private com.zimbra.common.mime.InternetAddress mAddress;

    JavaMailInternetAddress(com.zimbra.common.mime.InternetAddress addr, boolean parsed) {
        mAddress = parsed ? reverseIfNecessary(addr) : addr;
    }

    public JavaMailInternetAddress() {
        super();
        if (ZPARSER) {
            mAddress = new com.zimbra.common.mime.InternetAddress();
        }
    }

    public JavaMailInternetAddress(String address) throws AddressException {
        // use the superclass constructor since its members aren't readable by the subclass
        super(ZPARSER ? "a@b.com" : address);
        if (ZPARSER) {
            List<com.zimbra.common.mime.InternetAddress> addrs = com.zimbra.common.mime.InternetAddress.parseHeader(address);
            if (addrs.size() != 1) {
                throw new AddressException("Illegal address", address);
            }
            mAddress = reverseIfNecessary(addrs.get(0));
        }
    }

    public JavaMailInternetAddress(String address, boolean strict) throws AddressException {
        this(address);
        if (strict) {
            validate();
        }
    }

    public JavaMailInternetAddress(String address, String personal)
    throws UnsupportedEncodingException {
        this(address, personal, null);
    }

    public JavaMailInternetAddress(String address, String personal, String charset)
    throws UnsupportedEncodingException {
        super();
        if (ZPARSER) {
            mAddress = new com.zimbra.common.mime.InternetAddress(personal, address).setCharset(charset);
        } else {
            setAddress(address);
            setPersonal(personal, charset);
        }
    }

    com.zimbra.common.mime.InternetAddress reverseIfNecessary(com.zimbra.common.mime.InternetAddress iaddr) {
        if (iaddr != null && iaddr.getAddress() == null && iaddr.getPersonal() != null) {
            return new com.zimbra.common.mime.InternetAddress(iaddr.getAddress(), iaddr.getPersonal()).setCharset(iaddr.getCharset());
        } else {
            return iaddr;
        }
    }

    com.zimbra.common.mime.InternetAddress getZimbraInternetAddress() {
        return mAddress;
    }

    static com.zimbra.common.mime.InternetAddress asZimbraInternetAddress(Address address) {
        if (address == null) {
            return null;
        } else if (address instanceof JavaMailInternetAddress) {
            return ((JavaMailInternetAddress) address).getZimbraInternetAddress();
        } else if (address instanceof InternetAddress) {
            InternetAddress addr = (InternetAddress) address;
            return new com.zimbra.common.mime.InternetAddress(addr.getPersonal(), addr.getAddress());
        } else {
            return new com.zimbra.common.mime.InternetAddress(address.toString());
        }
    }

    static Address[] asJavaMailInternetAddresses(Collection<com.zimbra.common.mime.InternetAddress> iaddrs) {
        if (iaddrs == null) {
            return null;
        }

        InternetAddress[] addresses = new InternetAddress[iaddrs.size()];
        int i = 0;
        for (com.zimbra.common.mime.InternetAddress addr : iaddrs) {
            addresses[i++] = new JavaMailInternetAddress(addr, true);
        }
        return addresses;
    }

    public static InternetAddress[] parse(String addresslist) throws AddressException {
        if (ZPARSER) {
            return parseHeader(addresslist, true);
        } else {
            return InternetAddress.parse(addresslist);
        }
    }

    public static InternetAddress[] parse(String addresslist, boolean strict) throws AddressException {
        if (ZPARSER) {
            return parseHeader(addresslist, strict);
        } else {
            return InternetAddress.parse(addresslist, strict);
        }
    }

    public static InternetAddress[] parseHeader(String addresslist, boolean strict) throws AddressException {
        if (ZPARSER) {
            List<com.zimbra.common.mime.InternetAddress> addrs = com.zimbra.common.mime.InternetAddress.parseHeader(addresslist);
            InternetAddress[] jmaddrs = new InternetAddress[addrs.size()];
            for (int i = 0; i < addrs.size(); i++) {
                InternetAddress jmaddr = new JavaMailInternetAddress(addrs.get(i), true);
                if (strict) {
                    jmaddr.validate();
                }
                jmaddrs[i] = jmaddr;
            }
            return jmaddrs;
        } else {
            return InternetAddress.parseHeader(addresslist, strict);
        }
    }

    @Override public InternetAddress clone() {
        if (ZPARSER) {
            return new JavaMailInternetAddress(new com.zimbra.common.mime.InternetAddress(mAddress), false);
        } else {
            return (InternetAddress) super.clone();
        }
    }

    @Override public String getType() {
        return "rfc822";
    }

    @Override public void setAddress(String address) {
        if (ZPARSER) {
            mAddress.setAddress(address);
        } else {
            super.setAddress(address);
        }
    }

    @Override public void setPersonal(String name) throws UnsupportedEncodingException {
        if (ZPARSER) {
            mAddress.setPersonal(name);
        } else {
            super.setPersonal(name);
        }
    }

    @Override public void setPersonal(String name, String charset) throws UnsupportedEncodingException {
        if (ZPARSER) {
            mAddress.setPersonal(name).setCharset(charset);
        } else {
            super.setPersonal(name, charset);
        }
    }

    @Override public String getAddress() {
        if (ZPARSER) {
            return isGroup() ? toString() : mAddress.getAddress();
        } else {
            return super.getAddress();
        }
    }

    @Override public String getPersonal() {
        if (ZPARSER) {
            return isGroup() ? null : mAddress.getPersonal();
        } else {
            return super.getPersonal();
        }
    }

    @Override public String toString() {
        if (ZPARSER) {
            return mAddress.toString();
        } else {
            return super.toString();
        }
    }

    @Override public String toUnicodeString() {
        if (ZPARSER) {
            return mAddress.toUnicodeString();
        } else {
            return super.toUnicodeString();
        }
    }

    @Override public boolean equals(Object a) {
        if (ZPARSER) {
            if (a instanceof JavaMailInternetAddress) {
                return mAddress.equals(((JavaMailInternetAddress) a).getZimbraInternetAddress());
            } else if (a instanceof InternetAddress) {
                InternetAddress addr = (InternetAddress) a;
                return mAddress.equals(new com.zimbra.common.mime.InternetAddress(addr.getPersonal(), addr.getAddress()));
            } else {
                return false;
            }
        } else {
            return super.equals(a);
        }
    }

    @Override public int hashCode() {
        if (ZPARSER) {
            return mAddress.hashCode();
        } else {
            return super.hashCode();
        }
    }

    @Override public void validate() throws AddressException {
        if (ZPARSER) {
            String email = mAddress.getAddress();
            if (email == null || email.trim().isEmpty()) {
                throw new AddressException("Empty address", email);
            }
        } else {
            super.validate();
        }
    }

    @Override public boolean isGroup() {
        if (ZPARSER) {
            return mAddress instanceof com.zimbra.common.mime.InternetAddress.Group;
        } else {
            return super.isGroup();
        }
    }

    @Override public InternetAddress[] getGroup(boolean strict) throws AddressException {
        if (ZPARSER) {
            if (!isGroup()) {
                return null;
            }
            com.zimbra.common.mime.InternetAddress.Group group = (com.zimbra.common.mime.InternetAddress.Group) mAddress;
            List<com.zimbra.common.mime.InternetAddress> members = group.getMembers();
            InternetAddress[] addresses = new InternetAddress[members.size()];
            for (int i = 0; i < members.size(); i++) {
                addresses[i] = new JavaMailInternetAddress(members.get(i), true);
            }
            return addresses;
        } else {
            return super.getGroup(strict);
        }
    }
}
