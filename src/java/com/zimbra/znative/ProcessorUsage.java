/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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
/**
 * 
 */
package com.zimbra.znative;

public final class ProcessorUsage {
    private static final int OFFSET_UTICKS = 0;
    private static final int OFFSET_STICKS = 1;
    private static final int OFFSET_CUTICKS = 2;
    private static final int OFFSET_CSTICKS = 3;
    private static final int OFFSET_WTICKS = 4; /* wall clock time - return value of times() */
    private static final int OFFSET_MAX = 5;

    private final long[] mData = new long[OFFSET_MAX];
    
    public long uticks() {  return mData[OFFSET_UTICKS];  }
    public long sticks() {  return mData[OFFSET_STICKS];  }
    public long cuticks() {  return mData[OFFSET_CUTICKS];  }
    public long csticks() {  return mData[OFFSET_CUTICKS];  }
    public long wticks() { return mData[OFFSET_WTICKS]; }
    
    public long umillis() { return (uticks() * 1000) / Util.TICKS_PER_SECOND; }
    public long smillis() { return (sticks() * 1000) / Util.TICKS_PER_SECOND; }
    public long cumillis() { return (cuticks() * 1000) / Util.TICKS_PER_SECOND; }
    public long csmillis() { return (csticks() * 1000) / Util.TICKS_PER_SECOND; }
    public long wmillis() { return (wticks() * 1000) / Util.TICKS_PER_SECOND; }
    
    public static String usageInTicks(ProcessorUsage now, ProcessorUsage then) {
        StringBuilder sb = new StringBuilder(80);
        sb.append("uticks="); sb.append(now.uticks() - then.uticks());
        sb.append(" sticks="); sb.append(now.sticks() - then.sticks());
        sb.append(" cuticks="); sb.append(now.cuticks() - then.cuticks());
        sb.append(" csticks="); sb.append(now.csticks() - then.csticks());
        sb.append(" wticks="); sb.append(now.wticks() - then.wticks());
        return sb.toString();
    }
    
    public static String usageInMillis(ProcessorUsage now, ProcessorUsage then) {
        StringBuilder sb = new StringBuilder(80);
        sb.append("umillis="); sb.append(now.umillis() - then.umillis());
        sb.append(" smillis="); sb.append(now.smillis() - then.smillis());
        sb.append(" cumillis="); sb.append(now.cumillis() - then.cumillis());
        sb.append(" csmillis="); sb.append(now.csmillis() - then.csmillis());
        sb.append(" wmillis="); sb.append(now.wmillis() - then.wmillis());
        return sb.toString();
    }
    
    public String toString() {
        return "uticks=" + uticks() +
            " sticks=" + sticks() + 
            " cuticks=" + cuticks() + 
            " csticks=" + csticks() + 
            " wticks=" + wticks(); 
    }
    
    public String toStringMillis() {
        return "umillis=" + umillis() +
            " smillis=" + smillis() + 
            " cumillis=" + cumillis() + 
            " csmillis=" + csmillis() + 
            " wmillis=" + wmillis(); 
    }

    public static ProcessorUsage getProcessorUsage() {
        ProcessorUsage pu = new ProcessorUsage();
        if (Util.haveNativeCode()) {
            getProcessorUsage0(pu.mData);
        } else {
            /* all of elapsed wall clock time is time spent in this processor ... */ 
            pu.mData[OFFSET_UTICKS] = System.currentTimeMillis();
            pu.mData[OFFSET_STICKS] = 0;
            pu.mData[OFFSET_WTICKS] = System.currentTimeMillis();
        }
        return pu;
    }

    private static native void getProcessorUsage0(long[] ticks);
}
