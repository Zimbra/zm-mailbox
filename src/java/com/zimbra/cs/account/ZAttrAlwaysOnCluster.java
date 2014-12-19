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
     * imap_max_consecutive_error valueis reached.
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
     * imap_max_consecutive_error valueis reached.
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
     * imap_max_consecutive_error valueis reached.
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
     * imap_max_consecutive_error valueis reached.
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
     * imap_max_consecutive_error valueis reached.
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
     * flag to enable or disable to reuse imap data source connection
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
     * flag to enable or disable to reuse imap data source connection
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
     * flag to enable or disable to reuse imap data source connection
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
     * flag to enable or disable to reuse imap data source connection
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
     * flag to enable or disable to reuse imap data source connection
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
     * flag to enable or disable imap cache
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
     * flag to enable or disable imap cache
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
     * flag to enable or disable imap cache
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
     * flag to enable or disable imap cache
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
     * flag to enable or disable imap cache
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
     * Imap write chunk size
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
     * Imap write chunk size
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
     * Imap write chunk size
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
     * Imap write chunk size
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
     * Imap write chunk size
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
