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

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.soap.mail.type.Policy;
import com.zimbra.soap.mail.type.RetentionPolicy;

public class RetentionPolicyManager {
    
    public static String FN_KEEP = "keep";
    public static String FN_PURGE = "purge";
    public static String FN_ID = "id";
    public static String FN_LIFETIME = "lifetime";
    
    public static Metadata toMetadata(RetentionPolicy rp) {
        MetadataList keep = new MetadataList();
        MetadataList purge = new MetadataList();
        
        for (Policy p : rp.getKeepPolicy()) {
            keep.add(toMetadata(p));
        }
        for (Policy p : rp.getPurgePolicy()) {
            purge.add(toMetadata(p));
        }
        
        Metadata m = new Metadata();
        m.put(FN_KEEP, keep);
        m.put(FN_PURGE, purge);
        return m;
    }
    
    public static Metadata toMetadata(Policy p) {
        Metadata m = new Metadata();
        m.put(FN_ID, p.getId());
        m.put(FN_LIFETIME, p.getLifetime());
        return m;
    }

    public static RetentionPolicy retentionPolicyFromMetadata(Metadata m)
    throws ServiceException {
        if (m == null) {
            return new RetentionPolicy();
        }
        
        List<Policy> keep = Collections.emptyList();
        List<Policy> purge = Collections.emptyList();

        MetadataList keepMeta = m.getList(FN_KEEP, true);
        if (keepMeta != null) {
            keep = policyListFromMetadata(keepMeta); 
        }
        MetadataList purgeMeta = m.getList(FN_PURGE, true);
        if (purgeMeta != null) {
            purge = policyListFromMetadata(purgeMeta);
        }

        return new RetentionPolicy(keep, purge);
    }
    
    private static List<Policy> policyListFromMetadata(MetadataList ml)
    throws ServiceException {
        List<Policy> policyList = Lists.newArrayList();
        if (ml != null) {
            for (int i = 0; i < ml.size(); i++) {
                policyList.add(policyFromMetadata(ml.getMap(i)));
            }
        }
        return policyList;
    }
    
    private static Policy policyFromMetadata(Metadata m)
    throws ServiceException {
        String id = m.get(FN_ID, null);
        if (id != null) {
            return Policy.newSystemPolicy(id);
        } else {
            return Policy.newUserPolicy(m.get(FN_LIFETIME));
        }
    }
}
