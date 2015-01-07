/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.account.ZAttr;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.StringUtil;


/**
 * AUTO-GENERATED. DO NOT EDIT.
 *
 */
public abstract class ZAttrAlwaysOnCluster extends NamedEntry {

    public ZAttrAlwaysOnCluster(String name, String id, Map<String,Object> attrs, Map<String,Object> defaults, Provisioning prov) {
        super(name, id, attrs, defaults, prov);
    }

    ///// BEGIN-AUTO-GEN-REPLACE

    /**
     * RFC2256: common name(s) for which the entity is known by
     *
     * @return cn, or null if unset
     */
    @ZAttr(id=-1)
    public String getCn() {
        return getAttr(Provisioning.A_cn, null);
    }

    /**
     * RFC2256: common name(s) for which the entity is known by
     *
     * @param cn new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=-1)
    public void setCn(String cn) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_cn, cn);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * RFC2256: common name(s) for which the entity is known by
     *
     * @param cn new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setCn(String cn, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_cn, cn);
        return attrs;
    }

    /**
     * RFC2256: common name(s) for which the entity is known by
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=-1)
    public void unsetCn() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_cn, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * RFC2256: common name(s) for which the entity is known by
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetCn(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_cn, "");
        return attrs;
    }

    /**
     * RFC2256: descriptive information
     *
     * @return description, or empty array if unset
     */
    @ZAttr(id=-1)
    public String[] getDescription() {
        return getMultiAttr(Provisioning.A_description);
    }

