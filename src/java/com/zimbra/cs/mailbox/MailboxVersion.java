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
package com.zimbra.cs.mailbox;

import com.zimbra.common.service.ServiceException;

public final class MailboxVersion {
    // These should be incremented with changes to serialization format.
    private static final short CURRENT_MAJOR = 2; // range: 0 - Short.MAX_VALUE
    private static final short CURRENT_MINOR = 0; // range: 0 - Short.MAX_VALUE

    private short majorVer;
    private short minorVer;

    static final MailboxVersion CURRENT = new MailboxVersion();

    MailboxVersion() {
        majorVer = CURRENT_MAJOR;
        minorVer = CURRENT_MINOR;
    }

    MailboxVersion(short major, short minor) {
        majorVer = major;
        minorVer = minor;
    }

    MailboxVersion(MailboxVersion other) {
        majorVer = other.majorVer;
        minorVer = other.majorVer;
    }

    static MailboxVersion fromMetadata(Metadata md) throws ServiceException {
        // unknown version are set to 1.0
        short major = 1;
        short minor = 0;

        if (md != null) {
            major = (short) md.getLong("vmaj", 1);
            minor = (short) md.getLong("vmin", 0);
        }

        return new MailboxVersion(major, minor);
    }

    Metadata writeToMetadata(Metadata md) {
        md.put("vmaj", majorVer);
        md.put("vmin", minorVer);
        return md;
    }

    /**
     * Returns if this version is at least as high as the version specified by major and minor.
     *
     * @return true if this version is higher than or equal to major/minor, false if this version is lower
     */
    public boolean atLeast(int major, int minor) {
        return (majorVer > major || (majorVer == major && minorVer >= minor));
    }

    /**
     * Returns if this version is at least as high as version b.
     *
     * @return true if this version is higher than or equal to version b, false if this version is lower than version b
     */
    public boolean atLeast(MailboxVersion b) {
        return atLeast(b.majorVer, b.minorVer);
    }

    public boolean isLatest() {
        return (majorVer == CURRENT_MAJOR && minorVer == CURRENT_MINOR);
    }

    /**
     * Returns if this version is higher than latest known code version.
     */
    public boolean tooHigh() {
        return (majorVer > CURRENT_MAJOR || (majorVer == CURRENT_MAJOR && minorVer > CURRENT_MINOR));
    }

    @Override
    public String toString() {
        return majorVer + "." + minorVer;
    }
}
