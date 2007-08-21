/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
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
import java.util.List;
import java.util.Map;

import com.zimbra.cs.stats.RealtimeStatsCallback;
import com.zimbra.cs.stats.ZimbraPerf;

/**
 * A helper class for using java.lang.management APIs to monitor
 * JVM statistics at runtime
 */
public final class MemoryStats implements RealtimeStatsCallback {
    
    private static MemoryStats sInstance = null;
    
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
            "RuntimeTotal=%,d RuntimeMax=%,d  RuntimeFree=%,d  TotUsed=%,d  TotReserved=%,d  TotMax=%,d  CollectUsed=%,d\n",
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
            if (pool.getType() != MemoryType.HEAP) {
                continue;
            }
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
        return "gcollector_"+spaceToUs(gcName)+"_count";
    }
    
    private static final String getGCTimeColName(String gcName) {
        return "gcollector_"+spaceToUs(gcName)+"_time";
    }
    private static final String getPoolFreeSizeColName(String poolName) {
        return "mempool_"+spaceToUs(poolName)+"_free";
    }
    
    private static final String getPoolUsedSizeColName(String poolName) {
        return "mempool_"+spaceToUs(poolName)+"_used";
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
        
        // mempool_NAME_used, mempool_NAME_free
        for (String pool : getHeapPoolNames()) {
            pool = pool.toLowerCase();
            ZimbraPerf.addRealtimeStatName(getPoolUsedSizeColName(pool));
            ZimbraPerf.addRealtimeStatName(getPoolFreeSizeColName(pool));
        }
        
        // heap_used, heap_free
        ZimbraPerf.addRealtimeStatName("heap_used");
        ZimbraPerf.addRealtimeStatName("heap_free");
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.stats.RealtimeStatsCallback#getStatData()
     */
    public Map<String, Object> getStatData() {
        Map<String, Object> toRet = new HashMap<String, Object>();

        // GC times
        List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gc : gcs) {
            String gcName = gc.getName().toLowerCase();
            toRet.put(getGCCountColName(gcName), gc.getCollectionCount());
            toRet.put(getGCTimeColName(gcName), gc.getCollectionTime());
        }
        
        
        long heapTotal = 0;
        long heapUsed = 0;
        
        // mempool stats
        List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean pool : pools) {
            if (pool.getType() != MemoryType.HEAP) {
                continue;
            }
            String poolName = pool.getName().toLowerCase();
            MemoryUsage usage = pool.getUsage();
            heapTotal += usage.getMax();
            heapUsed += usage.getUsed();
            long curpoolFree = usage.getMax() - usage.getUsed();
            toRet.put(getPoolUsedSizeColName(poolName), usage.getUsed());
            toRet.put(getPoolFreeSizeColName(poolName), curpoolFree);
        }
        
        // heap stats
        toRet.put("heap_used", heapUsed);
        toRet.put("heap_free", heapTotal-heapUsed);
        
        return toRet;
    }
    
    
//    /**
//     * @return Summary statistics about the "Major" GCs
//     * 
//     */
//    public static Pair<Long /*num GCs*/, Long /*time elapsed*/> getMajorGCInfo() {
//        List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
//        long num = 0;
//        long time = 0;
//        
//        for (GarbageCollectorMXBean gc : gcs) {
//            if (!"Copy".equals(gc.getName())) {
//                num+=gc.getCollectionCount(); 
//                time+=gc.getCollectionTime();
//            }
//        }
//        return new Pair<Long, Long>(num, time);
//    }
//    
//    /**
//     * @return
//     */
//    public static Pair<Long /*num GCs*/, Long /*time elapsed*/> getMinorGCInfo() {
//        List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
//        long num = 0;
//        long time = 0;
//        
//        for (GarbageCollectorMXBean gc : gcs) {
//            if ("Copy".equals(gc.getName())) {
//                num+=gc.getCollectionCount(); 
//                time+=gc.getCollectionTime();
//            }
//        }
//        return new Pair<Long, Long>(num, time);
//    }
    
    private void doShutdown() { }
    
}
