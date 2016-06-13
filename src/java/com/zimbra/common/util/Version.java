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
package com.zimbra.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;

/**
 * Zimbra component version.
 * <p>
 * {@code <major number>.<minor number>.<patch number>_<release><release number>_<buildnumber>}
 * <p>
 * e.g.
 * <ul>
 *  <li>6
 *  <li>6.0
 *  <li>6.0.0
 *  <li>6.0.0_BETA1_1234
 *  <li>6.0.0_RC1_1234
 *  <li>6.0.0_GA_1234
 * </ul>
 */
public final class Version implements Comparable<Version> {

    public static final String FUTURE = "future";
    private static Pattern mPattern = Pattern.compile("([a-zA-Z]+)(\\d*)");

    private enum Release {
        BETA, M, RC, GA;

        public static Release fromString(String rel) throws ServiceException {
            try {
                return Release.valueOf(rel);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown release: " + rel, e);
            }
        }
    }

    private boolean mFuture;
    private int mMajor;
    private int mMinor;
    private int mPatch;
    private Release mRel;
    private int mRelNum;
    private String mVersion;

    public Version(String version) throws ServiceException {
        this(version, true);
    }

    public Version(String version, boolean strict) throws ServiceException {
        mVersion = version;
        if (FUTURE.equalsIgnoreCase(version)) {
            mFuture = true;
            return;
        }

        String ver = version;
        int underscoreAt = version.indexOf('_');
        int lastUnderscoreAt = version.lastIndexOf('_');
        if(lastUnderscoreAt == -1 || lastUnderscoreAt == underscoreAt)
            lastUnderscoreAt = version.length()-1;

        if (underscoreAt != -1) {
            ver = version.substring(0, underscoreAt);
            Matcher matcher = mPattern.matcher(version);
            if (matcher.find()) {
                mRel = Release.fromString(matcher.group(1));
                String relNum = matcher.group(2);
                if (!Strings.isNullOrEmpty(relNum))
                    mRelNum = Integer.parseInt(relNum);
            }
        }

        String[] parts = ver.split("\\.");

        try {
            if (parts.length == 1)
                mMajor = Integer.parseInt(parts[0]);
            else if (parts.length == 2) {
                mMajor = Integer.parseInt(parts[0]);
                mMinor = Integer.parseInt(parts[1]);
            } else if (parts.length == 3) {
                mMajor = Integer.parseInt(parts[0]);
                mMinor = Integer.parseInt(parts[1]);
                mPatch = Integer.parseInt(parts[2]);
            } else if (parts.length == 4 && !strict) {
                // so we can parse version number in ZCO/ZCB UA,
                // where version number is 4 segments, like 7.0.0.0
                // NOTE: the last segment is ignored.
                mMajor = Integer.parseInt(parts[0]);
                mMinor = Integer.parseInt(parts[1]);
                mPatch = Integer.parseInt(parts[2]);
            } else {
                throw ServiceException.FAILURE("invalid version format:" + version, null);
            }
        } catch (NumberFormatException e) {
            throw ServiceException.FAILURE("invalid version format:" + version, e);
        }

    }

    /**
     * Compares the two versions.
     *
     * e.g.
     * <ul>
     *  <li>{@code compare("5.0.10", "5.0.9") > 0}
     *  <li>{@code compare("5.0.10", "5.0.10") == 0}
     *  <li>{@code compare("5.0", "5.0.9") < 0}
     *  <li>{@code compare("5.0.10_RC1", "5.0.10_BETA3") > 0}
     *  <li>{@code compare("5.0.10_GA", "5.0.10_RC2") > 0}
     *  <li>{@code compare("5.0.10", "5.0.10_RC2") > 0}
     * </ul>
     *
     * @return a negative integer, zero, or a positive integer as
     * versionX is older than, equal to, or newer than the versionY.
     */
    public static int compare(String versionX, String versionY) throws ServiceException {
        Version x = new Version(versionX);
        Version y = new Version(versionY);
        return x.compareTo(y);
    }

    /**
     * Compares this object with the specified version.
     *
     * @param version
     * @return a negative integer, zero, or a positive integer as this object is
     * older than, equal to, or newer than the specified version.
     */
    public int compare(String version) throws ServiceException  {
        Version other = new Version(version);
        return compareTo(other);
    }

    /**
     * Compares this object with the specified version.
     *
     * @param version
     * @return a negative integer, zero, or a positive integer as this object is
     * older than, equal to, or newer than the specified version.
     */
    @Override
    public int compareTo(Version version) {
        if (mFuture) {
            if (version.mFuture)
                return 0;
            else
                return 1;
        } else if (version.mFuture)
            return -1;

        int r = mMajor - version.mMajor;
        if (r != 0)
            return r;

        r = mMinor - version.mMinor;
        if (r != 0)
            return r;

        r = mPatch - version.mPatch;
        if (r != 0)
            return r;

        if (mRel != null) {
            if (version.mRel != null) {
                r = mRel.ordinal() - version.mRel.ordinal();
                if (r != 0) {
                    return r;
                }
                return mRelNum - version.mRelNum;
            } else { // no Release means GA
                return mRel.ordinal() - Release.GA.ordinal();
            }
        } else { // no Release means GA
            if (version.mRel != null) {
                return Release.GA.ordinal() - version.mRel.ordinal();
            } else {
                return 0;
            }
        }
    }

    public boolean isSameMinorRelease(Version version) {
        return (this.mMajor == version.mMajor && this.mMinor == version.mMinor);
    }

    public boolean isLaterMajorMinorRelease(Version version) {
        return (this.mMajor > version.mMajor || (this.mMajor == version.mMajor && this.mMinor > version.mMinor));
    }

    public boolean isFuture() {
        return mFuture;
    }

    @Override
    public String toString() {
        return mVersion;
    }

}