    /**
     * RFC2256: descriptive information
     *
     * @param description new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=-1)
    public void setDescription(String[] description) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_description, description);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * RFC2256: descriptive information
     *
     * @param description new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setDescription(String[] description, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_description, description);
        return attrs;
    }

    /**
     * RFC2256: descriptive information
     *
     * @param description new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=-1)
    public void addDescription(String description) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_description, description);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * RFC2256: descriptive information
     *
     * @param description new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> addDescription(String description, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_description, description);
        return attrs;
    }

    /**
     * RFC2256: descriptive information
     *
     * @param description existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=-1)
    public void removeDescription(String description) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_description, description);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * RFC2256: descriptive information
     *
     * @param description existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> removeDescription(String description, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_description, description);
        return attrs;
    }

    /**
     * RFC2256: descriptive information
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=-1)
    public void unsetDescription() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_description, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * RFC2256: descriptive information
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetDescription(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_description, "");
        return attrs;
    }

    /**
     * Zimbra access control list
     *
     * @return zimbraACE, or empty array if unset
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=659)
    public String[] getACE() {
        return getMultiAttr(Provisioning.A_zimbraACE);
    }

    /**
     * Zimbra access control list
     *
     * @param zimbraACE new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=659)
    public void setACE(String[] zimbraACE) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraACE, zimbraACE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Zimbra access control list
     *
     * @param zimbraACE new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=659)
    public Map<String,Object> setACE(String[] zimbraACE, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraACE, zimbraACE);
        return attrs;
    }

    /**
     * Zimbra access control list
     *
     * @param zimbraACE new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=659)
    public void addACE(String zimbraACE) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraACE, zimbraACE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Zimbra access control list
     *
     * @param zimbraACE new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=659)
    public Map<String,Object> addACE(String zimbraACE, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraACE, zimbraACE);
        return attrs;
    }

    /**
     * Zimbra access control list
     *
     * @param zimbraACE existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=659)
    public void removeACE(String zimbraACE) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraACE, zimbraACE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Zimbra access control list
     *
     * @param zimbraACE existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=659)
    public Map<String,Object> removeACE(String zimbraACE, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraACE, zimbraACE);
        return attrs;
    }

    /**
     * Zimbra access control list
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=659)
    public void unsetACE() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraACE, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Zimbra access control list
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=659)
    public Map<String,Object> unsetACE(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraACE, "");
        return attrs;
    }

    /**
     * Max no of elements in acl admin cache credential
     *
     * @return zimbraAdminAclCacheCredentialMaxsize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1689)
    public int getAdminAclCacheCredentialMaxsize() {
        return getIntAttr(Provisioning.A_zimbraAdminAclCacheCredentialMaxsize, -1);
    }

    /**
     * Max no of elements in acl admin cache credential
     *
     * @param zimbraAdminAclCacheCredentialMaxsize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1689)
    public void setAdminAclCacheCredentialMaxsize(int zimbraAdminAclCacheCredentialMaxsize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminAclCacheCredentialMaxsize, Integer.toString(zimbraAdminAclCacheCredentialMaxsize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Max no of elements in acl admin cache credential
     *
     * @param zimbraAdminAclCacheCredentialMaxsize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1689)
    public Map<String,Object> setAdminAclCacheCredentialMaxsize(int zimbraAdminAclCacheCredentialMaxsize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminAclCacheCredentialMaxsize, Integer.toString(zimbraAdminAclCacheCredentialMaxsize));
        return attrs;
    }

    /**
     * Max no of elements in acl admin cache credential
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1689)
    public void unsetAdminAclCacheCredentialMaxsize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminAclCacheCredentialMaxsize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Max no of elements in acl admin cache credential
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1689)
    public Map<String,Object> unsetAdminAclCacheCredentialMaxsize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminAclCacheCredentialMaxsize, "");
        return attrs;
    }

    /**
     * Flag to enable or disable admin acl cache
     *
     * @return zimbraAdminAclCacheEnabled, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1690)
    public boolean isAdminAclCacheEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraAdminAclCacheEnabled, false);
    }

    /**
     * Flag to enable or disable admin acl cache
     *
     * @param zimbraAdminAclCacheEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1690)
    public void setAdminAclCacheEnabled(boolean zimbraAdminAclCacheEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminAclCacheEnabled, zimbraAdminAclCacheEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable or disable admin acl cache
     *
     * @param zimbraAdminAclCacheEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1690)
    public Map<String,Object> setAdminAclCacheEnabled(boolean zimbraAdminAclCacheEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminAclCacheEnabled, zimbraAdminAclCacheEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Flag to enable or disable admin acl cache
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1690)
    public void unsetAdminAclCacheEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminAclCacheEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable or disable admin acl cache
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1690)
    public Map<String,Object> unsetAdminAclCacheEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminAclCacheEnabled, "");
        return attrs;
    }

    /**
     * Max age of admin acl target cache in minutes
     *
     * @return zimbraAdminAclCacheTargetMaxAge, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1688)
    public int getAdminAclCacheTargetMaxAge() {
        return getIntAttr(Provisioning.A_zimbraAdminAclCacheTargetMaxAge, -1);
    }

    /**
     * Max age of admin acl target cache in minutes
     *
     * @param zimbraAdminAclCacheTargetMaxAge new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1688)
    public void setAdminAclCacheTargetMaxAge(int zimbraAdminAclCacheTargetMaxAge) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminAclCacheTargetMaxAge, Integer.toString(zimbraAdminAclCacheTargetMaxAge));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Max age of admin acl target cache in minutes
     *
     * @param zimbraAdminAclCacheTargetMaxAge new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1688)
    public Map<String,Object> setAdminAclCacheTargetMaxAge(int zimbraAdminAclCacheTargetMaxAge, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminAclCacheTargetMaxAge, Integer.toString(zimbraAdminAclCacheTargetMaxAge));
        return attrs;
    }

    /**
     * Max age of admin acl target cache in minutes
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1688)
    public void unsetAdminAclCacheTargetMaxAge() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminAclCacheTargetMaxAge, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Max age of admin acl target cache in minutes
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1688)
    public Map<String,Object> unsetAdminAclCacheTargetMaxAge(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminAclCacheTargetMaxAge, "");
        return attrs;
    }

    /**
     * Max no of elements in admin acl target cache
     *
     * @return zimbraAdminAclCacheTargetMaxsize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1720)
    public int getAdminAclCacheTargetMaxsize() {
        return getIntAttr(Provisioning.A_zimbraAdminAclCacheTargetMaxsize, -1);
    }

    /**
     * Max no of elements in admin acl target cache
     *
     * @param zimbraAdminAclCacheTargetMaxsize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1720)
    public void setAdminAclCacheTargetMaxsize(int zimbraAdminAclCacheTargetMaxsize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminAclCacheTargetMaxsize, Integer.toString(zimbraAdminAclCacheTargetMaxsize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Max no of elements in admin acl target cache
     *
     * @param zimbraAdminAclCacheTargetMaxsize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1720)
    public Map<String,Object> setAdminAclCacheTargetMaxsize(int zimbraAdminAclCacheTargetMaxsize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminAclCacheTargetMaxsize, Integer.toString(zimbraAdminAclCacheTargetMaxsize));
        return attrs;
    }

    /**
     * Max no of elements in admin acl target cache
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1720)
    public void unsetAdminAclCacheTargetMaxsize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminAclCacheTargetMaxsize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Max no of elements in admin acl target cache
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1720)
    public Map<String,Object> unsetAdminAclCacheTargetMaxsize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminAclCacheTargetMaxsize, "");
        return attrs;
    }

    /**
     * Size of the thread pool executor used by ComputeAggregateQuota call
     *
     * @return zimbraAdminComputeAggregateQuotaThreadPoolSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1744)
    public int getAdminComputeAggregateQuotaThreadPoolSize() {
        return getIntAttr(Provisioning.A_zimbraAdminComputeAggregateQuotaThreadPoolSize, -1);
    }

    /**
     * Size of the thread pool executor used by ComputeAggregateQuota call
     *
     * @param zimbraAdminComputeAggregateQuotaThreadPoolSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1744)
    public void setAdminComputeAggregateQuotaThreadPoolSize(int zimbraAdminComputeAggregateQuotaThreadPoolSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminComputeAggregateQuotaThreadPoolSize, Integer.toString(zimbraAdminComputeAggregateQuotaThreadPoolSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Size of the thread pool executor used by ComputeAggregateQuota call
     *
     * @param zimbraAdminComputeAggregateQuotaThreadPoolSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1744)
    public Map<String,Object> setAdminComputeAggregateQuotaThreadPoolSize(int zimbraAdminComputeAggregateQuotaThreadPoolSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminComputeAggregateQuotaThreadPoolSize, Integer.toString(zimbraAdminComputeAggregateQuotaThreadPoolSize));
        return attrs;
    }

    /**
     * Size of the thread pool executor used by ComputeAggregateQuota call
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1744)
    public void unsetAdminComputeAggregateQuotaThreadPoolSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminComputeAggregateQuotaThreadPoolSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Size of the thread pool executor used by ComputeAggregateQuota call
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1744)
    public Map<String,Object> unsetAdminComputeAggregateQuotaThreadPoolSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminComputeAggregateQuotaThreadPoolSize, "");
        return attrs;
    }

    /**
     * zimbra admin url scheme
     *
     * @return zimbraAdminServiceScheme, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1728)
    public String getAdminServiceScheme() {
        return getAttr(Provisioning.A_zimbraAdminServiceScheme, null);
    }

    /**
     * zimbra admin url scheme
     *
     * @param zimbraAdminServiceScheme new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1728)
    public void setAdminServiceScheme(String zimbraAdminServiceScheme) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminServiceScheme, zimbraAdminServiceScheme);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * zimbra admin url scheme
     *
     * @param zimbraAdminServiceScheme new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1728)
    public Map<String,Object> setAdminServiceScheme(String zimbraAdminServiceScheme, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminServiceScheme, zimbraAdminServiceScheme);
        return attrs;
    }

    /**
     * zimbra admin url scheme
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1728)
    public void unsetAdminServiceScheme() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminServiceScheme, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * zimbra admin url scheme
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1728)
    public Map<String,Object> unsetAdminServiceScheme(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminServiceScheme, "");
        return attrs;
    }

    /**
     * Maximum number of admin SOAP sessions a single user can have open at
     * once
     *
     * @return zimbraAdminSessionLimit, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1736)
    public int getAdminSessionLimit() {
        return getIntAttr(Provisioning.A_zimbraAdminSessionLimit, -1);
    }

    /**
     * Maximum number of admin SOAP sessions a single user can have open at
     * once
     *
     * @param zimbraAdminSessionLimit new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1736)
    public void setAdminSessionLimit(int zimbraAdminSessionLimit) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminSessionLimit, Integer.toString(zimbraAdminSessionLimit));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of admin SOAP sessions a single user can have open at
     * once
     *
     * @param zimbraAdminSessionLimit new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1736)
    public Map<String,Object> setAdminSessionLimit(int zimbraAdminSessionLimit, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminSessionLimit, Integer.toString(zimbraAdminSessionLimit));
        return attrs;
    }

    /**
     * Maximum number of admin SOAP sessions a single user can have open at
     * once
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1736)
    public void unsetAdminSessionLimit() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminSessionLimit, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of admin SOAP sessions a single user can have open at
     * once
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1736)
    public Map<String,Object> unsetAdminSessionLimit(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminSessionLimit, "");
        return attrs;
    }

    /**
     * Default timeout in seconds for zimbra admin wait set request
     *
     * @return zimbraAdminWaitsetDefaultRequestTimeout, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1729)
    public int getAdminWaitsetDefaultRequestTimeout() {
        return getIntAttr(Provisioning.A_zimbraAdminWaitsetDefaultRequestTimeout, -1);
    }

    /**
     * Default timeout in seconds for zimbra admin wait set request
     *
     * @param zimbraAdminWaitsetDefaultRequestTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1729)
    public void setAdminWaitsetDefaultRequestTimeout(int zimbraAdminWaitsetDefaultRequestTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminWaitsetDefaultRequestTimeout, Integer.toString(zimbraAdminWaitsetDefaultRequestTimeout));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Default timeout in seconds for zimbra admin wait set request
     *
     * @param zimbraAdminWaitsetDefaultRequestTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1729)
    public Map<String,Object> setAdminWaitsetDefaultRequestTimeout(int zimbraAdminWaitsetDefaultRequestTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminWaitsetDefaultRequestTimeout, Integer.toString(zimbraAdminWaitsetDefaultRequestTimeout));
        return attrs;
    }

    /**
     * Default timeout in seconds for zimbra admin wait set request
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1729)
    public void unsetAdminWaitsetDefaultRequestTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminWaitsetDefaultRequestTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Default timeout in seconds for zimbra admin wait set request
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1729)
    public Map<String,Object> unsetAdminWaitsetDefaultRequestTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminWaitsetDefaultRequestTimeout, "");
        return attrs;
    }

    /**
     * Maximum timeout in seconds for zimbra admin waitset request
     *
     * @return zimbraAdminWaitsetMaxRequestTimeout, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1730)
    public int getAdminWaitsetMaxRequestTimeout() {
        return getIntAttr(Provisioning.A_zimbraAdminWaitsetMaxRequestTimeout, -1);
    }

    /**
     * Maximum timeout in seconds for zimbra admin waitset request
     *
     * @param zimbraAdminWaitsetMaxRequestTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1730)
    public void setAdminWaitsetMaxRequestTimeout(int zimbraAdminWaitsetMaxRequestTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminWaitsetMaxRequestTimeout, Integer.toString(zimbraAdminWaitsetMaxRequestTimeout));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum timeout in seconds for zimbra admin waitset request
     *
     * @param zimbraAdminWaitsetMaxRequestTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1730)
    public Map<String,Object> setAdminWaitsetMaxRequestTimeout(int zimbraAdminWaitsetMaxRequestTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminWaitsetMaxRequestTimeout, Integer.toString(zimbraAdminWaitsetMaxRequestTimeout));
        return attrs;
    }

    /**
     * Maximum timeout in seconds for zimbra admin waitset request
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1730)
    public void unsetAdminWaitsetMaxRequestTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminWaitsetMaxRequestTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum timeout in seconds for zimbra admin waitset request
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1730)
    public Map<String,Object> unsetAdminWaitsetMaxRequestTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminWaitsetMaxRequestTimeout, "");
        return attrs;
    }

    /**
     * Minimum timeout for zimbra admin waitset request
     *
     * @return zimbraAdminWaitsetMinRequestTimeout, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1731)
    public int getAdminWaitsetMinRequestTimeout() {
        return getIntAttr(Provisioning.A_zimbraAdminWaitsetMinRequestTimeout, -1);
    }

    /**
     * Minimum timeout for zimbra admin waitset request
     *
     * @param zimbraAdminWaitsetMinRequestTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1731)
    public void setAdminWaitsetMinRequestTimeout(int zimbraAdminWaitsetMinRequestTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminWaitsetMinRequestTimeout, Integer.toString(zimbraAdminWaitsetMinRequestTimeout));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Minimum timeout for zimbra admin waitset request
     *
     * @param zimbraAdminWaitsetMinRequestTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1731)
    public Map<String,Object> setAdminWaitsetMinRequestTimeout(int zimbraAdminWaitsetMinRequestTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminWaitsetMinRequestTimeout, Integer.toString(zimbraAdminWaitsetMinRequestTimeout));
        return attrs;
    }

    /**
     * Minimum timeout for zimbra admin waitset request
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1731)
    public void unsetAdminWaitsetMinRequestTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminWaitsetMinRequestTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Minimum timeout for zimbra admin waitset request
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1731)
    public Map<String,Object> unsetAdminWaitsetMinRequestTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminWaitsetMinRequestTimeout, "");
        return attrs;
    }

    /**
     * flag to enable or disable anti spam restarts
     *
     * @return zimbraAntiSpamEnableRestarts, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1691)
    public boolean isAntiSpamEnableRestarts() {
        return getBooleanAttr(Provisioning.A_zimbraAntiSpamEnableRestarts, false);
    }

    /**
     * flag to enable or disable anti spam restarts
     *
     * @param zimbraAntiSpamEnableRestarts new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1691)
    public void setAntiSpamEnableRestarts(boolean zimbraAntiSpamEnableRestarts) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAntiSpamEnableRestarts, zimbraAntiSpamEnableRestarts ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * flag to enable or disable anti spam restarts
     *
     * @param zimbraAntiSpamEnableRestarts new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1691)
    public Map<String,Object> setAntiSpamEnableRestarts(boolean zimbraAntiSpamEnableRestarts, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAntiSpamEnableRestarts, zimbraAntiSpamEnableRestarts ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * flag to enable or disable anti spam restarts
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1691)
    public void unsetAntiSpamEnableRestarts() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAntiSpamEnableRestarts, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * flag to enable or disable anti spam restarts
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1691)
    public Map<String,Object> unsetAntiSpamEnableRestarts(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAntiSpamEnableRestarts, "");
        return attrs;
    }

    /**
     * flag to enable or disable anti spam rule compilations
     *
     * @return zimbraAntiSpamEnableRuleCompilation, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1693)
    public boolean isAntiSpamEnableRuleCompilation() {
        return getBooleanAttr(Provisioning.A_zimbraAntiSpamEnableRuleCompilation, false);
    }

    /**
     * flag to enable or disable anti spam rule compilations
     *
     * @param zimbraAntiSpamEnableRuleCompilation new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1693)
    public void setAntiSpamEnableRuleCompilation(boolean zimbraAntiSpamEnableRuleCompilation) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAntiSpamEnableRuleCompilation, zimbraAntiSpamEnableRuleCompilation ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * flag to enable or disable anti spam rule compilations
     *
     * @param zimbraAntiSpamEnableRuleCompilation new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1693)
    public Map<String,Object> setAntiSpamEnableRuleCompilation(boolean zimbraAntiSpamEnableRuleCompilation, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAntiSpamEnableRuleCompilation, zimbraAntiSpamEnableRuleCompilation ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * flag to enable or disable anti spam rule compilations
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1693)
    public void unsetAntiSpamEnableRuleCompilation() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAntiSpamEnableRuleCompilation, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * flag to enable or disable anti spam rule compilations
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1693)
    public Map<String,Object> unsetAntiSpamEnableRuleCompilation(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAntiSpamEnableRuleCompilation, "");
        return attrs;
    }

    /**
     * flag to enable or disable anti spam rule updates
     *
     * @return zimbraAntiSpamEnableRuleUpdates, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1692)
    public boolean isAntiSpamEnableRuleUpdates() {
        return getBooleanAttr(Provisioning.A_zimbraAntiSpamEnableRuleUpdates, false);
    }

    /**
     * flag to enable or disable anti spam rule updates
     *
     * @param zimbraAntiSpamEnableRuleUpdates new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1692)
    public void setAntiSpamEnableRuleUpdates(boolean zimbraAntiSpamEnableRuleUpdates) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAntiSpamEnableRuleUpdates, zimbraAntiSpamEnableRuleUpdates ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * flag to enable or disable anti spam rule updates
     *
     * @param zimbraAntiSpamEnableRuleUpdates new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1692)
    public Map<String,Object> setAntiSpamEnableRuleUpdates(boolean zimbraAntiSpamEnableRuleUpdates, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAntiSpamEnableRuleUpdates, zimbraAntiSpamEnableRuleUpdates ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * flag to enable or disable anti spam rule updates
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1692)
    public void unsetAntiSpamEnableRuleUpdates() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAntiSpamEnableRuleUpdates, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * flag to enable or disable anti spam rule updates
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1692)
    public Map<String,Object> unsetAntiSpamEnableRuleUpdates(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAntiSpamEnableRuleUpdates, "");
        return attrs;
    }

    /**
     * Initial sleep for the auto provision thread in millis
     *
     * @return zimbraAutoProvInitialSleep, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1757)
    public int getAutoProvInitialSleep() {
        return getIntAttr(Provisioning.A_zimbraAutoProvInitialSleep, -1);
    }

    /**
     * Initial sleep for the auto provision thread in millis
     *
     * @param zimbraAutoProvInitialSleep new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1757)
    public void setAutoProvInitialSleep(int zimbraAutoProvInitialSleep) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvInitialSleep, Integer.toString(zimbraAutoProvInitialSleep));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Initial sleep for the auto provision thread in millis
     *
     * @param zimbraAutoProvInitialSleep new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1757)
    public Map<String,Object> setAutoProvInitialSleep(int zimbraAutoProvInitialSleep, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvInitialSleep, Integer.toString(zimbraAutoProvInitialSleep));
        return attrs;
    }

    /**
     * Initial sleep for the auto provision thread in millis
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1757)
    public void unsetAutoProvInitialSleep() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvInitialSleep, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Initial sleep for the auto provision thread in millis
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1757)
    public Map<String,Object> unsetAutoProvInitialSleep(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAutoProvInitialSleep, "");
        return attrs;
    }

    /**
     * Flag to enable or disable allow calendar invite without method
     *
     * @return zimbraCalendarAllowInviteWithoutMethod, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1703)
    public boolean isCalendarAllowInviteWithoutMethod() {
        return getBooleanAttr(Provisioning.A_zimbraCalendarAllowInviteWithoutMethod, false);
    }

    /**
     * Flag to enable or disable allow calendar invite without method
     *
     * @param zimbraCalendarAllowInviteWithoutMethod new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1703)
    public void setCalendarAllowInviteWithoutMethod(boolean zimbraCalendarAllowInviteWithoutMethod) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarAllowInviteWithoutMethod, zimbraCalendarAllowInviteWithoutMethod ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable or disable allow calendar invite without method
     *
     * @param zimbraCalendarAllowInviteWithoutMethod new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1703)
    public Map<String,Object> setCalendarAllowInviteWithoutMethod(boolean zimbraCalendarAllowInviteWithoutMethod, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarAllowInviteWithoutMethod, zimbraCalendarAllowInviteWithoutMethod ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Flag to enable or disable allow calendar invite without method
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1703)
    public void unsetCalendarAllowInviteWithoutMethod() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarAllowInviteWithoutMethod, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable or disable allow calendar invite without method
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1703)
    public Map<String,Object> unsetCalendarAllowInviteWithoutMethod(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarAllowInviteWithoutMethod, "");
        return attrs;
    }

    /**
     * Activates the hack that converts standalone VEVENT/VTODO components
     * with STATUS:CANCELLED into EXDATEs on the series component. Introduced
     * in bug 36434
     *
     * @return zimbraCalendarAppleICalCompatibleCanceledInstances, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1758)
    public boolean isCalendarAppleICalCompatibleCanceledInstances() {
        return getBooleanAttr(Provisioning.A_zimbraCalendarAppleICalCompatibleCanceledInstances, false);
    }

    /**
     * Activates the hack that converts standalone VEVENT/VTODO components
     * with STATUS:CANCELLED into EXDATEs on the series component. Introduced
     * in bug 36434
     *
     * @param zimbraCalendarAppleICalCompatibleCanceledInstances new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1758)
    public void setCalendarAppleICalCompatibleCanceledInstances(boolean zimbraCalendarAppleICalCompatibleCanceledInstances) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarAppleICalCompatibleCanceledInstances, zimbraCalendarAppleICalCompatibleCanceledInstances ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Activates the hack that converts standalone VEVENT/VTODO components
     * with STATUS:CANCELLED into EXDATEs on the series component. Introduced
     * in bug 36434
     *
     * @param zimbraCalendarAppleICalCompatibleCanceledInstances new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1758)
    public Map<String,Object> setCalendarAppleICalCompatibleCanceledInstances(boolean zimbraCalendarAppleICalCompatibleCanceledInstances, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarAppleICalCompatibleCanceledInstances, zimbraCalendarAppleICalCompatibleCanceledInstances ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Activates the hack that converts standalone VEVENT/VTODO components
     * with STATUS:CANCELLED into EXDATEs on the series component. Introduced
     * in bug 36434
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1758)
    public void unsetCalendarAppleICalCompatibleCanceledInstances() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarAppleICalCompatibleCanceledInstances, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Activates the hack that converts standalone VEVENT/VTODO components
     * with STATUS:CANCELLED into EXDATEs on the series component. Introduced
     * in bug 36434
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1758)
    public Map<String,Object> unsetCalendarAppleICalCompatibleCanceledInstances(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarAppleICalCompatibleCanceledInstances, "");
        return attrs;
    }

    /**
     * Enable calendar cache.
     *
     * @return zimbraCalendarCacheEnabled, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1694)
    public boolean isCalendarCacheEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraCalendarCacheEnabled, false);
    }

    /**
     * Enable calendar cache.
     *
     * @param zimbraCalendarCacheEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1694)
    public void setCalendarCacheEnabled(boolean zimbraCalendarCacheEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCacheEnabled, zimbraCalendarCacheEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enable calendar cache.
     *
     * @param zimbraCalendarCacheEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1694)
    public Map<String,Object> setCalendarCacheEnabled(boolean zimbraCalendarCacheEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCacheEnabled, zimbraCalendarCacheEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Enable calendar cache.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1694)
    public void unsetCalendarCacheEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCacheEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enable calendar cache.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1694)
    public Map<String,Object> unsetCalendarCacheEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCacheEnabled, "");
        return attrs;
    }

    /**
     * Size of memory-cache LRU. Cache this many calendar folder in memory.
     *
     * @return zimbraCalendarCacheLRUSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1695)
    public int getCalendarCacheLRUSize() {
        return getIntAttr(Provisioning.A_zimbraCalendarCacheLRUSize, -1);
    }

    /**
     * Size of memory-cache LRU. Cache this many calendar folder in memory.
     *
     * @param zimbraCalendarCacheLRUSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1695)
    public void setCalendarCacheLRUSize(int zimbraCalendarCacheLRUSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCacheLRUSize, Integer.toString(zimbraCalendarCacheLRUSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Size of memory-cache LRU. Cache this many calendar folder in memory.
     *
     * @param zimbraCalendarCacheLRUSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1695)
    public Map<String,Object> setCalendarCacheLRUSize(int zimbraCalendarCacheLRUSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCacheLRUSize, Integer.toString(zimbraCalendarCacheLRUSize));
        return attrs;
    }

    /**
     * Size of memory-cache LRU. Cache this many calendar folder in memory.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1695)
    public void unsetCalendarCacheLRUSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCacheLRUSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Size of memory-cache LRU. Cache this many calendar folder in memory.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1695)
    public Map<String,Object> unsetCalendarCacheLRUSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCacheLRUSize, "");
        return attrs;
    }

    /**
     * Starting month in cached range. 0 means current month, -1 means last
     * month, etc.
     *
     * @return zimbraCalendarCacheRangeMonthFrom, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1696)
    public int getCalendarCacheRangeMonthFrom() {
        return getIntAttr(Provisioning.A_zimbraCalendarCacheRangeMonthFrom, -1);
    }

    /**
     * Starting month in cached range. 0 means current month, -1 means last
     * month, etc.
     *
     * @param zimbraCalendarCacheRangeMonthFrom new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1696)
    public void setCalendarCacheRangeMonthFrom(int zimbraCalendarCacheRangeMonthFrom) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCacheRangeMonthFrom, Integer.toString(zimbraCalendarCacheRangeMonthFrom));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Starting month in cached range. 0 means current month, -1 means last
     * month, etc.
     *
     * @param zimbraCalendarCacheRangeMonthFrom new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1696)
    public Map<String,Object> setCalendarCacheRangeMonthFrom(int zimbraCalendarCacheRangeMonthFrom, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCacheRangeMonthFrom, Integer.toString(zimbraCalendarCacheRangeMonthFrom));
        return attrs;
    }

    /**
     * Starting month in cached range. 0 means current month, -1 means last
     * month, etc.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1696)
    public void unsetCalendarCacheRangeMonthFrom() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCacheRangeMonthFrom, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Starting month in cached range. 0 means current month, -1 means last
     * month, etc.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1696)
    public Map<String,Object> unsetCalendarCacheRangeMonthFrom(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCacheRangeMonthFrom, "");
        return attrs;
    }

    /**
     * Number of months in cached range
     *
     * @return zimbraCalendarCacheRangeMonths, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1697)
    public int getCalendarCacheRangeMonths() {
        return getIntAttr(Provisioning.A_zimbraCalendarCacheRangeMonths, -1);
    }

    /**
     * Number of months in cached range
     *
     * @param zimbraCalendarCacheRangeMonths new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1697)
    public void setCalendarCacheRangeMonths(int zimbraCalendarCacheRangeMonths) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCacheRangeMonths, Integer.toString(zimbraCalendarCacheRangeMonths));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Number of months in cached range
     *
     * @param zimbraCalendarCacheRangeMonths new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1697)
    public Map<String,Object> setCalendarCacheRangeMonths(int zimbraCalendarCacheRangeMonths, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCacheRangeMonths, Integer.toString(zimbraCalendarCacheRangeMonths));
        return attrs;
    }

    /**
     * Number of months in cached range
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1697)
    public void unsetCalendarCacheRangeMonths() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCacheRangeMonths, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Number of months in cached range
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1697)
    public Map<String,Object> unsetCalendarCacheRangeMonths(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarCacheRangeMonths, "");
        return attrs;
    }

    /**
     * Calendar exchange form auth url
     *
     * @return zimbraCalendarExchangeFormAuthURL, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1699)
    public String getCalendarExchangeFormAuthURL() {
        return getAttr(Provisioning.A_zimbraCalendarExchangeFormAuthURL, null);
    }

    /**
     * Calendar exchange form auth url
     *
     * @param zimbraCalendarExchangeFormAuthURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1699)
    public void setCalendarExchangeFormAuthURL(String zimbraCalendarExchangeFormAuthURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarExchangeFormAuthURL, zimbraCalendarExchangeFormAuthURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Calendar exchange form auth url
     *
     * @param zimbraCalendarExchangeFormAuthURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1699)
    public Map<String,Object> setCalendarExchangeFormAuthURL(String zimbraCalendarExchangeFormAuthURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarExchangeFormAuthURL, zimbraCalendarExchangeFormAuthURL);
        return attrs;
    }

    /**
     * Calendar exchange form auth url
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1699)
    public void unsetCalendarExchangeFormAuthURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarExchangeFormAuthURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Calendar exchange form auth url
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1699)
    public Map<String,Object> unsetCalendarExchangeFormAuthURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarExchangeFormAuthURL, "");
        return attrs;
    }

    /**
     * Calendar free busy max days
     *
     * @return zimbraCalendarFreeBusyMaxDays, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1705)
    public int getCalendarFreeBusyMaxDays() {
        return getIntAttr(Provisioning.A_zimbraCalendarFreeBusyMaxDays, -1);
    }

    /**
     * Calendar free busy max days
     *
     * @param zimbraCalendarFreeBusyMaxDays new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1705)
    public void setCalendarFreeBusyMaxDays(int zimbraCalendarFreeBusyMaxDays) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarFreeBusyMaxDays, Integer.toString(zimbraCalendarFreeBusyMaxDays));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Calendar free busy max days
     *
     * @param zimbraCalendarFreeBusyMaxDays new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1705)
    public Map<String,Object> setCalendarFreeBusyMaxDays(int zimbraCalendarFreeBusyMaxDays, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarFreeBusyMaxDays, Integer.toString(zimbraCalendarFreeBusyMaxDays));
        return attrs;
    }

    /**
     * Calendar free busy max days
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1705)
    public void unsetCalendarFreeBusyMaxDays() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarFreeBusyMaxDays, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Calendar free busy max days
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1705)
    public Map<String,Object> unsetCalendarFreeBusyMaxDays(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarFreeBusyMaxDays, "");
        return attrs;
    }

    /**
     * Calendar ics export buffer size in bytes
     *
     * @return zimbraCalendarIcsExportBufferSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1702)
    public int getCalendarIcsExportBufferSize() {
        return getIntAttr(Provisioning.A_zimbraCalendarIcsExportBufferSize, -1);
    }

    /**
     * Calendar ics export buffer size in bytes
     *
     * @param zimbraCalendarIcsExportBufferSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1702)
    public void setCalendarIcsExportBufferSize(int zimbraCalendarIcsExportBufferSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarIcsExportBufferSize, Integer.toString(zimbraCalendarIcsExportBufferSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Calendar ics export buffer size in bytes
     *
     * @param zimbraCalendarIcsExportBufferSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1702)
    public Map<String,Object> setCalendarIcsExportBufferSize(int zimbraCalendarIcsExportBufferSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarIcsExportBufferSize, Integer.toString(zimbraCalendarIcsExportBufferSize));
        return attrs;
    }

    /**
     * Calendar ics export buffer size in bytes
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1702)
    public void unsetCalendarIcsExportBufferSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarIcsExportBufferSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Calendar ics export buffer size in bytes
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1702)
    public Map<String,Object> unsetCalendarIcsExportBufferSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarIcsExportBufferSize, "");
        return attrs;
    }

    /**
     * During ics import use full parser if ics size is less than or equal to
     * this; larger ics files are parsed with callback parser which
     * doesn&#039;t allow forward references to VTIMEZONE TZID
     *
     * @return zimbraCalendarIcsImportFullParseMaxSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1701)
    public int getCalendarIcsImportFullParseMaxSize() {
        return getIntAttr(Provisioning.A_zimbraCalendarIcsImportFullParseMaxSize, -1);
    }

    /**
     * During ics import use full parser if ics size is less than or equal to
     * this; larger ics files are parsed with callback parser which
     * doesn&#039;t allow forward references to VTIMEZONE TZID
     *
     * @param zimbraCalendarIcsImportFullParseMaxSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1701)
    public void setCalendarIcsImportFullParseMaxSize(int zimbraCalendarIcsImportFullParseMaxSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarIcsImportFullParseMaxSize, Integer.toString(zimbraCalendarIcsImportFullParseMaxSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * During ics import use full parser if ics size is less than or equal to
     * this; larger ics files are parsed with callback parser which
     * doesn&#039;t allow forward references to VTIMEZONE TZID
     *
     * @param zimbraCalendarIcsImportFullParseMaxSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1701)
    public Map<String,Object> setCalendarIcsImportFullParseMaxSize(int zimbraCalendarIcsImportFullParseMaxSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarIcsImportFullParseMaxSize, Integer.toString(zimbraCalendarIcsImportFullParseMaxSize));
        return attrs;
    }

    /**
     * During ics import use full parser if ics size is less than or equal to
     * this; larger ics files are parsed with callback parser which
     * doesn&#039;t allow forward references to VTIMEZONE TZID
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1701)
    public void unsetCalendarIcsImportFullParseMaxSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarIcsImportFullParseMaxSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * During ics import use full parser if ics size is less than or equal to
     * this; larger ics files are parsed with callback parser which
     * doesn&#039;t allow forward references to VTIMEZONE TZID
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1701)
    public Map<String,Object> unsetCalendarIcsImportFullParseMaxSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarIcsImportFullParseMaxSize, "");
        return attrs;
    }

    /**
     * Maximum retries for calender item
     *
     * @return zimbraCalendarItemGetMaxRetries, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1700)
    public int getCalendarItemGetMaxRetries() {
        return getIntAttr(Provisioning.A_zimbraCalendarItemGetMaxRetries, -1);
    }

    /**
     * Maximum retries for calender item
     *
     * @param zimbraCalendarItemGetMaxRetries new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1700)
    public void setCalendarItemGetMaxRetries(int zimbraCalendarItemGetMaxRetries) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarItemGetMaxRetries, Integer.toString(zimbraCalendarItemGetMaxRetries));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum retries for calender item
     *
     * @param zimbraCalendarItemGetMaxRetries new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1700)
    public Map<String,Object> setCalendarItemGetMaxRetries(int zimbraCalendarItemGetMaxRetries, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarItemGetMaxRetries, Integer.toString(zimbraCalendarItemGetMaxRetries));
        return attrs;
    }

    /**
     * Maximum retries for calender item
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1700)
    public void unsetCalendarItemGetMaxRetries() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarItemGetMaxRetries, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum retries for calender item
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1700)
    public Map<String,Object> unsetCalendarItemGetMaxRetries(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarItemGetMaxRetries, "");
        return attrs;
    }

    /**
     * Calendar maximum description in metadata in bytes
     *
     * @return zimbraCalendarMaxDescInMetadata, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1704)
    public int getCalendarMaxDescInMetadata() {
        return getIntAttr(Provisioning.A_zimbraCalendarMaxDescInMetadata, -1);
    }

    /**
     * Calendar maximum description in metadata in bytes
     *
     * @param zimbraCalendarMaxDescInMetadata new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1704)
    public void setCalendarMaxDescInMetadata(int zimbraCalendarMaxDescInMetadata) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarMaxDescInMetadata, Integer.toString(zimbraCalendarMaxDescInMetadata));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Calendar maximum description in metadata in bytes
     *
     * @param zimbraCalendarMaxDescInMetadata new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1704)
    public Map<String,Object> setCalendarMaxDescInMetadata(int zimbraCalendarMaxDescInMetadata, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarMaxDescInMetadata, Integer.toString(zimbraCalendarMaxDescInMetadata));
        return attrs;
    }

    /**
     * Calendar maximum description in metadata in bytes
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1704)
    public void unsetCalendarMaxDescInMetadata() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarMaxDescInMetadata, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Calendar maximum description in metadata in bytes
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1704)
    public Map<String,Object> unsetCalendarMaxDescInMetadata(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarMaxDescInMetadata, "");
        return attrs;
    }

    /**
     * Maximum stale items in calendar cache
     *
     * @return zimbraCalendarMaxStaleItems, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1698)
    public int getCalendarMaxStaleItems() {
        return getIntAttr(Provisioning.A_zimbraCalendarMaxStaleItems, -1);
    }

    /**
     * Maximum stale items in calendar cache
     *
     * @param zimbraCalendarMaxStaleItems new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1698)
    public void setCalendarMaxStaleItems(int zimbraCalendarMaxStaleItems) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarMaxStaleItems, Integer.toString(zimbraCalendarMaxStaleItems));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum stale items in calendar cache
     *
     * @param zimbraCalendarMaxStaleItems new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1698)
    public Map<String,Object> setCalendarMaxStaleItems(int zimbraCalendarMaxStaleItems, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarMaxStaleItems, Integer.toString(zimbraCalendarMaxStaleItems));
        return attrs;
    }

    /**
     * Maximum stale items in calendar cache
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1698)
    public void unsetCalendarMaxStaleItems() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarMaxStaleItems, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum stale items in calendar cache
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1698)
    public Map<String,Object> unsetCalendarMaxStaleItems(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarMaxStaleItems, "");
        return attrs;
    }

    /**
     * Max ldap search size for calendar resource
     *
     * @return zimbraCalendarResourceLdapSearchMaxSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1742)
    public int getCalendarResourceLdapSearchMaxSize() {
        return getIntAttr(Provisioning.A_zimbraCalendarResourceLdapSearchMaxSize, -1);
    }

    /**
     * Max ldap search size for calendar resource
     *
     * @param zimbraCalendarResourceLdapSearchMaxSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1742)
    public void setCalendarResourceLdapSearchMaxSize(int zimbraCalendarResourceLdapSearchMaxSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarResourceLdapSearchMaxSize, Integer.toString(zimbraCalendarResourceLdapSearchMaxSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Max ldap search size for calendar resource
     *
     * @param zimbraCalendarResourceLdapSearchMaxSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1742)
    public Map<String,Object> setCalendarResourceLdapSearchMaxSize(int zimbraCalendarResourceLdapSearchMaxSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarResourceLdapSearchMaxSize, Integer.toString(zimbraCalendarResourceLdapSearchMaxSize));
        return attrs;
    }

    /**
     * Max ldap search size for calendar resource
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1742)
    public void unsetCalendarResourceLdapSearchMaxSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarResourceLdapSearchMaxSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Max ldap search size for calendar resource
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1742)
    public Map<String,Object> unsetCalendarResourceLdapSearchMaxSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarResourceLdapSearchMaxSize, "");
        return attrs;
    }

    /**
     * Calendar search maximum days
     *
     * @return zimbraCalendarSearchMaxDays, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1706)
    public int getCalendarSearchMaxDays() {
        return getIntAttr(Provisioning.A_zimbraCalendarSearchMaxDays, -1);
    }

    /**
     * Calendar search maximum days
     *
     * @param zimbraCalendarSearchMaxDays new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1706)
    public void setCalendarSearchMaxDays(int zimbraCalendarSearchMaxDays) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarSearchMaxDays, Integer.toString(zimbraCalendarSearchMaxDays));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Calendar search maximum days
     *
     * @param zimbraCalendarSearchMaxDays new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1706)
    public Map<String,Object> setCalendarSearchMaxDays(int zimbraCalendarSearchMaxDays, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarSearchMaxDays, Integer.toString(zimbraCalendarSearchMaxDays));
        return attrs;
    }

    /**
     * Calendar search maximum days
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1706)
    public void unsetCalendarSearchMaxDays() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarSearchMaxDays, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Calendar search maximum days
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1706)
    public Map<String,Object> unsetCalendarSearchMaxDays(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarSearchMaxDays, "");
        return attrs;
    }

    /**
     * time object was created
     *
     * <p>Use getCreateTimestampAsString to access value as a string.
     *
     * @see #getCreateTimestampAsString()
     *
     * @return zimbraCreateTimestamp as Date, null if unset or unable to parse
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=790)
    public Date getCreateTimestamp() {
        return getGeneralizedTimeAttr(Provisioning.A_zimbraCreateTimestamp, null);
    }

    /**
     * time object was created
     *
     * @return zimbraCreateTimestamp, or null if unset
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=790)
    public String getCreateTimestampAsString() {
        return getAttr(Provisioning.A_zimbraCreateTimestamp, null);
    }

    /**
     * time object was created
     *
     * @param zimbraCreateTimestamp new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=790)
    public void setCreateTimestamp(Date zimbraCreateTimestamp) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCreateTimestamp, zimbraCreateTimestamp==null ? "" : DateUtil.toGeneralizedTime(zimbraCreateTimestamp));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * time object was created
     *
     * @param zimbraCreateTimestamp new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=790)
    public Map<String,Object> setCreateTimestamp(Date zimbraCreateTimestamp, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCreateTimestamp, zimbraCreateTimestamp==null ? "" : DateUtil.toGeneralizedTime(zimbraCreateTimestamp));
        return attrs;
    }

    /**
     * time object was created
     *
     * @param zimbraCreateTimestamp new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=790)
    public void setCreateTimestampAsString(String zimbraCreateTimestamp) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCreateTimestamp, zimbraCreateTimestamp);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * time object was created
     *
     * @param zimbraCreateTimestamp new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=790)
    public Map<String,Object> setCreateTimestampAsString(String zimbraCreateTimestamp, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCreateTimestamp, zimbraCreateTimestamp);
        return attrs;
    }

    /**
     * time object was created
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=790)
    public void unsetCreateTimestamp() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCreateTimestamp, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * time object was created
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=790)
    public Map<String,Object> unsetCreateTimestamp(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCreateTimestamp, "");
        return attrs;
    }

    /**
     * Flag to skip checking DL membership when removing appoitment
     * atttendees in ZD. Introduced in bug 68728&quot;
     *
     * @return zimbraDesktopCalendarCheckDLMembership, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1745)
    public boolean isDesktopCalendarCheckDLMembership() {
        return getBooleanAttr(Provisioning.A_zimbraDesktopCalendarCheckDLMembership, false);
    }

    /**
     * Flag to skip checking DL membership when removing appoitment
     * atttendees in ZD. Introduced in bug 68728&quot;
     *
     * @param zimbraDesktopCalendarCheckDLMembership new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1745)
    public void setDesktopCalendarCheckDLMembership(boolean zimbraDesktopCalendarCheckDLMembership) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDesktopCalendarCheckDLMembership, zimbraDesktopCalendarCheckDLMembership ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to skip checking DL membership when removing appoitment
     * atttendees in ZD. Introduced in bug 68728&quot;
     *
     * @param zimbraDesktopCalendarCheckDLMembership new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1745)
    public Map<String,Object> setDesktopCalendarCheckDLMembership(boolean zimbraDesktopCalendarCheckDLMembership, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDesktopCalendarCheckDLMembership, zimbraDesktopCalendarCheckDLMembership ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Flag to skip checking DL membership when removing appoitment
     * atttendees in ZD. Introduced in bug 68728&quot;
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1745)
    public void unsetDesktopCalendarCheckDLMembership() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDesktopCalendarCheckDLMembership, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to skip checking DL membership when removing appoitment
     * atttendees in ZD. Introduced in bug 68728&quot;
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1745)
    public Map<String,Object> unsetDesktopCalendarCheckDLMembership(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDesktopCalendarCheckDLMembership, "");
        return attrs;
    }

    /**
     * File path for ews logs
     *
     * @return zimbraEwsServiceLogFile, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1747)
    public String getEwsServiceLogFile() {
        return getAttr(Provisioning.A_zimbraEwsServiceLogFile, null);
    }

    /**
     * File path for ews logs
     *
     * @param zimbraEwsServiceLogFile new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1747)
    public void setEwsServiceLogFile(String zimbraEwsServiceLogFile) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEwsServiceLogFile, zimbraEwsServiceLogFile);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * File path for ews logs
     *
     * @param zimbraEwsServiceLogFile new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1747)
    public Map<String,Object> setEwsServiceLogFile(String zimbraEwsServiceLogFile, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEwsServiceLogFile, zimbraEwsServiceLogFile);
        return attrs;
    }

    /**
     * File path for ews logs
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1747)
    public void unsetEwsServiceLogFile() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEwsServiceLogFile, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * File path for ews logs
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1747)
    public Map<String,Object> unsetEwsServiceLogFile(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEwsServiceLogFile, "");
        return attrs;
    }

    /**
     * Path specifying the location of ews wsdl
     *
     * @return zimbraEwsWsdlLocation, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1746)
    public String getEwsWsdlLocation() {
        return getAttr(Provisioning.A_zimbraEwsWsdlLocation, null);
    }

    /**
     * Path specifying the location of ews wsdl
     *
     * @param zimbraEwsWsdlLocation new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1746)
    public void setEwsWsdlLocation(String zimbraEwsWsdlLocation) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEwsWsdlLocation, zimbraEwsWsdlLocation);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Path specifying the location of ews wsdl
     *
     * @param zimbraEwsWsdlLocation new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1746)
    public Map<String,Object> setEwsWsdlLocation(String zimbraEwsWsdlLocation, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEwsWsdlLocation, zimbraEwsWsdlLocation);
        return attrs;
    }

    /**
     * Path specifying the location of ews wsdl
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1746)
    public void unsetEwsWsdlLocation() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEwsWsdlLocation, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Path specifying the location of ews wsdl
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1746)
    public Map<String,Object> unsetEwsWsdlLocation(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEwsWsdlLocation, "");
        return attrs;
    }

    /**
     * Maximum age in minutes of GAL group email addresses in cache.
     * Applicable for a domain only when LDAP domain attribute
     * zimbraGalGroupIndicatorEnabled in TRUE.Default is 7 days
     *
     * @return zimbraGalGroupCacheMaxAge, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1748)
    public int getGalGroupCacheMaxAge() {
        return getIntAttr(Provisioning.A_zimbraGalGroupCacheMaxAge, -1);
    }

    /**
     * Maximum age in minutes of GAL group email addresses in cache.
     * Applicable for a domain only when LDAP domain attribute
     * zimbraGalGroupIndicatorEnabled in TRUE.Default is 7 days
     *
     * @param zimbraGalGroupCacheMaxAge new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1748)
    public void setGalGroupCacheMaxAge(int zimbraGalGroupCacheMaxAge) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalGroupCacheMaxAge, Integer.toString(zimbraGalGroupCacheMaxAge));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum age in minutes of GAL group email addresses in cache.
     * Applicable for a domain only when LDAP domain attribute
     * zimbraGalGroupIndicatorEnabled in TRUE.Default is 7 days
     *
     * @param zimbraGalGroupCacheMaxAge new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1748)
    public Map<String,Object> setGalGroupCacheMaxAge(int zimbraGalGroupCacheMaxAge, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalGroupCacheMaxAge, Integer.toString(zimbraGalGroupCacheMaxAge));
        return attrs;
    }

    /**
     * Maximum age in minutes of GAL group email addresses in cache.
     * Applicable for a domain only when LDAP domain attribute
     * zimbraGalGroupIndicatorEnabled in TRUE.Default is 7 days
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1748)
    public void unsetGalGroupCacheMaxAge() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalGroupCacheMaxAge, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum age in minutes of GAL group email addresses in cache.
     * Applicable for a domain only when LDAP domain attribute
     * zimbraGalGroupIndicatorEnabled in TRUE.Default is 7 days
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1748)
    public Map<String,Object> unsetGalGroupCacheMaxAge(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalGroupCacheMaxAge, "");
        return attrs;
    }

    /**
     * Maximum number of domains that can be put into the GAL group cache.
     * Applicable for a domain only when LDAP domain attribute
     * zimbraGalGroupIndicatorEnabled in TRUE.
     *
     * @return zimbraGalGroupCacheMaxSizeDomains, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1749)
    public int getGalGroupCacheMaxSizeDomains() {
        return getIntAttr(Provisioning.A_zimbraGalGroupCacheMaxSizeDomains, -1);
    }

    /**
     * Maximum number of domains that can be put into the GAL group cache.
     * Applicable for a domain only when LDAP domain attribute
     * zimbraGalGroupIndicatorEnabled in TRUE.
     *
     * @param zimbraGalGroupCacheMaxSizeDomains new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1749)
    public void setGalGroupCacheMaxSizeDomains(int zimbraGalGroupCacheMaxSizeDomains) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalGroupCacheMaxSizeDomains, Integer.toString(zimbraGalGroupCacheMaxSizeDomains));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of domains that can be put into the GAL group cache.
     * Applicable for a domain only when LDAP domain attribute
     * zimbraGalGroupIndicatorEnabled in TRUE.
     *
     * @param zimbraGalGroupCacheMaxSizeDomains new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1749)
    public Map<String,Object> setGalGroupCacheMaxSizeDomains(int zimbraGalGroupCacheMaxSizeDomains, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalGroupCacheMaxSizeDomains, Integer.toString(zimbraGalGroupCacheMaxSizeDomains));
        return attrs;
    }

    /**
     * Maximum number of domains that can be put into the GAL group cache.
     * Applicable for a domain only when LDAP domain attribute
     * zimbraGalGroupIndicatorEnabled in TRUE.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1749)
    public void unsetGalGroupCacheMaxSizeDomains() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalGroupCacheMaxSizeDomains, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of domains that can be put into the GAL group cache.
     * Applicable for a domain only when LDAP domain attribute
     * zimbraGalGroupIndicatorEnabled in TRUE.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1749)
    public Map<String,Object> unsetGalGroupCacheMaxSizeDomains(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalGroupCacheMaxSizeDomains, "");
        return attrs;
    }

    /**
     * Maximum number of GAL group email addresses cached per domain.
     * Applicable for a domain only when LDAP domain attribute
     * zimbraGalGroupIndicatorEnabled in TRUE.
     *
     * @return zimbraGalGroupCacheMaxSizePerDomain, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1750)
    public int getGalGroupCacheMaxSizePerDomain() {
        return getIntAttr(Provisioning.A_zimbraGalGroupCacheMaxSizePerDomain, -1);
    }

    /**
     * Maximum number of GAL group email addresses cached per domain.
     * Applicable for a domain only when LDAP domain attribute
     * zimbraGalGroupIndicatorEnabled in TRUE.
     *
     * @param zimbraGalGroupCacheMaxSizePerDomain new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1750)
    public void setGalGroupCacheMaxSizePerDomain(int zimbraGalGroupCacheMaxSizePerDomain) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalGroupCacheMaxSizePerDomain, Integer.toString(zimbraGalGroupCacheMaxSizePerDomain));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of GAL group email addresses cached per domain.
     * Applicable for a domain only when LDAP domain attribute
     * zimbraGalGroupIndicatorEnabled in TRUE.
     *
     * @param zimbraGalGroupCacheMaxSizePerDomain new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1750)
    public Map<String,Object> setGalGroupCacheMaxSizePerDomain(int zimbraGalGroupCacheMaxSizePerDomain, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalGroupCacheMaxSizePerDomain, Integer.toString(zimbraGalGroupCacheMaxSizePerDomain));
        return attrs;
    }

    /**
     * Maximum number of GAL group email addresses cached per domain.
     * Applicable for a domain only when LDAP domain attribute
     * zimbraGalGroupIndicatorEnabled in TRUE.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1750)
    public void unsetGalGroupCacheMaxSizePerDomain() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalGroupCacheMaxSizePerDomain, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of GAL group email addresses cached per domain.
     * Applicable for a domain only when LDAP domain attribute
     * zimbraGalGroupIndicatorEnabled in TRUE.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1750)
    public Map<String,Object> unsetGalGroupCacheMaxSizePerDomain(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalGroupCacheMaxSizePerDomain, "");
        return attrs;
    }

    /**
     * Zimbra Systems Unique ID
     *
     * @return zimbraId, or null if unset
     */
    @ZAttr(id=1)
    public String getId() {
        return getAttr(Provisioning.A_zimbraId, null);
    }

