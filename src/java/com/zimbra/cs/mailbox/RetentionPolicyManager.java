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
import java.util.Map;
import java.util.UUID;

import org.dom4j.DocumentException;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.mail.type.Policy;
import com.zimbra.soap.mail.type.RetentionPolicy;

public class RetentionPolicyManager {
    
    private static String FN_KEEP = "keep";
    private static String FN_PURGE = "purge";
    private static String FN_ID = "id";
    private static String FN_NAME = "name";
    private static String FN_LIFETIME = "lifetime";
    
    private class SystemPolicy {
        Map<String, Policy> keep = Maps.newHashMap();
        Map<String, Policy> purge = Maps.newHashMap();
    }
    
    private static RetentionPolicyManager instance = new RetentionPolicyManager();
    private static String SYSTEM_POLICY_KEY =
        RetentionPolicyManager.class.getSimpleName() + ".SYSTEM_POLICY";

    public static RetentionPolicyManager getInstance() {
        return instance;
    }
    
    private SystemPolicy getCachedSystemPolicy(Entry entry)
    throws ServiceException {
        SystemPolicy sp;
        synchronized (entry) {
            sp = (SystemPolicy) entry.getCachedData(SYSTEM_POLICY_KEY);
        
            if (sp == null) {
                String xml;
                if (entry instanceof Config)
                    xml = ((Config) entry).getMailPurgeSystemPolicy();
                else if (entry instanceof Cos)
                    xml = ((Cos) entry).getMailPurgeSystemPolicy();
                else
                    throw ServiceException.UNSUPPORTED();
            
                sp = new SystemPolicy();
                if (!Strings.isNullOrEmpty(xml)) {
                    ZimbraLog.purge.debug("Parsing system retention policy:\n%s", xml);
                    try {
                        Element el = Element.parseXML(xml);
                        RetentionPolicy rp = JaxbUtil.elementToJaxb(el, RetentionPolicy.class);
                        for (Policy p : rp.getKeepPolicy()) {
                            assert(p.getId() != null);
                            sp.keep.put(p.getId(), p);
                        }
                        for (Policy p : rp.getPurgePolicy()) {
                            assert(p.getId() != null);
                            sp.purge.put(p.getId(), p);
                        }
                    } catch (DocumentException e) {
                        throw ServiceException.FAILURE("Unable to parse system retention policy.", e);
                    }
                }
                entry.setCachedData(SYSTEM_POLICY_KEY, sp);
            }
        }
        return sp;
    }
    
    public Policy createSystemKeepPolicy(Entry entry, String name, String lifetime)
    throws ServiceException {
        validateLifetime(lifetime);
        Policy p = Policy.newSystemPolicy(generateId(), name, lifetime);
        synchronized (entry) {
            SystemPolicy sp = getCachedSystemPolicy(entry);
            sp.keep.put(p.getId(), p);
            saveSystemPolicy(entry, new RetentionPolicy(sp.keep.values(), sp.purge.values()));
        }
        return p;
    }
    
    public Policy createSystemPurgePolicy(Entry entry, String name, String lifetime)
    throws ServiceException {
        validateLifetime(lifetime);
        Policy p = Policy.newSystemPolicy(generateId(), name, lifetime);
        synchronized (entry) {
            SystemPolicy sp = getCachedSystemPolicy(entry);
            sp.purge.put(p.getId(), p);
            saveSystemPolicy(entry, new RetentionPolicy(sp.keep.values(), sp.purge.values()));
        }
        return p;
    }
    
    private static void validateLifetime(String lifetime)
    throws ServiceException {
        if (Strings.isNullOrEmpty(lifetime)) {
            throw ServiceException.INVALID_REQUEST("lifetime not specified", null);
        }
        long l = DateUtil.getTimeInterval(lifetime, -1);
        if (l == -1) {
            throw ServiceException.INVALID_REQUEST("Invalid lifetime value: " + lifetime, null);
        }
    }
    
