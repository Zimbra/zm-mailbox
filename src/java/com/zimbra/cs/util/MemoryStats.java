/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.stats.RealtimeStatsCallback;
import com.zimbra.cs.stats.ZimbraPerf;

/**
 * A helper class for using java.lang.management APIs to monitor
 * JVM statistics at runtime
 */
public final class MemoryStats implements RealtimeStatsCallback {

    private static final String MEMPOOL_PREFIX = "mpool_";
    private static final String USED_SUFFIX = "_used";
    private static final String FREE_SUFFIX = "_free";
    private static final String GC_PREFIX = "gc_";
    private static final String COUNT_SUFFIX = "_count";
    private static final String TIME_SUFFIX = "_ms";
    private static final String MINOR = "minor";
    private static final String MAJOR = "major";

    private static final String HEAP_USED = "heap_used";
    private static final String HEAP_FREE = "heap_free";

    private static final String GC_MINOR_COUNT = GC_PREFIX + MINOR + COUNT_SUFFIX; 
    private static final String GC_MINOR_TIME  = GC_PREFIX + MINOR + TIME_SUFFIX; 
    private static final String GC_MAJOR_COUNT = GC_PREFIX + MAJOR + COUNT_SUFFIX; 
    private static final String GC_MAJOR_TIME  = GC_PREFIX + MAJOR + TIME_SUFFIX; 

    // complete set of GC memory manager names from JDK memoryManager.hpp/cpp
    private static final String MEMMGR_GC_MAJOR_MSC = "MarkSweepCompact";
    private static final String MEMMGR_GC_MAJOR_CMS = "ConcurrentMarkSweep";
    private static final String MEMMGR_GC_MAJOR_PS_MARKSWEEP = "PS MarkSweep";
    private static final String MEMMGR_GC_MAJOR_TRAIN = "Train";  // obsoleted in JDK1.6
    //private static final String MEMMGR_GC_MINOR_COPY = "Copy";
    //private static final String MEMMGR_GC_MINOR_PARNEW = "ParNew";
    //private static final String MEMMGR_GC_MINOR_PS_SCAVENGE = "PS Scavenge";

    private static Set<String> sMajorCollectors;

    private static MemoryStats sInstance = null;

    static {
        // Set of major garbage collectors.  All others are minor collectors.
        sMajorCollectors = new HashSet<String>(4);
        sMajorCollectors.add(MEMMGR_GC_MAJOR_MSC);
        sMajorCollectors.add(MEMMGR_GC_MAJOR_CMS);
        sMajorCollectors.add(MEMMGR_GC_MAJOR_PS_MARKSWEEP);
        sMajorCollectors.add(MEMMGR_GC_MAJOR_TRAIN);
    }

