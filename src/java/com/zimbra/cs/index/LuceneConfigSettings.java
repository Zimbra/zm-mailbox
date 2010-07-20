/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.index;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.Zimbra;

/**
 *
 */
class LuceneConfigSettings {
    static final class Config {
        final boolean useDocScheduler;
        final long minMerge;
        final long maxMerge;
        final int mergeFactor;
        final boolean useCompoundFile;
        final boolean useSerialMergeScheduler;
        final int maxBufferedDocs;
        final int ramBufferSizeKB;

        final String prefix;

        private Config(boolean batchMode) {
            if (batchMode) {
                prefix = "zimbra_index_lucene_batch_";
            } else {
                prefix = "zimbra_index_lucene_nobatch_";
            }

            useDocScheduler = getBool("use_doc_scheduler", true);
            minMerge = getLong("min_merge", 10, 2, Integer.MAX_VALUE);
            maxMerge = getLong("max_merge", 10, 2, Integer.MAX_VALUE);
            mergeFactor = getInt("merge_factor", 3, 2, 1000);
            useCompoundFile = getBool("use_compound_file", true);
            useSerialMergeScheduler = getBool("use_serial_merge_scheduler", true);
            maxBufferedDocs = getInt("max_buffered_docs", 100, 0, 100000);
            ramBufferSizeKB = getInt("ram_buffer_size_kb", 100, 0, 100000);
        }

        private String lookup(String key) {
            String value = LC.get(prefix+key);
            assert(value!=null);
            if (value == null) {
                Zimbra.halt("Error loading LuceneConfigSettings, could not find \""+prefix+key+"\" in LC.class.  This is a programming bug (mistyped name?)");
            }
            return value;
        }

        private int getInt(String key, int def, int min, int max) {
            String k = lookup(key);
            if (k != null) {
                try {
                    int toRet = Integer.parseInt(k);
                    if (toRet < min) {
                        ZimbraLog.index.warn("Invalid LocalConfig value ("+k+") for \""+prefix+key+"\" using default value of "+def);
                        return min;
                    }
                    if (toRet > max) {
                        ZimbraLog.index.warn("Invalid LocalConfig value ("+k+") for \""+prefix+key+"\" using default value of "+def);
                        return max;
                    }
                    return toRet;
                } catch (NumberFormatException e) {
                    ZimbraLog.index.warn("Invalid LocalConfig value ("+k+") for \""+prefix+key+"\" using default value of "+def);
                }
            }
            return def;
        }

        private long getLong(String key, long def, long min, long max) {
            String k = lookup(key);
            if (k != null) {
                try {
                    long toRet = Long.parseLong(k);
                    if (toRet < min) {
                        ZimbraLog.index.warn("Invalid LocalConfig value ("+k+") for \""+prefix+key+"\" using default value of "+def);
                        return min;
                    }
                    if (toRet > max) {
                        ZimbraLog.index.warn("Invalid LocalConfig value ("+k+") for \""+prefix+key+"\" using default value of "+def);
                        return max;
                    }
                    return toRet;
                } catch (NumberFormatException e) {
                    ZimbraLog.index.warn("Invalid LocalConfig value ("+k+") for \""+prefix+key+"\" using default value of "+def);
                }
            }
            return def;
        }


        private boolean getBool(String key, boolean def) {
            String value = lookup(key);
            if (value != null) {
                return Boolean.valueOf(value);
            }
            return def;
        }
    }

    static final Config batched;
    static Config nonBatched;

    static {
        batched = new Config(true);
        nonBatched = new Config(false);
    }
}