    /**
     * Zimbra Systems Unique ID
     *
     * @param zimbraId new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=1)
    public void setId(String zimbraId) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraId, zimbraId);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Zimbra Systems Unique ID
     *
     * @param zimbraId new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=1)
    public Map<String,Object> setId(String zimbraId, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraId, zimbraId);
        return attrs;
    }

    /**
     * Zimbra Systems Unique ID
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=1)
    public void unsetId() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraId, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Zimbra Systems Unique ID
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=1)
    public Map<String,Object> unsetId(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraId, "");
        return attrs;
    }

    /**
     * Imap authenticated max idle time in seconds
     *
     * @return zimbraImapAuthenticatedMaxIdleTime, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1714)
    public int getImapAuthenticatedMaxIdleTime() {
        return getIntAttr(Provisioning.A_zimbraImapAuthenticatedMaxIdleTime, -1);
    }

    /**
     * Imap authenticated max idle time in seconds
     *
     * @param zimbraImapAuthenticatedMaxIdleTime new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1714)
    public void setImapAuthenticatedMaxIdleTime(int zimbraImapAuthenticatedMaxIdleTime) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapAuthenticatedMaxIdleTime, Integer.toString(zimbraImapAuthenticatedMaxIdleTime));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Imap authenticated max idle time in seconds
     *
     * @param zimbraImapAuthenticatedMaxIdleTime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1714)
    public Map<String,Object> setImapAuthenticatedMaxIdleTime(int zimbraImapAuthenticatedMaxIdleTime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapAuthenticatedMaxIdleTime, Integer.toString(zimbraImapAuthenticatedMaxIdleTime));
        return attrs;
    }

    /**
     * Imap authenticated max idle time in seconds
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1714)
    public void unsetImapAuthenticatedMaxIdleTime() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapAuthenticatedMaxIdleTime, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Imap authenticated max idle time in seconds
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1714)
    public Map<String,Object> unsetImapAuthenticatedMaxIdleTime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapAuthenticatedMaxIdleTime, "");
        return attrs;
    }

    /**
     * Enable debug for IMAP
     *
     * @return zimbraImapEnableDebug, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1755)
    public boolean isImapEnableDebug() {
        return getBooleanAttr(Provisioning.A_zimbraImapEnableDebug, false);
    }

    /**
     * Enable debug for IMAP
     *
     * @param zimbraImapEnableDebug new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1755)
    public void setImapEnableDebug(boolean zimbraImapEnableDebug) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapEnableDebug, zimbraImapEnableDebug ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enable debug for IMAP
     *
     * @param zimbraImapEnableDebug new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1755)
    public Map<String,Object> setImapEnableDebug(boolean zimbraImapEnableDebug, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapEnableDebug, zimbraImapEnableDebug ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Enable debug for IMAP
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1755)
    public void unsetImapEnableDebug() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapEnableDebug, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enable debug for IMAP
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1755)
    public Map<String,Object> unsetImapEnableDebug(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapEnableDebug, "");
        return attrs;
    }

    /**
     * Enable STARTTLS for Imap
     *
     * @return zimbraImapEnableStartTls, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1756)
    public boolean isImapEnableStartTls() {
        return getBooleanAttr(Provisioning.A_zimbraImapEnableStartTls, false);
    }

    /**
     * Enable STARTTLS for Imap
     *
     * @param zimbraImapEnableStartTls new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1756)
    public void setImapEnableStartTls(boolean zimbraImapEnableStartTls) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapEnableStartTls, zimbraImapEnableStartTls ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enable STARTTLS for Imap
     *
     * @param zimbraImapEnableStartTls new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1756)
    public Map<String,Object> setImapEnableStartTls(boolean zimbraImapEnableStartTls, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapEnableStartTls, zimbraImapEnableStartTls ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Enable STARTTLS for Imap
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1756)
    public void unsetImapEnableStartTls() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapEnableStartTls, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enable STARTTLS for Imap
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1756)
    public Map<String,Object> unsetImapEnableStartTls(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapEnableStartTls, "");
        return attrs;
    }

    /**
     * Ehcache: the maximum number of inactive IMAP cache entries on disk
     * before eviction.
     *
     * @return zimbraImapInactiveSessionCacheSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1708)
    public int getImapInactiveSessionCacheSize() {
        return getIntAttr(Provisioning.A_zimbraImapInactiveSessionCacheSize, -1);
    }

    /**
     * Ehcache: the maximum number of inactive IMAP cache entries on disk
     * before eviction.
     *
     * @param zimbraImapInactiveSessionCacheSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1708)
    public void setImapInactiveSessionCacheSize(int zimbraImapInactiveSessionCacheSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapInactiveSessionCacheSize, Integer.toString(zimbraImapInactiveSessionCacheSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Ehcache: the maximum number of inactive IMAP cache entries on disk
     * before eviction.
     *
     * @param zimbraImapInactiveSessionCacheSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1708)
    public Map<String,Object> setImapInactiveSessionCacheSize(int zimbraImapInactiveSessionCacheSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapInactiveSessionCacheSize, Integer.toString(zimbraImapInactiveSessionCacheSize));
        return attrs;
    }

    /**
     * Ehcache: the maximum number of inactive IMAP cache entries on disk
     * before eviction.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1708)
    public void unsetImapInactiveSessionCacheSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapInactiveSessionCacheSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Ehcache: the maximum number of inactive IMAP cache entries on disk
     * before eviction.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1708)
    public Map<String,Object> unsetImapInactiveSessionCacheSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapInactiveSessionCacheSize, "");
        return attrs;
    }

    /**
     * If greater than 0, drop the imap connection if
     * zimbraImapMaxConsecutiveError value is reached.
     *
     * @return zimbraImapMaxConsecutiveError, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1707)
    public int getImapMaxConsecutiveError() {
        return getIntAttr(Provisioning.A_zimbraImapMaxConsecutiveError, -1);
    }

    /**
     * If greater than 0, drop the imap connection if
     * zimbraImapMaxConsecutiveError value is reached.
     *
     * @param zimbraImapMaxConsecutiveError new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1707)
    public void setImapMaxConsecutiveError(int zimbraImapMaxConsecutiveError) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapMaxConsecutiveError, Integer.toString(zimbraImapMaxConsecutiveError));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * If greater than 0, drop the imap connection if
     * zimbraImapMaxConsecutiveError value is reached.
     *
     * @param zimbraImapMaxConsecutiveError new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1707)
    public Map<String,Object> setImapMaxConsecutiveError(int zimbraImapMaxConsecutiveError, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapMaxConsecutiveError, Integer.toString(zimbraImapMaxConsecutiveError));
        return attrs;
    }

    /**
     * If greater than 0, drop the imap connection if
     * zimbraImapMaxConsecutiveError value is reached.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1707)
    public void unsetImapMaxConsecutiveError() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapMaxConsecutiveError, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * If greater than 0, drop the imap connection if
     * zimbraImapMaxConsecutiveError value is reached.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1707)
    public Map<String,Object> unsetImapMaxConsecutiveError(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapMaxConsecutiveError, "");
        return attrs;
    }

    /**
     * Imap max idle time in seconds
     *
     * @return zimbraImapMaxIdleTime, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1713)
    public int getImapMaxIdleTime() {
        return getIntAttr(Provisioning.A_zimbraImapMaxIdleTime, -1);
    }

    /**
     * Imap max idle time in seconds
     *
     * @param zimbraImapMaxIdleTime new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1713)
    public void setImapMaxIdleTime(int zimbraImapMaxIdleTime) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapMaxIdleTime, Integer.toString(zimbraImapMaxIdleTime));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Imap max idle time in seconds
     *
     * @param zimbraImapMaxIdleTime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1713)
    public Map<String,Object> setImapMaxIdleTime(int zimbraImapMaxIdleTime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapMaxIdleTime, Integer.toString(zimbraImapMaxIdleTime));
        return attrs;
    }

    /**
     * Imap max idle time in seconds
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1713)
    public void unsetImapMaxIdleTime() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapMaxIdleTime, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Imap max idle time in seconds
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1713)
    public Map<String,Object> unsetImapMaxIdleTime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapMaxIdleTime, "");
        return attrs;
    }

    /**
     * Flag to enable or disable to reuse imap data source connection
     *
     * @return zimbraImapReuseDataSourceConnections, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1719)
    public boolean isImapReuseDataSourceConnections() {
        return getBooleanAttr(Provisioning.A_zimbraImapReuseDataSourceConnections, false);
    }

    /**
     * Flag to enable or disable to reuse imap data source connection
     *
     * @param zimbraImapReuseDataSourceConnections new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1719)
    public void setImapReuseDataSourceConnections(boolean zimbraImapReuseDataSourceConnections) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapReuseDataSourceConnections, zimbraImapReuseDataSourceConnections ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable or disable to reuse imap data source connection
     *
     * @param zimbraImapReuseDataSourceConnections new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1719)
    public Map<String,Object> setImapReuseDataSourceConnections(boolean zimbraImapReuseDataSourceConnections, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapReuseDataSourceConnections, zimbraImapReuseDataSourceConnections ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Flag to enable or disable to reuse imap data source connection
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1719)
    public void unsetImapReuseDataSourceConnections() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapReuseDataSourceConnections, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable or disable to reuse imap data source connection
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1719)
    public Map<String,Object> unsetImapReuseDataSourceConnections(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapReuseDataSourceConnections, "");
        return attrs;
    }

    /**
     * Maximum number of IMAP folders a single user can have open at once
     *
     * @return zimbraImapSessionLimit, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1737)
    public int getImapSessionLimit() {
        return getIntAttr(Provisioning.A_zimbraImapSessionLimit, -1);
    }

    /**
     * Maximum number of IMAP folders a single user can have open at once
     *
     * @param zimbraImapSessionLimit new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1737)
    public void setImapSessionLimit(int zimbraImapSessionLimit) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSessionLimit, Integer.toString(zimbraImapSessionLimit));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of IMAP folders a single user can have open at once
     *
     * @param zimbraImapSessionLimit new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1737)
    public Map<String,Object> setImapSessionLimit(int zimbraImapSessionLimit, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSessionLimit, Integer.toString(zimbraImapSessionLimit));
        return attrs;
    }

    /**
     * Maximum number of IMAP folders a single user can have open at once
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1737)
    public void unsetImapSessionLimit() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSessionLimit, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of IMAP folders a single user can have open at once
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1737)
    public Map<String,Object> unsetImapSessionLimit(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapSessionLimit, "");
        return attrs;
    }

    /**
     * Imap keep alive time in seconds
     *
     * @return zimbraImapThreadKeepAliveTime, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1712)
    public int getImapThreadKeepAliveTime() {
        return getIntAttr(Provisioning.A_zimbraImapThreadKeepAliveTime, -1);
    }

    /**
     * Imap keep alive time in seconds
     *
     * @param zimbraImapThreadKeepAliveTime new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1712)
    public void setImapThreadKeepAliveTime(int zimbraImapThreadKeepAliveTime) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapThreadKeepAliveTime, Integer.toString(zimbraImapThreadKeepAliveTime));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Imap keep alive time in seconds
     *
     * @param zimbraImapThreadKeepAliveTime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1712)
    public Map<String,Object> setImapThreadKeepAliveTime(int zimbraImapThreadKeepAliveTime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapThreadKeepAliveTime, Integer.toString(zimbraImapThreadKeepAliveTime));
        return attrs;
    }

    /**
     * Imap keep alive time in seconds
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1712)
    public void unsetImapThreadKeepAliveTime() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapThreadKeepAliveTime, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Imap keep alive time in seconds
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1712)
    public Map<String,Object> unsetImapThreadKeepAliveTime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapThreadKeepAliveTime, "");
        return attrs;
    }

    /**
     * Imap throttle acct limit
     *
     * @return zimbraImapThrottleAcctLimit, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1716)
    public int getImapThrottleAcctLimit() {
        return getIntAttr(Provisioning.A_zimbraImapThrottleAcctLimit, -1);
    }

    /**
     * Imap throttle acct limit
     *
     * @param zimbraImapThrottleAcctLimit new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1716)
    public void setImapThrottleAcctLimit(int zimbraImapThrottleAcctLimit) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapThrottleAcctLimit, Integer.toString(zimbraImapThrottleAcctLimit));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Imap throttle acct limit
     *
     * @param zimbraImapThrottleAcctLimit new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1716)
    public Map<String,Object> setImapThrottleAcctLimit(int zimbraImapThrottleAcctLimit, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapThrottleAcctLimit, Integer.toString(zimbraImapThrottleAcctLimit));
        return attrs;
    }

    /**
     * Imap throttle acct limit
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1716)
    public void unsetImapThrottleAcctLimit() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapThrottleAcctLimit, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Imap throttle acct limit
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1716)
    public Map<String,Object> unsetImapThrottleAcctLimit(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapThrottleAcctLimit, "");
        return attrs;
    }

    /**
     * Imap throttle command limit
     *
     * @return zimbraImapThrottleCommandLimit, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1717)
    public int getImapThrottleCommandLimit() {
        return getIntAttr(Provisioning.A_zimbraImapThrottleCommandLimit, -1);
    }

    /**
     * Imap throttle command limit
     *
     * @param zimbraImapThrottleCommandLimit new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1717)
    public void setImapThrottleCommandLimit(int zimbraImapThrottleCommandLimit) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapThrottleCommandLimit, Integer.toString(zimbraImapThrottleCommandLimit));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Imap throttle command limit
     *
     * @param zimbraImapThrottleCommandLimit new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1717)
    public Map<String,Object> setImapThrottleCommandLimit(int zimbraImapThrottleCommandLimit, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapThrottleCommandLimit, Integer.toString(zimbraImapThrottleCommandLimit));
        return attrs;
    }

    /**
     * Imap throttle command limit
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1717)
    public void unsetImapThrottleCommandLimit() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapThrottleCommandLimit, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Imap throttle command limit
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1717)
    public Map<String,Object> unsetImapThrottleCommandLimit(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapThrottleCommandLimit, "");
        return attrs;
    }

    /**
     * Imap throttle fetch
     *
     * @return zimbraImapThrottleFetch, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1718)
    public boolean isImapThrottleFetch() {
        return getBooleanAttr(Provisioning.A_zimbraImapThrottleFetch, false);
    }

    /**
     * Imap throttle fetch
     *
     * @param zimbraImapThrottleFetch new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1718)
    public void setImapThrottleFetch(boolean zimbraImapThrottleFetch) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapThrottleFetch, zimbraImapThrottleFetch ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Imap throttle fetch
     *
     * @param zimbraImapThrottleFetch new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1718)
    public Map<String,Object> setImapThrottleFetch(boolean zimbraImapThrottleFetch, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapThrottleFetch, zimbraImapThrottleFetch ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Imap throttle fetch
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1718)
    public void unsetImapThrottleFetch() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapThrottleFetch, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Imap throttle fetch
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1718)
    public Map<String,Object> unsetImapThrottleFetch(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapThrottleFetch, "");
        return attrs;
    }

    /**
     * Imap throttle ip limit
     *
     * @return zimbraImapThrottleIpLimit, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1715)
    public int getImapThrottleIpLimit() {
        return getIntAttr(Provisioning.A_zimbraImapThrottleIpLimit, -1);
    }

    /**
     * Imap throttle ip limit
     *
     * @param zimbraImapThrottleIpLimit new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1715)
    public void setImapThrottleIpLimit(int zimbraImapThrottleIpLimit) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapThrottleIpLimit, Integer.toString(zimbraImapThrottleIpLimit));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Imap throttle ip limit
     *
     * @param zimbraImapThrottleIpLimit new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1715)
    public Map<String,Object> setImapThrottleIpLimit(int zimbraImapThrottleIpLimit, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapThrottleIpLimit, Integer.toString(zimbraImapThrottleIpLimit));
        return attrs;
    }

    /**
     * Imap throttle ip limit
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1715)
    public void unsetImapThrottleIpLimit() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapThrottleIpLimit, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Imap throttle ip limit
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1715)
    public Map<String,Object> unsetImapThrottleIpLimit(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapThrottleIpLimit, "");
        return attrs;
    }

    /**
     * Timeout for imap in seconds
     *
     * @return zimbraImapTimeout, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1723)
    public int getImapTimeout() {
        return getIntAttr(Provisioning.A_zimbraImapTimeout, -1);
    }

    /**
     * Timeout for imap in seconds
     *
     * @param zimbraImapTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1723)
    public void setImapTimeout(int zimbraImapTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapTimeout, Integer.toString(zimbraImapTimeout));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Timeout for imap in seconds
     *
     * @param zimbraImapTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1723)
    public Map<String,Object> setImapTimeout(int zimbraImapTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapTimeout, Integer.toString(zimbraImapTimeout));
        return attrs;
    }

    /**
     * Timeout for imap in seconds
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1723)
    public void unsetImapTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Timeout for imap in seconds
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1723)
    public Map<String,Object> unsetImapTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapTimeout, "");
        return attrs;
    }

    /**
     * Flag to enable or disable imap cache
     *
     * @return zimbraImapUseEhcache, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1709)
    public boolean isImapUseEhcache() {
        return getBooleanAttr(Provisioning.A_zimbraImapUseEhcache, false);
    }

    /**
     * Flag to enable or disable imap cache
     *
     * @param zimbraImapUseEhcache new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1709)
    public void setImapUseEhcache(boolean zimbraImapUseEhcache) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapUseEhcache, zimbraImapUseEhcache ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable or disable imap cache
     *
     * @param zimbraImapUseEhcache new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1709)
    public Map<String,Object> setImapUseEhcache(boolean zimbraImapUseEhcache, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapUseEhcache, zimbraImapUseEhcache ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Flag to enable or disable imap cache
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1709)
    public void unsetImapUseEhcache() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapUseEhcache, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable or disable imap cache
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1709)
    public Map<String,Object> unsetImapUseEhcache(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapUseEhcache, "");
        return attrs;
    }

    /**
     * Imap write chunk size in bytes
     *
     * @return zimbraImapWriteChunkSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1711)
    public int getImapWriteChunkSize() {
        return getIntAttr(Provisioning.A_zimbraImapWriteChunkSize, -1);
    }

    /**
     * Imap write chunk size in bytes
     *
     * @param zimbraImapWriteChunkSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1711)
    public void setImapWriteChunkSize(int zimbraImapWriteChunkSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapWriteChunkSize, Integer.toString(zimbraImapWriteChunkSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Imap write chunk size in bytes
     *
     * @param zimbraImapWriteChunkSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1711)
    public Map<String,Object> setImapWriteChunkSize(int zimbraImapWriteChunkSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapWriteChunkSize, Integer.toString(zimbraImapWriteChunkSize));
        return attrs;
    }

    /**
     * Imap write chunk size in bytes
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1711)
    public void unsetImapWriteChunkSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapWriteChunkSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Imap write chunk size in bytes
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1711)
    public Map<String,Object> unsetImapWriteChunkSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapWriteChunkSize, "");
        return attrs;
    }

    /**
     * Set the IMAP session write timeout (nio)
     *
     * @return zimbraImapWriteTimeout, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1710)
    public int getImapWriteTimeout() {
        return getIntAttr(Provisioning.A_zimbraImapWriteTimeout, -1);
    }

    /**
     * Set the IMAP session write timeout (nio)
     *
     * @param zimbraImapWriteTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1710)
    public void setImapWriteTimeout(int zimbraImapWriteTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapWriteTimeout, Integer.toString(zimbraImapWriteTimeout));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Set the IMAP session write timeout (nio)
     *
     * @param zimbraImapWriteTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1710)
    public Map<String,Object> setImapWriteTimeout(int zimbraImapWriteTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapWriteTimeout, Integer.toString(zimbraImapWriteTimeout));
        return attrs;
    }

    /**
     * Set the IMAP session write timeout (nio)
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1710)
    public void unsetImapWriteTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapWriteTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Set the IMAP session write timeout (nio)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1710)
    public Map<String,Object> unsetImapWriteTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapWriteTimeout, "");
        return attrs;
    }

    /**
     * Mailbox lock read write flag
     *
     * @return zimbraMailBoxLockReadWrite, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1733)
    public boolean isMailBoxLockReadWrite() {
        return getBooleanAttr(Provisioning.A_zimbraMailBoxLockReadWrite, false);
    }

    /**
     * Mailbox lock read write flag
     *
     * @param zimbraMailBoxLockReadWrite new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1733)
    public void setMailBoxLockReadWrite(boolean zimbraMailBoxLockReadWrite) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailBoxLockReadWrite, zimbraMailBoxLockReadWrite ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Mailbox lock read write flag
     *
     * @param zimbraMailBoxLockReadWrite new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1733)
    public Map<String,Object> setMailBoxLockReadWrite(boolean zimbraMailBoxLockReadWrite, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailBoxLockReadWrite, zimbraMailBoxLockReadWrite ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Mailbox lock read write flag
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1733)
    public void unsetMailBoxLockReadWrite() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailBoxLockReadWrite, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Mailbox lock read write flag
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1733)
    public Map<String,Object> unsetMailBoxLockReadWrite(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailBoxLockReadWrite, "");
        return attrs;
    }

    /**
     * Maximum timeout in seconds for mailbox lock
     *
     * @return zimbraMailBoxLockTimeout, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1734)
    public int getMailBoxLockTimeout() {
        return getIntAttr(Provisioning.A_zimbraMailBoxLockTimeout, -1);
    }

    /**
     * Maximum timeout in seconds for mailbox lock
     *
     * @param zimbraMailBoxLockTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1734)
    public void setMailBoxLockTimeout(int zimbraMailBoxLockTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailBoxLockTimeout, Integer.toString(zimbraMailBoxLockTimeout));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum timeout in seconds for mailbox lock
     *
     * @param zimbraMailBoxLockTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1734)
    public Map<String,Object> setMailBoxLockTimeout(int zimbraMailBoxLockTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailBoxLockTimeout, Integer.toString(zimbraMailBoxLockTimeout));
        return attrs;
    }

    /**
     * Maximum timeout in seconds for mailbox lock
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1734)
    public void unsetMailBoxLockTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailBoxLockTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum timeout in seconds for mailbox lock
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1734)
    public Map<String,Object> unsetMailBoxLockTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailBoxLockTimeout, "");
        return attrs;
    }

    /**
     * Maximum allowed waiting threads on a mailbox lock
     *
     * @return zimbraMailboxLockMaxWaitingThreads, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1732)
    public int getMailboxLockMaxWaitingThreads() {
        return getIntAttr(Provisioning.A_zimbraMailboxLockMaxWaitingThreads, -1);
    }

    /**
     * Maximum allowed waiting threads on a mailbox lock
     *
     * @param zimbraMailboxLockMaxWaitingThreads new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1732)
    public void setMailboxLockMaxWaitingThreads(int zimbraMailboxLockMaxWaitingThreads) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxLockMaxWaitingThreads, Integer.toString(zimbraMailboxLockMaxWaitingThreads));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum allowed waiting threads on a mailbox lock
     *
     * @param zimbraMailboxLockMaxWaitingThreads new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1732)
    public Map<String,Object> setMailboxLockMaxWaitingThreads(int zimbraMailboxLockMaxWaitingThreads, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxLockMaxWaitingThreads, Integer.toString(zimbraMailboxLockMaxWaitingThreads));
        return attrs;
    }

    /**
     * Maximum allowed waiting threads on a mailbox lock
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1732)
    public void unsetMailboxLockMaxWaitingThreads() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxLockMaxWaitingThreads, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum allowed waiting threads on a mailbox lock
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1732)
    public Map<String,Object> unsetMailboxLockMaxWaitingThreads(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxLockMaxWaitingThreads, "");
        return attrs;
    }

    /**
     * list of host:port for memcached servers; set to empty value to disable
     * the use of memcached
     *
     * @return zimbraMemcachedClientServerList, or empty array if unset
     *
     * @since ZCS 5.0.17
     */
    @ZAttr(id=1014)
    public String[] getMemcachedClientServerList() {
        return getMultiAttr(Provisioning.A_zimbraMemcachedClientServerList);
    }

