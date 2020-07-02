/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
    private static final short CURRENT_MINOR = 43;  // range: 0 - Short.MAX_VALUE

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

    /** Returns if this version is at least as high as the version specified
     *  by major and minor.
     * @return true if this version is higher than or equal to major/minor,
     *         false if this version is lower */
    public boolean atLeast(int major, int minor) {
        return (mMajorVer > major ||
                (mMajorVer == major && mMinorVer >= minor));
    }

    /** Returns if this version is at least as high as version b.
     * @return true if this version is higher than or equal to version b,
     *         false if this version is lower than version b */
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
        return b != null && b.mMajorVer == mMajorVer && b.mMinorVer == mMinorVer;
    }
}
