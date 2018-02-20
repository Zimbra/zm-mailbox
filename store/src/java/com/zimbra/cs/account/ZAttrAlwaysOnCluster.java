/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import static com.zimbra.common.account.ProvisioningConstants.FALSE;
import static com.zimbra.common.account.ProvisioningConstants.TRUE;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.account.ZAttr;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.ldap.LdapDateUtil;


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
        return getAttr(Provisioning.A_cn, null, true);
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
        return getMultiAttr(Provisioning.A_description, true, true);
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
        return getMultiAttr(Provisioning.A_zimbraACE, true, true);
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
     * Ehcache: default expiration time for activesync cache values; default
     * is 5 minutes. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * <p>Use getActiveSyncEhcacheExpirationAsString to access value as a string.
     *
     * @see #getActiveSyncEhcacheExpirationAsString()
     *
     * @return zimbraActiveSyncEhcacheExpiration in millseconds, or -1 if unset
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3003)
    public long getActiveSyncEhcacheExpiration() {
        return getTimeInterval(Provisioning.A_zimbraActiveSyncEhcacheExpiration, -1L, true);
    }

    /**
     * Ehcache: default expiration time for activesync cache values; default
     * is 5 minutes. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * @return zimbraActiveSyncEhcacheExpiration, or null if unset
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3003)
    public String getActiveSyncEhcacheExpirationAsString() {
        return getAttr(Provisioning.A_zimbraActiveSyncEhcacheExpiration, null, true);
    }

    /**
     * Ehcache: default expiration time for activesync cache values; default
     * is 5 minutes. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * @param zimbraActiveSyncEhcacheExpiration new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3003)
    public void setActiveSyncEhcacheExpiration(String zimbraActiveSyncEhcacheExpiration) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncEhcacheExpiration, zimbraActiveSyncEhcacheExpiration);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Ehcache: default expiration time for activesync cache values; default
     * is 5 minutes. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * @param zimbraActiveSyncEhcacheExpiration new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3003)
    public Map<String,Object> setActiveSyncEhcacheExpiration(String zimbraActiveSyncEhcacheExpiration, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncEhcacheExpiration, zimbraActiveSyncEhcacheExpiration);
        return attrs;
    }

    /**
     * Ehcache: default expiration time for activesync cache values; default
     * is 5 minutes. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3003)
    public void unsetActiveSyncEhcacheExpiration() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncEhcacheExpiration, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Ehcache: default expiration time for activesync cache values; default
     * is 5 minutes. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3003)
    public Map<String,Object> unsetActiveSyncEhcacheExpiration(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncEhcacheExpiration, "");
        return attrs;
    }

    /**
     * Ehcache: the maximum heap size of the ActiveSync cache in Bytes before
     * eviction. By default this value is 10MB. This is a rough limit,Due to
     * internals of ehcache actual size in memory will often exceed this
     * limit by a modest margin.
     *
     * @return zimbraActiveSyncEhcacheHeapSize, or -1 if unset
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3001)
    public long getActiveSyncEhcacheHeapSize() {
        return getLongAttr(Provisioning.A_zimbraActiveSyncEhcacheHeapSize, -1L, true);
    }

    /**
     * Ehcache: the maximum heap size of the ActiveSync cache in Bytes before
     * eviction. By default this value is 10MB. This is a rough limit,Due to
     * internals of ehcache actual size in memory will often exceed this
     * limit by a modest margin.
     *
     * @param zimbraActiveSyncEhcacheHeapSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3001)
    public void setActiveSyncEhcacheHeapSize(long zimbraActiveSyncEhcacheHeapSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncEhcacheHeapSize, Long.toString(zimbraActiveSyncEhcacheHeapSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Ehcache: the maximum heap size of the ActiveSync cache in Bytes before
     * eviction. By default this value is 10MB. This is a rough limit,Due to
     * internals of ehcache actual size in memory will often exceed this
     * limit by a modest margin.
     *
     * @param zimbraActiveSyncEhcacheHeapSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3001)
    public Map<String,Object> setActiveSyncEhcacheHeapSize(long zimbraActiveSyncEhcacheHeapSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncEhcacheHeapSize, Long.toString(zimbraActiveSyncEhcacheHeapSize));
        return attrs;
    }

    /**
     * Ehcache: the maximum heap size of the ActiveSync cache in Bytes before
     * eviction. By default this value is 10MB. This is a rough limit,Due to
     * internals of ehcache actual size in memory will often exceed this
     * limit by a modest margin.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3001)
    public void unsetActiveSyncEhcacheHeapSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncEhcacheHeapSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Ehcache: the maximum heap size of the ActiveSync cache in Bytes before
     * eviction. By default this value is 10MB. This is a rough limit,Due to
     * internals of ehcache actual size in memory will often exceed this
     * limit by a modest margin.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3001)
    public Map<String,Object> unsetActiveSyncEhcacheHeapSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncEhcacheHeapSize, "");
        return attrs;
    }

    /**
     * Ehcache: the maximum disk size of the ActiveSync cache in Bytes before
     * eviction. By default this value is 10GB. This is a rough limit,Due to
     * internals of ehcache actual size on disk will often exceed this limit
     * by a modest margin.
     *
     * @return zimbraActiveSyncEhcacheMaxDiskSize, or -1 if unset
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3002)
    public long getActiveSyncEhcacheMaxDiskSize() {
        return getLongAttr(Provisioning.A_zimbraActiveSyncEhcacheMaxDiskSize, -1L, true);
    }

    /**
     * Ehcache: the maximum disk size of the ActiveSync cache in Bytes before
     * eviction. By default this value is 10GB. This is a rough limit,Due to
     * internals of ehcache actual size on disk will often exceed this limit
     * by a modest margin.
     *
     * @param zimbraActiveSyncEhcacheMaxDiskSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3002)
    public void setActiveSyncEhcacheMaxDiskSize(long zimbraActiveSyncEhcacheMaxDiskSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncEhcacheMaxDiskSize, Long.toString(zimbraActiveSyncEhcacheMaxDiskSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Ehcache: the maximum disk size of the ActiveSync cache in Bytes before
     * eviction. By default this value is 10GB. This is a rough limit,Due to
     * internals of ehcache actual size on disk will often exceed this limit
     * by a modest margin.
     *
     * @param zimbraActiveSyncEhcacheMaxDiskSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3002)
    public Map<String,Object> setActiveSyncEhcacheMaxDiskSize(long zimbraActiveSyncEhcacheMaxDiskSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncEhcacheMaxDiskSize, Long.toString(zimbraActiveSyncEhcacheMaxDiskSize));
        return attrs;
    }

    /**
     * Ehcache: the maximum disk size of the ActiveSync cache in Bytes before
     * eviction. By default this value is 10GB. This is a rough limit,Due to
     * internals of ehcache actual size on disk will often exceed this limit
     * by a modest margin.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3002)
    public void unsetActiveSyncEhcacheMaxDiskSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncEhcacheMaxDiskSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Ehcache: the maximum disk size of the ActiveSync cache in Bytes before
     * eviction. By default this value is 10GB. This is a rough limit,Due to
     * internals of ehcache actual size on disk will often exceed this limit
     * by a modest margin.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3002)
    public Map<String,Object> unsetActiveSyncEhcacheMaxDiskSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncEhcacheMaxDiskSize, "");
        return attrs;
    }

    /**
     * Timeout in milliseconds before retrying failed convert URLs.
     *
     * @return zimbraConvertPoolTimeout, or -1 if unset
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=2059)
    public int getConvertPoolTimeout() {
        return getIntAttr(Provisioning.A_zimbraConvertPoolTimeout, -1, true);
    }

    /**
     * Timeout in milliseconds before retrying failed convert URLs.
     *
     * @param zimbraConvertPoolTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=2059)
    public void setConvertPoolTimeout(int zimbraConvertPoolTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraConvertPoolTimeout, Integer.toString(zimbraConvertPoolTimeout));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Timeout in milliseconds before retrying failed convert URLs.
     *
     * @param zimbraConvertPoolTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=2059)
    public Map<String,Object> setConvertPoolTimeout(int zimbraConvertPoolTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraConvertPoolTimeout, Integer.toString(zimbraConvertPoolTimeout));
        return attrs;
    }

    /**
     * Timeout in milliseconds before retrying failed convert URLs.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=2059)
    public void unsetConvertPoolTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraConvertPoolTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Timeout in milliseconds before retrying failed convert URLs.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=2059)
    public Map<String,Object> unsetConvertPoolTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraConvertPoolTimeout, "");
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
        return getGeneralizedTimeAttr(Provisioning.A_zimbraCreateTimestamp, null, true);
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
        return getAttr(Provisioning.A_zimbraCreateTimestamp, null, true);
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
        attrs.put(Provisioning.A_zimbraCreateTimestamp, zimbraCreateTimestamp==null ? "" : LdapDateUtil.toGeneralizedTime(zimbraCreateTimestamp));
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
        attrs.put(Provisioning.A_zimbraCreateTimestamp, zimbraCreateTimestamp==null ? "" : LdapDateUtil.toGeneralizedTime(zimbraCreateTimestamp));
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
     * URL of the event storage backend to be queried for event analytics
     *
     * @return zimbraEventBackendURL, or null if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3045)
    public String getEventBackendURL() {
        return getAttr(Provisioning.A_zimbraEventBackendURL, null, true);
    }

    /**
     * URL of the event storage backend to be queried for event analytics
     *
     * @param zimbraEventBackendURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3045)
    public void setEventBackendURL(String zimbraEventBackendURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventBackendURL, zimbraEventBackendURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * URL of the event storage backend to be queried for event analytics
     *
     * @param zimbraEventBackendURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3045)
    public Map<String,Object> setEventBackendURL(String zimbraEventBackendURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventBackendURL, zimbraEventBackendURL);
        return attrs;
    }

    /**
     * URL of the event storage backend to be queried for event analytics
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3045)
    public void unsetEventBackendURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventBackendURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * URL of the event storage backend to be queried for event analytics
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3045)
    public Map<String,Object> unsetEventBackendURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventBackendURL, "");
        return attrs;
    }

    /**
     * If an event batch remains unmodified for this duration of time, it
     * will be flushed regardless of its size. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * <p>Use getEventBatchLifetimeAsString to access value as a string.
     *
     * @see #getEventBatchLifetimeAsString()
     *
     * @return zimbraEventBatchLifetime in millseconds, or -1 if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3042)
    public long getEventBatchLifetime() {
        return getTimeInterval(Provisioning.A_zimbraEventBatchLifetime, -1L, true);
    }

    /**
     * If an event batch remains unmodified for this duration of time, it
     * will be flushed regardless of its size. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @return zimbraEventBatchLifetime, or null if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3042)
    public String getEventBatchLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraEventBatchLifetime, null, true);
    }

    /**
     * If an event batch remains unmodified for this duration of time, it
     * will be flushed regardless of its size. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @param zimbraEventBatchLifetime new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3042)
    public void setEventBatchLifetime(String zimbraEventBatchLifetime) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventBatchLifetime, zimbraEventBatchLifetime);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * If an event batch remains unmodified for this duration of time, it
     * will be flushed regardless of its size. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @param zimbraEventBatchLifetime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3042)
    public Map<String,Object> setEventBatchLifetime(String zimbraEventBatchLifetime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventBatchLifetime, zimbraEventBatchLifetime);
        return attrs;
    }

    /**
     * If an event batch remains unmodified for this duration of time, it
     * will be flushed regardless of its size. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3042)
    public void unsetEventBatchLifetime() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventBatchLifetime, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * If an event batch remains unmodified for this duration of time, it
     * will be flushed regardless of its size. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3042)
    public Map<String,Object> unsetEventBatchLifetime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventBatchLifetime, "");
        return attrs;
    }

    /**
     * Event batch size used by batching EventLogHandlers such as
     * &quot;solrcloud&quot;. When this limit is reached, the batch is
     * flushed.
     *
     * @return zimbraEventBatchMaxSize, or -1 if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3041)
    public int getEventBatchMaxSize() {
        return getIntAttr(Provisioning.A_zimbraEventBatchMaxSize, -1, true);
    }

    /**
     * Event batch size used by batching EventLogHandlers such as
     * &quot;solrcloud&quot;. When this limit is reached, the batch is
     * flushed.
     *
     * @param zimbraEventBatchMaxSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3041)
    public void setEventBatchMaxSize(int zimbraEventBatchMaxSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventBatchMaxSize, Integer.toString(zimbraEventBatchMaxSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Event batch size used by batching EventLogHandlers such as
     * &quot;solrcloud&quot;. When this limit is reached, the batch is
     * flushed.
     *
     * @param zimbraEventBatchMaxSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3041)
    public Map<String,Object> setEventBatchMaxSize(int zimbraEventBatchMaxSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventBatchMaxSize, Integer.toString(zimbraEventBatchMaxSize));
        return attrs;
    }

    /**
     * Event batch size used by batching EventLogHandlers such as
     * &quot;solrcloud&quot;. When this limit is reached, the batch is
     * flushed.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3041)
    public void unsetEventBatchMaxSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventBatchMaxSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Event batch size used by batching EventLogHandlers such as
     * &quot;solrcloud&quot;. When this limit is reached, the batch is
     * flushed.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3041)
    public Map<String,Object> unsetEventBatchMaxSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventBatchMaxSize, "");
        return attrs;
    }

    /**
     * Event logging backends to be used for storing events. The value before
     * the first colon identifies the implementation Factory; everything
     * after is configuration. Current options are: 1) File backend that logs
     * events in a log file. It is recommended to keep this enabled. 2)
     * SolrCloud backend. Format is &quot;solrcloud:[ZooKeeper URL]&quot;
     *
     * @return zimbraEventLoggingBackends, or empty array if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3038)
    public String[] getEventLoggingBackends() {
        return getMultiAttr(Provisioning.A_zimbraEventLoggingBackends, true, true);
    }

    /**
     * Event logging backends to be used for storing events. The value before
     * the first colon identifies the implementation Factory; everything
     * after is configuration. Current options are: 1) File backend that logs
     * events in a log file. It is recommended to keep this enabled. 2)
     * SolrCloud backend. Format is &quot;solrcloud:[ZooKeeper URL]&quot;
     *
     * @param zimbraEventLoggingBackends new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3038)
    public void setEventLoggingBackends(String[] zimbraEventLoggingBackends) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventLoggingBackends, zimbraEventLoggingBackends);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Event logging backends to be used for storing events. The value before
     * the first colon identifies the implementation Factory; everything
     * after is configuration. Current options are: 1) File backend that logs
     * events in a log file. It is recommended to keep this enabled. 2)
     * SolrCloud backend. Format is &quot;solrcloud:[ZooKeeper URL]&quot;
     *
     * @param zimbraEventLoggingBackends new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3038)
    public Map<String,Object> setEventLoggingBackends(String[] zimbraEventLoggingBackends, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventLoggingBackends, zimbraEventLoggingBackends);
        return attrs;
    }

    /**
     * Event logging backends to be used for storing events. The value before
     * the first colon identifies the implementation Factory; everything
     * after is configuration. Current options are: 1) File backend that logs
     * events in a log file. It is recommended to keep this enabled. 2)
     * SolrCloud backend. Format is &quot;solrcloud:[ZooKeeper URL]&quot;
     *
     * @param zimbraEventLoggingBackends new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3038)
    public void addEventLoggingBackends(String zimbraEventLoggingBackends) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraEventLoggingBackends, zimbraEventLoggingBackends);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Event logging backends to be used for storing events. The value before
     * the first colon identifies the implementation Factory; everything
     * after is configuration. Current options are: 1) File backend that logs
     * events in a log file. It is recommended to keep this enabled. 2)
     * SolrCloud backend. Format is &quot;solrcloud:[ZooKeeper URL]&quot;
     *
     * @param zimbraEventLoggingBackends new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3038)
    public Map<String,Object> addEventLoggingBackends(String zimbraEventLoggingBackends, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraEventLoggingBackends, zimbraEventLoggingBackends);
        return attrs;
    }

    /**
     * Event logging backends to be used for storing events. The value before
     * the first colon identifies the implementation Factory; everything
     * after is configuration. Current options are: 1) File backend that logs
     * events in a log file. It is recommended to keep this enabled. 2)
     * SolrCloud backend. Format is &quot;solrcloud:[ZooKeeper URL]&quot;
     *
     * @param zimbraEventLoggingBackends existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3038)
    public void removeEventLoggingBackends(String zimbraEventLoggingBackends) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraEventLoggingBackends, zimbraEventLoggingBackends);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Event logging backends to be used for storing events. The value before
     * the first colon identifies the implementation Factory; everything
     * after is configuration. Current options are: 1) File backend that logs
     * events in a log file. It is recommended to keep this enabled. 2)
     * SolrCloud backend. Format is &quot;solrcloud:[ZooKeeper URL]&quot;
     *
     * @param zimbraEventLoggingBackends existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3038)
    public Map<String,Object> removeEventLoggingBackends(String zimbraEventLoggingBackends, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraEventLoggingBackends, zimbraEventLoggingBackends);
        return attrs;
    }

    /**
     * Event logging backends to be used for storing events. The value before
     * the first colon identifies the implementation Factory; everything
     * after is configuration. Current options are: 1) File backend that logs
     * events in a log file. It is recommended to keep this enabled. 2)
     * SolrCloud backend. Format is &quot;solrcloud:[ZooKeeper URL]&quot;
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3038)
    public void unsetEventLoggingBackends() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventLoggingBackends, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Event logging backends to be used for storing events. The value before
     * the first colon identifies the implementation Factory; everything
     * after is configuration. Current options are: 1) File backend that logs
     * events in a log file. It is recommended to keep this enabled. 2)
     * SolrCloud backend. Format is &quot;solrcloud:[ZooKeeper URL]&quot;
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3038)
    public Map<String,Object> unsetEventLoggingBackends(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventLoggingBackends, "");
        return attrs;
    }

    /**
     * Whether event logging is enabled
     *
     * @return zimbraEventLoggingEnabled, or false if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3044)
    public boolean isEventLoggingEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraEventLoggingEnabled, false, true);
    }

    /**
     * Whether event logging is enabled
     *
     * @param zimbraEventLoggingEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3044)
    public void setEventLoggingEnabled(boolean zimbraEventLoggingEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventLoggingEnabled, zimbraEventLoggingEnabled ? TRUE : FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether event logging is enabled
     *
     * @param zimbraEventLoggingEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3044)
    public Map<String,Object> setEventLoggingEnabled(boolean zimbraEventLoggingEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventLoggingEnabled, zimbraEventLoggingEnabled ? TRUE : FALSE);
        return attrs;
    }

    /**
     * Whether event logging is enabled
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3044)
    public void unsetEventLoggingEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventLoggingEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether event logging is enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3044)
    public Map<String,Object> unsetEventLoggingEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventLoggingEnabled, "");
        return attrs;
    }

    /**
     * Number of consumer threads used to process the event logging queue
     *
     * @return zimbraEventLoggingNumThreads, or -1 if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3039)
    public int getEventLoggingNumThreads() {
        return getIntAttr(Provisioning.A_zimbraEventLoggingNumThreads, -1, true);
    }

    /**
     * Number of consumer threads used to process the event logging queue
     *
     * @param zimbraEventLoggingNumThreads new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3039)
    public void setEventLoggingNumThreads(int zimbraEventLoggingNumThreads) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventLoggingNumThreads, Integer.toString(zimbraEventLoggingNumThreads));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Number of consumer threads used to process the event logging queue
     *
     * @param zimbraEventLoggingNumThreads new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3039)
    public Map<String,Object> setEventLoggingNumThreads(int zimbraEventLoggingNumThreads, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventLoggingNumThreads, Integer.toString(zimbraEventLoggingNumThreads));
        return attrs;
    }

    /**
     * Number of consumer threads used to process the event logging queue
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3039)
    public void unsetEventLoggingNumThreads() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventLoggingNumThreads, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Number of consumer threads used to process the event logging queue
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3039)
    public Map<String,Object> unsetEventLoggingNumThreads(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEventLoggingNumThreads, "");
        return attrs;
    }

    /**
     * HTTPs port on which zimbra extension server should listen
     *
     * <p>Use getExtensionBindPortAsString to access value as a string.
     *
     * @see #getExtensionBindPortAsString()
     *
     * @return zimbraExtensionBindPort, or -1 if unset
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=1980)
    public int getExtensionBindPort() {
        return getIntAttr(Provisioning.A_zimbraExtensionBindPort, -1, true);
    }

    /**
     * HTTPs port on which zimbra extension server should listen
     *
     * @return zimbraExtensionBindPort, or null if unset
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=1980)
    public String getExtensionBindPortAsString() {
        return getAttr(Provisioning.A_zimbraExtensionBindPort, null, true);
    }

    /**
     * HTTPs port on which zimbra extension server should listen
     *
     * @param zimbraExtensionBindPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=1980)
    public void setExtensionBindPort(int zimbraExtensionBindPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExtensionBindPort, Integer.toString(zimbraExtensionBindPort));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * HTTPs port on which zimbra extension server should listen
     *
     * @param zimbraExtensionBindPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=1980)
    public Map<String,Object> setExtensionBindPort(int zimbraExtensionBindPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExtensionBindPort, Integer.toString(zimbraExtensionBindPort));
        return attrs;
    }

    /**
     * HTTPs port on which zimbra extension server should listen
     *
     * @param zimbraExtensionBindPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=1980)
    public void setExtensionBindPortAsString(String zimbraExtensionBindPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExtensionBindPort, zimbraExtensionBindPort);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * HTTPs port on which zimbra extension server should listen
     *
     * @param zimbraExtensionBindPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=1980)
    public Map<String,Object> setExtensionBindPortAsString(String zimbraExtensionBindPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExtensionBindPort, zimbraExtensionBindPort);
        return attrs;
    }

    /**
     * HTTPs port on which zimbra extension server should listen
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=1980)
    public void unsetExtensionBindPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExtensionBindPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * HTTPs port on which zimbra extension server should listen
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=1980)
    public Map<String,Object> unsetExtensionBindPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExtensionBindPort, "");
        return attrs;
    }

    /**
     * Zimbra Systems Unique ID
     *
     * @return zimbraId, or null if unset
     */
    @ZAttr(id=1)
    public String getId() {
        return getAttr(Provisioning.A_zimbraId, null, true);
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
     * Ehcache: the maximum amount of disk space the imap active session
     * cache will consume in Bytes before eviction. By default this value is
     * 100 gigabytes. This is a rough limit,Due to internals of ehcache
     * actual size in memory will often exceed this limit by a modest margin.
     *
     * @return zimbraImapActiveSessionEhcacheMaxDiskSize, or -1 if unset
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3005)
    public long getImapActiveSessionEhcacheMaxDiskSize() {
        return getLongAttr(Provisioning.A_zimbraImapActiveSessionEhcacheMaxDiskSize, -1L, true);
    }

    /**
     * Ehcache: the maximum amount of disk space the imap active session
     * cache will consume in Bytes before eviction. By default this value is
     * 100 gigabytes. This is a rough limit,Due to internals of ehcache
     * actual size in memory will often exceed this limit by a modest margin.
     *
     * @param zimbraImapActiveSessionEhcacheMaxDiskSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3005)
    public void setImapActiveSessionEhcacheMaxDiskSize(long zimbraImapActiveSessionEhcacheMaxDiskSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapActiveSessionEhcacheMaxDiskSize, Long.toString(zimbraImapActiveSessionEhcacheMaxDiskSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Ehcache: the maximum amount of disk space the imap active session
     * cache will consume in Bytes before eviction. By default this value is
     * 100 gigabytes. This is a rough limit,Due to internals of ehcache
     * actual size in memory will often exceed this limit by a modest margin.
     *
     * @param zimbraImapActiveSessionEhcacheMaxDiskSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3005)
    public Map<String,Object> setImapActiveSessionEhcacheMaxDiskSize(long zimbraImapActiveSessionEhcacheMaxDiskSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapActiveSessionEhcacheMaxDiskSize, Long.toString(zimbraImapActiveSessionEhcacheMaxDiskSize));
        return attrs;
    }

    /**
     * Ehcache: the maximum amount of disk space the imap active session
     * cache will consume in Bytes before eviction. By default this value is
     * 100 gigabytes. This is a rough limit,Due to internals of ehcache
     * actual size in memory will often exceed this limit by a modest margin.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3005)
    public void unsetImapActiveSessionEhcacheMaxDiskSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapActiveSessionEhcacheMaxDiskSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Ehcache: the maximum amount of disk space the imap active session
     * cache will consume in Bytes before eviction. By default this value is
     * 100 gigabytes. This is a rough limit,Due to internals of ehcache
     * actual size in memory will often exceed this limit by a modest margin.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3005)
    public Map<String,Object> unsetImapActiveSessionEhcacheMaxDiskSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapActiveSessionEhcacheMaxDiskSize, "");
        return attrs;
    }

    /**
     * Ehcache: the maximum disk size of inactive IMAP cache in Bytes before
     * eviction.By default this value is 10GB.This is a rough limit,Due to
     * internals of ehcache actual size on disk will often exceed this limit
     * by a modest margin.
     *
     * @return zimbraImapInactiveSessionCacheMaxDiskSize, or -1 if unset
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=2013)
    public long getImapInactiveSessionCacheMaxDiskSize() {
        return getLongAttr(Provisioning.A_zimbraImapInactiveSessionCacheMaxDiskSize, -1L, true);
    }

    /**
     * Ehcache: the maximum disk size of inactive IMAP cache in Bytes before
     * eviction.By default this value is 10GB.This is a rough limit,Due to
     * internals of ehcache actual size on disk will often exceed this limit
     * by a modest margin.
     *
     * @param zimbraImapInactiveSessionCacheMaxDiskSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=2013)
    public void setImapInactiveSessionCacheMaxDiskSize(long zimbraImapInactiveSessionCacheMaxDiskSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapInactiveSessionCacheMaxDiskSize, Long.toString(zimbraImapInactiveSessionCacheMaxDiskSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Ehcache: the maximum disk size of inactive IMAP cache in Bytes before
     * eviction.By default this value is 10GB.This is a rough limit,Due to
     * internals of ehcache actual size on disk will often exceed this limit
     * by a modest margin.
     *
     * @param zimbraImapInactiveSessionCacheMaxDiskSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=2013)
    public Map<String,Object> setImapInactiveSessionCacheMaxDiskSize(long zimbraImapInactiveSessionCacheMaxDiskSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapInactiveSessionCacheMaxDiskSize, Long.toString(zimbraImapInactiveSessionCacheMaxDiskSize));
        return attrs;
    }

    /**
     * Ehcache: the maximum disk size of inactive IMAP cache in Bytes before
     * eviction.By default this value is 10GB.This is a rough limit,Due to
     * internals of ehcache actual size on disk will often exceed this limit
     * by a modest margin.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=2013)
    public void unsetImapInactiveSessionCacheMaxDiskSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapInactiveSessionCacheMaxDiskSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Ehcache: the maximum disk size of inactive IMAP cache in Bytes before
     * eviction.By default this value is 10GB.This is a rough limit,Due to
     * internals of ehcache actual size on disk will often exceed this limit
     * by a modest margin.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=2013)
    public Map<String,Object> unsetImapInactiveSessionCacheMaxDiskSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapInactiveSessionCacheMaxDiskSize, "");
        return attrs;
    }

    /**
     * Ehcache: the maximum amount of disk space the imap inactive session
     * cache will consume in Bytes before eviction. By default this value is
     * 100 gigabytes. This is a rough limit,Due to internals of ehcache
     * actual size in memory will often exceed this limit by a modest margin.
     *
     * @return zimbraImapInactiveSessionEhcacheMaxDiskSize, or -1 if unset
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3006)
    public long getImapInactiveSessionEhcacheMaxDiskSize() {
        return getLongAttr(Provisioning.A_zimbraImapInactiveSessionEhcacheMaxDiskSize, -1L, true);
    }

    /**
     * Ehcache: the maximum amount of disk space the imap inactive session
     * cache will consume in Bytes before eviction. By default this value is
     * 100 gigabytes. This is a rough limit,Due to internals of ehcache
     * actual size in memory will often exceed this limit by a modest margin.
     *
     * @param zimbraImapInactiveSessionEhcacheMaxDiskSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3006)
    public void setImapInactiveSessionEhcacheMaxDiskSize(long zimbraImapInactiveSessionEhcacheMaxDiskSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapInactiveSessionEhcacheMaxDiskSize, Long.toString(zimbraImapInactiveSessionEhcacheMaxDiskSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Ehcache: the maximum amount of disk space the imap inactive session
     * cache will consume in Bytes before eviction. By default this value is
     * 100 gigabytes. This is a rough limit,Due to internals of ehcache
     * actual size in memory will often exceed this limit by a modest margin.
     *
     * @param zimbraImapInactiveSessionEhcacheMaxDiskSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3006)
    public Map<String,Object> setImapInactiveSessionEhcacheMaxDiskSize(long zimbraImapInactiveSessionEhcacheMaxDiskSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapInactiveSessionEhcacheMaxDiskSize, Long.toString(zimbraImapInactiveSessionEhcacheMaxDiskSize));
        return attrs;
    }

    /**
     * Ehcache: the maximum amount of disk space the imap inactive session
     * cache will consume in Bytes before eviction. By default this value is
     * 100 gigabytes. This is a rough limit,Due to internals of ehcache
     * actual size in memory will often exceed this limit by a modest margin.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3006)
    public void unsetImapInactiveSessionEhcacheMaxDiskSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapInactiveSessionEhcacheMaxDiskSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Ehcache: the maximum amount of disk space the imap inactive session
     * cache will consume in Bytes before eviction. By default this value is
     * 100 gigabytes. This is a rough limit,Due to internals of ehcache
     * actual size in memory will often exceed this limit by a modest margin.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3006)
    public Map<String,Object> unsetImapInactiveSessionEhcacheMaxDiskSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapInactiveSessionEhcacheMaxDiskSize, "");
        return attrs;
    }

    /**
     * Ehcache: the maximum heap size of the inactive session cache in Bytes
     * before eviction. By default this value is 1 megabyte. This is a rough
     * limit,Due to internals of ehcache actual size in memory will often
     * exceed this limit by a modest margin.
     *
     * @return zimbraImapInactiveSessionEhcacheSize, or -1 if unset
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3004)
    public long getImapInactiveSessionEhcacheSize() {
        return getLongAttr(Provisioning.A_zimbraImapInactiveSessionEhcacheSize, -1L, true);
    }

    /**
     * Ehcache: the maximum heap size of the inactive session cache in Bytes
     * before eviction. By default this value is 1 megabyte. This is a rough
     * limit,Due to internals of ehcache actual size in memory will often
     * exceed this limit by a modest margin.
     *
     * @param zimbraImapInactiveSessionEhcacheSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3004)
    public void setImapInactiveSessionEhcacheSize(long zimbraImapInactiveSessionEhcacheSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapInactiveSessionEhcacheSize, Long.toString(zimbraImapInactiveSessionEhcacheSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Ehcache: the maximum heap size of the inactive session cache in Bytes
     * before eviction. By default this value is 1 megabyte. This is a rough
     * limit,Due to internals of ehcache actual size in memory will often
     * exceed this limit by a modest margin.
     *
     * @param zimbraImapInactiveSessionEhcacheSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3004)
    public Map<String,Object> setImapInactiveSessionEhcacheSize(long zimbraImapInactiveSessionEhcacheSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapInactiveSessionEhcacheSize, Long.toString(zimbraImapInactiveSessionEhcacheSize));
        return attrs;
    }

    /**
     * Ehcache: the maximum heap size of the inactive session cache in Bytes
     * before eviction. By default this value is 1 megabyte. This is a rough
     * limit,Due to internals of ehcache actual size in memory will often
     * exceed this limit by a modest margin.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3004)
    public void unsetImapInactiveSessionEhcacheSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapInactiveSessionEhcacheSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Ehcache: the maximum heap size of the inactive session cache in Bytes
     * before eviction. By default this value is 1 megabyte. This is a rough
     * limit,Due to internals of ehcache actual size in memory will often
     * exceed this limit by a modest margin.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.6
     */
    @ZAttr(id=3004)
    public Map<String,Object> unsetImapInactiveSessionEhcacheSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapInactiveSessionEhcacheSize, "");
        return attrs;
    }

    /**
     * Time that the mailstore will wait between attempts to request status
     * from an external indexing service (Solr or SolrCloud).. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * <p>Use getIndexPollingIntervalAsString to access value as a string.
     *
     * @see #getIndexPollingIntervalAsString()
     *
     * @return zimbraIndexPollingInterval in millseconds, or -1 if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3027)
    public long getIndexPollingInterval() {
        return getTimeInterval(Provisioning.A_zimbraIndexPollingInterval, -1L, true);
    }

    /**
     * Time that the mailstore will wait between attempts to request status
     * from an external indexing service (Solr or SolrCloud).. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @return zimbraIndexPollingInterval, or null if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3027)
    public String getIndexPollingIntervalAsString() {
        return getAttr(Provisioning.A_zimbraIndexPollingInterval, null, true);
    }

    /**
     * Time that the mailstore will wait between attempts to request status
     * from an external indexing service (Solr or SolrCloud).. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraIndexPollingInterval new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3027)
    public void setIndexPollingInterval(String zimbraIndexPollingInterval) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexPollingInterval, zimbraIndexPollingInterval);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Time that the mailstore will wait between attempts to request status
     * from an external indexing service (Solr or SolrCloud).. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraIndexPollingInterval new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3027)
    public Map<String,Object> setIndexPollingInterval(String zimbraIndexPollingInterval, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexPollingInterval, zimbraIndexPollingInterval);
        return attrs;
    }

    /**
     * Time that the mailstore will wait between attempts to request status
     * from an external indexing service (Solr or SolrCloud).. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3027)
    public void unsetIndexPollingInterval() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexPollingInterval, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Time that the mailstore will wait between attempts to request status
     * from an external indexing service (Solr or SolrCloud).. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3027)
    public Map<String,Object> unsetIndexPollingInterval(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexPollingInterval, "");
        return attrs;
    }

    /**
     * Maximum number of threads for re-indexing. Re-index threads are not
     * pooled.
     *
     * @return zimbraIndexReIndexThreads, or -1 if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3032)
    public int getIndexReIndexThreads() {
        return getIntAttr(Provisioning.A_zimbraIndexReIndexThreads, -1, true);
    }

    /**
     * Maximum number of threads for re-indexing. Re-index threads are not
     * pooled.
     *
     * @param zimbraIndexReIndexThreads new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3032)
    public void setIndexReIndexThreads(int zimbraIndexReIndexThreads) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReIndexThreads, Integer.toString(zimbraIndexReIndexThreads));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of threads for re-indexing. Re-index threads are not
     * pooled.
     *
     * @param zimbraIndexReIndexThreads new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3032)
    public Map<String,Object> setIndexReIndexThreads(int zimbraIndexReIndexThreads, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReIndexThreads, Integer.toString(zimbraIndexReIndexThreads));
        return attrs;
    }

    /**
     * Maximum number of threads for re-indexing. Re-index threads are not
     * pooled.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3032)
    public void unsetIndexReIndexThreads() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReIndexThreads, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of threads for re-indexing. Re-index threads are not
     * pooled.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3032)
    public Map<String,Object> unsetIndexReIndexThreads(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReIndexThreads, "");
        return attrs;
    }

    /**
     * Time that the mailstore will wait for an external indexing service to
     * fully replicate an index for a single mailbox. When using SolrCloud,
     * this is the time that a mailstore will wait for all replicas of a SOLR
     * collection to go live and sync up to the leader.. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * <p>Use getIndexReplicationTimeoutAsString to access value as a string.
     *
     * @see #getIndexReplicationTimeoutAsString()
     *
     * @return zimbraIndexReplicationTimeout in millseconds, or -1 if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3026)
    public long getIndexReplicationTimeout() {
        return getTimeInterval(Provisioning.A_zimbraIndexReplicationTimeout, -1L, true);
    }

    /**
     * Time that the mailstore will wait for an external indexing service to
     * fully replicate an index for a single mailbox. When using SolrCloud,
     * this is the time that a mailstore will wait for all replicas of a SOLR
     * collection to go live and sync up to the leader.. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @return zimbraIndexReplicationTimeout, or null if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3026)
    public String getIndexReplicationTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraIndexReplicationTimeout, null, true);
    }

    /**
     * Time that the mailstore will wait for an external indexing service to
     * fully replicate an index for a single mailbox. When using SolrCloud,
     * this is the time that a mailstore will wait for all replicas of a SOLR
     * collection to go live and sync up to the leader.. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraIndexReplicationTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3026)
    public void setIndexReplicationTimeout(String zimbraIndexReplicationTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReplicationTimeout, zimbraIndexReplicationTimeout);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Time that the mailstore will wait for an external indexing service to
     * fully replicate an index for a single mailbox. When using SolrCloud,
     * this is the time that a mailstore will wait for all replicas of a SOLR
     * collection to go live and sync up to the leader.. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraIndexReplicationTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3026)
    public Map<String,Object> setIndexReplicationTimeout(String zimbraIndexReplicationTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReplicationTimeout, zimbraIndexReplicationTimeout);
        return attrs;
    }

    /**
     * Time that the mailstore will wait for an external indexing service to
     * fully replicate an index for a single mailbox. When using SolrCloud,
     * this is the time that a mailstore will wait for all replicas of a SOLR
     * collection to go live and sync up to the leader.. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3026)
    public void unsetIndexReplicationTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReplicationTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Time that the mailstore will wait for an external indexing service to
     * fully replicate an index for a single mailbox. When using SolrCloud,
     * this is the time that a mailstore will wait for all replicas of a SOLR
     * collection to go live and sync up to the leader.. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3026)
    public Map<String,Object> unsetIndexReplicationTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReplicationTimeout, "");
        return attrs;
    }

    /**
     * The number of terms SOLR will cache during term enumeration queries
     *
     * @return zimbraIndexTermsCacheSize, or -1 if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3024)
    public int getIndexTermsCacheSize() {
        return getIntAttr(Provisioning.A_zimbraIndexTermsCacheSize, -1, true);
    }

    /**
     * The number of terms SOLR will cache during term enumeration queries
     *
     * @param zimbraIndexTermsCacheSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3024)
    public void setIndexTermsCacheSize(int zimbraIndexTermsCacheSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexTermsCacheSize, Integer.toString(zimbraIndexTermsCacheSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The number of terms SOLR will cache during term enumeration queries
     *
     * @param zimbraIndexTermsCacheSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3024)
    public Map<String,Object> setIndexTermsCacheSize(int zimbraIndexTermsCacheSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexTermsCacheSize, Integer.toString(zimbraIndexTermsCacheSize));
        return attrs;
    }

    /**
     * The number of terms SOLR will cache during term enumeration queries
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3024)
    public void unsetIndexTermsCacheSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexTermsCacheSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The number of terms SOLR will cache during term enumeration queries
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3024)
    public Map<String,Object> unsetIndexTermsCacheSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexTermsCacheSize, "");
        return attrs;
    }

    /**
     * Number of threads used for indexing items from the queue
     *
     * @return zimbraIndexThreads, or -1 if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3031)
    public int getIndexThreads() {
        return getIntAttr(Provisioning.A_zimbraIndexThreads, -1, true);
    }

    /**
     * Number of threads used for indexing items from the queue
     *
     * @param zimbraIndexThreads new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3031)
    public void setIndexThreads(int zimbraIndexThreads) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexThreads, Integer.toString(zimbraIndexThreads));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Number of threads used for indexing items from the queue
     *
     * @param zimbraIndexThreads new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3031)
    public Map<String,Object> setIndexThreads(int zimbraIndexThreads, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexThreads, Integer.toString(zimbraIndexThreads));
        return attrs;
    }

    /**
     * Number of threads used for indexing items from the queue
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3031)
    public void unsetIndexThreads() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexThreads, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Number of threads used for indexing items from the queue
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3031)
    public Map<String,Object> unsetIndexThreads(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexThreads, "");
        return attrs;
    }

    /**
     * URL for accessing external indexing service. First part of the URL
     * before the first colon identifies the implementation Factory and
     * should be registered by calling IndexStore.registerFactory(prefix,
     * classname). ZCS ships with two index Factory implementations: 1)
     * SolrIndex identified by prefix &quot;solr&quot;. 2) SolrCloudIndex
     * identified by prefix &quot;solrcloud&quot;. If using solrcloud, the
     * URL should be set to that of the ZooKeeper host managing the SOLR
     * cluster.
     *
     * @return zimbraIndexURL, or null if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3023)
    public String getIndexURL() {
        return getAttr(Provisioning.A_zimbraIndexURL, null, true);
    }

    /**
     * URL for accessing external indexing service. First part of the URL
     * before the first colon identifies the implementation Factory and
     * should be registered by calling IndexStore.registerFactory(prefix,
     * classname). ZCS ships with two index Factory implementations: 1)
     * SolrIndex identified by prefix &quot;solr&quot;. 2) SolrCloudIndex
     * identified by prefix &quot;solrcloud&quot;. If using solrcloud, the
     * URL should be set to that of the ZooKeeper host managing the SOLR
     * cluster.
     *
     * @param zimbraIndexURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3023)
    public void setIndexURL(String zimbraIndexURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexURL, zimbraIndexURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * URL for accessing external indexing service. First part of the URL
     * before the first colon identifies the implementation Factory and
     * should be registered by calling IndexStore.registerFactory(prefix,
     * classname). ZCS ships with two index Factory implementations: 1)
     * SolrIndex identified by prefix &quot;solr&quot;. 2) SolrCloudIndex
     * identified by prefix &quot;solrcloud&quot;. If using solrcloud, the
     * URL should be set to that of the ZooKeeper host managing the SOLR
     * cluster.
     *
     * @param zimbraIndexURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3023)
    public Map<String,Object> setIndexURL(String zimbraIndexURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexURL, zimbraIndexURL);
        return attrs;
    }

    /**
     * URL for accessing external indexing service. First part of the URL
     * before the first colon identifies the implementation Factory and
     * should be registered by calling IndexStore.registerFactory(prefix,
     * classname). ZCS ships with two index Factory implementations: 1)
     * SolrIndex identified by prefix &quot;solr&quot;. 2) SolrCloudIndex
     * identified by prefix &quot;solrcloud&quot;. If using solrcloud, the
     * URL should be set to that of the ZooKeeper host managing the SOLR
     * cluster.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3023)
    public void unsetIndexURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * URL for accessing external indexing service. First part of the URL
     * before the first colon identifies the implementation Factory and
     * should be registered by calling IndexStore.registerFactory(prefix,
     * classname). ZCS ships with two index Factory implementations: 1)
     * SolrIndex identified by prefix &quot;solr&quot;. 2) SolrCloudIndex
     * identified by prefix &quot;solrcloud&quot;. If using solrcloud, the
     * URL should be set to that of the ZooKeeper host managing the SOLR
     * cluster.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3023)
    public Map<String,Object> unsetIndexURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexURL, "");
        return attrs;
    }

    /**
     * The maximum number of items that can be held in memory while queued
     * for indexing
     *
     * @return zimbraIndexingQueueMaxSize, or -1 if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3030)
    public int getIndexingQueueMaxSize() {
        return getIntAttr(Provisioning.A_zimbraIndexingQueueMaxSize, -1, true);
    }

    /**
     * The maximum number of items that can be held in memory while queued
     * for indexing
     *
     * @param zimbraIndexingQueueMaxSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3030)
    public void setIndexingQueueMaxSize(int zimbraIndexingQueueMaxSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexingQueueMaxSize, Integer.toString(zimbraIndexingQueueMaxSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The maximum number of items that can be held in memory while queued
     * for indexing
     *
     * @param zimbraIndexingQueueMaxSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3030)
    public Map<String,Object> setIndexingQueueMaxSize(int zimbraIndexingQueueMaxSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexingQueueMaxSize, Integer.toString(zimbraIndexingQueueMaxSize));
        return attrs;
    }

    /**
     * The maximum number of items that can be held in memory while queued
     * for indexing
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3030)
    public void unsetIndexingQueueMaxSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexingQueueMaxSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The maximum number of items that can be held in memory while queued
     * for indexing
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3030)
    public Map<String,Object> unsetIndexingQueueMaxSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexingQueueMaxSize, "");
        return attrs;
    }

    /**
     * Time that mailstore will wait between attempts to take/put an indexing
     * task from/to a shared indexing queue.. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * <p>Use getIndexingQueuePollingIntervalAsString to access value as a string.
     *
     * @see #getIndexingQueuePollingIntervalAsString()
     *
     * @return zimbraIndexingQueuePollingInterval in millseconds, or -1 if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3033)
    public long getIndexingQueuePollingInterval() {
        return getTimeInterval(Provisioning.A_zimbraIndexingQueuePollingInterval, -1L, true);
    }

    /**
     * Time that mailstore will wait between attempts to take/put an indexing
     * task from/to a shared indexing queue.. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @return zimbraIndexingQueuePollingInterval, or null if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3033)
    public String getIndexingQueuePollingIntervalAsString() {
        return getAttr(Provisioning.A_zimbraIndexingQueuePollingInterval, null, true);
    }

    /**
     * Time that mailstore will wait between attempts to take/put an indexing
     * task from/to a shared indexing queue.. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @param zimbraIndexingQueuePollingInterval new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3033)
    public void setIndexingQueuePollingInterval(String zimbraIndexingQueuePollingInterval) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexingQueuePollingInterval, zimbraIndexingQueuePollingInterval);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Time that mailstore will wait between attempts to take/put an indexing
     * task from/to a shared indexing queue.. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @param zimbraIndexingQueuePollingInterval new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3033)
    public Map<String,Object> setIndexingQueuePollingInterval(String zimbraIndexingQueuePollingInterval, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexingQueuePollingInterval, zimbraIndexingQueuePollingInterval);
        return attrs;
    }

    /**
     * Time that mailstore will wait between attempts to take/put an indexing
     * task from/to a shared indexing queue.. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3033)
    public void unsetIndexingQueuePollingInterval() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexingQueuePollingInterval, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Time that mailstore will wait between attempts to take/put an indexing
     * task from/to a shared indexing queue.. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3033)
    public Map<String,Object> unsetIndexingQueuePollingInterval(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexingQueuePollingInterval, "");
        return attrs;
    }

    /**
     * Time that mailstore will wait for space to free up in the shared
     * indexing queue. Increase this value if you are reindexing several
     * large mailboxes simultaneously.. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * <p>Use getIndexingQueueTimeoutAsString to access value as a string.
     *
     * @see #getIndexingQueueTimeoutAsString()
     *
     * @return zimbraIndexingQueueTimeout in millseconds, or -1 if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3036)
    public long getIndexingQueueTimeout() {
        return getTimeInterval(Provisioning.A_zimbraIndexingQueueTimeout, -1L, true);
    }

    /**
     * Time that mailstore will wait for space to free up in the shared
     * indexing queue. Increase this value if you are reindexing several
     * large mailboxes simultaneously.. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @return zimbraIndexingQueueTimeout, or null if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3036)
    public String getIndexingQueueTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraIndexingQueueTimeout, null, true);
    }

    /**
     * Time that mailstore will wait for space to free up in the shared
     * indexing queue. Increase this value if you are reindexing several
     * large mailboxes simultaneously.. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param zimbraIndexingQueueTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3036)
    public void setIndexingQueueTimeout(String zimbraIndexingQueueTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexingQueueTimeout, zimbraIndexingQueueTimeout);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Time that mailstore will wait for space to free up in the shared
     * indexing queue. Increase this value if you are reindexing several
     * large mailboxes simultaneously.. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param zimbraIndexingQueueTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3036)
    public Map<String,Object> setIndexingQueueTimeout(String zimbraIndexingQueueTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexingQueueTimeout, zimbraIndexingQueueTimeout);
        return attrs;
    }

    /**
     * Time that mailstore will wait for space to free up in the shared
     * indexing queue. Increase this value if you are reindexing several
     * large mailboxes simultaneously.. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3036)
    public void unsetIndexingQueueTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexingQueueTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Time that mailstore will wait for space to free up in the shared
     * indexing queue. Increase this value if you are reindexing several
     * large mailboxes simultaneously.. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3036)
    public Map<String,Object> unsetIndexingQueueTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexingQueueTimeout, "");
        return attrs;
    }

    /**
     * Whether to include fractional seconds in LDAP gentime values (e.g.
     * zimbraPasswordLockoutFailureTime or
     * zimbraGalLastSuccessfulSyncTimestamp). Releases prior to 8.7 are
     * unable to parse gentime values which include fractional seconds;
     * therefore this value must remain set to FALSE in environments where
     * any release 8.6 or lower is present. It should be changed to TRUE once
     * all systems are upgraded to 8.7 or higher.
     *
     * @return zimbraLdapGentimeFractionalSecondsEnabled, or false if unset
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=2018)
    public boolean isLdapGentimeFractionalSecondsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraLdapGentimeFractionalSecondsEnabled, false, true);
    }

    /**
     * Whether to include fractional seconds in LDAP gentime values (e.g.
     * zimbraPasswordLockoutFailureTime or
     * zimbraGalLastSuccessfulSyncTimestamp). Releases prior to 8.7 are
     * unable to parse gentime values which include fractional seconds;
     * therefore this value must remain set to FALSE in environments where
     * any release 8.6 or lower is present. It should be changed to TRUE once
     * all systems are upgraded to 8.7 or higher.
     *
     * @param zimbraLdapGentimeFractionalSecondsEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=2018)
    public void setLdapGentimeFractionalSecondsEnabled(boolean zimbraLdapGentimeFractionalSecondsEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLdapGentimeFractionalSecondsEnabled, zimbraLdapGentimeFractionalSecondsEnabled ? TRUE : FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to include fractional seconds in LDAP gentime values (e.g.
     * zimbraPasswordLockoutFailureTime or
     * zimbraGalLastSuccessfulSyncTimestamp). Releases prior to 8.7 are
     * unable to parse gentime values which include fractional seconds;
     * therefore this value must remain set to FALSE in environments where
     * any release 8.6 or lower is present. It should be changed to TRUE once
     * all systems are upgraded to 8.7 or higher.
     *
     * @param zimbraLdapGentimeFractionalSecondsEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=2018)
    public Map<String,Object> setLdapGentimeFractionalSecondsEnabled(boolean zimbraLdapGentimeFractionalSecondsEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLdapGentimeFractionalSecondsEnabled, zimbraLdapGentimeFractionalSecondsEnabled ? TRUE : FALSE);
        return attrs;
    }

    /**
     * Whether to include fractional seconds in LDAP gentime values (e.g.
     * zimbraPasswordLockoutFailureTime or
     * zimbraGalLastSuccessfulSyncTimestamp). Releases prior to 8.7 are
     * unable to parse gentime values which include fractional seconds;
     * therefore this value must remain set to FALSE in environments where
     * any release 8.6 or lower is present. It should be changed to TRUE once
     * all systems are upgraded to 8.7 or higher.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=2018)
    public void unsetLdapGentimeFractionalSecondsEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLdapGentimeFractionalSecondsEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether to include fractional seconds in LDAP gentime values (e.g.
     * zimbraPasswordLockoutFailureTime or
     * zimbraGalLastSuccessfulSyncTimestamp). Releases prior to 8.7 are
     * unable to parse gentime values which include fractional seconds;
     * therefore this value must remain set to FALSE in environments where
     * any release 8.6 or lower is present. It should be changed to TRUE once
     * all systems are upgraded to 8.7 or higher.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=2018)
    public Map<String,Object> unsetLdapGentimeFractionalSecondsEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLdapGentimeFractionalSecondsEnabled, "");
        return attrs;
    }

    /**
     * Time in milliseconds between IMAP/POP/LMTP rate limiter stale entry
     * cleanup cycle.. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * <p>Use getMailboxThrottleReapIntervalAsString to access value as a string.
     *
     * @see #getMailboxThrottleReapIntervalAsString()
     *
     * @return zimbraMailboxThrottleReapInterval in millseconds, or -1 if unset
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=2053)
    public long getMailboxThrottleReapInterval() {
        return getTimeInterval(Provisioning.A_zimbraMailboxThrottleReapInterval, -1L, true);
    }

    /**
     * Time in milliseconds between IMAP/POP/LMTP rate limiter stale entry
     * cleanup cycle.. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * @return zimbraMailboxThrottleReapInterval, or null if unset
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=2053)
    public String getMailboxThrottleReapIntervalAsString() {
        return getAttr(Provisioning.A_zimbraMailboxThrottleReapInterval, null, true);
    }

    /**
     * Time in milliseconds between IMAP/POP/LMTP rate limiter stale entry
     * cleanup cycle.. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * @param zimbraMailboxThrottleReapInterval new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=2053)
    public void setMailboxThrottleReapInterval(String zimbraMailboxThrottleReapInterval) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxThrottleReapInterval, zimbraMailboxThrottleReapInterval);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Time in milliseconds between IMAP/POP/LMTP rate limiter stale entry
     * cleanup cycle.. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * @param zimbraMailboxThrottleReapInterval new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=2053)
    public Map<String,Object> setMailboxThrottleReapInterval(String zimbraMailboxThrottleReapInterval, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxThrottleReapInterval, zimbraMailboxThrottleReapInterval);
        return attrs;
    }

    /**
     * Time in milliseconds between IMAP/POP/LMTP rate limiter stale entry
     * cleanup cycle.. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=2053)
    public void unsetMailboxThrottleReapInterval() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxThrottleReapInterval, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Time in milliseconds between IMAP/POP/LMTP rate limiter stale entry
     * cleanup cycle.. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=2053)
    public Map<String,Object> unsetMailboxThrottleReapInterval(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxThrottleReapInterval, "");
        return attrs;
    }

    /**
     * Maximum number of times a server will re-attempt to send a document to
     * an indexing service. When this value is higher than 0 and an attempt
     * to index a document fails, the server will put the document into the
     * indexing queue for additional indexing attempts.
     *
     * @return zimbraMaxIndexingRetries, or -1 if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3034)
    public int getMaxIndexingRetries() {
        return getIntAttr(Provisioning.A_zimbraMaxIndexingRetries, -1, true);
    }

    /**
     * Maximum number of times a server will re-attempt to send a document to
     * an indexing service. When this value is higher than 0 and an attempt
     * to index a document fails, the server will put the document into the
     * indexing queue for additional indexing attempts.
     *
     * @param zimbraMaxIndexingRetries new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3034)
    public void setMaxIndexingRetries(int zimbraMaxIndexingRetries) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMaxIndexingRetries, Integer.toString(zimbraMaxIndexingRetries));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of times a server will re-attempt to send a document to
     * an indexing service. When this value is higher than 0 and an attempt
     * to index a document fails, the server will put the document into the
     * indexing queue for additional indexing attempts.
     *
     * @param zimbraMaxIndexingRetries new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3034)
    public Map<String,Object> setMaxIndexingRetries(int zimbraMaxIndexingRetries, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMaxIndexingRetries, Integer.toString(zimbraMaxIndexingRetries));
        return attrs;
    }

    /**
     * Maximum number of times a server will re-attempt to send a document to
     * an indexing service. When this value is higher than 0 and an attempt
     * to index a document fails, the server will put the document into the
     * indexing queue for additional indexing attempts.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3034)
    public void unsetMaxIndexingRetries() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMaxIndexingRetries, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of times a server will re-attempt to send a document to
     * an indexing service. When this value is higher than 0 and an attempt
     * to index a document fails, the server will put the document into the
     * indexing queue for additional indexing attempts.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3034)
    public Map<String,Object> unsetMaxIndexingRetries(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMaxIndexingRetries, "");
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
        return getMultiAttr(Provisioning.A_zimbraMemcachedClientServerList, true, true);
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
     * Maximum number of items to put into a single reindexing task.
     *
     * @return zimbraReindexBatchSize, or -1 if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3035)
    public int getReindexBatchSize() {
        return getIntAttr(Provisioning.A_zimbraReindexBatchSize, -1, true);
    }

    /**
     * Maximum number of items to put into a single reindexing task.
     *
     * @param zimbraReindexBatchSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3035)
    public void setReindexBatchSize(int zimbraReindexBatchSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReindexBatchSize, Integer.toString(zimbraReindexBatchSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of items to put into a single reindexing task.
     *
     * @param zimbraReindexBatchSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3035)
    public Map<String,Object> setReindexBatchSize(int zimbraReindexBatchSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReindexBatchSize, Integer.toString(zimbraReindexBatchSize));
        return attrs;
    }

    /**
     * Maximum number of items to put into a single reindexing task.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3035)
    public void unsetReindexBatchSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReindexBatchSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of items to put into a single reindexing task.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3035)
    public Map<String,Object> unsetReindexBatchSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReindexBatchSize, "");
        return attrs;
    }

    /**
     * Enable the use of Server Name Indication (SNI) TLS extension in the
     * proxy. When set to TRUE, and using SNI capable clients, there is no
     * need for a separate IP address per domain (zimbraVirtualIPAddress)
     *
     * @return zimbraReverseProxySNIEnabled, or false if unset
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=1818)
    public boolean isReverseProxySNIEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraReverseProxySNIEnabled, false, true);
    }

    /**
     * Enable the use of Server Name Indication (SNI) TLS extension in the
     * proxy. When set to TRUE, and using SNI capable clients, there is no
     * need for a separate IP address per domain (zimbraVirtualIPAddress)
     *
     * @param zimbraReverseProxySNIEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=1818)
    public void setReverseProxySNIEnabled(boolean zimbraReverseProxySNIEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxySNIEnabled, zimbraReverseProxySNIEnabled ? TRUE : FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enable the use of Server Name Indication (SNI) TLS extension in the
     * proxy. When set to TRUE, and using SNI capable clients, there is no
     * need for a separate IP address per domain (zimbraVirtualIPAddress)
     *
     * @param zimbraReverseProxySNIEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=1818)
    public Map<String,Object> setReverseProxySNIEnabled(boolean zimbraReverseProxySNIEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxySNIEnabled, zimbraReverseProxySNIEnabled ? TRUE : FALSE);
        return attrs;
    }

    /**
     * Enable the use of Server Name Indication (SNI) TLS extension in the
     * proxy. When set to TRUE, and using SNI capable clients, there is no
     * need for a separate IP address per domain (zimbraVirtualIPAddress)
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=1818)
    public void unsetReverseProxySNIEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxySNIEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enable the use of Server Name Indication (SNI) TLS extension in the
     * proxy. When set to TRUE, and using SNI capable clients, there is no
     * need for a separate IP address per domain (zimbraVirtualIPAddress)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.0,9.0.0
     */
    @ZAttr(id=1818)
    public Map<String,Object> unsetReverseProxySNIEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReverseProxySNIEnabled, "");
        return attrs;
    }

    /**
     * Maximum time an entry in the short term All Effective Rights cache
     * will be regarded as valid. If value is 0, the cache is disabled. The
     * cache is particularly useful when significant use is made of delegated
     * administration. This cache can improve performance by avoiding
     * recomputing All Effective Rights of named entries like accounts
     * frequently in a short period of time. All Effective Rights are
     * computations of the rights that named entries like accounts have -
     * although when used, they are checked separately. The longer the value
     * of this setting is, the more stale the view of the details is likely
     * to be. For this reason, the maximum accepted value is 30m. Larger
     * values will be treated as being 30m . Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * <p>Use getShortTermAllEffectiveRightsCacheExpirationAsString to access value as a string.
     *
     * @see #getShortTermAllEffectiveRightsCacheExpirationAsString()
     *
     * @return zimbraShortTermAllEffectiveRightsCacheExpiration in millseconds, or -1 if unset
     *
     * @since ZCS 8.7.0
     */
    @ZAttr(id=1903)
    public long getShortTermAllEffectiveRightsCacheExpiration() {
        return getTimeInterval(Provisioning.A_zimbraShortTermAllEffectiveRightsCacheExpiration, -1L, true);
    }

    /**
     * Maximum time an entry in the short term All Effective Rights cache
     * will be regarded as valid. If value is 0, the cache is disabled. The
     * cache is particularly useful when significant use is made of delegated
     * administration. This cache can improve performance by avoiding
     * recomputing All Effective Rights of named entries like accounts
     * frequently in a short period of time. All Effective Rights are
     * computations of the rights that named entries like accounts have -
     * although when used, they are checked separately. The longer the value
     * of this setting is, the more stale the view of the details is likely
     * to be. For this reason, the maximum accepted value is 30m. Larger
     * values will be treated as being 30m . Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @return zimbraShortTermAllEffectiveRightsCacheExpiration, or null if unset
     *
     * @since ZCS 8.7.0
     */
    @ZAttr(id=1903)
    public String getShortTermAllEffectiveRightsCacheExpirationAsString() {
        return getAttr(Provisioning.A_zimbraShortTermAllEffectiveRightsCacheExpiration, null, true);
    }

    /**
     * Maximum time an entry in the short term All Effective Rights cache
     * will be regarded as valid. If value is 0, the cache is disabled. The
     * cache is particularly useful when significant use is made of delegated
     * administration. This cache can improve performance by avoiding
     * recomputing All Effective Rights of named entries like accounts
     * frequently in a short period of time. All Effective Rights are
     * computations of the rights that named entries like accounts have -
     * although when used, they are checked separately. The longer the value
     * of this setting is, the more stale the view of the details is likely
     * to be. For this reason, the maximum accepted value is 30m. Larger
     * values will be treated as being 30m . Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @param zimbraShortTermAllEffectiveRightsCacheExpiration new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.0
     */
    @ZAttr(id=1903)
    public void setShortTermAllEffectiveRightsCacheExpiration(String zimbraShortTermAllEffectiveRightsCacheExpiration) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraShortTermAllEffectiveRightsCacheExpiration, zimbraShortTermAllEffectiveRightsCacheExpiration);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum time an entry in the short term All Effective Rights cache
     * will be regarded as valid. If value is 0, the cache is disabled. The
     * cache is particularly useful when significant use is made of delegated
     * administration. This cache can improve performance by avoiding
     * recomputing All Effective Rights of named entries like accounts
     * frequently in a short period of time. All Effective Rights are
     * computations of the rights that named entries like accounts have -
     * although when used, they are checked separately. The longer the value
     * of this setting is, the more stale the view of the details is likely
     * to be. For this reason, the maximum accepted value is 30m. Larger
     * values will be treated as being 30m . Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @param zimbraShortTermAllEffectiveRightsCacheExpiration new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.0
     */
    @ZAttr(id=1903)
    public Map<String,Object> setShortTermAllEffectiveRightsCacheExpiration(String zimbraShortTermAllEffectiveRightsCacheExpiration, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraShortTermAllEffectiveRightsCacheExpiration, zimbraShortTermAllEffectiveRightsCacheExpiration);
        return attrs;
    }

    /**
     * Maximum time an entry in the short term All Effective Rights cache
     * will be regarded as valid. If value is 0, the cache is disabled. The
     * cache is particularly useful when significant use is made of delegated
     * administration. This cache can improve performance by avoiding
     * recomputing All Effective Rights of named entries like accounts
     * frequently in a short period of time. All Effective Rights are
     * computations of the rights that named entries like accounts have -
     * although when used, they are checked separately. The longer the value
     * of this setting is, the more stale the view of the details is likely
     * to be. For this reason, the maximum accepted value is 30m. Larger
     * values will be treated as being 30m . Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.0
     */
    @ZAttr(id=1903)
    public void unsetShortTermAllEffectiveRightsCacheExpiration() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraShortTermAllEffectiveRightsCacheExpiration, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum time an entry in the short term All Effective Rights cache
     * will be regarded as valid. If value is 0, the cache is disabled. The
     * cache is particularly useful when significant use is made of delegated
     * administration. This cache can improve performance by avoiding
     * recomputing All Effective Rights of named entries like accounts
     * frequently in a short period of time. All Effective Rights are
     * computations of the rights that named entries like accounts have -
     * although when used, they are checked separately. The longer the value
     * of this setting is, the more stale the view of the details is likely
     * to be. For this reason, the maximum accepted value is 30m. Larger
     * values will be treated as being 30m . Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.0
     */
    @ZAttr(id=1903)
    public Map<String,Object> unsetShortTermAllEffectiveRightsCacheExpiration(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraShortTermAllEffectiveRightsCacheExpiration, "");
        return attrs;
    }

    /**
     * Maximum number of entries in the short term All Effective Rights
     * cache. This cache can improve performance by avoiding recomputing All
     * Effective Rights of named entries like accounts frequently in a short
     * period of time. Can disable the cache be specifying a value of 0
     *
     * @return zimbraShortTermAllEffectiveRightsCacheSize, or -1 if unset
     *
     * @since ZCS 8.7.0
     */
    @ZAttr(id=1902)
    public int getShortTermAllEffectiveRightsCacheSize() {
        return getIntAttr(Provisioning.A_zimbraShortTermAllEffectiveRightsCacheSize, -1, true);
    }

    /**
     * Maximum number of entries in the short term All Effective Rights
     * cache. This cache can improve performance by avoiding recomputing All
     * Effective Rights of named entries like accounts frequently in a short
     * period of time. Can disable the cache be specifying a value of 0
     *
     * @param zimbraShortTermAllEffectiveRightsCacheSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.0
     */
    @ZAttr(id=1902)
    public void setShortTermAllEffectiveRightsCacheSize(int zimbraShortTermAllEffectiveRightsCacheSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraShortTermAllEffectiveRightsCacheSize, Integer.toString(zimbraShortTermAllEffectiveRightsCacheSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of entries in the short term All Effective Rights
     * cache. This cache can improve performance by avoiding recomputing All
     * Effective Rights of named entries like accounts frequently in a short
     * period of time. Can disable the cache be specifying a value of 0
     *
     * @param zimbraShortTermAllEffectiveRightsCacheSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.0
     */
    @ZAttr(id=1902)
    public Map<String,Object> setShortTermAllEffectiveRightsCacheSize(int zimbraShortTermAllEffectiveRightsCacheSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraShortTermAllEffectiveRightsCacheSize, Integer.toString(zimbraShortTermAllEffectiveRightsCacheSize));
        return attrs;
    }

    /**
     * Maximum number of entries in the short term All Effective Rights
     * cache. This cache can improve performance by avoiding recomputing All
     * Effective Rights of named entries like accounts frequently in a short
     * period of time. Can disable the cache be specifying a value of 0
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.0
     */
    @ZAttr(id=1902)
    public void unsetShortTermAllEffectiveRightsCacheSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraShortTermAllEffectiveRightsCacheSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of entries in the short term All Effective Rights
     * cache. This cache can improve performance by avoiding recomputing All
     * Effective Rights of named entries like accounts frequently in a short
     * period of time. Can disable the cache be specifying a value of 0
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.0
     */
    @ZAttr(id=1902)
    public Map<String,Object> unsetShortTermAllEffectiveRightsCacheSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraShortTermAllEffectiveRightsCacheSize, "");
        return attrs;
    }

    /**
     * Maximum time an entry in the Grantee cache will be regarded as valid.
     * If value is 0, the cache is disabled. This cache can improve
     * performance by avoiding recomputing details frequently in a short
     * period of time, for instance for each entry in search results. The
     * cache is particularly useful when significant use is made of delegated
     * administration. Grantees objects provide a view of what rights a
     * grantee has - although those are checked separately. The longer the
     * value of this setting is, the more stale the view of the details is
     * likely to be. For this reason, the maximum accepted value is 30m.
     * Larger values will be treated as being 30m . Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * <p>Use getShortTermGranteeCacheExpirationAsString to access value as a string.
     *
     * @see #getShortTermGranteeCacheExpirationAsString()
     *
     * @return zimbraShortTermGranteeCacheExpiration in millseconds, or -1 if unset
     *
     * @since ZCS 8.7.0
     */
    @ZAttr(id=1901)
    public long getShortTermGranteeCacheExpiration() {
        return getTimeInterval(Provisioning.A_zimbraShortTermGranteeCacheExpiration, -1L, true);
    }

    /**
     * Maximum time an entry in the Grantee cache will be regarded as valid.
     * If value is 0, the cache is disabled. This cache can improve
     * performance by avoiding recomputing details frequently in a short
     * period of time, for instance for each entry in search results. The
     * cache is particularly useful when significant use is made of delegated
     * administration. Grantees objects provide a view of what rights a
     * grantee has - although those are checked separately. The longer the
     * value of this setting is, the more stale the view of the details is
     * likely to be. For this reason, the maximum accepted value is 30m.
     * Larger values will be treated as being 30m . Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @return zimbraShortTermGranteeCacheExpiration, or null if unset
     *
     * @since ZCS 8.7.0
     */
    @ZAttr(id=1901)
    public String getShortTermGranteeCacheExpirationAsString() {
        return getAttr(Provisioning.A_zimbraShortTermGranteeCacheExpiration, null, true);
    }

    /**
     * Maximum time an entry in the Grantee cache will be regarded as valid.
     * If value is 0, the cache is disabled. This cache can improve
     * performance by avoiding recomputing details frequently in a short
     * period of time, for instance for each entry in search results. The
     * cache is particularly useful when significant use is made of delegated
     * administration. Grantees objects provide a view of what rights a
     * grantee has - although those are checked separately. The longer the
     * value of this setting is, the more stale the view of the details is
     * likely to be. For this reason, the maximum accepted value is 30m.
     * Larger values will be treated as being 30m . Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @param zimbraShortTermGranteeCacheExpiration new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.0
     */
    @ZAttr(id=1901)
    public void setShortTermGranteeCacheExpiration(String zimbraShortTermGranteeCacheExpiration) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraShortTermGranteeCacheExpiration, zimbraShortTermGranteeCacheExpiration);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum time an entry in the Grantee cache will be regarded as valid.
     * If value is 0, the cache is disabled. This cache can improve
     * performance by avoiding recomputing details frequently in a short
     * period of time, for instance for each entry in search results. The
     * cache is particularly useful when significant use is made of delegated
     * administration. Grantees objects provide a view of what rights a
     * grantee has - although those are checked separately. The longer the
     * value of this setting is, the more stale the view of the details is
     * likely to be. For this reason, the maximum accepted value is 30m.
     * Larger values will be treated as being 30m . Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @param zimbraShortTermGranteeCacheExpiration new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.0
     */
    @ZAttr(id=1901)
    public Map<String,Object> setShortTermGranteeCacheExpiration(String zimbraShortTermGranteeCacheExpiration, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraShortTermGranteeCacheExpiration, zimbraShortTermGranteeCacheExpiration);
        return attrs;
    }

    /**
     * Maximum time an entry in the Grantee cache will be regarded as valid.
     * If value is 0, the cache is disabled. This cache can improve
     * performance by avoiding recomputing details frequently in a short
     * period of time, for instance for each entry in search results. The
     * cache is particularly useful when significant use is made of delegated
     * administration. Grantees objects provide a view of what rights a
     * grantee has - although those are checked separately. The longer the
     * value of this setting is, the more stale the view of the details is
     * likely to be. For this reason, the maximum accepted value is 30m.
     * Larger values will be treated as being 30m . Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.0
     */
    @ZAttr(id=1901)
    public void unsetShortTermGranteeCacheExpiration() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraShortTermGranteeCacheExpiration, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum time an entry in the Grantee cache will be regarded as valid.
     * If value is 0, the cache is disabled. This cache can improve
     * performance by avoiding recomputing details frequently in a short
     * period of time, for instance for each entry in search results. The
     * cache is particularly useful when significant use is made of delegated
     * administration. Grantees objects provide a view of what rights a
     * grantee has - although those are checked separately. The longer the
     * value of this setting is, the more stale the view of the details is
     * likely to be. For this reason, the maximum accepted value is 30m.
     * Larger values will be treated as being 30m . Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.0
     */
    @ZAttr(id=1901)
    public Map<String,Object> unsetShortTermGranteeCacheExpiration(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraShortTermGranteeCacheExpiration, "");
        return attrs;
    }

    /**
     * Maximum number of entries in the short term Grantee cache. This cache
     * can improve performance by avoiding recomputing details frequently in
     * a short period of time, for instance for each entry in search results.
     * Can disable the cache be specifying a value of 0
     *
     * @return zimbraShortTermGranteeCacheSize, or -1 if unset
     *
     * @since ZCS 8.7.0
     */
    @ZAttr(id=1900)
    public int getShortTermGranteeCacheSize() {
        return getIntAttr(Provisioning.A_zimbraShortTermGranteeCacheSize, -1, true);
    }

    /**
     * Maximum number of entries in the short term Grantee cache. This cache
     * can improve performance by avoiding recomputing details frequently in
     * a short period of time, for instance for each entry in search results.
     * Can disable the cache be specifying a value of 0
     *
     * @param zimbraShortTermGranteeCacheSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.0
     */
    @ZAttr(id=1900)
    public void setShortTermGranteeCacheSize(int zimbraShortTermGranteeCacheSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraShortTermGranteeCacheSize, Integer.toString(zimbraShortTermGranteeCacheSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of entries in the short term Grantee cache. This cache
     * can improve performance by avoiding recomputing details frequently in
     * a short period of time, for instance for each entry in search results.
     * Can disable the cache be specifying a value of 0
     *
     * @param zimbraShortTermGranteeCacheSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.0
     */
    @ZAttr(id=1900)
    public Map<String,Object> setShortTermGranteeCacheSize(int zimbraShortTermGranteeCacheSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraShortTermGranteeCacheSize, Integer.toString(zimbraShortTermGranteeCacheSize));
        return attrs;
    }

    /**
     * Maximum number of entries in the short term Grantee cache. This cache
     * can improve performance by avoiding recomputing details frequently in
     * a short period of time, for instance for each entry in search results.
     * Can disable the cache be specifying a value of 0
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.7.0
     */
    @ZAttr(id=1900)
    public void unsetShortTermGranteeCacheSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraShortTermGranteeCacheSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of entries in the short term Grantee cache. This cache
     * can improve performance by avoiding recomputing details frequently in
     * a short period of time, for instance for each entry in search results.
     * Can disable the cache be specifying a value of 0
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.7.0
     */
    @ZAttr(id=1900)
    public Map<String,Object> unsetShortTermGranteeCacheSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraShortTermGranteeCacheSize, "");
        return attrs;
    }

    /**
     * The number of times the server will attempt to connect to a SOLR core.
     * 1 is the recommended setting.
     *
     * @return zimbraSolrMaxRetries, or -1 if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3025)
    public int getSolrMaxRetries() {
        return getIntAttr(Provisioning.A_zimbraSolrMaxRetries, -1, true);
    }

    /**
     * The number of times the server will attempt to connect to a SOLR core.
     * 1 is the recommended setting.
     *
     * @param zimbraSolrMaxRetries new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3025)
    public void setSolrMaxRetries(int zimbraSolrMaxRetries) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSolrMaxRetries, Integer.toString(zimbraSolrMaxRetries));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The number of times the server will attempt to connect to a SOLR core.
     * 1 is the recommended setting.
     *
     * @param zimbraSolrMaxRetries new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3025)
    public Map<String,Object> setSolrMaxRetries(int zimbraSolrMaxRetries, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSolrMaxRetries, Integer.toString(zimbraSolrMaxRetries));
        return attrs;
    }

    /**
     * The number of times the server will attempt to connect to a SOLR core.
     * 1 is the recommended setting.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3025)
    public void unsetSolrMaxRetries() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSolrMaxRetries, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The number of times the server will attempt to connect to a SOLR core.
     * 1 is the recommended setting.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3025)
    public Map<String,Object> unsetSolrMaxRetries(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSolrMaxRetries, "");
        return attrs;
    }

    /**
     * Limit for how many replicas of the same SOLR Collection are allowed to
     * be created on each node.
     *
     * @return zimbraSolrMaxShardsPerNode, or -1 if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3037)
    public int getSolrMaxShardsPerNode() {
        return getIntAttr(Provisioning.A_zimbraSolrMaxShardsPerNode, -1, true);
    }

    /**
     * Limit for how many replicas of the same SOLR Collection are allowed to
     * be created on each node.
     *
     * @param zimbraSolrMaxShardsPerNode new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3037)
    public void setSolrMaxShardsPerNode(int zimbraSolrMaxShardsPerNode) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSolrMaxShardsPerNode, Integer.toString(zimbraSolrMaxShardsPerNode));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Limit for how many replicas of the same SOLR Collection are allowed to
     * be created on each node.
     *
     * @param zimbraSolrMaxShardsPerNode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3037)
    public Map<String,Object> setSolrMaxShardsPerNode(int zimbraSolrMaxShardsPerNode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSolrMaxShardsPerNode, Integer.toString(zimbraSolrMaxShardsPerNode));
        return attrs;
    }

    /**
     * Limit for how many replicas of the same SOLR Collection are allowed to
     * be created on each node.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3037)
    public void unsetSolrMaxShardsPerNode() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSolrMaxShardsPerNode, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Limit for how many replicas of the same SOLR Collection are allowed to
     * be created on each node.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3037)
    public Map<String,Object> unsetSolrMaxShardsPerNode(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSolrMaxShardsPerNode, "");
        return attrs;
    }

    /**
     * Number of instances of a Solr Collection that will be created for each
     * mailbox. This value is used in replicationFactor parameter when
     * creating Solr Collections in SolrCloud with via Collections API.
     *
     * @return zimbraSolrReplicationFactor, or -1 if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3028)
    public int getSolrReplicationFactor() {
        return getIntAttr(Provisioning.A_zimbraSolrReplicationFactor, -1, true);
    }

    /**
     * Number of instances of a Solr Collection that will be created for each
     * mailbox. This value is used in replicationFactor parameter when
     * creating Solr Collections in SolrCloud with via Collections API.
     *
     * @param zimbraSolrReplicationFactor new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3028)
    public void setSolrReplicationFactor(int zimbraSolrReplicationFactor) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSolrReplicationFactor, Integer.toString(zimbraSolrReplicationFactor));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Number of instances of a Solr Collection that will be created for each
     * mailbox. This value is used in replicationFactor parameter when
     * creating Solr Collections in SolrCloud with via Collections API.
     *
     * @param zimbraSolrReplicationFactor new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3028)
    public Map<String,Object> setSolrReplicationFactor(int zimbraSolrReplicationFactor, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSolrReplicationFactor, Integer.toString(zimbraSolrReplicationFactor));
        return attrs;
    }

    /**
     * Number of instances of a Solr Collection that will be created for each
     * mailbox. This value is used in replicationFactor parameter when
     * creating Solr Collections in SolrCloud with via Collections API.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3028)
    public void unsetSolrReplicationFactor() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSolrReplicationFactor, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Number of instances of a Solr Collection that will be created for each
     * mailbox. This value is used in replicationFactor parameter when
     * creating Solr Collections in SolrCloud with via Collections API.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3028)
    public Map<String,Object> unsetSolrReplicationFactor(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSolrReplicationFactor, "");
        return attrs;
    }

    /**
     * Timeout for ZooKeeper requests. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * <p>Use getZKClientTimeoutAsString to access value as a string.
     *
     * @see #getZKClientTimeoutAsString()
     *
     * @return zimbraZKClientTimeout in millseconds, or -1 if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3029)
    public long getZKClientTimeout() {
        return getTimeInterval(Provisioning.A_zimbraZKClientTimeout, -1L, true);
    }

    /**
     * Timeout for ZooKeeper requests. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @return zimbraZKClientTimeout, or null if unset
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3029)
    public String getZKClientTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraZKClientTimeout, null, true);
    }

    /**
     * Timeout for ZooKeeper requests. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param zimbraZKClientTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3029)
    public void setZKClientTimeout(String zimbraZKClientTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZKClientTimeout, zimbraZKClientTimeout);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Timeout for ZooKeeper requests. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param zimbraZKClientTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3029)
    public Map<String,Object> setZKClientTimeout(String zimbraZKClientTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZKClientTimeout, zimbraZKClientTimeout);
        return attrs;
    }

    /**
     * Timeout for ZooKeeper requests. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3029)
    public void unsetZKClientTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZKClientTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Timeout for ZooKeeper requests. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.8.6
     */
    @ZAttr(id=3029)
    public Map<String,Object> unsetZKClientTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZKClientTimeout, "");
        return attrs;
    }

    /**
     * list of host:port for zookeeper servers; set to empty value to disable
     * the use of zookeeper
     *
     * @return zimbraZookeeperClientServerList, or empty array if unset
     *
     * @since ZCS 8.5.0
     */
    @ZAttr(id=1447)
    public String[] getZookeeperClientServerList() {
        return getMultiAttr(Provisioning.A_zimbraZookeeperClientServerList, true, true);
    }

    /**
     * list of host:port for zookeeper servers; set to empty value to disable
     * the use of zookeeper
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
     * list of host:port for zookeeper servers; set to empty value to disable
     * the use of zookeeper
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
     * list of host:port for zookeeper servers; set to empty value to disable
     * the use of zookeeper
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
     * list of host:port for zookeeper servers; set to empty value to disable
     * the use of zookeeper
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
     * list of host:port for zookeeper servers; set to empty value to disable
     * the use of zookeeper
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
     * list of host:port for zookeeper servers; set to empty value to disable
     * the use of zookeeper
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
     * list of host:port for zookeeper servers; set to empty value to disable
     * the use of zookeeper
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
     * list of host:port for zookeeper servers; set to empty value to disable
     * the use of zookeeper
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