    /**
     * Updates the properties of the system policy with the given id.
     * @return {@code null} if a {@code Policy} with the given id could not be found
     */
    public Policy modifySystemPolicy(Entry entry, String id, String name, String lifetime)
    throws ServiceException {
        validateLifetime(lifetime);
        synchronized (entry) {
            SystemPolicy sp = getCachedSystemPolicy(entry);
            if (sp.keep.containsKey(id)) {
                Policy p = Policy.newSystemPolicy(id, name, lifetime);
                sp.keep.put(id, p);
                saveSystemPolicy(entry, new RetentionPolicy(sp.keep.values(), sp.purge.values()));
                return p;
            }
        
            if (sp.purge.containsKey(id)) {
                Policy p = Policy.newSystemPolicy(id, name, lifetime);
                sp.purge.put(id, p);
                saveSystemPolicy(entry, new RetentionPolicy(sp.keep.values(), sp.purge.values()));
                return p;
            }
        }
        return null;
    }
    
    /**
     * Deletes the system policy with the given id.
     * @return {@code true} if the policy was successfully deleted, {@code false}
     * if no policy exists with the given id
     */
    public boolean deleteSystemPolicy(Entry entry, String id)
    throws ServiceException {
        synchronized (entry) {
            SystemPolicy sp = getCachedSystemPolicy(entry);
            Policy p = sp.keep.remove(id);
            if (p == null) {
                p = sp.purge.remove(id);
            }
            if (p != null) {
                saveSystemPolicy(entry, new RetentionPolicy(sp.keep.values(), sp.purge.values()));
                return true;
            }
        }
        return false;
    }
    
    private void saveSystemPolicy(Entry entry, RetentionPolicy rp)
    throws ServiceException {
        String xml = JaxbUtil.jaxbToElement(rp, XMLElement.mFactory).prettyPrint();
        if (entry instanceof Config)
            ((Config)entry).setMailPurgeSystemPolicy(xml);
        else if (entry instanceof Cos)
            ((Cos)entry).setMailPurgeSystemPolicy(xml);
    }
    
    private String generateId() {
        return UUID.randomUUID().toString();
    }
    
    public RetentionPolicy getSystemRetentionPolicy(Entry entry)
    throws ServiceException {
        SystemPolicy sp = getCachedSystemPolicy(entry);
        return new RetentionPolicy(sp.keep.values(), sp.purge.values());
    }

    public RetentionPolicy getSystemRetentionPolicy(Account acct)
    throws ServiceException {
        // Check CoS first, if not found get from Config
        RetentionPolicy retentionPolicy = null;
        Cos cos = acct.getCOS();
        if (cos != null)
            retentionPolicy = RetentionPolicyManager.getInstance().getSystemRetentionPolicy(cos);
        if (retentionPolicy == null || !retentionPolicy.isSet()) {
            Config config = Provisioning.getInstance().getConfig();
            retentionPolicy = RetentionPolicyManager.getInstance().getSystemRetentionPolicy(config);
        }
        return retentionPolicy;
    }
    
    /**
     * Returns a new {@code RetentionPolicy} that has the latest system policy
     * data for any elements in {@code rp} of type {@link Policy.Type#SYSTEM}.
     */
    private RetentionPolicy getCompleteRetentionPolicy(RetentionPolicy master, RetentionPolicy rp)
    throws ServiceException {
        return new RetentionPolicy(
            getLatestList(master, rp.getKeepPolicy()), getLatestList(master, rp.getPurgePolicy()));
    }
    
    public RetentionPolicy getCompleteRetentionPolicy(Account acct, RetentionPolicy rp) throws ServiceException {
        // Check CoS first, if not found get from Config
        RetentionPolicy retentionPolicy = RetentionPolicyManager.getInstance().getSystemRetentionPolicy(acct);
        retentionPolicy = RetentionPolicyManager.getInstance().getCompleteRetentionPolicy(retentionPolicy, rp);
        return retentionPolicy;
    }

