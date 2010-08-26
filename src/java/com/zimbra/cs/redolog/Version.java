/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on 2004. 8. 4.
 *
 */
package com.zimbra.cs.redolog;

import java.io.IOException;

/**
 * Versioning of redo log serialization
 */
public class Version {

    // These should be incremented with changes to serialization format.
    private static final short CURRENT_MAJOR = 1;   // range: 0 - Short.MAX_VALUE
    private static final short CURRENT_MINOR = 29;  // range: 0 - Short.MAX_VALUE

    /**
     * Returns a version object with latest major and minor version
     * supported by code.
     * @return
     */
    public static Version latest() {
        return new Version(CURRENT_MAJOR, CURRENT_MINOR);
    }

    private short mMajorVer;
    private short mMinorVer;

    public Version() {
        mMajorVer = CURRENT_MAJOR;
        mMinorVer = CURRENT_MINOR;
    }

    public Version(int major, int minor) {
        mMajorVer = (short) major;
        mMinorVer = (short) minor;
    }

    public Version(Version b) {
        this(b.mMajorVer, b.mMinorVer);
    }

    /**
     * Returns if this version is at least as high as the version specified
     * by major and minor.
     * @param major
     * @param minor
     * @return true if this version is higher than or equal to major/minor,
     *         false if this version is lower
     */
    public boolean atLeast(int major, int minor) {
        return (mMajorVer > major ||
                (mMajorVer == major && mMinorVer >= minor));
    }

    /**
     * Returns if this version is at least as high as version b.
     * @param b
     * @return true if this version is higher than or equal to version b,
     *         false if this version is lower than version b
     */
    public boolean atLeast(Version b) {
        return atLeast(b.mMajorVer, b.mMinorVer);
    }

    public boolean isLatest() {
        return (mMajorVer == CURRENT_MAJOR && mMinorVer == CURRENT_MINOR);
    }

    /**
     * Returns if this version is higher than latest known code version.
     * @return
     */
    public boolean tooHigh() {
        return (mMajorVer > CURRENT_MAJOR ||
                (mMajorVer == CURRENT_MAJOR && mMinorVer > CURRENT_MINOR));
    }

    @Override public String toString() {
        return Integer.toString(mMajorVer) + "." + Integer.toString(mMinorVer);
    }

    public void serialize(RedoLogOutput out) throws IOException {
        out.writeShort(mMajorVer);
        out.writeShort(mMinorVer);
    }

    public void deserialize(RedoLogInput in) throws IOException {
        mMajorVer = in.readShort();
        mMinorVer = in.readShort();
        if (mMajorVer < 0 || mMinorVer < 0)
            throw new IOException("Negative version number: major=" + mMajorVer + ", minor=" + mMinorVer);
    }

    @Override public boolean equals(Object obj) {
        Version b = (Version) obj;
        return b.mMajorVer == mMajorVer && b.mMinorVer == mMinorVer;
    }
}