    /**
     * list of host:port for memcached servers; set to empty value to disable
     * the use of memcached
     *
     * @param zimbraMemcachedClientServerList new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.17
     */
    @ZAttr(id=1014)
    public void setMemcachedClientServerList(String[] zimbraMemcachedClientServerList) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMemcachedClientServerList, zimbraMemcachedClientServerList);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * list of host:port for memcached servers; set to empty value to disable
     * the use of memcached
     *
     * @param zimbraMemcachedClientServerList new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.17
     */
    @ZAttr(id=1014)
    public Map<String,Object> setMemcachedClientServerList(String[] zimbraMemcachedClientServerList, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMemcachedClientServerList, zimbraMemcachedClientServerList);
        return attrs;
    }

    /**
     * list of host:port for memcached servers; set to empty value to disable
     * the use of memcached
     *
     * @param zimbraMemcachedClientServerList new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.17
     */
    @ZAttr(id=1014)
    public void addMemcachedClientServerList(String zimbraMemcachedClientServerList) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMemcachedClientServerList, zimbraMemcachedClientServerList);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * list of host:port for memcached servers; set to empty value to disable
     * the use of memcached
     *
     * @param zimbraMemcachedClientServerList new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.17
     */
    @ZAttr(id=1014)
    public Map<String,Object> addMemcachedClientServerList(String zimbraMemcachedClientServerList, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMemcachedClientServerList, zimbraMemcachedClientServerList);
        return attrs;
    }

    /**
     * list of host:port for memcached servers; set to empty value to disable
     * the use of memcached
     *
     * @param zimbraMemcachedClientServerList existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.17
     */
    @ZAttr(id=1014)
    public void removeMemcachedClientServerList(String zimbraMemcachedClientServerList) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMemcachedClientServerList, zimbraMemcachedClientServerList);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * list of host:port for memcached servers; set to empty value to disable
     * the use of memcached
     *
     * @param zimbraMemcachedClientServerList existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.17
     */
    @ZAttr(id=1014)
    public Map<String,Object> removeMemcachedClientServerList(String zimbraMemcachedClientServerList, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMemcachedClientServerList, zimbraMemcachedClientServerList);
        return attrs;
    }

    /**
     * list of host:port for memcached servers; set to empty value to disable
     * the use of memcached
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 5.0.17
     */
    @ZAttr(id=1014)
    public void unsetMemcachedClientServerList() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMemcachedClientServerList, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * list of host:port for memcached servers; set to empty value to disable
     * the use of memcached
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.17
     */
    @ZAttr(id=1014)
    public Map<String,Object> unsetMemcachedClientServerList(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMemcachedClientServerList, "");
        return attrs;
    }

    /**
     * Enable debug for pop3
     *
     * @return zimbraPop3EnableDebug, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1727)
    public boolean isPop3EnableDebug() {
        return getBooleanAttr(Provisioning.A_zimbraPop3EnableDebug, false);
    }

    /**
     * Enable debug for pop3
     *
     * @param zimbraPop3EnableDebug new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1727)
    public void setPop3EnableDebug(boolean zimbraPop3EnableDebug) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3EnableDebug, zimbraPop3EnableDebug ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enable debug for pop3
     *
     * @param zimbraPop3EnableDebug new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1727)
    public Map<String,Object> setPop3EnableDebug(boolean zimbraPop3EnableDebug, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3EnableDebug, zimbraPop3EnableDebug ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Enable debug for pop3
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1727)
    public void unsetPop3EnableDebug() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3EnableDebug, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enable debug for pop3
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1727)
    public Map<String,Object> unsetPop3EnableDebug(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3EnableDebug, "");
        return attrs;
    }

    /**
     * Enable STARTTLS for POP3
     *
     * @return zimbraPop3EnableStartTls, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1725)
    public boolean isPop3EnableStartTls() {
        return getBooleanAttr(Provisioning.A_zimbraPop3EnableStartTls, false);
    }

    /**
     * Enable STARTTLS for POP3
     *
     * @param zimbraPop3EnableStartTls new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1725)
    public void setPop3EnableStartTls(boolean zimbraPop3EnableStartTls) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3EnableStartTls, zimbraPop3EnableStartTls ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enable STARTTLS for POP3
     *
     * @param zimbraPop3EnableStartTls new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1725)
    public Map<String,Object> setPop3EnableStartTls(boolean zimbraPop3EnableStartTls, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3EnableStartTls, zimbraPop3EnableStartTls ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Enable STARTTLS for POP3
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1725)
    public void unsetPop3EnableStartTls() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3EnableStartTls, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enable STARTTLS for POP3
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1725)
    public Map<String,Object> unsetPop3EnableStartTls(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3EnableStartTls, "");
        return attrs;
    }

    /**
     * Pop3 timeout in seconds
     *
     * @return zimbraPop3Timeout, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1726)
    public int getPop3Timeout() {
        return getIntAttr(Provisioning.A_zimbraPop3Timeout, -1);
    }

    /**
     * Pop3 timeout in seconds
     *
     * @param zimbraPop3Timeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1726)
    public void setPop3Timeout(int zimbraPop3Timeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3Timeout, Integer.toString(zimbraPop3Timeout));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Pop3 timeout in seconds
     *
     * @param zimbraPop3Timeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1726)
    public Map<String,Object> setPop3Timeout(int zimbraPop3Timeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3Timeout, Integer.toString(zimbraPop3Timeout));
        return attrs;
    }

    /**
     * Pop3 timeout in seconds
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1726)
    public void unsetPop3Timeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3Timeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Pop3 timeout in seconds
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1726)
    public Map<String,Object> unsetPop3Timeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3Timeout, "");
        return attrs;
    }

    /**
     * Maximum pending notifications allowed
     *
     * @return zimbraSessionMaxPendingNotifications, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1740)
    public int getSessionMaxPendingNotifications() {
        return getIntAttr(Provisioning.A_zimbraSessionMaxPendingNotifications, -1);
    }

    /**
     * Maximum pending notifications allowed
     *
     * @param zimbraSessionMaxPendingNotifications new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1740)
    public void setSessionMaxPendingNotifications(int zimbraSessionMaxPendingNotifications) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSessionMaxPendingNotifications, Integer.toString(zimbraSessionMaxPendingNotifications));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum pending notifications allowed
     *
     * @param zimbraSessionMaxPendingNotifications new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1740)
    public Map<String,Object> setSessionMaxPendingNotifications(int zimbraSessionMaxPendingNotifications, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSessionMaxPendingNotifications, Integer.toString(zimbraSessionMaxPendingNotifications));
        return attrs;
    }

    /**
     * Maximum pending notifications allowed
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1740)
    public void unsetSessionMaxPendingNotifications() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSessionMaxPendingNotifications, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum pending notifications allowed
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1740)
    public Map<String,Object> unsetSessionMaxPendingNotifications(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSessionMaxPendingNotifications, "");
        return attrs;
    }

    /**
     * Enable smtp debug
     *
     * @return zimbraSmtpEnableDebug, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1724)
    public boolean isSmtpEnableDebug() {
        return getBooleanAttr(Provisioning.A_zimbraSmtpEnableDebug, false);
    }

    /**
     * Enable smtp debug
     *
     * @param zimbraSmtpEnableDebug new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1724)
    public void setSmtpEnableDebug(boolean zimbraSmtpEnableDebug) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpEnableDebug, zimbraSmtpEnableDebug ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enable smtp debug
     *
     * @param zimbraSmtpEnableDebug new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1724)
    public Map<String,Object> setSmtpEnableDebug(boolean zimbraSmtpEnableDebug, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpEnableDebug, zimbraSmtpEnableDebug ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Enable smtp debug
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1724)
    public void unsetSmtpEnableDebug() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpEnableDebug, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enable smtp debug
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1724)
    public Map<String,Object> unsetSmtpEnableDebug(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpEnableDebug, "");
        return attrs;
    }

    /**
     * Enable STARTTLS for smtp
     *
     * @return zimbraSmtpEnableStartTls, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1743)
    public boolean isSmtpEnableStartTls() {
        return getBooleanAttr(Provisioning.A_zimbraSmtpEnableStartTls, false);
    }

    /**
     * Enable STARTTLS for smtp
     *
     * @param zimbraSmtpEnableStartTls new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1743)
    public void setSmtpEnableStartTls(boolean zimbraSmtpEnableStartTls) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpEnableStartTls, zimbraSmtpEnableStartTls ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enable STARTTLS for smtp
     *
     * @param zimbraSmtpEnableStartTls new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1743)
    public Map<String,Object> setSmtpEnableStartTls(boolean zimbraSmtpEnableStartTls, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpEnableStartTls, zimbraSmtpEnableStartTls ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Enable STARTTLS for smtp
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1743)
    public void unsetSmtpEnableStartTls() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpEnableStartTls, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enable STARTTLS for smtp
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1743)
    public Map<String,Object> unsetSmtpEnableStartTls(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpEnableStartTls, "");
        return attrs;
    }

    /**
     * Flag to enable zimbra client for SMTP
     *
     * @return zimbraSmtpUseZimbraClient, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1735)
    public boolean isSmtpUseZimbraClient() {
        return getBooleanAttr(Provisioning.A_zimbraSmtpUseZimbraClient, false);
    }

    /**
     * Flag to enable zimbra client for SMTP
     *
     * @param zimbraSmtpUseZimbraClient new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1735)
    public void setSmtpUseZimbraClient(boolean zimbraSmtpUseZimbraClient) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpUseZimbraClient, zimbraSmtpUseZimbraClient ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable zimbra client for SMTP
     *
     * @param zimbraSmtpUseZimbraClient new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1735)
    public Map<String,Object> setSmtpUseZimbraClient(boolean zimbraSmtpUseZimbraClient, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpUseZimbraClient, zimbraSmtpUseZimbraClient ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Flag to enable zimbra client for SMTP
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1735)
    public void unsetSmtpUseZimbraClient() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpUseZimbraClient, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable zimbra client for SMTP
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1735)
    public Map<String,Object> unsetSmtpUseZimbraClient(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSmtpUseZimbraClient, "");
        return attrs;
    }

    /**
     * Maximum number of active soap sessions per account. Carefully increase
     * this.value to avoid &quot;Too many SOAP sessions” errors in
     * mailbox.log.
     *
     * @return zimbraSoapSessionLimit, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1738)
    public int getSoapSessionLimit() {
        return getIntAttr(Provisioning.A_zimbraSoapSessionLimit, -1);
    }

    /**
     * Maximum number of active soap sessions per account. Carefully increase
     * this.value to avoid &quot;Too many SOAP sessions” errors in
     * mailbox.log.
     *
     * @param zimbraSoapSessionLimit new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1738)
    public void setSoapSessionLimit(int zimbraSoapSessionLimit) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSoapSessionLimit, Integer.toString(zimbraSoapSessionLimit));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of active soap sessions per account. Carefully increase
     * this.value to avoid &quot;Too many SOAP sessions” errors in
     * mailbox.log.
     *
     * @param zimbraSoapSessionLimit new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1738)
    public Map<String,Object> setSoapSessionLimit(int zimbraSoapSessionLimit, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSoapSessionLimit, Integer.toString(zimbraSoapSessionLimit));
        return attrs;
    }

    /**
     * Maximum number of active soap sessions per account. Carefully increase
     * this.value to avoid &quot;Too many SOAP sessions” errors in
     * mailbox.log.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1738)
    public void unsetSoapSessionLimit() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSoapSessionLimit, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of active soap sessions per account. Carefully increase
     * this.value to avoid &quot;Too many SOAP sessions” errors in
     * mailbox.log.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1738)
    public Map<String,Object> unsetSoapSessionLimit(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSoapSessionLimit, "");
        return attrs;
    }

    /**
     * Idle timeout in seconds for SOAP sessions
     *
     * @return zimbraSoapSessionTimeout, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1741)
    public int getSoapSessionTimeout() {
        return getIntAttr(Provisioning.A_zimbraSoapSessionTimeout, -1);
    }

    /**
     * Idle timeout in seconds for SOAP sessions
     *
     * @param zimbraSoapSessionTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1741)
    public void setSoapSessionTimeout(int zimbraSoapSessionTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSoapSessionTimeout, Integer.toString(zimbraSoapSessionTimeout));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Idle timeout in seconds for SOAP sessions
     *
     * @param zimbraSoapSessionTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1741)
    public Map<String,Object> setSoapSessionTimeout(int zimbraSoapSessionTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSoapSessionTimeout, Integer.toString(zimbraSoapSessionTimeout));
        return attrs;
    }

    /**
     * Idle timeout in seconds for SOAP sessions
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1741)
    public void unsetSoapSessionTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSoapSessionTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Idle timeout in seconds for SOAP sessions
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1741)
    public Map<String,Object> unsetSoapSessionTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSoapSessionTimeout, "");
        return attrs;
    }

    /**
     * Maximum number of files to keep in ExternalStoreManager&#039;s local
     * file cache
     *
     * @return zimbraStoreExternalLocalCacheMaxFiles, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1753)
    public int getStoreExternalLocalCacheMaxFiles() {
        return getIntAttr(Provisioning.A_zimbraStoreExternalLocalCacheMaxFiles, -1);
    }

    /**
     * Maximum number of files to keep in ExternalStoreManager&#039;s local
     * file cache
     *
     * @param zimbraStoreExternalLocalCacheMaxFiles new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1753)
    public void setStoreExternalLocalCacheMaxFiles(int zimbraStoreExternalLocalCacheMaxFiles) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStoreExternalLocalCacheMaxFiles, Integer.toString(zimbraStoreExternalLocalCacheMaxFiles));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of files to keep in ExternalStoreManager&#039;s local
     * file cache
     *
     * @param zimbraStoreExternalLocalCacheMaxFiles new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1753)
    public Map<String,Object> setStoreExternalLocalCacheMaxFiles(int zimbraStoreExternalLocalCacheMaxFiles, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStoreExternalLocalCacheMaxFiles, Integer.toString(zimbraStoreExternalLocalCacheMaxFiles));
        return attrs;
    }

    /**
     * Maximum number of files to keep in ExternalStoreManager&#039;s local
     * file cache
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1753)
    public void unsetStoreExternalLocalCacheMaxFiles() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStoreExternalLocalCacheMaxFiles, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of files to keep in ExternalStoreManager&#039;s local
     * file cache
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1753)
    public Map<String,Object> unsetStoreExternalLocalCacheMaxFiles(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStoreExternalLocalCacheMaxFiles, "");
        return attrs;
    }

    /**
     * Maximum number of bytes to keep in ExternalStoreManager&#039;s local
     * file cache. Default value is 1GB
     *
     * @return zimbraStoreExternalLocalCacheMaxSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1752)
    public int getStoreExternalLocalCacheMaxSize() {
        return getIntAttr(Provisioning.A_zimbraStoreExternalLocalCacheMaxSize, -1);
    }

    /**
     * Maximum number of bytes to keep in ExternalStoreManager&#039;s local
     * file cache. Default value is 1GB
     *
     * @param zimbraStoreExternalLocalCacheMaxSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1752)
    public void setStoreExternalLocalCacheMaxSize(int zimbraStoreExternalLocalCacheMaxSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStoreExternalLocalCacheMaxSize, Integer.toString(zimbraStoreExternalLocalCacheMaxSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of bytes to keep in ExternalStoreManager&#039;s local
     * file cache. Default value is 1GB
     *
     * @param zimbraStoreExternalLocalCacheMaxSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1752)
    public Map<String,Object> setStoreExternalLocalCacheMaxSize(int zimbraStoreExternalLocalCacheMaxSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStoreExternalLocalCacheMaxSize, Integer.toString(zimbraStoreExternalLocalCacheMaxSize));
        return attrs;
    }

    /**
     * Maximum number of bytes to keep in ExternalStoreManager&#039;s local
     * file cache. Default value is 1GB
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1752)
    public void unsetStoreExternalLocalCacheMaxSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStoreExternalLocalCacheMaxSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of bytes to keep in ExternalStoreManager&#039;s local
     * file cache. Default value is 1GB
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1752)
    public Map<String,Object> unsetStoreExternalLocalCacheMaxSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStoreExternalLocalCacheMaxSize, "");
        return attrs;
    }

    /**
     * Minimum time in millis to keep idle entries in
     * ExternalStoreManager&#039;s local file cache
     *
     * @return zimbraStoreExternalLocalCacheMinLifetime, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1754)
    public int getStoreExternalLocalCacheMinLifetime() {
        return getIntAttr(Provisioning.A_zimbraStoreExternalLocalCacheMinLifetime, -1);
    }

    /**
     * Minimum time in millis to keep idle entries in
     * ExternalStoreManager&#039;s local file cache
     *
     * @param zimbraStoreExternalLocalCacheMinLifetime new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1754)
    public void setStoreExternalLocalCacheMinLifetime(int zimbraStoreExternalLocalCacheMinLifetime) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStoreExternalLocalCacheMinLifetime, Integer.toString(zimbraStoreExternalLocalCacheMinLifetime));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Minimum time in millis to keep idle entries in
     * ExternalStoreManager&#039;s local file cache
     *
     * @param zimbraStoreExternalLocalCacheMinLifetime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1754)
    public Map<String,Object> setStoreExternalLocalCacheMinLifetime(int zimbraStoreExternalLocalCacheMinLifetime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStoreExternalLocalCacheMinLifetime, Integer.toString(zimbraStoreExternalLocalCacheMinLifetime));
        return attrs;
    }

    /**
     * Minimum time in millis to keep idle entries in
     * ExternalStoreManager&#039;s local file cache
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1754)
    public void unsetStoreExternalLocalCacheMinLifetime() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStoreExternalLocalCacheMinLifetime, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Minimum time in millis to keep idle entries in
     * ExternalStoreManager&#039;s local file cache
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1754)
    public Map<String,Object> unsetStoreExternalLocalCacheMinLifetime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStoreExternalLocalCacheMinLifetime, "");
        return attrs;
    }

    /**
     * Maximum number of consecutive IOExceptions before aborting during
     * mailbox deletion
     *
     * @return zimbraStoreExternalMaxIOExceptionsForDelete, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1751)
    public int getStoreExternalMaxIOExceptionsForDelete() {
        return getIntAttr(Provisioning.A_zimbraStoreExternalMaxIOExceptionsForDelete, -1);
    }

    /**
     * Maximum number of consecutive IOExceptions before aborting during
     * mailbox deletion
     *
     * @param zimbraStoreExternalMaxIOExceptionsForDelete new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1751)
    public void setStoreExternalMaxIOExceptionsForDelete(int zimbraStoreExternalMaxIOExceptionsForDelete) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStoreExternalMaxIOExceptionsForDelete, Integer.toString(zimbraStoreExternalMaxIOExceptionsForDelete));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of consecutive IOExceptions before aborting during
     * mailbox deletion
     *
     * @param zimbraStoreExternalMaxIOExceptionsForDelete new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1751)
    public Map<String,Object> setStoreExternalMaxIOExceptionsForDelete(int zimbraStoreExternalMaxIOExceptionsForDelete, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStoreExternalMaxIOExceptionsForDelete, Integer.toString(zimbraStoreExternalMaxIOExceptionsForDelete));
        return attrs;
    }

    /**
     * Maximum number of consecutive IOExceptions before aborting during
     * mailbox deletion
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1751)
    public void unsetStoreExternalMaxIOExceptionsForDelete() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStoreExternalMaxIOExceptionsForDelete, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of consecutive IOExceptions before aborting during
     * mailbox deletion
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1751)
    public Map<String,Object> unsetStoreExternalMaxIOExceptionsForDelete(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraStoreExternalMaxIOExceptionsForDelete, "");
        return attrs;
    }

    /**
     * Maximum number of per session sync listeners for ActiveSync. Carefully
     * increase this value to avoid &quot;Too many SYNCLISTENER
     * sessions&quot; errors in sync.log.
     *
     * @return zimbraSyncSessionLimit, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1739)
    public int getSyncSessionLimit() {
        return getIntAttr(Provisioning.A_zimbraSyncSessionLimit, -1);
    }

    /**
     * Maximum number of per session sync listeners for ActiveSync. Carefully
     * increase this value to avoid &quot;Too many SYNCLISTENER
     * sessions&quot; errors in sync.log.
     *
     * @param zimbraSyncSessionLimit new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1739)
    public void setSyncSessionLimit(int zimbraSyncSessionLimit) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSyncSessionLimit, Integer.toString(zimbraSyncSessionLimit));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of per session sync listeners for ActiveSync. Carefully
     * increase this value to avoid &quot;Too many SYNCLISTENER
     * sessions&quot; errors in sync.log.
     *
     * @param zimbraSyncSessionLimit new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1739)
    public Map<String,Object> setSyncSessionLimit(int zimbraSyncSessionLimit, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSyncSessionLimit, Integer.toString(zimbraSyncSessionLimit));
        return attrs;
    }

    /**
     * Maximum number of per session sync listeners for ActiveSync. Carefully
     * increase this value to avoid &quot;Too many SYNCLISTENER
     * sessions&quot; errors in sync.log.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1739)
    public void unsetSyncSessionLimit() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSyncSessionLimit, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of per session sync listeners for ActiveSync. Carefully
     * increase this value to avoid &quot;Too many SYNCLISTENER
     * sessions&quot; errors in sync.log.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1739)
    public Map<String,Object> unsetSyncSessionLimit(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSyncSessionLimit, "");
        return attrs;
    }

    /**
     * Deprecated since: 9.0.0_BETA1. Multi-server AlwaysOn clusters no
     * longer require ZooKeeper.. Orig desc: list of host:port for ZooKeeper
     * servers; set to empty value to disable the use of ZooKeeper.
     *
     * @return zimbraZookeeperClientServerList, or empty array if unset
     *
     * @since ZCS 8.5.0
     */
    @ZAttr(id=1447)
    public String[] getZookeeperClientServerList() {
        return getMultiAttr(Provisioning.A_zimbraZookeeperClientServerList);
    }

    /**
     * Deprecated since: 9.0.0_BETA1. Multi-server AlwaysOn clusters no
     * longer require ZooKeeper.. Orig desc: list of host:port for ZooKeeper
     * servers; set to empty value to disable the use of ZooKeeper.
     *
     * @param zimbraZookeeperClientServerList new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.5.0
     */
    @ZAttr(id=1447)
    public void setZookeeperClientServerList(String[] zimbraZookeeperClientServerList) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZookeeperClientServerList, zimbraZookeeperClientServerList);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 9.0.0_BETA1. Multi-server AlwaysOn clusters no
     * longer require ZooKeeper.. Orig desc: list of host:port for ZooKeeper
     * servers; set to empty value to disable the use of ZooKeeper.
     *
     * @param zimbraZookeeperClientServerList new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.5.0
     */
    @ZAttr(id=1447)
    public Map<String,Object> setZookeeperClientServerList(String[] zimbraZookeeperClientServerList, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZookeeperClientServerList, zimbraZookeeperClientServerList);
        return attrs;
    }

    /**
     * Deprecated since: 9.0.0_BETA1. Multi-server AlwaysOn clusters no
     * longer require ZooKeeper.. Orig desc: list of host:port for ZooKeeper
     * servers; set to empty value to disable the use of ZooKeeper.
     *
     * @param zimbraZookeeperClientServerList new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.5.0
     */
    @ZAttr(id=1447)
    public void addZookeeperClientServerList(String zimbraZookeeperClientServerList) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraZookeeperClientServerList, zimbraZookeeperClientServerList);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 9.0.0_BETA1. Multi-server AlwaysOn clusters no
     * longer require ZooKeeper.. Orig desc: list of host:port for ZooKeeper
     * servers; set to empty value to disable the use of ZooKeeper.
     *
     * @param zimbraZookeeperClientServerList new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.5.0
     */
    @ZAttr(id=1447)
    public Map<String,Object> addZookeeperClientServerList(String zimbraZookeeperClientServerList, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraZookeeperClientServerList, zimbraZookeeperClientServerList);
        return attrs;
    }

    /**
     * Deprecated since: 9.0.0_BETA1. Multi-server AlwaysOn clusters no
     * longer require ZooKeeper.. Orig desc: list of host:port for ZooKeeper
     * servers; set to empty value to disable the use of ZooKeeper.
     *
     * @param zimbraZookeeperClientServerList existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.5.0
     */
    @ZAttr(id=1447)
    public void removeZookeeperClientServerList(String zimbraZookeeperClientServerList) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraZookeeperClientServerList, zimbraZookeeperClientServerList);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 9.0.0_BETA1. Multi-server AlwaysOn clusters no
     * longer require ZooKeeper.. Orig desc: list of host:port for ZooKeeper
     * servers; set to empty value to disable the use of ZooKeeper.
     *
     * @param zimbraZookeeperClientServerList existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.5.0
     */
    @ZAttr(id=1447)
    public Map<String,Object> removeZookeeperClientServerList(String zimbraZookeeperClientServerList, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraZookeeperClientServerList, zimbraZookeeperClientServerList);
        return attrs;
    }

    /**
     * Deprecated since: 9.0.0_BETA1. Multi-server AlwaysOn clusters no
     * longer require ZooKeeper.. Orig desc: list of host:port for ZooKeeper
     * servers; set to empty value to disable the use of ZooKeeper.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.5.0
     */
    @ZAttr(id=1447)
    public void unsetZookeeperClientServerList() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZookeeperClientServerList, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Deprecated since: 9.0.0_BETA1. Multi-server AlwaysOn clusters no
     * longer require ZooKeeper.. Orig desc: list of host:port for ZooKeeper
     * servers; set to empty value to disable the use of ZooKeeper.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.5.0
     */
    @ZAttr(id=1447)
    public Map<String,Object> unsetZookeeperClientServerList(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZookeeperClientServerList, "");
        return attrs;
    }

    ///// END-AUTO-GEN-REPLACE
}
