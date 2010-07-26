/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.account.accesscontrol;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.directory.Attributes;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.ldap.LdapUtil;

public class SearchGrants {
    private Provisioning mProv;
    private Set<TargetType> mTargetTypes;
    private Set<String> mGranteeIds;
    
    SearchGrants(Provisioning prov, Set<TargetType> targetTypes, Set<String> granteeIds) {
        mProv = prov;
        mTargetTypes = targetTypes;
        mGranteeIds = granteeIds;
    }
    
    static class GrantsOnTarget {
        private Entry mTargetEntry;
        private ZimbraACL mAcl;
        
        private GrantsOnTarget(Entry targetEntry, ZimbraACL acl) {
            mTargetEntry = targetEntry;
            mAcl = acl;
        }
        
        Entry getTargetEntry() { return mTargetEntry; }
        ZimbraACL getAcl() { return mAcl; }
    }
    
    static class SearchGrantsResults {
        private Provisioning mProv;
        
        // map of raw(in ldap data form, quick way for staging grants found in search visitor, because 
        // we don't want to do much processing in the visitor while taking a ldap connection) search results
        //    key: target id (or name if zimlet)
        //    value: grants on this target
        private Map<String, GrantsOnTargetRaw> mRawResults = new HashMap<String, GrantsOnTargetRaw>();
     
        // results in the form usable by callers
        private Set<GrantsOnTarget> mResults;
            
        SearchGrantsResults(Provisioning prov) {
            mProv = prov;
        }
        
        private void addResult(GrantsOnTargetRaw result) {
            mRawResults.put(result.getTargetId(), result);
        }
        
        /**
         * Returns a mpa of target entry and ZimbraACL object on the target
         * 
         * @return
         * @throws ServiceException
         */
        Set<GrantsOnTarget> getResults() throws ServiceException {
            if (mResults == null) {
                mResults = new HashSet<GrantsOnTarget>();
                for (GrantsOnTargetRaw grants : mRawResults.values()) {
                    GrantsOnTarget grantsOnTarget = getGrants(mProv, grants);
                    mResults.add(grantsOnTarget);
                }
            }
            return mResults;
        }
        
        /**
         * converts a SearchGrantResult to <Entry, ZimbraACL> pair.
         * 
         * @param prov
         * @param sgr
         * @return
         */
        private GrantsOnTarget getGrants(Provisioning prov, GrantsOnTargetRaw sgr) 
        throws ServiceException {
            
            TargetType tt;
            if (sgr.objectClass.contains(AttributeClass.calendarResource.getOCName()))
                tt = TargetType.calresource;
            else if (sgr.objectClass.contains(AttributeClass.account.getOCName()))
                tt = TargetType.account;
            else if (sgr.objectClass.contains(AttributeClass.cos.getOCName()))
                tt = TargetType.cos;
            else if (sgr.objectClass.contains(AttributeClass.distributionList.getOCName()))
                tt = TargetType.dl;
            else if (sgr.objectClass.contains(AttributeClass.domain.getOCName()))
                tt = TargetType.domain;
            else if (sgr.objectClass.contains(AttributeClass.server.getOCName()))
                tt = TargetType.server;
            else if (sgr.objectClass.contains(AttributeClass.xmppComponent.getOCName()))
                tt = TargetType.xmppcomponent;
            else if (sgr.objectClass.contains(AttributeClass.zimletEntry.getOCName()))
                tt = TargetType.zimlet;
            else if (sgr.objectClass.contains(AttributeClass.globalConfig.getOCName()))
                tt = TargetType.config;
            else if (sgr.objectClass.contains(AttributeClass.aclTarget.getOCName()))
                tt = TargetType.global;
            else 
                throw ServiceException.FAILURE("cannot determine target type from SearchGrantResult. " + sgr.dump(), null);
            
            Entry entry = null;
            try {
                if (tt == TargetType.zimlet)
                    entry = TargetType.lookupTarget(prov, tt, TargetBy.name, sgr.cn);
                else
                    entry = TargetType.lookupTarget(prov, tt, TargetBy.id, sgr.zimbraId);
                if (entry == null) {
                    ZimbraLog.acl.warn("canot find target by id " + sgr.zimbraId);
                    throw ServiceException.FAILURE("canot find target by id " + sgr.zimbraId + ". " + sgr.dump(), null);
                }
                ZimbraACL acl = new ZimbraACL(sgr.zimbraACE, tt, entry.getLabel());
                return new GrantsOnTarget(entry, acl);
            } catch (ServiceException e) {
                throw ServiceException.FAILURE("canot find target by id " + sgr.zimbraId + ". " + sgr.dump(), null);
            }
        }
       
    }
    
