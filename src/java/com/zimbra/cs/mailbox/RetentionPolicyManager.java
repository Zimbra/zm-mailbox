/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.cs.mailbox;

import java.util.List;


import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;

public class RetentionPolicyManager {
    
    public static String FN_ID = "id";
    public static String FN_INTERVAL = "int";
    
    public static Metadata encode(RetentionPolicy p) {
        Metadata m = new Metadata();
        m.put(FN_ID, p.getId());
        m.put(FN_INTERVAL, p.getLifetimeString());
        return m;
    }

    public static MetadataList encode(Iterable<RetentionPolicy> policy) {
        MetadataList list = new MetadataList();
        for (RetentionPolicy p : policy) {
            list.add(encode(p));
        }
        return list;
    }
    
    public static RetentionPolicy decode(Metadata m)
    throws ServiceException {
        String id = m.get(FN_ID, null);
        String interval = m.get(FN_INTERVAL, null);
        if (id != null) {
            return RetentionPolicy.newSystemPolicy(id);
        } else {
            return RetentionPolicy.newUserPolicy(interval);
        }
    }
    
    public static List<RetentionPolicy> decode(MetadataList metaList)
    throws ServiceException {
        List<RetentionPolicy> list = Lists.newArrayList();
        for (int i = 0; i < metaList.size(); i++) {
            list.add(decode(metaList.getMap(i)));
        }
        return list;
    }
}
