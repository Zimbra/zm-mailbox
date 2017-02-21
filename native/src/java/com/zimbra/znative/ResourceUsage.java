/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
/**
 * 
 */
package com.zimbra.znative;

public final class ResourceUsage {
    public static final int TYPE_SELF = 0;
    public static final int TYPE_CHILDREN = 0;


    private ResourceUsage() { }

    private boolean mSelf;
    
    private final long[] mData = new long[RESOURCE_MAX];

    public static final int RESOURCE_UTIME = 0;
    public static final int RESOURCE_STIME = 1;
    public static final int RESOURCE_MAXRSS = 2;
    public static final int RESOURCE_IXRSS = 3;
    public static final int RESOURCE_IDRSS = 4;
    public static final int RESOURCE_ISRSS = 5;
    public static final int RESOURCE_MINFLT = 6;
    public static final int RESOURCE_MAJFLT = 7;
    public static final int RESOURCE_NSWAP = 8;
    public static final int RESOURCE_INBLOCK = 9;
    public static final int RESOURCE_OUBLOCK = 10;
    public static final int RESOURCE_MSGSND = 11;
    public static final int RESOURCE_MSGRCV = 12;
    public static final int RESOURCE_NSIGNALS = 13;
    public static final int RESOURCE_NVCSW = 14;
    public static final int RESOURCE_NIVCSW = 15;
    private static final int RESOURCE_MAX = 16;
    
    public long get(int field) {
        return mData[field]; 
    }

    public String toString() {
        return "utime=" + mData[RESOURCE_UTIME] + 
            " stime=" + mData[RESOURCE_STIME] +
            " maxrss=" + mData[RESOURCE_MAXRSS] +
            " ixrss=" + mData[RESOURCE_IXRSS] +
            " idrss=" + mData[RESOURCE_IDRSS] +
            " isrss=" + mData[RESOURCE_ISRSS] +
            " minflt=" + mData[RESOURCE_MINFLT] +
            " majflt=" + mData[RESOURCE_MAJFLT] +
            " nswap=" + mData[RESOURCE_NSWAP] +
            " inblock=" + mData[RESOURCE_INBLOCK] +
            " oublock=" + mData[RESOURCE_OUBLOCK] +
            " msgsnd=" + mData[RESOURCE_MSGSND] +
            " msgrcv=" + mData[RESOURCE_MSGRCV] +
            " nsignals=" + mData[RESOURCE_NSIGNALS] +
            " nvcsw=" + mData[RESOURCE_NVCSW] +
            " nivcsw=" + mData[RESOURCE_NIVCSW];
    }
    
    public static ResourceUsage getResourceUsage(int type) {
        ResourceUsage ru = new ResourceUsage();
        if (Util.haveNativeCode()) {
            getResourceUsage0(type, ru.mData);
        }
        return ru;
    }

    private static native void getResourceUsage0(int type, long[] ticks);
}