    /*
     * grants found on a target based on the search criteria
     */
    private static class GrantsOnTargetRaw {
        private String cn;
        private String zimbraId;
        private Set<String> objectClass;
        private String[] zimbraACE;
        
        private String dump() {
            StringBuilder sb = new StringBuilder();
            sb.append("SearchGrantResult: ");
            sb.append("cn=" + cn);
            sb.append("zimbraId=" + zimbraId);
            sb.append(", objectClass=[");
            for (String oc : objectClass)
                sb.append(oc + ", ");
            sb.append("]");
            sb.append(", zimbraACE=[");
            for (String ace : zimbraACE)
                sb.append(ace + ", ");
            
            return sb.toString();
        }
        
        private String[] getMultiAttrString(Map<String, Object> attrs, String attrName) {
            Object obj = attrs.get(attrName);
            if (obj instanceof String) {
                String[] values = new String[1];
                values[0] = (String)obj;
                return values;
            } else
                return (String[])obj;
        }
        
        private GrantsOnTargetRaw(Map<String, Object> attrs) {
            cn = (String)attrs.get(Provisioning.A_cn);
            zimbraId = (String)attrs.get(Provisioning.A_zimbraId);
            objectClass = new HashSet<String>(Arrays.asList(getMultiAttrString(attrs, Provisioning.A_objectClass)));
            zimbraACE = getMultiAttrString(attrs, Provisioning.A_zimbraACE);
        }
        
        private String getTargetId() {
            // urg! zimlet does not have an id, use cn.
            // need to return something for the map key for SearchGrantVisitor.visit
            // id is only used for grants granted on group-ed entries (account, cr, dl)
            // in computeRightsOnGroupShape
            return zimbraId!=null?zimbraId:cn;
        }
    }
    
    private static class SearchGrantVisitor implements LdapUtil.SearchLdapVisitor {
        SearchGrantsResults mResults; 
        
        SearchGrantVisitor(SearchGrantsResults results) {
            mResults = results;
        }

        public void visit(String dn, Map<String, Object> attrs, Attributes ldapAttrs) {
            GrantsOnTargetRaw sgr = new GrantsOnTargetRaw(attrs);
            mResults.addResult(sgr);
        }
    }
    
    /**
    *
    * search grants granted to any of the grantees specified in granteeIds 
    * granted on any of the target types specified in targetTypes.
    *
    * @param prov
    * @return
    * @throws ServiceException
    */
    SearchGrantsResults doSearch() throws ServiceException {
       
       Map<String, Set<String>> basesAndOcs = TargetType.getSearchBasesAndOCs(mProv, mTargetTypes);
       
       SearchGrantsResults results = new SearchGrantsResults(mProv);
       SearchGrantVisitor visitor = new SearchGrantVisitor(results);
       
       for (Map.Entry<String, Set<String>> entry : basesAndOcs.entrySet())
           search(entry.getKey(), entry.getValue(), visitor);
       
       return results;
    }
    
    private void search(String base, Set<String> ocs, SearchGrantVisitor visitor) throws ServiceException {
        
        // query
        StringBuilder ocQuery = new StringBuilder();
        ocQuery.append("(|");
        for (String oc : ocs)
            ocQuery.append("(" + Provisioning.A_objectClass + "=" + oc + ")");
        ocQuery.append(")");
        
        StringBuilder granteeQuery = new StringBuilder();
        granteeQuery.append("(|");
        for (String granteeId : mGranteeIds)
            granteeQuery.append("(" + Provisioning.A_zimbraACE + "=" + granteeId + "*)");
        granteeQuery.append(")");
        
        String query = "(&" + granteeQuery + ocQuery + ")";
        
        String returnAttrs[] = new String[] {Provisioning.A_cn,
                                             Provisioning.A_zimbraId,
                                             Provisioning.A_objectClass,
                                             Provisioning.A_zimbraACE};
        
        
        LdapUtil.searchLdapOnMaster(base, query, returnAttrs, visitor);
    }
}