    /**
     * Output human-readable information about the garbage collectors in the system
     * 
     * @return
     */
    public static String dumpGarbageCollectors() {
        StringBuilder sb = new StringBuilder();
        
        List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gc : gcs) {
            long timePerGc = 0;
            if (gc.getCollectionCount() > 0) {
                timePerGc = gc.getCollectionTime() / gc.getCollectionCount();
            } 
            sb.append(new Formatter().format("GC: %s(%s)  count=%d  time=%d(%,dms per collection)\n", gc.getName(), gc.isValid() ? "VALID" : "INVALID",
                gc.getCollectionCount(), gc.getCollectionTime(), timePerGc));
            sb.append(new Formatter().format("\tPools: \""));
            for (String s : gc.getMemoryPoolNames()) {
                sb.append(s).append(", ");
            }
            sb.append("\"\n");
        }
        return sb.toString();
    }
    
    /**
     * Output human-readable stats about the memory pools in the system
     * 
     * @return
     */
    public static String dumpMemoryPools() {
        StringBuilder sb = new StringBuilder();
        
        long totalUsed = 0;
        long totalReserved = 0;
        long totalMax = 0;
        long collectUsed = 0;

        List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean pool : pools) {
            MemoryUsage usage = pool.getUsage();
            if (pool.getType() != MemoryType.HEAP) {
                continue;
            }
            sb.append(new Formatter().format("\t\"%s\" memory used: %,d  reserved: %,d  max: %,d", pool.getName(), 
                usage.getUsed(), usage.getCommitted(), usage.getMax()));
            
            totalUsed += usage.getUsed();
            totalReserved += usage.getCommitted();
            totalMax += usage.getMax();
            
            MemoryUsage collect = pool.getCollectionUsage();
            if (collect != null) {
                sb.append(new Formatter().format(" collectUsed: %,d", collect.getUsed()));
                if (collect.getUsed() > 0) {
                    collectUsed += collect.getUsed();
                } else {
                    collectUsed += usage.getUsed();
                }
            } else {
                collectUsed += usage.getUsed();
            }
            sb.append('\n');
        }
        sb.append(new Formatter().format(
            "RuntimeTotal=%,d  RuntimeMax=%,d  RuntimeFree=%,d  TotUsed=%,d  TotReserved=%,d  TotMax=%,d  CollectUsed=%,d\n",
            Runtime.getRuntime().totalMemory(), Runtime.getRuntime().maxMemory(), Runtime.getRuntime().freeMemory(), 
            totalUsed, totalReserved, totalMax, collectUsed));
        return sb.toString();
    }
    
    /**
     * @return The name of all the Garbage Collectors in the system
     * 
     * For the default (serial) hotspot settings, this means:
     *     Copy - The copy collector, Eden and Minor GCs in Survivor
     *     MarkSweepCompact - Major GCs 
     * 
     */
    public static String[] getGarbageCollectorNames() {
        List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
        String[] toRet = new String[gcs.size()];
        int i = 0;
        for (GarbageCollectorMXBean gc : gcs) {
            toRet[i] = gc.getName();
            i++;
        }
        return toRet;
    }

    /**
     * @return A list of the HEAP memory pools in the VM, that is the pools used for dynamic
     * allocations.  Non-HEAP pools (code cache, permanent gen, etc) are not included
     * 
     * For HotSpot default settings, the memory pools are:
     *    Eden (young gen) -- new allocs go here.  Managed by Copy Collector.
     *    Survivor
     *    Tenured -- long-lived objects.  Managed by MarkSweepCompact Collector
     *    
     */
    public static String[] getHeapPoolNames() {
        List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
        ArrayList<String> array = new ArrayList<String>(pools.size());
        
        for (MemoryPoolMXBean pool : pools) {
//          for bug 16398, include non-heap pools (so we get permgen info)            
//            if (pool.getType() != MemoryType.HEAP) {
//                continue;
//            }
            array.add(pool.getName());
        }
        String[] toRet = new String[array.size()];
        return array.toArray(toRet);
    }
    
    public static void shutdown() {
        sInstance.doShutdown();
        sInstance = null;
    }
    
    
    /**
     * Register us with the ZimbraPerf object so that we can log memory stats periodically to the stats file
     */
    public static void startup() {
        sInstance = new MemoryStats();
    }

    private static final String getGCCountColName(String gcName) {
        return GC_PREFIX + spaceToUs(gcName) + COUNT_SUFFIX;
    }

    private static final String getGCTimeColName(String gcName) {
        return GC_PREFIX + spaceToUs(gcName) + TIME_SUFFIX;
    }

    private static final String getPoolFreeSizeColName(String poolName) {
        return MEMPOOL_PREFIX + spaceToUs(poolName) + FREE_SUFFIX;
    }

    private static final String getPoolUsedSizeColName(String poolName) {
        return MEMPOOL_PREFIX + spaceToUs(poolName) + USED_SUFFIX;
    }

    private static final String spaceToUs(String s) {
        return s.replace(' ', '_');
    }

    private MemoryStats() {
        ZimbraPerf.addStatsCallback(this);

        // gcollector_NAME_count,  gcollector_NAME_time
        for (String gc : getGarbageCollectorNames()) {
            gc = gc.toLowerCase();
            ZimbraPerf.addRealtimeStatName(getGCCountColName(gc));
            ZimbraPerf.addRealtimeStatName(getGCTimeColName(gc));
        }
        ZimbraPerf.addRealtimeStatName(GC_MINOR_COUNT);
        ZimbraPerf.addRealtimeStatName(GC_MINOR_TIME);
        ZimbraPerf.addRealtimeStatName(GC_MAJOR_COUNT);
        ZimbraPerf.addRealtimeStatName(GC_MAJOR_TIME);

        // mempool_NAME_used, mempool_NAME_free
        for (String pool : getHeapPoolNames()) {
            pool = pool.toLowerCase();
            ZimbraPerf.addRealtimeStatName(getPoolUsedSizeColName(pool));
            ZimbraPerf.addRealtimeStatName(getPoolFreeSizeColName(pool));
        }
        
        // heap_used, heap_free
        ZimbraPerf.addRealtimeStatName(HEAP_USED);
        ZimbraPerf.addRealtimeStatName(HEAP_FREE);
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.common.stats.RealtimeStatsCallback#getStatData()
     */
    public Map<String, Object> getStatData() {
        Map<String, Object> toRet = new HashMap<String, Object>();

        // GC times
        long minorCount = 0, minorTime = 0, majorCount = 0, majorTime = 0;
        List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gc : gcs) {
            String gcName = gc.getName();
            long count = gc.getCollectionCount();
            long time = gc.getCollectionTime();
            String gcNameLower = gcName.toLowerCase();
            toRet.put(getGCCountColName(gcNameLower), count);
            toRet.put(getGCTimeColName(gcNameLower), time);
            if (sMajorCollectors.contains(gcName)) {
                majorCount += count;
                majorTime  += time;
            } else {
                minorCount += count;
                minorTime  += time;
            }
        }
        toRet.put(GC_MINOR_COUNT, minorCount);
        toRet.put(GC_MINOR_TIME,  minorTime);
        toRet.put(GC_MAJOR_COUNT, majorCount);
        toRet.put(GC_MAJOR_TIME,  majorTime);

        long heapTotal = 0;
        long heapUsed = 0;
        
        // mempool stats
        List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean pool : pools) {
// for bug 16398, include non-heap pools (so we get permgen info)            
//            if (pool.getType() != MemoryType.HEAP) {
//                continue;
//            }
            String poolName = pool.getName().toLowerCase();
            MemoryUsage usage = pool.getUsage();
            long committed = usage.getCommitted();
            long used = usage.getUsed();
            if (pool.getType() == MemoryType.HEAP) {
                heapTotal += committed;
                heapUsed += used;
            }
            long curpoolFree = committed - used;
            toRet.put(getPoolUsedSizeColName(poolName), used);
            toRet.put(getPoolFreeSizeColName(poolName), curpoolFree);
        }
        
        // heap stats
        toRet.put(HEAP_USED, heapUsed);
        toRet.put(HEAP_FREE, heapTotal - heapUsed);
        
        return toRet;
    }

    private void doShutdown() { }

}
