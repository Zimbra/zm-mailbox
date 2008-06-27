/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.stats;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.stats.RealtimeStatsCallback;
import com.zimbra.cs.mailbox.MessageCache;


public class ServerStatsCallback implements RealtimeStatsCallback {

    public Map<String, Object> getStatData() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put(ZimbraPerf.RTS_MBOX_CACHE_SIZE, ZimbraPerf.getMailboxCacheSize());
        data.put(ZimbraPerf.RTS_MSG_CACHE_BYTES, MessageCache.getDataSize());
        data.put(ZimbraPerf.RTS_MSG_CACHE_SIZE, MessageCache.getSize());
        return data;
    }

}
