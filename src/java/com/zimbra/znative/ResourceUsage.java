/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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