    private List<Policy> getLatestList(RetentionPolicy master, Iterable<Policy> list)
    throws ServiceException {
        List<Policy> latestList = Lists.newArrayList();
        for (Policy policy : list) {
            if (policy.getType() == Policy.Type.USER) {
                latestList.add(policy);
            } else {
                Policy latest = master.getPolicyById(policy.getId());
                if (latest != null) {
                    latestList.add(latest);
                }
            }
        }
        return latestList;
    }
    
    public Policy getPolicyById(Entry entry, String id)
    throws ServiceException {
        SystemPolicy sp = getCachedSystemPolicy(entry);
        Policy p = sp.keep.get(id);
        if (p != null) {
            return p;
        }
        p = sp.purge.get(id);
        if (p != null) {
            return p;
        }
        return null;
    }

    /**
     * Persists retention policy to {@code Metadata}. 
     * @param rp retention policy data
     * @param forMailbox {@code true} if this is mailbox retention policy,
     *     {@code false} if this is system retention policy.  For mailbox
     *     policy, only the id is persisted.
     * @return
     */
    public static Metadata toMetadata(RetentionPolicy rp, boolean forMailbox) {
        MetadataList keep = new MetadataList();
        MetadataList purge = new MetadataList();
        
        for (Policy p : rp.getKeepPolicy()) {
            keep.add(toMetadata(p, forMailbox));
        }
        for (Policy p : rp.getPurgePolicy()) {
            purge.add(toMetadata(p, forMailbox));
        }
        
        Metadata m = new Metadata();
        m.put(FN_KEEP, keep);
        m.put(FN_PURGE, purge);
        return m;
    }
    
    /**
     * Persists retention policy to {@code Metadata}. 
     * @param rp retention policy data
     * @param forMailbox {@code true} if this is mailbox retention policy,
     *     {@code false} if this is system retention policy.  For mailbox
     *     policy, only the id is persisted.
     * @return
     */
    public static Metadata toMetadata(Policy p, boolean forMailbox) {
        Metadata m = new Metadata();
        if (p.getType() == Policy.Type.USER) {
            m.put(FN_LIFETIME, p.getLifetime());
        } else {
            m.put(FN_ID, p.getId());
            if (!forMailbox) {
                m.put(FN_NAME, p.getName());
                m.put(FN_LIFETIME, p.getLifetime());
            }
        }
        return m;
    }

    public static RetentionPolicy retentionPolicyFromMetadata(Metadata m, boolean forMailbox)
    throws ServiceException {
        if (m == null) {
            return new RetentionPolicy();
        }
        
        List<Policy> keep = Collections.emptyList();
        List<Policy> purge = Collections.emptyList();

        MetadataList keepMeta = m.getList(FN_KEEP, true);
        if (keepMeta != null) {
            keep = policyListFromMetadata(keepMeta, forMailbox); 
        }
        MetadataList purgeMeta = m.getList(FN_PURGE, true);
        if (purgeMeta != null) {
            purge = policyListFromMetadata(purgeMeta, forMailbox);
        }

        return new RetentionPolicy(keep, purge);
    }
    
    private static List<Policy> policyListFromMetadata(MetadataList ml, boolean forMailbox)
    throws ServiceException {
        List<Policy> policyList = Lists.newArrayList();
        if (ml != null) {
            for (int i = 0; i < ml.size(); i++) {
                Policy policy = policyFromMetadata(ml.getMap(i), forMailbox);
                if (policy != null) {
                    policyList.add(policy);
                }
            }
        }
        return policyList;
    }
    
    private static Policy policyFromMetadata(Metadata m, boolean forMailbox)
    throws ServiceException {
        String id = m.get(FN_ID, null);
        if (id != null) {
            if (forMailbox) {
                return Policy.newSystemPolicy(id);
            } else {
                return Policy.newSystemPolicy(id, m.get(FN_NAME), m.get(FN_LIFETIME));
            }
        } else {
            return Policy.newUserPolicy(m.get(FN_LIFETIME));
        }
    }
}
