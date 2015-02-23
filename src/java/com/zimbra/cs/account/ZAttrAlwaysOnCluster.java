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
import com.zimbra.common.account.ZAttrProvisioning;
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
     * Size of the zimbra auth token cache size
     *
     * @return zimbaAuthTokenCacheSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1888)
    public int getZimbaAuthTokenCacheSize() {
        return getIntAttr(Provisioning.A_zimbaAuthTokenCacheSize, -1);
    }

    /**
     * Size of the zimbra auth token cache size
     *
     * @param zimbaAuthTokenCacheSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1888)
    public void setZimbaAuthTokenCacheSize(int zimbaAuthTokenCacheSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbaAuthTokenCacheSize, Integer.toString(zimbaAuthTokenCacheSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Size of the zimbra auth token cache size
     *
     * @param zimbaAuthTokenCacheSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1888)
    public Map<String,Object> setZimbaAuthTokenCacheSize(int zimbaAuthTokenCacheSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbaAuthTokenCacheSize, Integer.toString(zimbaAuthTokenCacheSize));
        return attrs;
    }

    /**
     * Size of the zimbra auth token cache size
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1888)
    public void unsetZimbaAuthTokenCacheSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbaAuthTokenCacheSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Size of the zimbra auth token cache size
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1888)
    public Map<String,Object> unsetZimbaAuthTokenCacheSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbaAuthTokenCacheSize, "");
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
     * Flag to use AutoDiscover service url
     *
     * @return zimbraActiveSyncAutoDiscoverUseServiceUrl, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1776)
    public boolean isActiveSyncAutoDiscoverUseServiceUrl() {
        return getBooleanAttr(Provisioning.A_zimbraActiveSyncAutoDiscoverUseServiceUrl, false);
    }

    /**
     * Flag to use AutoDiscover service url
     *
     * @param zimbraActiveSyncAutoDiscoverUseServiceUrl new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1776)
    public void setActiveSyncAutoDiscoverUseServiceUrl(boolean zimbraActiveSyncAutoDiscoverUseServiceUrl) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncAutoDiscoverUseServiceUrl, zimbraActiveSyncAutoDiscoverUseServiceUrl ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to use AutoDiscover service url
     *
     * @param zimbraActiveSyncAutoDiscoverUseServiceUrl new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1776)
    public Map<String,Object> setActiveSyncAutoDiscoverUseServiceUrl(boolean zimbraActiveSyncAutoDiscoverUseServiceUrl, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncAutoDiscoverUseServiceUrl, zimbraActiveSyncAutoDiscoverUseServiceUrl ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Flag to use AutoDiscover service url
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1776)
    public void unsetActiveSyncAutoDiscoverUseServiceUrl() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncAutoDiscoverUseServiceUrl, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to use AutoDiscover service url
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1776)
    public Map<String,Object> unsetActiveSyncAutoDiscoverUseServiceUrl(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncAutoDiscoverUseServiceUrl, "");
        return attrs;
    }

    /**
     * ActiveSync auto discovery url
     *
     * @return zimbraActiveSyncAutoDiscoveryUrl, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1775)
    public String getActiveSyncAutoDiscoveryUrl() {
        return getAttr(Provisioning.A_zimbraActiveSyncAutoDiscoveryUrl, null);
    }

    /**
     * ActiveSync auto discovery url
     *
     * @param zimbraActiveSyncAutoDiscoveryUrl new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1775)
    public void setActiveSyncAutoDiscoveryUrl(String zimbraActiveSyncAutoDiscoveryUrl) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncAutoDiscoveryUrl, zimbraActiveSyncAutoDiscoveryUrl);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * ActiveSync auto discovery url
     *
     * @param zimbraActiveSyncAutoDiscoveryUrl new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1775)
    public Map<String,Object> setActiveSyncAutoDiscoveryUrl(String zimbraActiveSyncAutoDiscoveryUrl, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncAutoDiscoveryUrl, zimbraActiveSyncAutoDiscoveryUrl);
        return attrs;
    }

    /**
     * ActiveSync auto discovery url
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1775)
    public void unsetActiveSyncAutoDiscoveryUrl() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncAutoDiscoveryUrl, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * ActiveSync auto discovery url
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1775)
    public Map<String,Object> unsetActiveSyncAutoDiscoveryUrl(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncAutoDiscoveryUrl, "");
        return attrs;
    }

    /**
     * Maximum allowed contact image size in bytes. Default value in 2MB
     *
     * @return zimbraActiveSyncContactImageSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1774)
    public int getActiveSyncContactImageSize() {
        return getIntAttr(Provisioning.A_zimbraActiveSyncContactImageSize, -1);
    }

    /**
     * Maximum allowed contact image size in bytes. Default value in 2MB
     *
     * @param zimbraActiveSyncContactImageSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1774)
    public void setActiveSyncContactImageSize(int zimbraActiveSyncContactImageSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncContactImageSize, Integer.toString(zimbraActiveSyncContactImageSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum allowed contact image size in bytes. Default value in 2MB
     *
     * @param zimbraActiveSyncContactImageSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1774)
    public Map<String,Object> setActiveSyncContactImageSize(int zimbraActiveSyncContactImageSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncContactImageSize, Integer.toString(zimbraActiveSyncContactImageSize));
        return attrs;
    }

    /**
     * Maximum allowed contact image size in bytes. Default value in 2MB
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1774)
    public void unsetActiveSyncContactImageSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncContactImageSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum allowed contact image size in bytes. Default value in 2MB
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1774)
    public Map<String,Object> unsetActiveSyncContactImageSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncContactImageSize, "");
        return attrs;
    }

    /**
     * General cache size for ActiveSync. It is used by active sync in
     * pingLockCache, syncLockCache, syncRequest and syncResponse cache.
     *
     * @return zimbraActiveSyncGeneralCacheSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1767)
    public int getActiveSyncGeneralCacheSize() {
        return getIntAttr(Provisioning.A_zimbraActiveSyncGeneralCacheSize, -1);
    }

    /**
     * General cache size for ActiveSync. It is used by active sync in
     * pingLockCache, syncLockCache, syncRequest and syncResponse cache.
     *
     * @param zimbraActiveSyncGeneralCacheSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1767)
    public void setActiveSyncGeneralCacheSize(int zimbraActiveSyncGeneralCacheSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncGeneralCacheSize, Integer.toString(zimbraActiveSyncGeneralCacheSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * General cache size for ActiveSync. It is used by active sync in
     * pingLockCache, syncLockCache, syncRequest and syncResponse cache.
     *
     * @param zimbraActiveSyncGeneralCacheSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1767)
    public Map<String,Object> setActiveSyncGeneralCacheSize(int zimbraActiveSyncGeneralCacheSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncGeneralCacheSize, Integer.toString(zimbraActiveSyncGeneralCacheSize));
        return attrs;
    }

    /**
     * General cache size for ActiveSync. It is used by active sync in
     * pingLockCache, syncLockCache, syncRequest and syncResponse cache.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1767)
    public void unsetActiveSyncGeneralCacheSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncGeneralCacheSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * General cache size for ActiveSync. It is used by active sync in
     * pingLockCache, syncLockCache, syncRequest and syncResponse cache.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1767)
    public Map<String,Object> unsetActiveSyncGeneralCacheSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncGeneralCacheSize, "");
        return attrs;
    }

    /**
     * ActiveSync maximum value for heartbeat interval in seconds. Make sure
     * it&#039;s less than nginx&#039;s
     * zimbraReverseProxyUpstreamPollingTimeout, which is now 3600 seconds.
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * <p>Use getActiveSyncHeartbeatIntervalMaxAsString to access value as a string.
     *
     * @see #getActiveSyncHeartbeatIntervalMaxAsString()
     *
     * @return zimbraActiveSyncHeartbeatIntervalMax in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1780)
    public long getActiveSyncHeartbeatIntervalMax() {
        return getTimeInterval(Provisioning.A_zimbraActiveSyncHeartbeatIntervalMax, -1L);
    }

    /**
     * ActiveSync maximum value for heartbeat interval in seconds. Make sure
     * it&#039;s less than nginx&#039;s
     * zimbraReverseProxyUpstreamPollingTimeout, which is now 3600 seconds.
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @return zimbraActiveSyncHeartbeatIntervalMax, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1780)
    public String getActiveSyncHeartbeatIntervalMaxAsString() {
        return getAttr(Provisioning.A_zimbraActiveSyncHeartbeatIntervalMax, null);
    }

    /**
     * ActiveSync maximum value for heartbeat interval in seconds. Make sure
     * it&#039;s less than nginx&#039;s
     * zimbraReverseProxyUpstreamPollingTimeout, which is now 3600 seconds.
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraActiveSyncHeartbeatIntervalMax new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1780)
    public void setActiveSyncHeartbeatIntervalMax(String zimbraActiveSyncHeartbeatIntervalMax) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncHeartbeatIntervalMax, zimbraActiveSyncHeartbeatIntervalMax);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * ActiveSync maximum value for heartbeat interval in seconds. Make sure
     * it&#039;s less than nginx&#039;s
     * zimbraReverseProxyUpstreamPollingTimeout, which is now 3600 seconds.
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraActiveSyncHeartbeatIntervalMax new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1780)
    public Map<String,Object> setActiveSyncHeartbeatIntervalMax(String zimbraActiveSyncHeartbeatIntervalMax, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncHeartbeatIntervalMax, zimbraActiveSyncHeartbeatIntervalMax);
        return attrs;
    }

    /**
     * ActiveSync maximum value for heartbeat interval in seconds. Make sure
     * it&#039;s less than nginx&#039;s
     * zimbraReverseProxyUpstreamPollingTimeout, which is now 3600 seconds.
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1780)
    public void unsetActiveSyncHeartbeatIntervalMax() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncHeartbeatIntervalMax, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * ActiveSync maximum value for heartbeat interval in seconds. Make sure
     * it&#039;s less than nginx&#039;s
     * zimbraReverseProxyUpstreamPollingTimeout, which is now 3600 seconds.
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1780)
    public Map<String,Object> unsetActiveSyncHeartbeatIntervalMax(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncHeartbeatIntervalMax, "");
        return attrs;
    }

    /**
     * ActiveSync minimum value for heartbeat interval in seconds. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * <p>Use getActiveSyncHeartbeatIntervalMinAsString to access value as a string.
     *
     * @see #getActiveSyncHeartbeatIntervalMinAsString()
     *
     * @return zimbraActiveSyncHeartbeatIntervalMin in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1779)
    public long getActiveSyncHeartbeatIntervalMin() {
        return getTimeInterval(Provisioning.A_zimbraActiveSyncHeartbeatIntervalMin, -1L);
    }

    /**
     * ActiveSync minimum value for heartbeat interval in seconds. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @return zimbraActiveSyncHeartbeatIntervalMin, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1779)
    public String getActiveSyncHeartbeatIntervalMinAsString() {
        return getAttr(Provisioning.A_zimbraActiveSyncHeartbeatIntervalMin, null);
    }

    /**
     * ActiveSync minimum value for heartbeat interval in seconds. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraActiveSyncHeartbeatIntervalMin new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1779)
    public void setActiveSyncHeartbeatIntervalMin(String zimbraActiveSyncHeartbeatIntervalMin) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncHeartbeatIntervalMin, zimbraActiveSyncHeartbeatIntervalMin);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * ActiveSync minimum value for heartbeat interval in seconds. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraActiveSyncHeartbeatIntervalMin new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1779)
    public Map<String,Object> setActiveSyncHeartbeatIntervalMin(String zimbraActiveSyncHeartbeatIntervalMin, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncHeartbeatIntervalMin, zimbraActiveSyncHeartbeatIntervalMin);
        return attrs;
    }

    /**
     * ActiveSync minimum value for heartbeat interval in seconds. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1779)
    public void unsetActiveSyncHeartbeatIntervalMin() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncHeartbeatIntervalMin, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * ActiveSync minimum value for heartbeat interval in seconds. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1779)
    public Map<String,Object> unsetActiveSyncHeartbeatIntervalMin(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncHeartbeatIntervalMin, "");
        return attrs;
    }

    /**
     * ActiveSync metadata cache expiration time in seconds. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * <p>Use getActiveSyncMetadataCacheExpirationAsString to access value as a string.
     *
     * @see #getActiveSyncMetadataCacheExpirationAsString()
     *
     * @return zimbraActiveSyncMetadataCacheExpiration in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1777)
    public long getActiveSyncMetadataCacheExpiration() {
        return getTimeInterval(Provisioning.A_zimbraActiveSyncMetadataCacheExpiration, -1L);
    }

    /**
     * ActiveSync metadata cache expiration time in seconds. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @return zimbraActiveSyncMetadataCacheExpiration, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1777)
    public String getActiveSyncMetadataCacheExpirationAsString() {
        return getAttr(Provisioning.A_zimbraActiveSyncMetadataCacheExpiration, null);
    }

    /**
     * ActiveSync metadata cache expiration time in seconds. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraActiveSyncMetadataCacheExpiration new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1777)
    public void setActiveSyncMetadataCacheExpiration(String zimbraActiveSyncMetadataCacheExpiration) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncMetadataCacheExpiration, zimbraActiveSyncMetadataCacheExpiration);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * ActiveSync metadata cache expiration time in seconds. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraActiveSyncMetadataCacheExpiration new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1777)
    public Map<String,Object> setActiveSyncMetadataCacheExpiration(String zimbraActiveSyncMetadataCacheExpiration, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncMetadataCacheExpiration, zimbraActiveSyncMetadataCacheExpiration);
        return attrs;
    }

    /**
     * ActiveSync metadata cache expiration time in seconds. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1777)
    public void unsetActiveSyncMetadataCacheExpiration() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncMetadataCacheExpiration, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * ActiveSync metadata cache expiration time in seconds. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1777)
    public Map<String,Object> unsetActiveSyncMetadataCacheExpiration(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncMetadataCacheExpiration, "");
        return attrs;
    }

    /**
     * ActiveSync metadata cache max size
     *
     * @return zimbraActiveSyncMetadataCacheMaxSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1778)
    public int getActiveSyncMetadataCacheMaxSize() {
        return getIntAttr(Provisioning.A_zimbraActiveSyncMetadataCacheMaxSize, -1);
    }

    /**
     * ActiveSync metadata cache max size
     *
     * @param zimbraActiveSyncMetadataCacheMaxSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1778)
    public void setActiveSyncMetadataCacheMaxSize(int zimbraActiveSyncMetadataCacheMaxSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncMetadataCacheMaxSize, Integer.toString(zimbraActiveSyncMetadataCacheMaxSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * ActiveSync metadata cache max size
     *
     * @param zimbraActiveSyncMetadataCacheMaxSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1778)
    public Map<String,Object> setActiveSyncMetadataCacheMaxSize(int zimbraActiveSyncMetadataCacheMaxSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncMetadataCacheMaxSize, Integer.toString(zimbraActiveSyncMetadataCacheMaxSize));
        return attrs;
    }

    /**
     * ActiveSync metadata cache max size
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1778)
    public void unsetActiveSyncMetadataCacheMaxSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncMetadataCacheMaxSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * ActiveSync metadata cache max size
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1778)
    public Map<String,Object> unsetActiveSyncMetadataCacheMaxSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncMetadataCacheMaxSize, "");
        return attrs;
    }

    /**
     * Flag to enable or disable to allow parallel syncing of folders
     *
     * @return zimbraActiveSyncParallelSyncEnabled, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1765)
    public boolean isActiveSyncParallelSyncEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraActiveSyncParallelSyncEnabled, false);
    }

    /**
     * Flag to enable or disable to allow parallel syncing of folders
     *
     * @param zimbraActiveSyncParallelSyncEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1765)
    public void setActiveSyncParallelSyncEnabled(boolean zimbraActiveSyncParallelSyncEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncParallelSyncEnabled, zimbraActiveSyncParallelSyncEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable or disable to allow parallel syncing of folders
     *
     * @param zimbraActiveSyncParallelSyncEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1765)
    public Map<String,Object> setActiveSyncParallelSyncEnabled(boolean zimbraActiveSyncParallelSyncEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncParallelSyncEnabled, zimbraActiveSyncParallelSyncEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Flag to enable or disable to allow parallel syncing of folders
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1765)
    public void unsetActiveSyncParallelSyncEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncParallelSyncEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable or disable to allow parallel syncing of folders
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1765)
    public Map<String,Object> unsetActiveSyncParallelSyncEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncParallelSyncEnabled, "");
        return attrs;
    }

    /**
     * Maximum results allowed for ActiveSync results.
     *
     * @return zimbraActiveSyncSearchMaxResults, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1766)
    public int getActiveSyncSearchMaxResults() {
        return getIntAttr(Provisioning.A_zimbraActiveSyncSearchMaxResults, -1);
    }

    /**
     * Maximum results allowed for ActiveSync results.
     *
     * @param zimbraActiveSyncSearchMaxResults new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1766)
    public void setActiveSyncSearchMaxResults(int zimbraActiveSyncSearchMaxResults) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncSearchMaxResults, Integer.toString(zimbraActiveSyncSearchMaxResults));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum results allowed for ActiveSync results.
     *
     * @param zimbraActiveSyncSearchMaxResults new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1766)
    public Map<String,Object> setActiveSyncSearchMaxResults(int zimbraActiveSyncSearchMaxResults, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncSearchMaxResults, Integer.toString(zimbraActiveSyncSearchMaxResults));
        return attrs;
    }

    /**
     * Maximum results allowed for ActiveSync results.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1766)
    public void unsetActiveSyncSearchMaxResults() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncSearchMaxResults, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum results allowed for ActiveSync results.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1766)
    public Map<String,Object> unsetActiveSyncSearchMaxResults(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncSearchMaxResults, "");
        return attrs;
    }

    /**
     * ActiveSync SyncState cache heap size. Use suffixes B, K, M, G for
     * bytes, kilobytes, megabytes and gigabytes respectively
     *
     * @return zimbraActiveSyncSyncStateItemCacheHeapSize, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1768)
    public String getActiveSyncSyncStateItemCacheHeapSize() {
        return getAttr(Provisioning.A_zimbraActiveSyncSyncStateItemCacheHeapSize, null);
    }

    /**
     * ActiveSync SyncState cache heap size. Use suffixes B, K, M, G for
     * bytes, kilobytes, megabytes and gigabytes respectively
     *
     * @param zimbraActiveSyncSyncStateItemCacheHeapSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1768)
    public void setActiveSyncSyncStateItemCacheHeapSize(String zimbraActiveSyncSyncStateItemCacheHeapSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncSyncStateItemCacheHeapSize, zimbraActiveSyncSyncStateItemCacheHeapSize);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * ActiveSync SyncState cache heap size. Use suffixes B, K, M, G for
     * bytes, kilobytes, megabytes and gigabytes respectively
     *
     * @param zimbraActiveSyncSyncStateItemCacheHeapSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1768)
    public Map<String,Object> setActiveSyncSyncStateItemCacheHeapSize(String zimbraActiveSyncSyncStateItemCacheHeapSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncSyncStateItemCacheHeapSize, zimbraActiveSyncSyncStateItemCacheHeapSize);
        return attrs;
    }

    /**
     * ActiveSync SyncState cache heap size. Use suffixes B, K, M, G for
     * bytes, kilobytes, megabytes and gigabytes respectively
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1768)
    public void unsetActiveSyncSyncStateItemCacheHeapSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncSyncStateItemCacheHeapSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * ActiveSync SyncState cache heap size. Use suffixes B, K, M, G for
     * bytes, kilobytes, megabytes and gigabytes respectively
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1768)
    public Map<String,Object> unsetActiveSyncSyncStateItemCacheHeapSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncSyncStateItemCacheHeapSize, "");
        return attrs;
    }

    /**
     * Supported ActiveSync versions by zimbra
     *
     * @return zimbraActiveSyncVersions, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1773)
    public String getActiveSyncVersions() {
        return getAttr(Provisioning.A_zimbraActiveSyncVersions, null);
    }

    /**
     * Supported ActiveSync versions by zimbra
     *
     * @param zimbraActiveSyncVersions new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1773)
    public void setActiveSyncVersions(String zimbraActiveSyncVersions) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncVersions, zimbraActiveSyncVersions);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Supported ActiveSync versions by zimbra
     *
     * @param zimbraActiveSyncVersions new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1773)
    public Map<String,Object> setActiveSyncVersions(String zimbraActiveSyncVersions, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncVersions, zimbraActiveSyncVersions);
        return attrs;
    }

    /**
     * Supported ActiveSync versions by zimbra
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1773)
    public void unsetActiveSyncVersions() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncVersions, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Supported ActiveSync versions by zimbra
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1773)
    public Map<String,Object> unsetActiveSyncVersions(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraActiveSyncVersions, "");
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
     * Default timeout in seconds for zimbra admin wait set request. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * <p>Use getAdminWaitsetDefaultRequestTimeoutAsString to access value as a string.
     *
     * @see #getAdminWaitsetDefaultRequestTimeoutAsString()
     *
     * @return zimbraAdminWaitsetDefaultRequestTimeout in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1729)
    public long getAdminWaitsetDefaultRequestTimeout() {
        return getTimeInterval(Provisioning.A_zimbraAdminWaitsetDefaultRequestTimeout, -1L);
    }

    /**
     * Default timeout in seconds for zimbra admin wait set request. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @return zimbraAdminWaitsetDefaultRequestTimeout, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1729)
    public String getAdminWaitsetDefaultRequestTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraAdminWaitsetDefaultRequestTimeout, null);
    }

    /**
     * Default timeout in seconds for zimbra admin wait set request. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraAdminWaitsetDefaultRequestTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1729)
    public void setAdminWaitsetDefaultRequestTimeout(String zimbraAdminWaitsetDefaultRequestTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminWaitsetDefaultRequestTimeout, zimbraAdminWaitsetDefaultRequestTimeout);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Default timeout in seconds for zimbra admin wait set request. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraAdminWaitsetDefaultRequestTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1729)
    public Map<String,Object> setAdminWaitsetDefaultRequestTimeout(String zimbraAdminWaitsetDefaultRequestTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminWaitsetDefaultRequestTimeout, zimbraAdminWaitsetDefaultRequestTimeout);
        return attrs;
    }

    /**
     * Default timeout in seconds for zimbra admin wait set request. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
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
     * Default timeout in seconds for zimbra admin wait set request. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
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
     * Maximum timeout in seconds for zimbra admin waitset request. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * <p>Use getAdminWaitsetMaxRequestTimeoutAsString to access value as a string.
     *
     * @see #getAdminWaitsetMaxRequestTimeoutAsString()
     *
     * @return zimbraAdminWaitsetMaxRequestTimeout in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1730)
    public long getAdminWaitsetMaxRequestTimeout() {
        return getTimeInterval(Provisioning.A_zimbraAdminWaitsetMaxRequestTimeout, -1L);
    }

    /**
     * Maximum timeout in seconds for zimbra admin waitset request. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @return zimbraAdminWaitsetMaxRequestTimeout, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1730)
    public String getAdminWaitsetMaxRequestTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraAdminWaitsetMaxRequestTimeout, null);
    }

    /**
     * Maximum timeout in seconds for zimbra admin waitset request. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraAdminWaitsetMaxRequestTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1730)
    public void setAdminWaitsetMaxRequestTimeout(String zimbraAdminWaitsetMaxRequestTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminWaitsetMaxRequestTimeout, zimbraAdminWaitsetMaxRequestTimeout);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum timeout in seconds for zimbra admin waitset request. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraAdminWaitsetMaxRequestTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1730)
    public Map<String,Object> setAdminWaitsetMaxRequestTimeout(String zimbraAdminWaitsetMaxRequestTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminWaitsetMaxRequestTimeout, zimbraAdminWaitsetMaxRequestTimeout);
        return attrs;
    }

    /**
     * Maximum timeout in seconds for zimbra admin waitset request. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
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
     * Maximum timeout in seconds for zimbra admin waitset request. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
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
     * Minimum timeout in seconds for zimbra admin waitset request. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * <p>Use getAdminWaitsetMinRequestTimeoutAsString to access value as a string.
     *
     * @see #getAdminWaitsetMinRequestTimeoutAsString()
     *
     * @return zimbraAdminWaitsetMinRequestTimeout in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1731)
    public long getAdminWaitsetMinRequestTimeout() {
        return getTimeInterval(Provisioning.A_zimbraAdminWaitsetMinRequestTimeout, -1L);
    }

    /**
     * Minimum timeout in seconds for zimbra admin waitset request. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @return zimbraAdminWaitsetMinRequestTimeout, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1731)
    public String getAdminWaitsetMinRequestTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraAdminWaitsetMinRequestTimeout, null);
    }

    /**
     * Minimum timeout in seconds for zimbra admin waitset request. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraAdminWaitsetMinRequestTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1731)
    public void setAdminWaitsetMinRequestTimeout(String zimbraAdminWaitsetMinRequestTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminWaitsetMinRequestTimeout, zimbraAdminWaitsetMinRequestTimeout);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Minimum timeout in seconds for zimbra admin waitset request. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraAdminWaitsetMinRequestTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1731)
    public Map<String,Object> setAdminWaitsetMinRequestTimeout(String zimbraAdminWaitsetMinRequestTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminWaitsetMinRequestTimeout, zimbraAdminWaitsetMinRequestTimeout);
        return attrs;
    }

    /**
     * Minimum timeout in seconds for zimbra admin waitset request. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
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
     * Minimum timeout in seconds for zimbra admin waitset request. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
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
     * Queue size for the deregistered auth tokens
     *
     * @return zimbraAuthDeregisteredAuthTokenQueueSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1841)
    public int getAuthDeregisteredAuthTokenQueueSize() {
        return getIntAttr(Provisioning.A_zimbraAuthDeregisteredAuthTokenQueueSize, -1);
    }

    /**
     * Queue size for the deregistered auth tokens
     *
     * @param zimbraAuthDeregisteredAuthTokenQueueSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1841)
    public void setAuthDeregisteredAuthTokenQueueSize(int zimbraAuthDeregisteredAuthTokenQueueSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthDeregisteredAuthTokenQueueSize, Integer.toString(zimbraAuthDeregisteredAuthTokenQueueSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Queue size for the deregistered auth tokens
     *
     * @param zimbraAuthDeregisteredAuthTokenQueueSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1841)
    public Map<String,Object> setAuthDeregisteredAuthTokenQueueSize(int zimbraAuthDeregisteredAuthTokenQueueSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthDeregisteredAuthTokenQueueSize, Integer.toString(zimbraAuthDeregisteredAuthTokenQueueSize));
        return attrs;
    }

    /**
     * Queue size for the deregistered auth tokens
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1841)
    public void unsetAuthDeregisteredAuthTokenQueueSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthDeregisteredAuthTokenQueueSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Queue size for the deregistered auth tokens
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1841)
    public Map<String,Object> unsetAuthDeregisteredAuthTokenQueueSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthDeregisteredAuthTokenQueueSize, "");
        return attrs;
    }

    /**
     * An ordered comma-seperated list of auth providers
     *
     * @return zimbraAuthProvider, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1887)
    public String getAuthProvider() {
        return getAttr(Provisioning.A_zimbraAuthProvider, null);
    }

    /**
     * An ordered comma-seperated list of auth providers
     *
     * @param zimbraAuthProvider new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1887)
    public void setAuthProvider(String zimbraAuthProvider) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthProvider, zimbraAuthProvider);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * An ordered comma-seperated list of auth providers
     *
     * @param zimbraAuthProvider new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1887)
    public Map<String,Object> setAuthProvider(String zimbraAuthProvider, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthProvider, zimbraAuthProvider);
        return attrs;
    }

    /**
     * An ordered comma-seperated list of auth providers
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1887)
    public void unsetAuthProvider() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthProvider, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * An ordered comma-seperated list of auth providers
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1887)
    public Map<String,Object> unsetAuthProvider(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthProvider, "");
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
     * Blob store input stream buffer size in kilobytes
     *
     * @return zimbraBlobStoreInputStreamBufferSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1895)
    public int getBlobStoreInputStreamBufferSize() {
        return getIntAttr(Provisioning.A_zimbraBlobStoreInputStreamBufferSize, -1);
    }

    /**
     * Blob store input stream buffer size in kilobytes
     *
     * @param zimbraBlobStoreInputStreamBufferSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1895)
    public void setBlobStoreInputStreamBufferSize(int zimbraBlobStoreInputStreamBufferSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBlobStoreInputStreamBufferSize, Integer.toString(zimbraBlobStoreInputStreamBufferSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Blob store input stream buffer size in kilobytes
     *
     * @param zimbraBlobStoreInputStreamBufferSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1895)
    public Map<String,Object> setBlobStoreInputStreamBufferSize(int zimbraBlobStoreInputStreamBufferSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBlobStoreInputStreamBufferSize, Integer.toString(zimbraBlobStoreInputStreamBufferSize));
        return attrs;
    }

    /**
     * Blob store input stream buffer size in kilobytes
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1895)
    public void unsetBlobStoreInputStreamBufferSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBlobStoreInputStreamBufferSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Blob store input stream buffer size in kilobytes
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1895)
    public Map<String,Object> unsetBlobStoreInputStreamBufferSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBlobStoreInputStreamBufferSize, "");
        return attrs;
    }

    /**
     * Files older than this many minutes are auto-deleted from store
     * incoming directory. Default is 8 hours. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * <p>Use getBlobStoreSweeperMaxAgeAsString to access value as a string.
     *
     * @see #getBlobStoreSweeperMaxAgeAsString()
     *
     * @return zimbraBlobStoreSweeperMaxAge in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1870)
    public long getBlobStoreSweeperMaxAge() {
        return getTimeInterval(Provisioning.A_zimbraBlobStoreSweeperMaxAge, -1L);
    }

    /**
     * Files older than this many minutes are auto-deleted from store
     * incoming directory. Default is 8 hours. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @return zimbraBlobStoreSweeperMaxAge, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1870)
    public String getBlobStoreSweeperMaxAgeAsString() {
        return getAttr(Provisioning.A_zimbraBlobStoreSweeperMaxAge, null);
    }

    /**
     * Files older than this many minutes are auto-deleted from store
     * incoming directory. Default is 8 hours. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @param zimbraBlobStoreSweeperMaxAge new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1870)
    public void setBlobStoreSweeperMaxAge(String zimbraBlobStoreSweeperMaxAge) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBlobStoreSweeperMaxAge, zimbraBlobStoreSweeperMaxAge);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Files older than this many minutes are auto-deleted from store
     * incoming directory. Default is 8 hours. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @param zimbraBlobStoreSweeperMaxAge new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1870)
    public Map<String,Object> setBlobStoreSweeperMaxAge(String zimbraBlobStoreSweeperMaxAge, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBlobStoreSweeperMaxAge, zimbraBlobStoreSweeperMaxAge);
        return attrs;
    }

    /**
     * Files older than this many minutes are auto-deleted from store
     * incoming directory. Default is 8 hours. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1870)
    public void unsetBlobStoreSweeperMaxAge() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBlobStoreSweeperMaxAge, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Files older than this many minutes are auto-deleted from store
     * incoming directory. Default is 8 hours. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1870)
    public Map<String,Object> unsetBlobStoreSweeperMaxAge(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBlobStoreSweeperMaxAge, "");
        return attrs;
    }

    /**
     * Minimum TTL in millis for uncompressed file cache used by File blob
     * store. Must be in valid duration format: {digits}{time-unit}. digits:
     * 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d -
     * days, ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * <p>Use getBlobStoreUncompressedCacheMinLifetimeAsString to access value as a string.
     *
     * @see #getBlobStoreUncompressedCacheMinLifetimeAsString()
     *
     * @return zimbraBlobStoreUncompressedCacheMinLifetime in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1890)
    public long getBlobStoreUncompressedCacheMinLifetime() {
        return getTimeInterval(Provisioning.A_zimbraBlobStoreUncompressedCacheMinLifetime, -1L);
    }

    /**
     * Minimum TTL in millis for uncompressed file cache used by File blob
     * store. Must be in valid duration format: {digits}{time-unit}. digits:
     * 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d -
     * days, ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @return zimbraBlobStoreUncompressedCacheMinLifetime, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1890)
    public String getBlobStoreUncompressedCacheMinLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraBlobStoreUncompressedCacheMinLifetime, null);
    }

    /**
     * Minimum TTL in millis for uncompressed file cache used by File blob
     * store. Must be in valid duration format: {digits}{time-unit}. digits:
     * 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d -
     * days, ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraBlobStoreUncompressedCacheMinLifetime new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1890)
    public void setBlobStoreUncompressedCacheMinLifetime(String zimbraBlobStoreUncompressedCacheMinLifetime) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBlobStoreUncompressedCacheMinLifetime, zimbraBlobStoreUncompressedCacheMinLifetime);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Minimum TTL in millis for uncompressed file cache used by File blob
     * store. Must be in valid duration format: {digits}{time-unit}. digits:
     * 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d -
     * days, ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraBlobStoreUncompressedCacheMinLifetime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1890)
    public Map<String,Object> setBlobStoreUncompressedCacheMinLifetime(String zimbraBlobStoreUncompressedCacheMinLifetime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBlobStoreUncompressedCacheMinLifetime, zimbraBlobStoreUncompressedCacheMinLifetime);
        return attrs;
    }

    /**
     * Minimum TTL in millis for uncompressed file cache used by File blob
     * store. Must be in valid duration format: {digits}{time-unit}. digits:
     * 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d -
     * days, ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1890)
    public void unsetBlobStoreUncompressedCacheMinLifetime() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBlobStoreUncompressedCacheMinLifetime, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Minimum TTL in millis for uncompressed file cache used by File blob
     * store. Must be in valid duration format: {digits}{time-unit}. digits:
     * 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d -
     * days, ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1890)
    public Map<String,Object> unsetBlobStoreUncompressedCacheMinLifetime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBlobStoreUncompressedCacheMinLifetime, "");
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
     * Enable contact ranking table
     *
     * @return zimbraContactRankingEnabled, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1816)
    public boolean isContactRankingEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraContactRankingEnabled, false);
    }

    /**
     * Enable contact ranking table
     *
     * @param zimbraContactRankingEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1816)
    public void setContactRankingEnabled(boolean zimbraContactRankingEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraContactRankingEnabled, zimbraContactRankingEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enable contact ranking table
     *
     * @param zimbraContactRankingEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1816)
    public Map<String,Object> setContactRankingEnabled(boolean zimbraContactRankingEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraContactRankingEnabled, zimbraContactRankingEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Enable contact ranking table
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1816)
    public void unsetContactRankingEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraContactRankingEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enable contact ranking table
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1816)
    public Map<String,Object> unsetContactRankingEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraContactRankingEnabled, "");
        return attrs;
    }

    /**
     * Flag to enable contact ranking table
     *
     * @return zimbraConversationIgnoreMaillistPrefix, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1792)
    public boolean isConversationIgnoreMaillistPrefix() {
        return getBooleanAttr(Provisioning.A_zimbraConversationIgnoreMaillistPrefix, false);
    }

    /**
     * Flag to enable contact ranking table
     *
     * @param zimbraConversationIgnoreMaillistPrefix new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1792)
    public void setConversationIgnoreMaillistPrefix(boolean zimbraConversationIgnoreMaillistPrefix) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraConversationIgnoreMaillistPrefix, zimbraConversationIgnoreMaillistPrefix ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable contact ranking table
     *
     * @param zimbraConversationIgnoreMaillistPrefix new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1792)
    public Map<String,Object> setConversationIgnoreMaillistPrefix(boolean zimbraConversationIgnoreMaillistPrefix, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraConversationIgnoreMaillistPrefix, zimbraConversationIgnoreMaillistPrefix ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Flag to enable contact ranking table
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1792)
    public void unsetConversationIgnoreMaillistPrefix() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraConversationIgnoreMaillistPrefix, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable contact ranking table
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1792)
    public Map<String,Object> unsetConversationIgnoreMaillistPrefix(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraConversationIgnoreMaillistPrefix, "");
        return attrs;
    }

    /**
     * Conversation max age in millis. Deletes rows from open_conversation
     * whose items are older than the given no of days. Default is 31 days..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * <p>Use getConversationMaxAgeAsString to access value as a string.
     *
     * @see #getConversationMaxAgeAsString()
     *
     * @return zimbraConversationMaxAge in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1793)
    public long getConversationMaxAge() {
        return getTimeInterval(Provisioning.A_zimbraConversationMaxAge, -1L);
    }

    /**
     * Conversation max age in millis. Deletes rows from open_conversation
     * whose items are older than the given no of days. Default is 31 days..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @return zimbraConversationMaxAge, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1793)
    public String getConversationMaxAgeAsString() {
        return getAttr(Provisioning.A_zimbraConversationMaxAge, null);
    }

    /**
     * Conversation max age in millis. Deletes rows from open_conversation
     * whose items are older than the given no of days. Default is 31 days..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraConversationMaxAge new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1793)
    public void setConversationMaxAge(String zimbraConversationMaxAge) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraConversationMaxAge, zimbraConversationMaxAge);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Conversation max age in millis. Deletes rows from open_conversation
     * whose items are older than the given no of days. Default is 31 days..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraConversationMaxAge new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1793)
    public Map<String,Object> setConversationMaxAge(String zimbraConversationMaxAge, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraConversationMaxAge, zimbraConversationMaxAge);
        return attrs;
    }

    /**
     * Conversation max age in millis. Deletes rows from open_conversation
     * whose items are older than the given no of days. Default is 31 days..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1793)
    public void unsetConversationMaxAge() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraConversationMaxAge, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Conversation max age in millis. Deletes rows from open_conversation
     * whose items are older than the given no of days. Default is 31 days..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1793)
    public Map<String,Object> unsetConversationMaxAge(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraConversationMaxAge, "");
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
     * Flag to use auto discover service url for the ews
     *
     * @return zimbraEwsAutoDiscoverUseServiceUrl, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1844)
    public boolean isEwsAutoDiscoverUseServiceUrl() {
        return getBooleanAttr(Provisioning.A_zimbraEwsAutoDiscoverUseServiceUrl, false);
    }

    /**
     * Flag to use auto discover service url for the ews
     *
     * @param zimbraEwsAutoDiscoverUseServiceUrl new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1844)
    public void setEwsAutoDiscoverUseServiceUrl(boolean zimbraEwsAutoDiscoverUseServiceUrl) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEwsAutoDiscoverUseServiceUrl, zimbraEwsAutoDiscoverUseServiceUrl ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to use auto discover service url for the ews
     *
     * @param zimbraEwsAutoDiscoverUseServiceUrl new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1844)
    public Map<String,Object> setEwsAutoDiscoverUseServiceUrl(boolean zimbraEwsAutoDiscoverUseServiceUrl, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEwsAutoDiscoverUseServiceUrl, zimbraEwsAutoDiscoverUseServiceUrl ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Flag to use auto discover service url for the ews
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1844)
    public void unsetEwsAutoDiscoverUseServiceUrl() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEwsAutoDiscoverUseServiceUrl, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to use auto discover service url for the ews
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1844)
    public Map<String,Object> unsetEwsAutoDiscoverUseServiceUrl(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraEwsAutoDiscoverUseServiceUrl, "");
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
     * Flag for not populating envelope sender when message is redirected by
     * mail filters. See bug 56566
     *
     * @return zimbraFilterNullEnvelopeSenderForDSNRedirect, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1796)
    public boolean isFilterNullEnvelopeSenderForDSNRedirect() {
        return getBooleanAttr(Provisioning.A_zimbraFilterNullEnvelopeSenderForDSNRedirect, false);
    }

    /**
     * Flag for not populating envelope sender when message is redirected by
     * mail filters. See bug 56566
     *
     * @param zimbraFilterNullEnvelopeSenderForDSNRedirect new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1796)
    public void setFilterNullEnvelopeSenderForDSNRedirect(boolean zimbraFilterNullEnvelopeSenderForDSNRedirect) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFilterNullEnvelopeSenderForDSNRedirect, zimbraFilterNullEnvelopeSenderForDSNRedirect ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag for not populating envelope sender when message is redirected by
     * mail filters. See bug 56566
     *
     * @param zimbraFilterNullEnvelopeSenderForDSNRedirect new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1796)
    public Map<String,Object> setFilterNullEnvelopeSenderForDSNRedirect(boolean zimbraFilterNullEnvelopeSenderForDSNRedirect, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFilterNullEnvelopeSenderForDSNRedirect, zimbraFilterNullEnvelopeSenderForDSNRedirect ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Flag for not populating envelope sender when message is redirected by
     * mail filters. See bug 56566
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1796)
    public void unsetFilterNullEnvelopeSenderForDSNRedirect() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFilterNullEnvelopeSenderForDSNRedirect, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag for not populating envelope sender when message is redirected by
     * mail filters. See bug 56566
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1796)
    public Map<String,Object> unsetFilterNullEnvelopeSenderForDSNRedirect(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFilterNullEnvelopeSenderForDSNRedirect, "");
        return attrs;
    }

    /**
     * Flag to disable the listing of interval when you have no data about
     * account&#039;s free busy interval. One example is user doesn&#039;t
     * have the acl to fetch free busy intervals of this account
     *
     * @return zimbraFreeBusyDisableNoDataStatus, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1797)
    public boolean isFreeBusyDisableNoDataStatus() {
        return getBooleanAttr(Provisioning.A_zimbraFreeBusyDisableNoDataStatus, false);
    }

    /**
     * Flag to disable the listing of interval when you have no data about
     * account&#039;s free busy interval. One example is user doesn&#039;t
     * have the acl to fetch free busy intervals of this account
     *
     * @param zimbraFreeBusyDisableNoDataStatus new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1797)
    public void setFreeBusyDisableNoDataStatus(boolean zimbraFreeBusyDisableNoDataStatus) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreeBusyDisableNoDataStatus, zimbraFreeBusyDisableNoDataStatus ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to disable the listing of interval when you have no data about
     * account&#039;s free busy interval. One example is user doesn&#039;t
     * have the acl to fetch free busy intervals of this account
     *
     * @param zimbraFreeBusyDisableNoDataStatus new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1797)
    public Map<String,Object> setFreeBusyDisableNoDataStatus(boolean zimbraFreeBusyDisableNoDataStatus, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreeBusyDisableNoDataStatus, zimbraFreeBusyDisableNoDataStatus ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Flag to disable the listing of interval when you have no data about
     * account&#039;s free busy interval. One example is user doesn&#039;t
     * have the acl to fetch free busy intervals of this account
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1797)
    public void unsetFreeBusyDisableNoDataStatus() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreeBusyDisableNoDataStatus, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to disable the listing of interval when you have no data about
     * account&#039;s free busy interval. One example is user doesn&#039;t
     * have the acl to fetch free busy intervals of this account
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1797)
    public Map<String,Object> unsetFreeBusyDisableNoDataStatus(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreeBusyDisableNoDataStatus, "");
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
     * Flag to disable the gal sync timeout
     *
     * @return zimbraGalSyncConnectionDisableTimeout, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1845)
    public boolean isGalSyncConnectionDisableTimeout() {
        return getBooleanAttr(Provisioning.A_zimbraGalSyncConnectionDisableTimeout, false);
    }

    /**
     * Flag to disable the gal sync timeout
     *
     * @param zimbraGalSyncConnectionDisableTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1845)
    public void setGalSyncConnectionDisableTimeout(boolean zimbraGalSyncConnectionDisableTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncConnectionDisableTimeout, zimbraGalSyncConnectionDisableTimeout ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to disable the gal sync timeout
     *
     * @param zimbraGalSyncConnectionDisableTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1845)
    public Map<String,Object> setGalSyncConnectionDisableTimeout(boolean zimbraGalSyncConnectionDisableTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncConnectionDisableTimeout, zimbraGalSyncConnectionDisableTimeout ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Flag to disable the gal sync timeout
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1845)
    public void unsetGalSyncConnectionDisableTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncConnectionDisableTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to disable the gal sync timeout
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1845)
    public Map<String,Object> unsetGalSyncConnectionDisableTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncConnectionDisableTimeout, "");
        return attrs;
    }

    /**
     * Sets the maximum galsync mailbox contact item cache.Set value to at
     * least the number of contact items in the galsync mailbox.
     *
     * @return zimbraGalSyncMailboxMailItemCache, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1852)
    public int getGalSyncMailboxMailItemCache() {
        return getIntAttr(Provisioning.A_zimbraGalSyncMailboxMailItemCache, -1);
    }

    /**
     * Sets the maximum galsync mailbox contact item cache.Set value to at
     * least the number of contact items in the galsync mailbox.
     *
     * @param zimbraGalSyncMailboxMailItemCache new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1852)
    public void setGalSyncMailboxMailItemCache(int zimbraGalSyncMailboxMailItemCache) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncMailboxMailItemCache, Integer.toString(zimbraGalSyncMailboxMailItemCache));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Sets the maximum galsync mailbox contact item cache.Set value to at
     * least the number of contact items in the galsync mailbox.
     *
     * @param zimbraGalSyncMailboxMailItemCache new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1852)
    public Map<String,Object> setGalSyncMailboxMailItemCache(int zimbraGalSyncMailboxMailItemCache, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncMailboxMailItemCache, Integer.toString(zimbraGalSyncMailboxMailItemCache));
        return attrs;
    }

    /**
     * Sets the maximum galsync mailbox contact item cache.Set value to at
     * least the number of contact items in the galsync mailbox.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1852)
    public void unsetGalSyncMailboxMailItemCache() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncMailboxMailItemCache, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Sets the maximum galsync mailbox contact item cache.Set value to at
     * least the number of contact items in the galsync mailbox.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1852)
    public Map<String,Object> unsetGalSyncMailboxMailItemCache(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraGalSyncMailboxMailItemCache, "");
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
     * Maximum image size in bytes to resize
     *
     * @return zimbraImapNioEnabled, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1802)
    public boolean isImapNioEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraImapNioEnabled, false);
    }

    /**
     * Maximum image size in bytes to resize
     *
     * @param zimbraImapNioEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1802)
    public void setImapNioEnabled(boolean zimbraImapNioEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapNioEnabled, zimbraImapNioEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum image size in bytes to resize
     *
     * @param zimbraImapNioEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1802)
    public Map<String,Object> setImapNioEnabled(boolean zimbraImapNioEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapNioEnabled, zimbraImapNioEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Maximum image size in bytes to resize
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1802)
    public void unsetImapNioEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapNioEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum image size in bytes to resize
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1802)
    public Map<String,Object> unsetImapNioEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapNioEnabled, "");
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
     * Zimbra index db first term cutoff percentage
     *
     * @return zimbraIndexDbFirstTermCutOffPercentage, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1880)
    public int getIndexDbFirstTermCutOffPercentage() {
        return getIntAttr(Provisioning.A_zimbraIndexDbFirstTermCutOffPercentage, -1);
    }

    /**
     * Zimbra index db first term cutoff percentage
     *
     * @param zimbraIndexDbFirstTermCutOffPercentage new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1880)
    public void setIndexDbFirstTermCutOffPercentage(int zimbraIndexDbFirstTermCutOffPercentage) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexDbFirstTermCutOffPercentage, Integer.toString(zimbraIndexDbFirstTermCutOffPercentage));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Zimbra index db first term cutoff percentage
     *
     * @param zimbraIndexDbFirstTermCutOffPercentage new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1880)
    public Map<String,Object> setIndexDbFirstTermCutOffPercentage(int zimbraIndexDbFirstTermCutOffPercentage, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexDbFirstTermCutOffPercentage, Integer.toString(zimbraIndexDbFirstTermCutOffPercentage));
        return attrs;
    }

    /**
     * Zimbra index db first term cutoff percentage
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1880)
    public void unsetIndexDbFirstTermCutOffPercentage() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexDbFirstTermCutOffPercentage, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Zimbra index db first term cutoff percentage
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1880)
    public Map<String,Object> unsetIndexDbFirstTermCutOffPercentage(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexDbFirstTermCutOffPercentage, "");
        return attrs;
    }

    /**
     * If indexing of an item is failed. It can be tried after the delay
     * specified in seconds
     *
     * @return zimbraIndexDeferredItemsFailureDelay, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1762)
    public int getIndexDeferredItemsFailureDelay() {
        return getIntAttr(Provisioning.A_zimbraIndexDeferredItemsFailureDelay, -1);
    }

    /**
     * If indexing of an item is failed. It can be tried after the delay
     * specified in seconds
     *
     * @param zimbraIndexDeferredItemsFailureDelay new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1762)
    public void setIndexDeferredItemsFailureDelay(int zimbraIndexDeferredItemsFailureDelay) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexDeferredItemsFailureDelay, Integer.toString(zimbraIndexDeferredItemsFailureDelay));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * If indexing of an item is failed. It can be tried after the delay
     * specified in seconds
     *
     * @param zimbraIndexDeferredItemsFailureDelay new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1762)
    public Map<String,Object> setIndexDeferredItemsFailureDelay(int zimbraIndexDeferredItemsFailureDelay, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexDeferredItemsFailureDelay, Integer.toString(zimbraIndexDeferredItemsFailureDelay));
        return attrs;
    }

    /**
     * If indexing of an item is failed. It can be tried after the delay
     * specified in seconds
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1762)
    public void unsetIndexDeferredItemsFailureDelay() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexDeferredItemsFailureDelay, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * If indexing of an item is failed. It can be tried after the delay
     * specified in seconds
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1762)
    public Map<String,Object> unsetIndexDeferredItemsFailureDelay(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexDeferredItemsFailureDelay, "");
        return attrs;
    }

    /**
     * flag to enable and disable the database hints.
     *
     * @return zimbraIndexDisableDatabaseHints, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1881)
    public boolean isIndexDisableDatabaseHints() {
        return getBooleanAttr(Provisioning.A_zimbraIndexDisableDatabaseHints, false);
    }

    /**
     * flag to enable and disable the database hints.
     *
     * @param zimbraIndexDisableDatabaseHints new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1881)
    public void setIndexDisableDatabaseHints(boolean zimbraIndexDisableDatabaseHints) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexDisableDatabaseHints, zimbraIndexDisableDatabaseHints ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * flag to enable and disable the database hints.
     *
     * @param zimbraIndexDisableDatabaseHints new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1881)
    public Map<String,Object> setIndexDisableDatabaseHints(boolean zimbraIndexDisableDatabaseHints, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexDisableDatabaseHints, zimbraIndexDisableDatabaseHints ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * flag to enable and disable the database hints.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1881)
    public void unsetIndexDisableDatabaseHints() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexDisableDatabaseHints, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * flag to enable and disable the database hints.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1881)
    public Map<String,Object> unsetIndexDisableDatabaseHints(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexDisableDatabaseHints, "");
        return attrs;
    }

    /**
     * Disable perf counters for lucene indexer
     *
     * @return zimbraIndexDisablePerfCounters, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1791)
    public boolean isIndexDisablePerfCounters() {
        return getBooleanAttr(Provisioning.A_zimbraIndexDisablePerfCounters, false);
    }

    /**
     * Disable perf counters for lucene indexer
     *
     * @param zimbraIndexDisablePerfCounters new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1791)
    public void setIndexDisablePerfCounters(boolean zimbraIndexDisablePerfCounters) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexDisablePerfCounters, zimbraIndexDisablePerfCounters ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Disable perf counters for lucene indexer
     *
     * @param zimbraIndexDisablePerfCounters new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1791)
    public Map<String,Object> setIndexDisablePerfCounters(boolean zimbraIndexDisablePerfCounters, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexDisablePerfCounters, zimbraIndexDisablePerfCounters ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Disable perf counters for lucene indexer
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1791)
    public void unsetIndexDisablePerfCounters() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexDisablePerfCounters, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Disable perf counters for lucene indexer
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1791)
    public Map<String,Object> unsetIndexDisablePerfCounters(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexDisablePerfCounters, "");
        return attrs;
    }

    /**
     * Lucene index io implementation
     *
     * <p>Valid values: [mmap, nio, simple]
     *
     * @return zimbraIndexLuceneIoImpl, or null if unset and/or has invalid value
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1811)
    public ZAttrProvisioning.IndexLuceneIoImpl getIndexLuceneIoImpl() {
        try { String v = getAttr(Provisioning.A_zimbraIndexLuceneIoImpl); return v == null ? null : ZAttrProvisioning.IndexLuceneIoImpl.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * Lucene index io implementation
     *
     * <p>Valid values: [mmap, nio, simple]
     *
     * @return zimbraIndexLuceneIoImpl, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1811)
    public String getIndexLuceneIoImplAsString() {
        return getAttr(Provisioning.A_zimbraIndexLuceneIoImpl, null);
    }

    /**
     * Lucene index io implementation
     *
     * <p>Valid values: [mmap, nio, simple]
     *
     * @param zimbraIndexLuceneIoImpl new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1811)
    public void setIndexLuceneIoImpl(ZAttrProvisioning.IndexLuceneIoImpl zimbraIndexLuceneIoImpl) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexLuceneIoImpl, zimbraIndexLuceneIoImpl.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Lucene index io implementation
     *
     * <p>Valid values: [mmap, nio, simple]
     *
     * @param zimbraIndexLuceneIoImpl new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1811)
    public Map<String,Object> setIndexLuceneIoImpl(ZAttrProvisioning.IndexLuceneIoImpl zimbraIndexLuceneIoImpl, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexLuceneIoImpl, zimbraIndexLuceneIoImpl.toString());
        return attrs;
    }

    /**
     * Lucene index io implementation
     *
     * <p>Valid values: [mmap, nio, simple]
     *
     * @param zimbraIndexLuceneIoImpl new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1811)
    public void setIndexLuceneIoImplAsString(String zimbraIndexLuceneIoImpl) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexLuceneIoImpl, zimbraIndexLuceneIoImpl);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Lucene index io implementation
     *
     * <p>Valid values: [mmap, nio, simple]
     *
     * @param zimbraIndexLuceneIoImpl new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1811)
    public Map<String,Object> setIndexLuceneIoImplAsString(String zimbraIndexLuceneIoImpl, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexLuceneIoImpl, zimbraIndexLuceneIoImpl);
        return attrs;
    }

    /**
     * Lucene index io implementation
     *
     * <p>Valid values: [mmap, nio, simple]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1811)
    public void unsetIndexLuceneIoImpl() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexLuceneIoImpl, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Lucene index io implementation
     *
     * <p>Valid values: [mmap, nio, simple]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1811)
    public Map<String,Object> unsetIndexLuceneIoImpl(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexLuceneIoImpl, "");
        return attrs;
    }

    /**
     * Determines how often segment indices are merged by addDocument
     * function. With smaller values, less RAM is used while indexing, and
     * searches are faster, but indexing speed is slower. With larger values,
     * more RAM is used during indexing, and while searches is slower,
     * indexing is faster. Thus larger values greater than 10 are best for
     * batch index creation, and smaller values less than 10 for indices that
     * are interactively maintained
     *
     * @return zimbraIndexLuceneMergeFactor, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1812)
    public int getIndexLuceneMergeFactor() {
        return getIntAttr(Provisioning.A_zimbraIndexLuceneMergeFactor, -1);
    }

    /**
     * Determines how often segment indices are merged by addDocument
     * function. With smaller values, less RAM is used while indexing, and
     * searches are faster, but indexing speed is slower. With larger values,
     * more RAM is used during indexing, and while searches is slower,
     * indexing is faster. Thus larger values greater than 10 are best for
     * batch index creation, and smaller values less than 10 for indices that
     * are interactively maintained
     *
     * @param zimbraIndexLuceneMergeFactor new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1812)
    public void setIndexLuceneMergeFactor(int zimbraIndexLuceneMergeFactor) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexLuceneMergeFactor, Integer.toString(zimbraIndexLuceneMergeFactor));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Determines how often segment indices are merged by addDocument
     * function. With smaller values, less RAM is used while indexing, and
     * searches are faster, but indexing speed is slower. With larger values,
     * more RAM is used during indexing, and while searches is slower,
     * indexing is faster. Thus larger values greater than 10 are best for
     * batch index creation, and smaller values less than 10 for indices that
     * are interactively maintained
     *
     * @param zimbraIndexLuceneMergeFactor new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1812)
    public Map<String,Object> setIndexLuceneMergeFactor(int zimbraIndexLuceneMergeFactor, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexLuceneMergeFactor, Integer.toString(zimbraIndexLuceneMergeFactor));
        return attrs;
    }

    /**
     * Determines how often segment indices are merged by addDocument
     * function. With smaller values, less RAM is used while indexing, and
     * searches are faster, but indexing speed is slower. With larger values,
     * more RAM is used during indexing, and while searches is slower,
     * indexing is faster. Thus larger values greater than 10 are best for
     * batch index creation, and smaller values less than 10 for indices that
     * are interactively maintained
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1812)
    public void unsetIndexLuceneMergeFactor() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexLuceneMergeFactor, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Determines how often segment indices are merged by addDocument
     * function. With smaller values, less RAM is used while indexing, and
     * searches are faster, but indexing speed is slower. With larger values,
     * more RAM is used during indexing, and while searches is slower,
     * indexing is faster. Thus larger values greater than 10 are best for
     * batch index creation, and smaller values less than 10 for indices that
     * are interactively maintained
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1812)
    public Map<String,Object> unsetIndexLuceneMergeFactor(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexLuceneMergeFactor, "");
        return attrs;
    }

    /**
     * When set to TRUE, server will commit changes to Solr after every
     * update request. When set to FALSE, Solr commits will be performed
     * according to Solr configuration. Set to TRUE for automated testing.
     * Recommended production setting is FALSE.
     *
     * @return zimbraIndexManualCommit, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1813)
    public boolean isIndexManualCommit() {
        return getBooleanAttr(Provisioning.A_zimbraIndexManualCommit, false);
    }

    /**
     * When set to TRUE, server will commit changes to Solr after every
     * update request. When set to FALSE, Solr commits will be performed
     * according to Solr configuration. Set to TRUE for automated testing.
     * Recommended production setting is FALSE.
     *
     * @param zimbraIndexManualCommit new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1813)
    public void setIndexManualCommit(boolean zimbraIndexManualCommit) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexManualCommit, zimbraIndexManualCommit ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * When set to TRUE, server will commit changes to Solr after every
     * update request. When set to FALSE, Solr commits will be performed
     * according to Solr configuration. Set to TRUE for automated testing.
     * Recommended production setting is FALSE.
     *
     * @param zimbraIndexManualCommit new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1813)
    public Map<String,Object> setIndexManualCommit(boolean zimbraIndexManualCommit, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexManualCommit, zimbraIndexManualCommit ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * When set to TRUE, server will commit changes to Solr after every
     * update request. When set to FALSE, Solr commits will be performed
     * according to Solr configuration. Set to TRUE for automated testing.
     * Recommended production setting is FALSE.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1813)
    public void unsetIndexManualCommit() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexManualCommit, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * When set to TRUE, server will commit changes to Solr after every
     * update request. When set to FALSE, Solr commits will be performed
     * according to Solr configuration. Set to TRUE for automated testing.
     * Recommended production setting is FALSE.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1813)
    public Map<String,Object> unsetIndexManualCommit(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexManualCommit, "");
        return attrs;
    }

    /**
     * When batching index operations into a Mailbox transaction, the maximum
     * aggregate size of items that we will allow in a single transaction.
     *
     * @return zimbraIndexMaxTransactionBytes, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1814)
    public int getIndexMaxTransactionBytes() {
        return getIntAttr(Provisioning.A_zimbraIndexMaxTransactionBytes, -1);
    }

    /**
     * When batching index operations into a Mailbox transaction, the maximum
     * aggregate size of items that we will allow in a single transaction.
     *
     * @param zimbraIndexMaxTransactionBytes new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1814)
    public void setIndexMaxTransactionBytes(int zimbraIndexMaxTransactionBytes) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexMaxTransactionBytes, Integer.toString(zimbraIndexMaxTransactionBytes));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * When batching index operations into a Mailbox transaction, the maximum
     * aggregate size of items that we will allow in a single transaction.
     *
     * @param zimbraIndexMaxTransactionBytes new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1814)
    public Map<String,Object> setIndexMaxTransactionBytes(int zimbraIndexMaxTransactionBytes, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexMaxTransactionBytes, Integer.toString(zimbraIndexMaxTransactionBytes));
        return attrs;
    }

    /**
     * When batching index operations into a Mailbox transaction, the maximum
     * aggregate size of items that we will allow in a single transaction.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1814)
    public void unsetIndexMaxTransactionBytes() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexMaxTransactionBytes, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * When batching index operations into a Mailbox transaction, the maximum
     * aggregate size of items that we will allow in a single transaction.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1814)
    public Map<String,Object> unsetIndexMaxTransactionBytes(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexMaxTransactionBytes, "");
        return attrs;
    }

    /**
     * When batching index operations, the maximum number of mail items we
     * allow in a single transaction.
     *
     * @return zimbraIndexMaxTransactionItems, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1815)
    public int getIndexMaxTransactionItems() {
        return getIntAttr(Provisioning.A_zimbraIndexMaxTransactionItems, -1);
    }

    /**
     * When batching index operations, the maximum number of mail items we
     * allow in a single transaction.
     *
     * @param zimbraIndexMaxTransactionItems new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1815)
    public void setIndexMaxTransactionItems(int zimbraIndexMaxTransactionItems) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexMaxTransactionItems, Integer.toString(zimbraIndexMaxTransactionItems));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * When batching index operations, the maximum number of mail items we
     * allow in a single transaction.
     *
     * @param zimbraIndexMaxTransactionItems new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1815)
    public Map<String,Object> setIndexMaxTransactionItems(int zimbraIndexMaxTransactionItems, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexMaxTransactionItems, Integer.toString(zimbraIndexMaxTransactionItems));
        return attrs;
    }

    /**
     * When batching index operations, the maximum number of mail items we
     * allow in a single transaction.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1815)
    public void unsetIndexMaxTransactionItems() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexMaxTransactionItems, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * When batching index operations, the maximum number of mail items we
     * allow in a single transaction.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1815)
    public Map<String,Object> unsetIndexMaxTransactionItems(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexMaxTransactionItems, "");
        return attrs;
    }

    /**
     * Maximum number of threads for re-index. Re-index threads are not
     * pooled.
     *
     * @return zimbraIndexReIndexThreads, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1862)
    public int getIndexReIndexThreads() {
        return getIntAttr(Provisioning.A_zimbraIndexReIndexThreads, -1);
    }

    /**
     * Maximum number of threads for re-index. Re-index threads are not
     * pooled.
     *
     * @param zimbraIndexReIndexThreads new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1862)
    public void setIndexReIndexThreads(int zimbraIndexReIndexThreads) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReIndexThreads, Integer.toString(zimbraIndexReIndexThreads));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of threads for re-index. Re-index threads are not
     * pooled.
     *
     * @param zimbraIndexReIndexThreads new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1862)
    public Map<String,Object> setIndexReIndexThreads(int zimbraIndexReIndexThreads, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReIndexThreads, Integer.toString(zimbraIndexReIndexThreads));
        return attrs;
    }

    /**
     * Maximum number of threads for re-index. Re-index threads are not
     * pooled.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1862)
    public void unsetIndexReIndexThreads() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReIndexThreads, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of threads for re-index. Re-index threads are not
     * pooled.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1862)
    public Map<String,Object> unsetIndexReIndexThreads(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReIndexThreads, "");
        return attrs;
    }

    /**
     * Maximum number of IndexReaders in the search index cache
     *
     * @return zimbraIndexReaderCacheSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1790)
    public int getIndexReaderCacheSize() {
        return getIntAttr(Provisioning.A_zimbraIndexReaderCacheSize, -1);
    }

    /**
     * Maximum number of IndexReaders in the search index cache
     *
     * @param zimbraIndexReaderCacheSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1790)
    public void setIndexReaderCacheSize(int zimbraIndexReaderCacheSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReaderCacheSize, Integer.toString(zimbraIndexReaderCacheSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of IndexReaders in the search index cache
     *
     * @param zimbraIndexReaderCacheSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1790)
    public Map<String,Object> setIndexReaderCacheSize(int zimbraIndexReaderCacheSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReaderCacheSize, Integer.toString(zimbraIndexReaderCacheSize));
        return attrs;
    }

    /**
     * Maximum number of IndexReaders in the search index cache
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1790)
    public void unsetIndexReaderCacheSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReaderCacheSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of IndexReaders in the search index cache
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1790)
    public Map<String,Object> unsetIndexReaderCacheSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReaderCacheSize, "");
        return attrs;
    }

    /**
     * TTL in seconds for index reader cache. If idle for longer than this
     * value (seconds) then remove the IndexReader from the cache.. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * <p>Use getIndexReaderCacheTtlAsString to access value as a string.
     *
     * @see #getIndexReaderCacheTtlAsString()
     *
     * @return zimbraIndexReaderCacheTtl in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1817)
    public long getIndexReaderCacheTtl() {
        return getTimeInterval(Provisioning.A_zimbraIndexReaderCacheTtl, -1L);
    }

    /**
     * TTL in seconds for index reader cache. If idle for longer than this
     * value (seconds) then remove the IndexReader from the cache.. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @return zimbraIndexReaderCacheTtl, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1817)
    public String getIndexReaderCacheTtlAsString() {
        return getAttr(Provisioning.A_zimbraIndexReaderCacheTtl, null);
    }

    /**
     * TTL in seconds for index reader cache. If idle for longer than this
     * value (seconds) then remove the IndexReader from the cache.. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraIndexReaderCacheTtl new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1817)
    public void setIndexReaderCacheTtl(String zimbraIndexReaderCacheTtl) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReaderCacheTtl, zimbraIndexReaderCacheTtl);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * TTL in seconds for index reader cache. If idle for longer than this
     * value (seconds) then remove the IndexReader from the cache.. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraIndexReaderCacheTtl new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1817)
    public Map<String,Object> setIndexReaderCacheTtl(String zimbraIndexReaderCacheTtl, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReaderCacheTtl, zimbraIndexReaderCacheTtl);
        return attrs;
    }

    /**
     * TTL in seconds for index reader cache. If idle for longer than this
     * value (seconds) then remove the IndexReader from the cache.. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1817)
    public void unsetIndexReaderCacheTtl() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReaderCacheTtl, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * TTL in seconds for index reader cache. If idle for longer than this
     * value (seconds) then remove the IndexReader from the cache.. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1817)
    public Map<String,Object> unsetIndexReaderCacheTtl(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReaderCacheTtl, "");
        return attrs;
    }

    /**
     * The maximum number of IndexReaders in the GAL search index cache. The
     * GAL sync account Lucene index is cached separately with no automatic
     * eviction.
     *
     * @return zimbraIndexReaderGalSyncCacheSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1846)
    public int getIndexReaderGalSyncCacheSize() {
        return getIntAttr(Provisioning.A_zimbraIndexReaderGalSyncCacheSize, -1);
    }

    /**
     * The maximum number of IndexReaders in the GAL search index cache. The
     * GAL sync account Lucene index is cached separately with no automatic
     * eviction.
     *
     * @param zimbraIndexReaderGalSyncCacheSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1846)
    public void setIndexReaderGalSyncCacheSize(int zimbraIndexReaderGalSyncCacheSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReaderGalSyncCacheSize, Integer.toString(zimbraIndexReaderGalSyncCacheSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The maximum number of IndexReaders in the GAL search index cache. The
     * GAL sync account Lucene index is cached separately with no automatic
     * eviction.
     *
     * @param zimbraIndexReaderGalSyncCacheSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1846)
    public Map<String,Object> setIndexReaderGalSyncCacheSize(int zimbraIndexReaderGalSyncCacheSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReaderGalSyncCacheSize, Integer.toString(zimbraIndexReaderGalSyncCacheSize));
        return attrs;
    }

    /**
     * The maximum number of IndexReaders in the GAL search index cache. The
     * GAL sync account Lucene index is cached separately with no automatic
     * eviction.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1846)
    public void unsetIndexReaderGalSyncCacheSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReaderGalSyncCacheSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The maximum number of IndexReaders in the GAL search index cache. The
     * GAL sync account Lucene index is cached separately with no automatic
     * eviction.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1846)
    public Map<String,Object> unsetIndexReaderGalSyncCacheSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexReaderGalSyncCacheSize, "");
        return attrs;
    }

    /**
     * Tagged item count join query cut off
     *
     * @return zimbraIndexTaggedItemCountJoinQueryCutoff, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1882)
    public int getIndexTaggedItemCountJoinQueryCutoff() {
        return getIntAttr(Provisioning.A_zimbraIndexTaggedItemCountJoinQueryCutoff, -1);
    }

    /**
     * Tagged item count join query cut off
     *
     * @param zimbraIndexTaggedItemCountJoinQueryCutoff new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1882)
    public void setIndexTaggedItemCountJoinQueryCutoff(int zimbraIndexTaggedItemCountJoinQueryCutoff) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexTaggedItemCountJoinQueryCutoff, Integer.toString(zimbraIndexTaggedItemCountJoinQueryCutoff));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Tagged item count join query cut off
     *
     * @param zimbraIndexTaggedItemCountJoinQueryCutoff new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1882)
    public Map<String,Object> setIndexTaggedItemCountJoinQueryCutoff(int zimbraIndexTaggedItemCountJoinQueryCutoff, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexTaggedItemCountJoinQueryCutoff, Integer.toString(zimbraIndexTaggedItemCountJoinQueryCutoff));
        return attrs;
    }

    /**
     * Tagged item count join query cut off
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1882)
    public void unsetIndexTaggedItemCountJoinQueryCutoff() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexTaggedItemCountJoinQueryCutoff, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Tagged item count join query cut off
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1882)
    public Map<String,Object> unsetIndexTaggedItemCountJoinQueryCutoff(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexTaggedItemCountJoinQueryCutoff, "");
        return attrs;
    }

    /**
     * Minimum allowable timeout (seconds) specified to NoOpRequest
     *
     * @return zimbraIndexTermsCacheSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1871)
    public int getIndexTermsCacheSize() {
        return getIntAttr(Provisioning.A_zimbraIndexTermsCacheSize, -1);
    }

    /**
     * Minimum allowable timeout (seconds) specified to NoOpRequest
     *
     * @param zimbraIndexTermsCacheSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1871)
    public void setIndexTermsCacheSize(int zimbraIndexTermsCacheSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexTermsCacheSize, Integer.toString(zimbraIndexTermsCacheSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Minimum allowable timeout (seconds) specified to NoOpRequest
     *
     * @param zimbraIndexTermsCacheSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1871)
    public Map<String,Object> setIndexTermsCacheSize(int zimbraIndexTermsCacheSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexTermsCacheSize, Integer.toString(zimbraIndexTermsCacheSize));
        return attrs;
    }

    /**
     * Minimum allowable timeout (seconds) specified to NoOpRequest
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1871)
    public void unsetIndexTermsCacheSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexTermsCacheSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Minimum allowable timeout (seconds) specified to NoOpRequest
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1871)
    public Map<String,Object> unsetIndexTermsCacheSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexTermsCacheSize, "");
        return attrs;
    }

    /**
     * No of threads used by lucene indexer
     *
     * @return zimbraIndexThreads, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1781)
    public int getIndexThreads() {
        return getIntAttr(Provisioning.A_zimbraIndexThreads, -1);
    }

    /**
     * No of threads used by lucene indexer
     *
     * @param zimbraIndexThreads new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1781)
    public void setIndexThreads(int zimbraIndexThreads) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexThreads, Integer.toString(zimbraIndexThreads));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * No of threads used by lucene indexer
     *
     * @param zimbraIndexThreads new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1781)
    public Map<String,Object> setIndexThreads(int zimbraIndexThreads, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexThreads, Integer.toString(zimbraIndexThreads));
        return attrs;
    }

    /**
     * No of threads used by lucene indexer
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1781)
    public void unsetIndexThreads() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexThreads, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * No of threads used by lucene indexer
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1781)
    public Map<String,Object> unsetIndexThreads(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexThreads, "");
        return attrs;
    }

    /**
     * Maximum wildcard expansions for each individual term in the query.
     *
     * @return zimbraIndexWildcardMaxTermsExpanded, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1847)
    public int getIndexWildcardMaxTermsExpanded() {
        return getIntAttr(Provisioning.A_zimbraIndexWildcardMaxTermsExpanded, -1);
    }

    /**
     * Maximum wildcard expansions for each individual term in the query.
     *
     * @param zimbraIndexWildcardMaxTermsExpanded new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1847)
    public void setIndexWildcardMaxTermsExpanded(int zimbraIndexWildcardMaxTermsExpanded) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexWildcardMaxTermsExpanded, Integer.toString(zimbraIndexWildcardMaxTermsExpanded));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum wildcard expansions for each individual term in the query.
     *
     * @param zimbraIndexWildcardMaxTermsExpanded new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1847)
    public Map<String,Object> setIndexWildcardMaxTermsExpanded(int zimbraIndexWildcardMaxTermsExpanded, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexWildcardMaxTermsExpanded, Integer.toString(zimbraIndexWildcardMaxTermsExpanded));
        return attrs;
    }

    /**
     * Maximum wildcard expansions for each individual term in the query.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1847)
    public void unsetIndexWildcardMaxTermsExpanded() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexWildcardMaxTermsExpanded, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum wildcard expansions for each individual term in the query.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1847)
    public Map<String,Object> unsetIndexWildcardMaxTermsExpanded(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexWildcardMaxTermsExpanded, "");
        return attrs;
    }

    /**
     * Maximum number of items that can be held in memory while queued for
     * indexing
     *
     * @return zimbraIndexingQueueMaxSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1786)
    public int getIndexingQueueMaxSize() {
        return getIntAttr(Provisioning.A_zimbraIndexingQueueMaxSize, -1);
    }

    /**
     * Maximum number of items that can be held in memory while queued for
     * indexing
     *
     * @param zimbraIndexingQueueMaxSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1786)
    public void setIndexingQueueMaxSize(int zimbraIndexingQueueMaxSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexingQueueMaxSize, Integer.toString(zimbraIndexingQueueMaxSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of items that can be held in memory while queued for
     * indexing
     *
     * @param zimbraIndexingQueueMaxSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1786)
    public Map<String,Object> setIndexingQueueMaxSize(int zimbraIndexingQueueMaxSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexingQueueMaxSize, Integer.toString(zimbraIndexingQueueMaxSize));
        return attrs;
    }

    /**
     * Maximum number of items that can be held in memory while queued for
     * indexing
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1786)
    public void unsetIndexingQueueMaxSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexingQueueMaxSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of items that can be held in memory while queued for
     * indexing
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1786)
    public Map<String,Object> unsetIndexingQueueMaxSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexingQueueMaxSize, "");
        return attrs;
    }

    /**
     * Class that implements access to shared indexing queue. When this
     * attribute is empty, servers will send documents to Solr for indexing
     * as soon as documents arrive.
     *
     * @return zimbraIndexingQueueProvider, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1833)
    public String getIndexingQueueProvider() {
        return getAttr(Provisioning.A_zimbraIndexingQueueProvider, null);
    }

    /**
     * Class that implements access to shared indexing queue. When this
     * attribute is empty, servers will send documents to Solr for indexing
     * as soon as documents arrive.
     *
     * @param zimbraIndexingQueueProvider new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1833)
    public void setIndexingQueueProvider(String zimbraIndexingQueueProvider) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexingQueueProvider, zimbraIndexingQueueProvider);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Class that implements access to shared indexing queue. When this
     * attribute is empty, servers will send documents to Solr for indexing
     * as soon as documents arrive.
     *
     * @param zimbraIndexingQueueProvider new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1833)
    public Map<String,Object> setIndexingQueueProvider(String zimbraIndexingQueueProvider, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexingQueueProvider, zimbraIndexingQueueProvider);
        return attrs;
    }

    /**
     * Class that implements access to shared indexing queue. When this
     * attribute is empty, servers will send documents to Solr for indexing
     * as soon as documents arrive.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1833)
    public void unsetIndexingQueueProvider() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexingQueueProvider, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Class that implements access to shared indexing queue. When this
     * attribute is empty, servers will send documents to Solr for indexing
     * as soon as documents arrive.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1833)
    public Map<String,Object> unsetIndexingQueueProvider(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIndexingQueueProvider, "");
        return attrs;
    }

    /**
     * Enabled Kerberos debugging
     *
     * @return zimbraKerberosDebugEnabled, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1798)
    public boolean isKerberosDebugEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraKerberosDebugEnabled, false);
    }

    /**
     * Enabled Kerberos debugging
     *
     * @param zimbraKerberosDebugEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1798)
    public void setKerberosDebugEnabled(boolean zimbraKerberosDebugEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraKerberosDebugEnabled, zimbraKerberosDebugEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enabled Kerberos debugging
     *
     * @param zimbraKerberosDebugEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1798)
    public Map<String,Object> setKerberosDebugEnabled(boolean zimbraKerberosDebugEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraKerberosDebugEnabled, zimbraKerberosDebugEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Enabled Kerberos debugging
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1798)
    public void unsetKerberosDebugEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraKerberosDebugEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enabled Kerberos debugging
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1798)
    public Map<String,Object> unsetKerberosDebugEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraKerberosDebugEnabled, "");
        return attrs;
    }

    /**
     * Flag to get Kerobos Service Principal from Interface Address
     *
     * @return zimbraKerobosServicePrincipalFromInterfaceAddress, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1799)
    public boolean isKerobosServicePrincipalFromInterfaceAddress() {
        return getBooleanAttr(Provisioning.A_zimbraKerobosServicePrincipalFromInterfaceAddress, false);
    }

    /**
     * Flag to get Kerobos Service Principal from Interface Address
     *
     * @param zimbraKerobosServicePrincipalFromInterfaceAddress new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1799)
    public void setKerobosServicePrincipalFromInterfaceAddress(boolean zimbraKerobosServicePrincipalFromInterfaceAddress) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraKerobosServicePrincipalFromInterfaceAddress, zimbraKerobosServicePrincipalFromInterfaceAddress ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to get Kerobos Service Principal from Interface Address
     *
     * @param zimbraKerobosServicePrincipalFromInterfaceAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1799)
    public Map<String,Object> setKerobosServicePrincipalFromInterfaceAddress(boolean zimbraKerobosServicePrincipalFromInterfaceAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraKerobosServicePrincipalFromInterfaceAddress, zimbraKerobosServicePrincipalFromInterfaceAddress ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Flag to get Kerobos Service Principal from Interface Address
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1799)
    public void unsetKerobosServicePrincipalFromInterfaceAddress() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraKerobosServicePrincipalFromInterfaceAddress, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to get Kerobos Service Principal from Interface Address
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1799)
    public Map<String,Object> unsetKerobosServicePrincipalFromInterfaceAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraKerobosServicePrincipalFromInterfaceAddress, "");
        return attrs;
    }

    /**
     * Max line length for in the lmtp message as per the rfc 822
     *
     * @return zimbraLmtpMaxLineLength, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1848)
    public int getLmtpMaxLineLength() {
        return getIntAttr(Provisioning.A_zimbraLmtpMaxLineLength, -1);
    }

    /**
     * Max line length for in the lmtp message as per the rfc 822
     *
     * @param zimbraLmtpMaxLineLength new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1848)
    public void setLmtpMaxLineLength(int zimbraLmtpMaxLineLength) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpMaxLineLength, Integer.toString(zimbraLmtpMaxLineLength));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Max line length for in the lmtp message as per the rfc 822
     *
     * @param zimbraLmtpMaxLineLength new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1848)
    public Map<String,Object> setLmtpMaxLineLength(int zimbraLmtpMaxLineLength, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpMaxLineLength, Integer.toString(zimbraLmtpMaxLineLength));
        return attrs;
    }

    /**
     * Max line length for in the lmtp message as per the rfc 822
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1848)
    public void unsetLmtpMaxLineLength() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpMaxLineLength, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Max line length for in the lmtp message as per the rfc 822
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1848)
    public Map<String,Object> unsetLmtpMaxLineLength(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpMaxLineLength, "");
        return attrs;
    }

    /**
     * Lmtp ip throttle. Maximum lmtp requests per second per account.
     *
     * @return zimbraLmtpThrottleIpLimit, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1800)
    public int getLmtpThrottleIpLimit() {
        return getIntAttr(Provisioning.A_zimbraLmtpThrottleIpLimit, -1);
    }

    /**
     * Lmtp ip throttle. Maximum lmtp requests per second per account.
     *
     * @param zimbraLmtpThrottleIpLimit new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1800)
    public void setLmtpThrottleIpLimit(int zimbraLmtpThrottleIpLimit) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpThrottleIpLimit, Integer.toString(zimbraLmtpThrottleIpLimit));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Lmtp ip throttle. Maximum lmtp requests per second per account.
     *
     * @param zimbraLmtpThrottleIpLimit new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1800)
    public Map<String,Object> setLmtpThrottleIpLimit(int zimbraLmtpThrottleIpLimit, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpThrottleIpLimit, Integer.toString(zimbraLmtpThrottleIpLimit));
        return attrs;
    }

    /**
     * Lmtp ip throttle. Maximum lmtp requests per second per account.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1800)
    public void unsetLmtpThrottleIpLimit() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpThrottleIpLimit, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Lmtp ip throttle. Maximum lmtp requests per second per account.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1800)
    public Map<String,Object> unsetLmtpThrottleIpLimit(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpThrottleIpLimit, "");
        return attrs;
    }

    /**
     * If true, validate the content of incoming LMTP messages
     *
     * @return zimbraLmtpValidateMessages, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1849)
    public boolean isLmtpValidateMessages() {
        return getBooleanAttr(Provisioning.A_zimbraLmtpValidateMessages, false);
    }

    /**
     * If true, validate the content of incoming LMTP messages
     *
     * @param zimbraLmtpValidateMessages new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1849)
    public void setLmtpValidateMessages(boolean zimbraLmtpValidateMessages) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpValidateMessages, zimbraLmtpValidateMessages ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * If true, validate the content of incoming LMTP messages
     *
     * @param zimbraLmtpValidateMessages new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1849)
    public Map<String,Object> setLmtpValidateMessages(boolean zimbraLmtpValidateMessages, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpValidateMessages, zimbraLmtpValidateMessages ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * If true, validate the content of incoming LMTP messages
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1849)
    public void unsetLmtpValidateMessages() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpValidateMessages, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * If true, validate the content of incoming LMTP messages
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1849)
    public Map<String,Object> unsetLmtpValidateMessages(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLmtpValidateMessages, "");
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
     * Flag to enable notes
     *
     * @return zimbraMailNotesEnabled, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1804)
    public boolean isMailNotesEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraMailNotesEnabled, false);
    }

    /**
     * Flag to enable notes
     *
     * @param zimbraMailNotesEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1804)
    public void setMailNotesEnabled(boolean zimbraMailNotesEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailNotesEnabled, zimbraMailNotesEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable notes
     *
     * @param zimbraMailNotesEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1804)
    public Map<String,Object> setMailNotesEnabled(boolean zimbraMailNotesEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailNotesEnabled, zimbraMailNotesEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Flag to enable notes
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1804)
    public void unsetMailNotesEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailNotesEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable notes
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1804)
    public Map<String,Object> unsetMailNotesEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailNotesEnabled, "");
        return attrs;
    }

    /**
     * Disable timeout for archive formatter.Introduced in bug 56458. This is
     * a workaround for an issue in Jetty 6.1.22.zc6m when we upgrade we
     * should re-evaluate/remove these settings and the code that uses them.
     *
     * @return zimbraMailboxArchiveFormatterDisableTimeout, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1991)
    public boolean isMailboxArchiveFormatterDisableTimeout() {
        return getBooleanAttr(Provisioning.A_zimbraMailboxArchiveFormatterDisableTimeout, false);
    }

    /**
     * Disable timeout for archive formatter.Introduced in bug 56458. This is
     * a workaround for an issue in Jetty 6.1.22.zc6m when we upgrade we
     * should re-evaluate/remove these settings and the code that uses them.
     *
     * @param zimbraMailboxArchiveFormatterDisableTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1991)
    public void setMailboxArchiveFormatterDisableTimeout(boolean zimbraMailboxArchiveFormatterDisableTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxArchiveFormatterDisableTimeout, zimbraMailboxArchiveFormatterDisableTimeout ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Disable timeout for archive formatter.Introduced in bug 56458. This is
     * a workaround for an issue in Jetty 6.1.22.zc6m when we upgrade we
     * should re-evaluate/remove these settings and the code that uses them.
     *
     * @param zimbraMailboxArchiveFormatterDisableTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1991)
    public Map<String,Object> setMailboxArchiveFormatterDisableTimeout(boolean zimbraMailboxArchiveFormatterDisableTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxArchiveFormatterDisableTimeout, zimbraMailboxArchiveFormatterDisableTimeout ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Disable timeout for archive formatter.Introduced in bug 56458. This is
     * a workaround for an issue in Jetty 6.1.22.zc6m when we upgrade we
     * should re-evaluate/remove these settings and the code that uses them.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1991)
    public void unsetMailboxArchiveFormatterDisableTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxArchiveFormatterDisableTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Disable timeout for archive formatter.Introduced in bug 56458. This is
     * a workaround for an issue in Jetty 6.1.22.zc6m when we upgrade we
     * should re-evaluate/remove these settings and the code that uses them.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1991)
    public Map<String,Object> unsetMailboxArchiveFormatterDisableTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxArchiveFormatterDisableTimeout, "");
        return attrs;
    }

    /**
     * Archive formatter search chunk size in bytes
     *
     * @return zimbraMailboxArchiveFormatterSearchChunkSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1993)
    public int getMailboxArchiveFormatterSearchChunkSize() {
        return getIntAttr(Provisioning.A_zimbraMailboxArchiveFormatterSearchChunkSize, -1);
    }

    /**
     * Archive formatter search chunk size in bytes
     *
     * @param zimbraMailboxArchiveFormatterSearchChunkSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1993)
    public void setMailboxArchiveFormatterSearchChunkSize(int zimbraMailboxArchiveFormatterSearchChunkSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxArchiveFormatterSearchChunkSize, Integer.toString(zimbraMailboxArchiveFormatterSearchChunkSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Archive formatter search chunk size in bytes
     *
     * @param zimbraMailboxArchiveFormatterSearchChunkSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1993)
    public Map<String,Object> setMailboxArchiveFormatterSearchChunkSize(int zimbraMailboxArchiveFormatterSearchChunkSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxArchiveFormatterSearchChunkSize, Integer.toString(zimbraMailboxArchiveFormatterSearchChunkSize));
        return attrs;
    }

    /**
     * Archive formatter search chunk size in bytes
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1993)
    public void unsetMailboxArchiveFormatterSearchChunkSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxArchiveFormatterSearchChunkSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Archive formatter search chunk size in bytes
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1993)
    public Map<String,Object> unsetMailboxArchiveFormatterSearchChunkSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxArchiveFormatterSearchChunkSize, "");
        return attrs;
    }

    /**
     * The frequency, in number of changes, at which a mailbox&#039;s
     * ZIMBRA.MAILBOX.CHANGE_CHECKPOINT highwater change value is written.
     *
     * @return zimbraMailboxChangeCheckpointFrequency, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1851)
    public int getMailboxChangeCheckpointFrequency() {
        return getIntAttr(Provisioning.A_zimbraMailboxChangeCheckpointFrequency, -1);
    }

    /**
     * The frequency, in number of changes, at which a mailbox&#039;s
     * ZIMBRA.MAILBOX.CHANGE_CHECKPOINT highwater change value is written.
     *
     * @param zimbraMailboxChangeCheckpointFrequency new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1851)
    public void setMailboxChangeCheckpointFrequency(int zimbraMailboxChangeCheckpointFrequency) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxChangeCheckpointFrequency, Integer.toString(zimbraMailboxChangeCheckpointFrequency));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The frequency, in number of changes, at which a mailbox&#039;s
     * ZIMBRA.MAILBOX.CHANGE_CHECKPOINT highwater change value is written.
     *
     * @param zimbraMailboxChangeCheckpointFrequency new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1851)
    public Map<String,Object> setMailboxChangeCheckpointFrequency(int zimbraMailboxChangeCheckpointFrequency, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxChangeCheckpointFrequency, Integer.toString(zimbraMailboxChangeCheckpointFrequency));
        return attrs;
    }

    /**
     * The frequency, in number of changes, at which a mailbox&#039;s
     * ZIMBRA.MAILBOX.CHANGE_CHECKPOINT highwater change value is written.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1851)
    public void unsetMailboxChangeCheckpointFrequency() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxChangeCheckpointFrequency, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The frequency, in number of changes, at which a mailbox&#039;s
     * ZIMBRA.MAILBOX.CHANGE_CHECKPOINT highwater change value is written.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1851)
    public Map<String,Object> unsetMailboxChangeCheckpointFrequency(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxChangeCheckpointFrequency, "");
        return attrs;
    }

    /**
     * Disable timeout for csv formatter. Introduced in bug 56458. This is a
     * workaround for an issue in Jetty 6.1.22.zc6m when we upgrade we should
     * re-evaluate/remove these settings and the code that uses them.
     *
     * @return zimbraMailboxCsvFormatterDisableTimeout, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1992)
    public boolean isMailboxCsvFormatterDisableTimeout() {
        return getBooleanAttr(Provisioning.A_zimbraMailboxCsvFormatterDisableTimeout, false);
    }

    /**
     * Disable timeout for csv formatter. Introduced in bug 56458. This is a
     * workaround for an issue in Jetty 6.1.22.zc6m when we upgrade we should
     * re-evaluate/remove these settings and the code that uses them.
     *
     * @param zimbraMailboxCsvFormatterDisableTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1992)
    public void setMailboxCsvFormatterDisableTimeout(boolean zimbraMailboxCsvFormatterDisableTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxCsvFormatterDisableTimeout, zimbraMailboxCsvFormatterDisableTimeout ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Disable timeout for csv formatter. Introduced in bug 56458. This is a
     * workaround for an issue in Jetty 6.1.22.zc6m when we upgrade we should
     * re-evaluate/remove these settings and the code that uses them.
     *
     * @param zimbraMailboxCsvFormatterDisableTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1992)
    public Map<String,Object> setMailboxCsvFormatterDisableTimeout(boolean zimbraMailboxCsvFormatterDisableTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxCsvFormatterDisableTimeout, zimbraMailboxCsvFormatterDisableTimeout ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Disable timeout for csv formatter. Introduced in bug 56458. This is a
     * workaround for an issue in Jetty 6.1.22.zc6m when we upgrade we should
     * re-evaluate/remove these settings and the code that uses them.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1992)
    public void unsetMailboxCsvFormatterDisableTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxCsvFormatterDisableTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Disable timeout for csv formatter. Introduced in bug 56458. This is a
     * workaround for an issue in Jetty 6.1.22.zc6m when we upgrade we should
     * re-evaluate/remove these settings and the code that uses them.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1992)
    public Map<String,Object> unsetMailboxCsvFormatterDisableTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxCsvFormatterDisableTimeout, "");
        return attrs;
    }

    /**
     * The max idle time for an HTTP DAV Method in milliseconds. Timeout 0
     * implies an infinite timeout If not setting to 0, suggest at least
     * 600000, 10 minutes. Introduced in bug 79865. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * <p>Use getMailboxDAVConnectionMaxIdleTimeAsString to access value as a string.
     *
     * @see #getMailboxDAVConnectionMaxIdleTimeAsString()
     *
     * @return zimbraMailboxDAVConnectionMaxIdleTime in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1896)
    public long getMailboxDAVConnectionMaxIdleTime() {
        return getTimeInterval(Provisioning.A_zimbraMailboxDAVConnectionMaxIdleTime, -1L);
    }

    /**
     * The max idle time for an HTTP DAV Method in milliseconds. Timeout 0
     * implies an infinite timeout If not setting to 0, suggest at least
     * 600000, 10 minutes. Introduced in bug 79865. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @return zimbraMailboxDAVConnectionMaxIdleTime, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1896)
    public String getMailboxDAVConnectionMaxIdleTimeAsString() {
        return getAttr(Provisioning.A_zimbraMailboxDAVConnectionMaxIdleTime, null);
    }

    /**
     * The max idle time for an HTTP DAV Method in milliseconds. Timeout 0
     * implies an infinite timeout If not setting to 0, suggest at least
     * 600000, 10 minutes. Introduced in bug 79865. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @param zimbraMailboxDAVConnectionMaxIdleTime new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1896)
    public void setMailboxDAVConnectionMaxIdleTime(String zimbraMailboxDAVConnectionMaxIdleTime) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxDAVConnectionMaxIdleTime, zimbraMailboxDAVConnectionMaxIdleTime);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The max idle time for an HTTP DAV Method in milliseconds. Timeout 0
     * implies an infinite timeout If not setting to 0, suggest at least
     * 600000, 10 minutes. Introduced in bug 79865. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @param zimbraMailboxDAVConnectionMaxIdleTime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1896)
    public Map<String,Object> setMailboxDAVConnectionMaxIdleTime(String zimbraMailboxDAVConnectionMaxIdleTime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxDAVConnectionMaxIdleTime, zimbraMailboxDAVConnectionMaxIdleTime);
        return attrs;
    }

    /**
     * The max idle time for an HTTP DAV Method in milliseconds. Timeout 0
     * implies an infinite timeout If not setting to 0, suggest at least
     * 600000, 10 minutes. Introduced in bug 79865. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1896)
    public void unsetMailboxDAVConnectionMaxIdleTime() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxDAVConnectionMaxIdleTime, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The max idle time for an HTTP DAV Method in milliseconds. Timeout 0
     * implies an infinite timeout If not setting to 0, suggest at least
     * 600000, 10 minutes. Introduced in bug 79865. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1896)
    public Map<String,Object> unsetMailboxDAVConnectionMaxIdleTime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxDAVConnectionMaxIdleTime, "");
        return attrs;
    }

    /**
     * Sleep time in milliseconds to give other threads a chance to use the
     * mailbox between deletion batches.. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * <p>Use getMailboxDeleteFolderThreadSleepAsString to access value as a string.
     *
     * @see #getMailboxDeleteFolderThreadSleepAsString()
     *
     * @return zimbraMailboxDeleteFolderThreadSleep in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1795)
    public long getMailboxDeleteFolderThreadSleep() {
        return getTimeInterval(Provisioning.A_zimbraMailboxDeleteFolderThreadSleep, -1L);
    }

    /**
     * Sleep time in milliseconds to give other threads a chance to use the
     * mailbox between deletion batches.. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @return zimbraMailboxDeleteFolderThreadSleep, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1795)
    public String getMailboxDeleteFolderThreadSleepAsString() {
        return getAttr(Provisioning.A_zimbraMailboxDeleteFolderThreadSleep, null);
    }

    /**
     * Sleep time in milliseconds to give other threads a chance to use the
     * mailbox between deletion batches.. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param zimbraMailboxDeleteFolderThreadSleep new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1795)
    public void setMailboxDeleteFolderThreadSleep(String zimbraMailboxDeleteFolderThreadSleep) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxDeleteFolderThreadSleep, zimbraMailboxDeleteFolderThreadSleep);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Sleep time in milliseconds to give other threads a chance to use the
     * mailbox between deletion batches.. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param zimbraMailboxDeleteFolderThreadSleep new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1795)
    public Map<String,Object> setMailboxDeleteFolderThreadSleep(String zimbraMailboxDeleteFolderThreadSleep, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxDeleteFolderThreadSleep, zimbraMailboxDeleteFolderThreadSleep);
        return attrs;
    }

    /**
     * Sleep time in milliseconds to give other threads a chance to use the
     * mailbox between deletion batches.. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1795)
    public void unsetMailboxDeleteFolderThreadSleep() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxDeleteFolderThreadSleep, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Sleep time in milliseconds to give other threads a chance to use the
     * mailbox between deletion batches.. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1795)
    public Map<String,Object> unsetMailboxDeleteFolderThreadSleep(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxDeleteFolderThreadSleep, "");
        return attrs;
    }

    /**
     * Flag to enable disk cache servlet flush
     *
     * @return zimbraMailboxDiskCacheFlush, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1842)
    public boolean isMailboxDiskCacheFlush() {
        return getBooleanAttr(Provisioning.A_zimbraMailboxDiskCacheFlush, false);
    }

    /**
     * Flag to enable disk cache servlet flush
     *
     * @param zimbraMailboxDiskCacheFlush new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1842)
    public void setMailboxDiskCacheFlush(boolean zimbraMailboxDiskCacheFlush) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxDiskCacheFlush, zimbraMailboxDiskCacheFlush ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable disk cache servlet flush
     *
     * @param zimbraMailboxDiskCacheFlush new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1842)
    public Map<String,Object> setMailboxDiskCacheFlush(boolean zimbraMailboxDiskCacheFlush, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxDiskCacheFlush, zimbraMailboxDiskCacheFlush ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Flag to enable disk cache servlet flush
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1842)
    public void unsetMailboxDiskCacheFlush() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxDiskCacheFlush, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable disk cache servlet flush
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1842)
    public Map<String,Object> unsetMailboxDiskCacheFlush(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxDiskCacheFlush, "");
        return attrs;
    }

    /**
     * Size of the cache used by Disk Cache Servlet
     *
     * @return zimbraMailboxDiskCacheSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1843)
    public int getMailboxDiskCacheSize() {
        return getIntAttr(Provisioning.A_zimbraMailboxDiskCacheSize, -1);
    }

    /**
     * Size of the cache used by Disk Cache Servlet
     *
     * @param zimbraMailboxDiskCacheSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1843)
    public void setMailboxDiskCacheSize(int zimbraMailboxDiskCacheSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxDiskCacheSize, Integer.toString(zimbraMailboxDiskCacheSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Size of the cache used by Disk Cache Servlet
     *
     * @param zimbraMailboxDiskCacheSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1843)
    public Map<String,Object> setMailboxDiskCacheSize(int zimbraMailboxDiskCacheSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxDiskCacheSize, Integer.toString(zimbraMailboxDiskCacheSize));
        return attrs;
    }

    /**
     * Size of the cache used by Disk Cache Servlet
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1843)
    public void unsetMailboxDiskCacheSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxDiskCacheSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Size of the cache used by Disk Cache Servlet
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1843)
    public Map<String,Object> unsetMailboxDiskCacheSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxDiskCacheSize, "");
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
     * The maximum size of a mailboxs internal LRU item cache when there are
     * sessions active.
     *
     * @return zimbraMailboxMailItemActiveCache, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1850)
    public int getMailboxMailItemActiveCache() {
        return getIntAttr(Provisioning.A_zimbraMailboxMailItemActiveCache, -1);
    }

    /**
     * The maximum size of a mailboxs internal LRU item cache when there are
     * sessions active.
     *
     * @param zimbraMailboxMailItemActiveCache new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1850)
    public void setMailboxMailItemActiveCache(int zimbraMailboxMailItemActiveCache) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxMailItemActiveCache, Integer.toString(zimbraMailboxMailItemActiveCache));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The maximum size of a mailboxs internal LRU item cache when there are
     * sessions active.
     *
     * @param zimbraMailboxMailItemActiveCache new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1850)
    public Map<String,Object> setMailboxMailItemActiveCache(int zimbraMailboxMailItemActiveCache, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxMailItemActiveCache, Integer.toString(zimbraMailboxMailItemActiveCache));
        return attrs;
    }

    /**
     * The maximum size of a mailboxs internal LRU item cache when there are
     * sessions active.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1850)
    public void unsetMailboxMailItemActiveCache() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxMailItemActiveCache, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The maximum size of a mailboxs internal LRU item cache when there are
     * sessions active.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1850)
    public Map<String,Object> unsetMailboxMailItemActiveCache(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxMailItemActiveCache, "");
        return attrs;
    }

    /**
     * The maximum size of a mailbox&#039;s internal LRU item cache when it
     * has no active sessions.
     *
     * @return zimbraMailboxMailItemInactiveCache, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1855)
    public int getMailboxMailItemInactiveCache() {
        return getIntAttr(Provisioning.A_zimbraMailboxMailItemInactiveCache, -1);
    }

    /**
     * The maximum size of a mailbox&#039;s internal LRU item cache when it
     * has no active sessions.
     *
     * @param zimbraMailboxMailItemInactiveCache new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1855)
    public void setMailboxMailItemInactiveCache(int zimbraMailboxMailItemInactiveCache) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxMailItemInactiveCache, Integer.toString(zimbraMailboxMailItemInactiveCache));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The maximum size of a mailbox&#039;s internal LRU item cache when it
     * has no active sessions.
     *
     * @param zimbraMailboxMailItemInactiveCache new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1855)
    public Map<String,Object> setMailboxMailItemInactiveCache(int zimbraMailboxMailItemInactiveCache, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxMailItemInactiveCache, Integer.toString(zimbraMailboxMailItemInactiveCache));
        return attrs;
    }

    /**
     * The maximum size of a mailbox&#039;s internal LRU item cache when it
     * has no active sessions.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1855)
    public void unsetMailboxMailItemInactiveCache() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxMailItemInactiveCache, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The maximum size of a mailbox&#039;s internal LRU item cache when it
     * has no active sessions.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1855)
    public Map<String,Object> unsetMailboxMailItemInactiveCache(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxMailItemInactiveCache, "");
        return attrs;
    }

    /**
     * The maximum number of mailboxes that will be pinned in memory before
     * the mailbox manager starts allowing them to be purged via
     * SoftReference garbage collection.
     *
     * @return zimbraMailboxManagerHardrefCache, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1856)
    public int getMailboxManagerHardrefCache() {
        return getIntAttr(Provisioning.A_zimbraMailboxManagerHardrefCache, -1);
    }

    /**
     * The maximum number of mailboxes that will be pinned in memory before
     * the mailbox manager starts allowing them to be purged via
     * SoftReference garbage collection.
     *
     * @param zimbraMailboxManagerHardrefCache new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1856)
    public void setMailboxManagerHardrefCache(int zimbraMailboxManagerHardrefCache) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxManagerHardrefCache, Integer.toString(zimbraMailboxManagerHardrefCache));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The maximum number of mailboxes that will be pinned in memory before
     * the mailbox manager starts allowing them to be purged via
     * SoftReference garbage collection.
     *
     * @param zimbraMailboxManagerHardrefCache new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1856)
    public Map<String,Object> setMailboxManagerHardrefCache(int zimbraMailboxManagerHardrefCache, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxManagerHardrefCache, Integer.toString(zimbraMailboxManagerHardrefCache));
        return attrs;
    }

    /**
     * The maximum number of mailboxes that will be pinned in memory before
     * the mailbox manager starts allowing them to be purged via
     * SoftReference garbage collection.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1856)
    public void unsetMailboxManagerHardrefCache() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxManagerHardrefCache, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * The maximum number of mailboxes that will be pinned in memory before
     * the mailbox manager starts allowing them to be purged via
     * SoftReference garbage collection.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1856)
    public Map<String,Object> unsetMailboxManagerHardrefCache(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxManagerHardrefCache, "");
        return attrs;
    }

    /**
     * Maximum number of HTTP requests per account before ZimbraQoSFilter
     * suspends further requests. Aligns with &quot;Exceeded the max requests
     * limit. Suspending...&quot; warnings in mailbox.log..
     *
     * @return zimbraMailboxMaxConcurrentHttpRequestsPerAccount, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1883)
    public int getMailboxMaxConcurrentHttpRequestsPerAccount() {
        return getIntAttr(Provisioning.A_zimbraMailboxMaxConcurrentHttpRequestsPerAccount, -1);
    }

    /**
     * Maximum number of HTTP requests per account before ZimbraQoSFilter
     * suspends further requests. Aligns with &quot;Exceeded the max requests
     * limit. Suspending...&quot; warnings in mailbox.log..
     *
     * @param zimbraMailboxMaxConcurrentHttpRequestsPerAccount new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1883)
    public void setMailboxMaxConcurrentHttpRequestsPerAccount(int zimbraMailboxMaxConcurrentHttpRequestsPerAccount) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxMaxConcurrentHttpRequestsPerAccount, Integer.toString(zimbraMailboxMaxConcurrentHttpRequestsPerAccount));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of HTTP requests per account before ZimbraQoSFilter
     * suspends further requests. Aligns with &quot;Exceeded the max requests
     * limit. Suspending...&quot; warnings in mailbox.log..
     *
     * @param zimbraMailboxMaxConcurrentHttpRequestsPerAccount new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1883)
    public Map<String,Object> setMailboxMaxConcurrentHttpRequestsPerAccount(int zimbraMailboxMaxConcurrentHttpRequestsPerAccount, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxMaxConcurrentHttpRequestsPerAccount, Integer.toString(zimbraMailboxMaxConcurrentHttpRequestsPerAccount));
        return attrs;
    }

    /**
     * Maximum number of HTTP requests per account before ZimbraQoSFilter
     * suspends further requests. Aligns with &quot;Exceeded the max requests
     * limit. Suspending...&quot; warnings in mailbox.log..
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1883)
    public void unsetMailboxMaxConcurrentHttpRequestsPerAccount() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxMaxConcurrentHttpRequestsPerAccount, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of HTTP requests per account before ZimbraQoSFilter
     * suspends further requests. Aligns with &quot;Exceeded the max requests
     * limit. Suspending...&quot; warnings in mailbox.log..
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1883)
    public Map<String,Object> unsetMailboxMaxConcurrentHttpRequestsPerAccount(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxMaxConcurrentHttpRequestsPerAccount, "");
        return attrs;
    }

    /**
     * max number of concurrent HTTP requests per HTTP session .0 means no
     * limit. A change is in effect from new session.
     *
     * @return zimbraMailboxMaxConcurrentRequestsPerSession, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1884)
    public int getMailboxMaxConcurrentRequestsPerSession() {
        return getIntAttr(Provisioning.A_zimbraMailboxMaxConcurrentRequestsPerSession, -1);
    }

    /**
     * max number of concurrent HTTP requests per HTTP session .0 means no
     * limit. A change is in effect from new session.
     *
     * @param zimbraMailboxMaxConcurrentRequestsPerSession new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1884)
    public void setMailboxMaxConcurrentRequestsPerSession(int zimbraMailboxMaxConcurrentRequestsPerSession) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxMaxConcurrentRequestsPerSession, Integer.toString(zimbraMailboxMaxConcurrentRequestsPerSession));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * max number of concurrent HTTP requests per HTTP session .0 means no
     * limit. A change is in effect from new session.
     *
     * @param zimbraMailboxMaxConcurrentRequestsPerSession new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1884)
    public Map<String,Object> setMailboxMaxConcurrentRequestsPerSession(int zimbraMailboxMaxConcurrentRequestsPerSession, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxMaxConcurrentRequestsPerSession, Integer.toString(zimbraMailboxMaxConcurrentRequestsPerSession));
        return attrs;
    }

    /**
     * max number of concurrent HTTP requests per HTTP session .0 means no
     * limit. A change is in effect from new session.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1884)
    public void unsetMailboxMaxConcurrentRequestsPerSession() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxMaxConcurrentRequestsPerSession, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * max number of concurrent HTTP requests per HTTP session .0 means no
     * limit. A change is in effect from new session.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1884)
    public Map<String,Object> unsetMailboxMaxConcurrentRequestsPerSession(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxMaxConcurrentRequestsPerSession, "");
        return attrs;
    }

    /**
     * Time in seconds the server will allow a NoOpRequest to block if wait=1
     * is specified by the client. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * <p>Use getMailboxNoopDefaultTimeoutAsString to access value as a string.
     *
     * @see #getMailboxNoopDefaultTimeoutAsString()
     *
     * @return zimbraMailboxNoopDefaultTimeout in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1859)
    public long getMailboxNoopDefaultTimeout() {
        return getTimeInterval(Provisioning.A_zimbraMailboxNoopDefaultTimeout, -1L);
    }

    /**
     * Time in seconds the server will allow a NoOpRequest to block if wait=1
     * is specified by the client. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @return zimbraMailboxNoopDefaultTimeout, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1859)
    public String getMailboxNoopDefaultTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraMailboxNoopDefaultTimeout, null);
    }

    /**
     * Time in seconds the server will allow a NoOpRequest to block if wait=1
     * is specified by the client. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param zimbraMailboxNoopDefaultTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1859)
    public void setMailboxNoopDefaultTimeout(String zimbraMailboxNoopDefaultTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxNoopDefaultTimeout, zimbraMailboxNoopDefaultTimeout);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Time in seconds the server will allow a NoOpRequest to block if wait=1
     * is specified by the client. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param zimbraMailboxNoopDefaultTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1859)
    public Map<String,Object> setMailboxNoopDefaultTimeout(String zimbraMailboxNoopDefaultTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxNoopDefaultTimeout, zimbraMailboxNoopDefaultTimeout);
        return attrs;
    }

    /**
     * Time in seconds the server will allow a NoOpRequest to block if wait=1
     * is specified by the client. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1859)
    public void unsetMailboxNoopDefaultTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxNoopDefaultTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Time in seconds the server will allow a NoOpRequest to block if wait=1
     * is specified by the client. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1859)
    public Map<String,Object> unsetMailboxNoopDefaultTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxNoopDefaultTimeout, "");
        return attrs;
    }

    /**
     * Maximum allowable timeout (seconds) specified to NoOpRequest. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * <p>Use getMailboxNoopMaxTimeoutAsString to access value as a string.
     *
     * @see #getMailboxNoopMaxTimeoutAsString()
     *
     * @return zimbraMailboxNoopMaxTimeout in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1860)
    public long getMailboxNoopMaxTimeout() {
        return getTimeInterval(Provisioning.A_zimbraMailboxNoopMaxTimeout, -1L);
    }

    /**
     * Maximum allowable timeout (seconds) specified to NoOpRequest. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @return zimbraMailboxNoopMaxTimeout, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1860)
    public String getMailboxNoopMaxTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraMailboxNoopMaxTimeout, null);
    }

    /**
     * Maximum allowable timeout (seconds) specified to NoOpRequest. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraMailboxNoopMaxTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1860)
    public void setMailboxNoopMaxTimeout(String zimbraMailboxNoopMaxTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxNoopMaxTimeout, zimbraMailboxNoopMaxTimeout);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum allowable timeout (seconds) specified to NoOpRequest. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraMailboxNoopMaxTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1860)
    public Map<String,Object> setMailboxNoopMaxTimeout(String zimbraMailboxNoopMaxTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxNoopMaxTimeout, zimbraMailboxNoopMaxTimeout);
        return attrs;
    }

    /**
     * Maximum allowable timeout (seconds) specified to NoOpRequest. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1860)
    public void unsetMailboxNoopMaxTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxNoopMaxTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum allowable timeout (seconds) specified to NoOpRequest. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1860)
    public Map<String,Object> unsetMailboxNoopMaxTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxNoopMaxTimeout, "");
        return attrs;
    }

    /**
     * Minimum allowable timeout (seconds) specified to NoOpRequest. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * <p>Use getMailboxNoopMinTimeoutAsString to access value as a string.
     *
     * @see #getMailboxNoopMinTimeoutAsString()
     *
     * @return zimbraMailboxNoopMinTimeout in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1861)
    public long getMailboxNoopMinTimeout() {
        return getTimeInterval(Provisioning.A_zimbraMailboxNoopMinTimeout, -1L);
    }

    /**
     * Minimum allowable timeout (seconds) specified to NoOpRequest. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @return zimbraMailboxNoopMinTimeout, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1861)
    public String getMailboxNoopMinTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraMailboxNoopMinTimeout, null);
    }

    /**
     * Minimum allowable timeout (seconds) specified to NoOpRequest. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraMailboxNoopMinTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1861)
    public void setMailboxNoopMinTimeout(String zimbraMailboxNoopMinTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxNoopMinTimeout, zimbraMailboxNoopMinTimeout);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Minimum allowable timeout (seconds) specified to NoOpRequest. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraMailboxNoopMinTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1861)
    public Map<String,Object> setMailboxNoopMinTimeout(String zimbraMailboxNoopMinTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxNoopMinTimeout, zimbraMailboxNoopMinTimeout);
        return attrs;
    }

    /**
     * Minimum allowable timeout (seconds) specified to NoOpRequest. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1861)
    public void unsetMailboxNoopMinTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxNoopMinTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Minimum allowable timeout (seconds) specified to NoOpRequest. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1861)
    public Map<String,Object> unsetMailboxNoopMinTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxNoopMinTimeout, "");
        return attrs;
    }

    /**
     * Initial sleep time in millis for mailbox purge thread. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * <p>Use getMailboxPurgeInitialSleepAsString to access value as a string.
     *
     * @see #getMailboxPurgeInitialSleepAsString()
     *
     * @return zimbraMailboxPurgeInitialSleep in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1878)
    public long getMailboxPurgeInitialSleep() {
        return getTimeInterval(Provisioning.A_zimbraMailboxPurgeInitialSleep, -1L);
    }

    /**
     * Initial sleep time in millis for mailbox purge thread. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @return zimbraMailboxPurgeInitialSleep, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1878)
    public String getMailboxPurgeInitialSleepAsString() {
        return getAttr(Provisioning.A_zimbraMailboxPurgeInitialSleep, null);
    }

    /**
     * Initial sleep time in millis for mailbox purge thread. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraMailboxPurgeInitialSleep new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1878)
    public void setMailboxPurgeInitialSleep(String zimbraMailboxPurgeInitialSleep) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxPurgeInitialSleep, zimbraMailboxPurgeInitialSleep);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Initial sleep time in millis for mailbox purge thread. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraMailboxPurgeInitialSleep new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1878)
    public Map<String,Object> setMailboxPurgeInitialSleep(String zimbraMailboxPurgeInitialSleep, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxPurgeInitialSleep, zimbraMailboxPurgeInitialSleep);
        return attrs;
    }

    /**
     * Initial sleep time in millis for mailbox purge thread. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1878)
    public void unsetMailboxPurgeInitialSleep() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxPurgeInitialSleep, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Initial sleep time in millis for mailbox purge thread. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1878)
    public Map<String,Object> unsetMailboxPurgeInitialSleep(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxPurgeInitialSleep, "");
        return attrs;
    }

    /**
     * Minimise server resources for small servers such as Zimbra Desktop.
     *
     * @return zimbraMailboxResourceBundleMinimizeResources, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1857)
    public boolean isMailboxResourceBundleMinimizeResources() {
        return getBooleanAttr(Provisioning.A_zimbraMailboxResourceBundleMinimizeResources, false);
    }

    /**
     * Minimise server resources for small servers such as Zimbra Desktop.
     *
     * @param zimbraMailboxResourceBundleMinimizeResources new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1857)
    public void setMailboxResourceBundleMinimizeResources(boolean zimbraMailboxResourceBundleMinimizeResources) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxResourceBundleMinimizeResources, zimbraMailboxResourceBundleMinimizeResources ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Minimise server resources for small servers such as Zimbra Desktop.
     *
     * @param zimbraMailboxResourceBundleMinimizeResources new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1857)
    public Map<String,Object> setMailboxResourceBundleMinimizeResources(boolean zimbraMailboxResourceBundleMinimizeResources, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxResourceBundleMinimizeResources, zimbraMailboxResourceBundleMinimizeResources ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Minimise server resources for small servers such as Zimbra Desktop.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1857)
    public void unsetMailboxResourceBundleMinimizeResources() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxResourceBundleMinimizeResources, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Minimise server resources for small servers such as Zimbra Desktop.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1857)
    public Map<String,Object> unsetMailboxResourceBundleMinimizeResources(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxResourceBundleMinimizeResources, "");
        return attrs;
    }

    /**
     * Cache control value for the rest api response
     *
     * @return zimbraMailboxRestResponseCacheControl, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1879)
    public String getMailboxRestResponseCacheControl() {
        return getAttr(Provisioning.A_zimbraMailboxRestResponseCacheControl, null);
    }

    /**
     * Cache control value for the rest api response
     *
     * @param zimbraMailboxRestResponseCacheControl new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1879)
    public void setMailboxRestResponseCacheControl(String zimbraMailboxRestResponseCacheControl) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxRestResponseCacheControl, zimbraMailboxRestResponseCacheControl);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Cache control value for the rest api response
     *
     * @param zimbraMailboxRestResponseCacheControl new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1879)
    public Map<String,Object> setMailboxRestResponseCacheControl(String zimbraMailboxRestResponseCacheControl, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxRestResponseCacheControl, zimbraMailboxRestResponseCacheControl);
        return attrs;
    }

    /**
     * Cache control value for the rest api response
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1879)
    public void unsetMailboxRestResponseCacheControl() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxRestResponseCacheControl, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Cache control value for the rest api response
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1879)
    public Map<String,Object> unsetMailboxRestResponseCacheControl(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxRestResponseCacheControl, "");
        return attrs;
    }

    /**
     * Whether delegated admin rights are supported or not
     *
     * @return zimbraMailboxRightsDelegatedAdminSupported, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1864)
    public boolean isMailboxRightsDelegatedAdminSupported() {
        return getBooleanAttr(Provisioning.A_zimbraMailboxRightsDelegatedAdminSupported, false);
    }

    /**
     * Whether delegated admin rights are supported or not
     *
     * @param zimbraMailboxRightsDelegatedAdminSupported new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1864)
    public void setMailboxRightsDelegatedAdminSupported(boolean zimbraMailboxRightsDelegatedAdminSupported) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxRightsDelegatedAdminSupported, zimbraMailboxRightsDelegatedAdminSupported ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether delegated admin rights are supported or not
     *
     * @param zimbraMailboxRightsDelegatedAdminSupported new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1864)
    public Map<String,Object> setMailboxRightsDelegatedAdminSupported(boolean zimbraMailboxRightsDelegatedAdminSupported, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxRightsDelegatedAdminSupported, zimbraMailboxRightsDelegatedAdminSupported ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether delegated admin rights are supported or not
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1864)
    public void unsetMailboxRightsDelegatedAdminSupported() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxRightsDelegatedAdminSupported, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether delegated admin rights are supported or not
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1864)
    public Map<String,Object> unsetMailboxRightsDelegatedAdminSupported(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxRightsDelegatedAdminSupported, "");
        return attrs;
    }

    /**
     * Number of milliseconds to wait before retrying after a failed
     * connection to an SMTP host. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * <p>Use getMailboxSmtpHostRetryWaitAsString to access value as a string.
     *
     * @see #getMailboxSmtpHostRetryWaitAsString()
     *
     * @return zimbraMailboxSmtpHostRetryWait in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1865)
    public long getMailboxSmtpHostRetryWait() {
        return getTimeInterval(Provisioning.A_zimbraMailboxSmtpHostRetryWait, -1L);
    }

    /**
     * Number of milliseconds to wait before retrying after a failed
     * connection to an SMTP host. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @return zimbraMailboxSmtpHostRetryWait, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1865)
    public String getMailboxSmtpHostRetryWaitAsString() {
        return getAttr(Provisioning.A_zimbraMailboxSmtpHostRetryWait, null);
    }

    /**
     * Number of milliseconds to wait before retrying after a failed
     * connection to an SMTP host. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param zimbraMailboxSmtpHostRetryWait new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1865)
    public void setMailboxSmtpHostRetryWait(String zimbraMailboxSmtpHostRetryWait) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSmtpHostRetryWait, zimbraMailboxSmtpHostRetryWait);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Number of milliseconds to wait before retrying after a failed
     * connection to an SMTP host. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param zimbraMailboxSmtpHostRetryWait new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1865)
    public Map<String,Object> setMailboxSmtpHostRetryWait(String zimbraMailboxSmtpHostRetryWait, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSmtpHostRetryWait, zimbraMailboxSmtpHostRetryWait);
        return attrs;
    }

    /**
     * Number of milliseconds to wait before retrying after a failed
     * connection to an SMTP host. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1865)
    public void unsetMailboxSmtpHostRetryWait() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSmtpHostRetryWait, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Number of milliseconds to wait before retrying after a failed
     * connection to an SMTP host. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1865)
    public Map<String,Object> unsetMailboxSmtpHostRetryWait(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSmtpHostRetryWait, "");
        return attrs;
    }

    /**
     * Flag to enable smtptolmtp server
     *
     * @return zimbraMailboxSmtpToLmtpEnabled, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1885)
    public boolean isMailboxSmtpToLmtpEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraMailboxSmtpToLmtpEnabled, false);
    }

    /**
     * Flag to enable smtptolmtp server
     *
     * @param zimbraMailboxSmtpToLmtpEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1885)
    public void setMailboxSmtpToLmtpEnabled(boolean zimbraMailboxSmtpToLmtpEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSmtpToLmtpEnabled, zimbraMailboxSmtpToLmtpEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable smtptolmtp server
     *
     * @param zimbraMailboxSmtpToLmtpEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1885)
    public Map<String,Object> setMailboxSmtpToLmtpEnabled(boolean zimbraMailboxSmtpToLmtpEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSmtpToLmtpEnabled, zimbraMailboxSmtpToLmtpEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Flag to enable smtptolmtp server
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1885)
    public void unsetMailboxSmtpToLmtpEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSmtpToLmtpEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable smtptolmtp server
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1885)
    public Map<String,Object> unsetMailboxSmtpToLmtpEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSmtpToLmtpEnabled, "");
        return attrs;
    }

    /**
     * SmtptoLmtp port
     *
     * <p>Use getMailboxSmtpToLmtpPortAsString to access value as a string.
     *
     * @see #getMailboxSmtpToLmtpPortAsString()
     *
     * @return zimbraMailboxSmtpToLmtpPort, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1886)
    public int getMailboxSmtpToLmtpPort() {
        return getIntAttr(Provisioning.A_zimbraMailboxSmtpToLmtpPort, -1);
    }

    /**
     * SmtptoLmtp port
     *
     * @return zimbraMailboxSmtpToLmtpPort, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1886)
    public String getMailboxSmtpToLmtpPortAsString() {
        return getAttr(Provisioning.A_zimbraMailboxSmtpToLmtpPort, null);
    }

    /**
     * SmtptoLmtp port
     *
     * @param zimbraMailboxSmtpToLmtpPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1886)
    public void setMailboxSmtpToLmtpPort(int zimbraMailboxSmtpToLmtpPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSmtpToLmtpPort, Integer.toString(zimbraMailboxSmtpToLmtpPort));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * SmtptoLmtp port
     *
     * @param zimbraMailboxSmtpToLmtpPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1886)
    public Map<String,Object> setMailboxSmtpToLmtpPort(int zimbraMailboxSmtpToLmtpPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSmtpToLmtpPort, Integer.toString(zimbraMailboxSmtpToLmtpPort));
        return attrs;
    }

    /**
     * SmtptoLmtp port
     *
     * @param zimbraMailboxSmtpToLmtpPort new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1886)
    public void setMailboxSmtpToLmtpPortAsString(String zimbraMailboxSmtpToLmtpPort) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSmtpToLmtpPort, zimbraMailboxSmtpToLmtpPort);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * SmtptoLmtp port
     *
     * @param zimbraMailboxSmtpToLmtpPort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1886)
    public Map<String,Object> setMailboxSmtpToLmtpPortAsString(String zimbraMailboxSmtpToLmtpPort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSmtpToLmtpPort, zimbraMailboxSmtpToLmtpPort);
        return attrs;
    }

    /**
     * SmtptoLmtp port
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1886)
    public void unsetMailboxSmtpToLmtpPort() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSmtpToLmtpPort, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * SmtptoLmtp port
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1886)
    public Map<String,Object> unsetMailboxSmtpToLmtpPort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSmtpToLmtpPort, "");
        return attrs;
    }

    /**
     * Flag to enable logging of slow soap api calls.
     *
     * @return zimbraMailboxSoapApiSlowLoggingEnabled, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1866)
    public boolean isMailboxSoapApiSlowLoggingEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraMailboxSoapApiSlowLoggingEnabled, false);
    }

    /**
     * Flag to enable logging of slow soap api calls.
     *
     * @param zimbraMailboxSoapApiSlowLoggingEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1866)
    public void setMailboxSoapApiSlowLoggingEnabled(boolean zimbraMailboxSoapApiSlowLoggingEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSoapApiSlowLoggingEnabled, zimbraMailboxSoapApiSlowLoggingEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable logging of slow soap api calls.
     *
     * @param zimbraMailboxSoapApiSlowLoggingEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1866)
    public Map<String,Object> setMailboxSoapApiSlowLoggingEnabled(boolean zimbraMailboxSoapApiSlowLoggingEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSoapApiSlowLoggingEnabled, zimbraMailboxSoapApiSlowLoggingEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Flag to enable logging of slow soap api calls.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1866)
    public void unsetMailboxSoapApiSlowLoggingEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSoapApiSlowLoggingEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable logging of slow soap api calls.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1866)
    public Map<String,Object> unsetMailboxSoapApiSlowLoggingEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSoapApiSlowLoggingEnabled, "");
        return attrs;
    }

    /**
     * Threshold time to determine slow soap api calls.make sure it&#039;s
     * less than nginx&#039;s zimbraReverseProxyUpstreamPollingTimeout, which
     * is now 3600 seconds. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * <p>Use getMailboxSoapApiSlowLoggingThresholdAsString to access value as a string.
     *
     * @see #getMailboxSoapApiSlowLoggingThresholdAsString()
     *
     * @return zimbraMailboxSoapApiSlowLoggingThreshold in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1867)
    public long getMailboxSoapApiSlowLoggingThreshold() {
        return getTimeInterval(Provisioning.A_zimbraMailboxSoapApiSlowLoggingThreshold, -1L);
    }

    /**
     * Threshold time to determine slow soap api calls.make sure it&#039;s
     * less than nginx&#039;s zimbraReverseProxyUpstreamPollingTimeout, which
     * is now 3600 seconds. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @return zimbraMailboxSoapApiSlowLoggingThreshold, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1867)
    public String getMailboxSoapApiSlowLoggingThresholdAsString() {
        return getAttr(Provisioning.A_zimbraMailboxSoapApiSlowLoggingThreshold, null);
    }

    /**
     * Threshold time to determine slow soap api calls.make sure it&#039;s
     * less than nginx&#039;s zimbraReverseProxyUpstreamPollingTimeout, which
     * is now 3600 seconds. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param zimbraMailboxSoapApiSlowLoggingThreshold new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1867)
    public void setMailboxSoapApiSlowLoggingThreshold(String zimbraMailboxSoapApiSlowLoggingThreshold) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSoapApiSlowLoggingThreshold, zimbraMailboxSoapApiSlowLoggingThreshold);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Threshold time to determine slow soap api calls.make sure it&#039;s
     * less than nginx&#039;s zimbraReverseProxyUpstreamPollingTimeout, which
     * is now 3600 seconds. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param zimbraMailboxSoapApiSlowLoggingThreshold new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1867)
    public Map<String,Object> setMailboxSoapApiSlowLoggingThreshold(String zimbraMailboxSoapApiSlowLoggingThreshold, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSoapApiSlowLoggingThreshold, zimbraMailboxSoapApiSlowLoggingThreshold);
        return attrs;
    }

    /**
     * Threshold time to determine slow soap api calls.make sure it&#039;s
     * less than nginx&#039;s zimbraReverseProxyUpstreamPollingTimeout, which
     * is now 3600 seconds. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1867)
    public void unsetMailboxSoapApiSlowLoggingThreshold() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSoapApiSlowLoggingThreshold, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Threshold time to determine slow soap api calls.make sure it&#039;s
     * less than nginx&#039;s zimbraReverseProxyUpstreamPollingTimeout, which
     * is now 3600 seconds. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1867)
    public Map<String,Object> unsetMailboxSoapApiSlowLoggingThreshold(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSoapApiSlowLoggingThreshold, "");
        return attrs;
    }

    /**
     * For Junk/Not Junk Msg/ConvActionRequests this queue size limits the
     * the server workqueue for processing the forwards.
     *
     * @return zimbraMailboxSpamHandlerSpamReportQueueSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1868)
    public int getMailboxSpamHandlerSpamReportQueueSize() {
        return getIntAttr(Provisioning.A_zimbraMailboxSpamHandlerSpamReportQueueSize, -1);
    }

    /**
     * For Junk/Not Junk Msg/ConvActionRequests this queue size limits the
     * the server workqueue for processing the forwards.
     *
     * @param zimbraMailboxSpamHandlerSpamReportQueueSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1868)
    public void setMailboxSpamHandlerSpamReportQueueSize(int zimbraMailboxSpamHandlerSpamReportQueueSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSpamHandlerSpamReportQueueSize, Integer.toString(zimbraMailboxSpamHandlerSpamReportQueueSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * For Junk/Not Junk Msg/ConvActionRequests this queue size limits the
     * the server workqueue for processing the forwards.
     *
     * @param zimbraMailboxSpamHandlerSpamReportQueueSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1868)
    public Map<String,Object> setMailboxSpamHandlerSpamReportQueueSize(int zimbraMailboxSpamHandlerSpamReportQueueSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSpamHandlerSpamReportQueueSize, Integer.toString(zimbraMailboxSpamHandlerSpamReportQueueSize));
        return attrs;
    }

    /**
     * For Junk/Not Junk Msg/ConvActionRequests this queue size limits the
     * the server workqueue for processing the forwards.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1868)
    public void unsetMailboxSpamHandlerSpamReportQueueSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSpamHandlerSpamReportQueueSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * For Junk/Not Junk Msg/ConvActionRequests this queue size limits the
     * the server workqueue for processing the forwards.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1868)
    public Map<String,Object> unsetMailboxSpamHandlerSpamReportQueueSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxSpamHandlerSpamReportQueueSize, "");
        return attrs;
    }

    /**
     * Print warn logs when the specified percentage of threads are used out
     * of total in tcpserver
     *
     * @return zimbraMailboxThreadPoolWarnPercent, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1869)
    public int getMailboxThreadPoolWarnPercent() {
        return getIntAttr(Provisioning.A_zimbraMailboxThreadPoolWarnPercent, -1);
    }

    /**
     * Print warn logs when the specified percentage of threads are used out
     * of total in tcpserver
     *
     * @param zimbraMailboxThreadPoolWarnPercent new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1869)
    public void setMailboxThreadPoolWarnPercent(int zimbraMailboxThreadPoolWarnPercent) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxThreadPoolWarnPercent, Integer.toString(zimbraMailboxThreadPoolWarnPercent));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Print warn logs when the specified percentage of threads are used out
     * of total in tcpserver
     *
     * @param zimbraMailboxThreadPoolWarnPercent new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1869)
    public Map<String,Object> setMailboxThreadPoolWarnPercent(int zimbraMailboxThreadPoolWarnPercent, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxThreadPoolWarnPercent, Integer.toString(zimbraMailboxThreadPoolWarnPercent));
        return attrs;
    }

    /**
     * Print warn logs when the specified percentage of threads are used out
     * of total in tcpserver
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1869)
    public void unsetMailboxThreadPoolWarnPercent() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxThreadPoolWarnPercent, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Print warn logs when the specified percentage of threads are used out
     * of total in tcpserver
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1869)
    public Map<String,Object> unsetMailboxThreadPoolWarnPercent(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxThreadPoolWarnPercent, "");
        return attrs;
    }

    /**
     * Max age in millis used for purging the items in tombstone. Default is
     * 3 months. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * <p>Use getMailboxTombstoneMaxAgeAsString to access value as a string.
     *
     * @see #getMailboxTombstoneMaxAgeAsString()
     *
     * @return zimbraMailboxTombstoneMaxAge in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1889)
    public long getMailboxTombstoneMaxAge() {
        return getTimeInterval(Provisioning.A_zimbraMailboxTombstoneMaxAge, -1L);
    }

    /**
     * Max age in millis used for purging the items in tombstone. Default is
     * 3 months. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * @return zimbraMailboxTombstoneMaxAge, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1889)
    public String getMailboxTombstoneMaxAgeAsString() {
        return getAttr(Provisioning.A_zimbraMailboxTombstoneMaxAge, null);
    }

    /**
     * Max age in millis used for purging the items in tombstone. Default is
     * 3 months. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * @param zimbraMailboxTombstoneMaxAge new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1889)
    public void setMailboxTombstoneMaxAge(String zimbraMailboxTombstoneMaxAge) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxTombstoneMaxAge, zimbraMailboxTombstoneMaxAge);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Max age in millis used for purging the items in tombstone. Default is
     * 3 months. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * @param zimbraMailboxTombstoneMaxAge new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1889)
    public Map<String,Object> setMailboxTombstoneMaxAge(String zimbraMailboxTombstoneMaxAge, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxTombstoneMaxAge, zimbraMailboxTombstoneMaxAge);
        return attrs;
    }

    /**
     * Max age in millis used for purging the items in tombstone. Default is
     * 3 months. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1889)
    public void unsetMailboxTombstoneMaxAge() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxTombstoneMaxAge, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Max age in millis used for purging the items in tombstone. Default is
     * 3 months. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1889)
    public Map<String,Object> unsetMailboxTombstoneMaxAge(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxTombstoneMaxAge, "");
        return attrs;
    }

    /**
     * Whether volume paths are relative or absolute.
     *
     * @return zimbraMailboxVolumeRelativePath, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1863)
    public boolean isMailboxVolumeRelativePath() {
        return getBooleanAttr(Provisioning.A_zimbraMailboxVolumeRelativePath, false);
    }

    /**
     * Whether volume paths are relative or absolute.
     *
     * @param zimbraMailboxVolumeRelativePath new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1863)
    public void setMailboxVolumeRelativePath(boolean zimbraMailboxVolumeRelativePath) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxVolumeRelativePath, zimbraMailboxVolumeRelativePath ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether volume paths are relative or absolute.
     *
     * @param zimbraMailboxVolumeRelativePath new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1863)
    public Map<String,Object> setMailboxVolumeRelativePath(boolean zimbraMailboxVolumeRelativePath, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxVolumeRelativePath, zimbraMailboxVolumeRelativePath ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether volume paths are relative or absolute.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1863)
    public void unsetMailboxVolumeRelativePath() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxVolumeRelativePath, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether volume paths are relative or absolute.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1863)
    public Map<String,Object> unsetMailboxVolumeRelativePath(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxVolumeRelativePath, "");
        return attrs;
    }

    /**
     * Default Timeout in seconds a non-admin WaitSetRequest will block..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * <p>Use getMailboxWaitsetDefaultRequestTimeoutAsString to access value as a string.
     *
     * @see #getMailboxWaitsetDefaultRequestTimeoutAsString()
     *
     * @return zimbraMailboxWaitsetDefaultRequestTimeout in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1872)
    public long getMailboxWaitsetDefaultRequestTimeout() {
        return getTimeInterval(Provisioning.A_zimbraMailboxWaitsetDefaultRequestTimeout, -1L);
    }

    /**
     * Default Timeout in seconds a non-admin WaitSetRequest will block..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @return zimbraMailboxWaitsetDefaultRequestTimeout, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1872)
    public String getMailboxWaitsetDefaultRequestTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraMailboxWaitsetDefaultRequestTimeout, null);
    }

    /**
     * Default Timeout in seconds a non-admin WaitSetRequest will block..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraMailboxWaitsetDefaultRequestTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1872)
    public void setMailboxWaitsetDefaultRequestTimeout(String zimbraMailboxWaitsetDefaultRequestTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetDefaultRequestTimeout, zimbraMailboxWaitsetDefaultRequestTimeout);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Default Timeout in seconds a non-admin WaitSetRequest will block..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraMailboxWaitsetDefaultRequestTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1872)
    public Map<String,Object> setMailboxWaitsetDefaultRequestTimeout(String zimbraMailboxWaitsetDefaultRequestTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetDefaultRequestTimeout, zimbraMailboxWaitsetDefaultRequestTimeout);
        return attrs;
    }

    /**
     * Default Timeout in seconds a non-admin WaitSetRequest will block..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1872)
    public void unsetMailboxWaitsetDefaultRequestTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetDefaultRequestTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Default Timeout in seconds a non-admin WaitSetRequest will block..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1872)
    public Map<String,Object> unsetMailboxWaitsetDefaultRequestTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetDefaultRequestTimeout, "");
        return attrs;
    }

    /**
     * Initial timeout in milliseconds to wait before processing any
     * WaitSetRequest.. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * <p>Use getMailboxWaitsetInitialSleepTimeAsString to access value as a string.
     *
     * @see #getMailboxWaitsetInitialSleepTimeAsString()
     *
     * @return zimbraMailboxWaitsetInitialSleepTime in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1873)
    public long getMailboxWaitsetInitialSleepTime() {
        return getTimeInterval(Provisioning.A_zimbraMailboxWaitsetInitialSleepTime, -1L);
    }

    /**
     * Initial timeout in milliseconds to wait before processing any
     * WaitSetRequest.. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @return zimbraMailboxWaitsetInitialSleepTime, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1873)
    public String getMailboxWaitsetInitialSleepTimeAsString() {
        return getAttr(Provisioning.A_zimbraMailboxWaitsetInitialSleepTime, null);
    }

    /**
     * Initial timeout in milliseconds to wait before processing any
     * WaitSetRequest.. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param zimbraMailboxWaitsetInitialSleepTime new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1873)
    public void setMailboxWaitsetInitialSleepTime(String zimbraMailboxWaitsetInitialSleepTime) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetInitialSleepTime, zimbraMailboxWaitsetInitialSleepTime);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Initial timeout in milliseconds to wait before processing any
     * WaitSetRequest.. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param zimbraMailboxWaitsetInitialSleepTime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1873)
    public Map<String,Object> setMailboxWaitsetInitialSleepTime(String zimbraMailboxWaitsetInitialSleepTime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetInitialSleepTime, zimbraMailboxWaitsetInitialSleepTime);
        return attrs;
    }

    /**
     * Initial timeout in milliseconds to wait before processing any
     * WaitSetRequest.. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1873)
    public void unsetMailboxWaitsetInitialSleepTime() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetInitialSleepTime, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Initial timeout in milliseconds to wait before processing any
     * WaitSetRequest.. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1873)
    public Map<String,Object> unsetMailboxWaitsetInitialSleepTime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetInitialSleepTime, "");
        return attrs;
    }

    /**
     * Maximum number of non-admin WaitSets a single account may have open.
     *
     * @return zimbraMailboxWaitsetMaxPerAccount, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1874)
    public int getMailboxWaitsetMaxPerAccount() {
        return getIntAttr(Provisioning.A_zimbraMailboxWaitsetMaxPerAccount, -1);
    }

    /**
     * Maximum number of non-admin WaitSets a single account may have open.
     *
     * @param zimbraMailboxWaitsetMaxPerAccount new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1874)
    public void setMailboxWaitsetMaxPerAccount(int zimbraMailboxWaitsetMaxPerAccount) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetMaxPerAccount, Integer.toString(zimbraMailboxWaitsetMaxPerAccount));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of non-admin WaitSets a single account may have open.
     *
     * @param zimbraMailboxWaitsetMaxPerAccount new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1874)
    public Map<String,Object> setMailboxWaitsetMaxPerAccount(int zimbraMailboxWaitsetMaxPerAccount, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetMaxPerAccount, Integer.toString(zimbraMailboxWaitsetMaxPerAccount));
        return attrs;
    }

    /**
     * Maximum number of non-admin WaitSets a single account may have open.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1874)
    public void unsetMailboxWaitsetMaxPerAccount() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetMaxPerAccount, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum number of non-admin WaitSets a single account may have open.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1874)
    public Map<String,Object> unsetMailboxWaitsetMaxPerAccount(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetMaxPerAccount, "");
        return attrs;
    }

    /**
     * Maximum Timeout in seconds a non-admin WaitSetRequest will block..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * <p>Use getMailboxWaitsetMaxRequestTimeoutAsString to access value as a string.
     *
     * @see #getMailboxWaitsetMaxRequestTimeoutAsString()
     *
     * @return zimbraMailboxWaitsetMaxRequestTimeout in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1875)
    public long getMailboxWaitsetMaxRequestTimeout() {
        return getTimeInterval(Provisioning.A_zimbraMailboxWaitsetMaxRequestTimeout, -1L);
    }

    /**
     * Maximum Timeout in seconds a non-admin WaitSetRequest will block..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @return zimbraMailboxWaitsetMaxRequestTimeout, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1875)
    public String getMailboxWaitsetMaxRequestTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraMailboxWaitsetMaxRequestTimeout, null);
    }

    /**
     * Maximum Timeout in seconds a non-admin WaitSetRequest will block..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraMailboxWaitsetMaxRequestTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1875)
    public void setMailboxWaitsetMaxRequestTimeout(String zimbraMailboxWaitsetMaxRequestTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetMaxRequestTimeout, zimbraMailboxWaitsetMaxRequestTimeout);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum Timeout in seconds a non-admin WaitSetRequest will block..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraMailboxWaitsetMaxRequestTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1875)
    public Map<String,Object> setMailboxWaitsetMaxRequestTimeout(String zimbraMailboxWaitsetMaxRequestTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetMaxRequestTimeout, zimbraMailboxWaitsetMaxRequestTimeout);
        return attrs;
    }

    /**
     * Maximum Timeout in seconds a non-admin WaitSetRequest will block..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1875)
    public void unsetMailboxWaitsetMaxRequestTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetMaxRequestTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum Timeout in seconds a non-admin WaitSetRequest will block..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1875)
    public Map<String,Object> unsetMailboxWaitsetMaxRequestTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetMaxRequestTimeout, "");
        return attrs;
    }

    /**
     * Minimum Timeout (seconds) a non-admin WaitSetRequest will block. Must
     * be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * <p>Use getMailboxWaitsetMinRequestTimeoutAsString to access value as a string.
     *
     * @see #getMailboxWaitsetMinRequestTimeoutAsString()
     *
     * @return zimbraMailboxWaitsetMinRequestTimeout in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1876)
    public long getMailboxWaitsetMinRequestTimeout() {
        return getTimeInterval(Provisioning.A_zimbraMailboxWaitsetMinRequestTimeout, -1L);
    }

    /**
     * Minimum Timeout (seconds) a non-admin WaitSetRequest will block. Must
     * be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @return zimbraMailboxWaitsetMinRequestTimeout, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1876)
    public String getMailboxWaitsetMinRequestTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraMailboxWaitsetMinRequestTimeout, null);
    }

    /**
     * Minimum Timeout (seconds) a non-admin WaitSetRequest will block. Must
     * be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraMailboxWaitsetMinRequestTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1876)
    public void setMailboxWaitsetMinRequestTimeout(String zimbraMailboxWaitsetMinRequestTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetMinRequestTimeout, zimbraMailboxWaitsetMinRequestTimeout);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Minimum Timeout (seconds) a non-admin WaitSetRequest will block. Must
     * be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraMailboxWaitsetMinRequestTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1876)
    public Map<String,Object> setMailboxWaitsetMinRequestTimeout(String zimbraMailboxWaitsetMinRequestTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetMinRequestTimeout, zimbraMailboxWaitsetMinRequestTimeout);
        return attrs;
    }

    /**
     * Minimum Timeout (seconds) a non-admin WaitSetRequest will block. Must
     * be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1876)
    public void unsetMailboxWaitsetMinRequestTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetMinRequestTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Minimum Timeout (seconds) a non-admin WaitSetRequest will block. Must
     * be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1876)
    public Map<String,Object> unsetMailboxWaitsetMinRequestTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetMinRequestTimeout, "");
        return attrs;
    }

    /**
     * Time in milliseconds to sleep handling a WaitSetRequest if there is no
     * data after initial check. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * <p>Use getMailboxWaitsetNoDataSleepTimeAsString to access value as a string.
     *
     * @see #getMailboxWaitsetNoDataSleepTimeAsString()
     *
     * @return zimbraMailboxWaitsetNoDataSleepTime in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1877)
    public long getMailboxWaitsetNoDataSleepTime() {
        return getTimeInterval(Provisioning.A_zimbraMailboxWaitsetNoDataSleepTime, -1L);
    }

    /**
     * Time in milliseconds to sleep handling a WaitSetRequest if there is no
     * data after initial check. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @return zimbraMailboxWaitsetNoDataSleepTime, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1877)
    public String getMailboxWaitsetNoDataSleepTimeAsString() {
        return getAttr(Provisioning.A_zimbraMailboxWaitsetNoDataSleepTime, null);
    }

    /**
     * Time in milliseconds to sleep handling a WaitSetRequest if there is no
     * data after initial check. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param zimbraMailboxWaitsetNoDataSleepTime new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1877)
    public void setMailboxWaitsetNoDataSleepTime(String zimbraMailboxWaitsetNoDataSleepTime) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetNoDataSleepTime, zimbraMailboxWaitsetNoDataSleepTime);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Time in milliseconds to sleep handling a WaitSetRequest if there is no
     * data after initial check. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param zimbraMailboxWaitsetNoDataSleepTime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1877)
    public Map<String,Object> setMailboxWaitsetNoDataSleepTime(String zimbraMailboxWaitsetNoDataSleepTime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetNoDataSleepTime, zimbraMailboxWaitsetNoDataSleepTime);
        return attrs;
    }

    /**
     * Time in milliseconds to sleep handling a WaitSetRequest if there is no
     * data after initial check. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1877)
    public void unsetMailboxWaitsetNoDataSleepTime() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetNoDataSleepTime, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Time in milliseconds to sleep handling a WaitSetRequest if there is no
     * data after initial check. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1877)
    public Map<String,Object> unsetMailboxWaitsetNoDataSleepTime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxWaitsetNoDataSleepTime, "");
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
     * Milter server drops client connection which are inactive for more than
     * zimbraMilterMaxIdleTIme seconds. postfix 2.11 has 2 timeouts which
     * affect whether to accept a message when DATA is coming slowly from the
     * remote system. One is every 300s (smtpd_timeout?) which fires when not
     * data arrives for that time.The other gets noticed once all data has
     * been read if more than 3600s (ipc_timeout?) has passed since the
     * connection was initiated and results in &#039;451 4.3.0 Error: queue
     * file write error&#039;.Commands are sent to milter for &quot;mail
     * from&quot; and &quot;rcpt to&quot; entries, then potentially no
     * further communication is made until all data for the message has been
     * read, so milter_max_idle_time needs to be long enough for that. The
     * value of milter_max_idle_time is to ensure we drop the connection if
     * there is a problem at the MTA end - hence the default value is
     * slightly longer than the max time the MTA should need. . Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * <p>Use getMilterMaxIdleTimeAsString to access value as a string.
     *
     * @see #getMilterMaxIdleTimeAsString()
     *
     * @return zimbraMilterMaxIdleTime in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1769)
    public long getMilterMaxIdleTime() {
        return getTimeInterval(Provisioning.A_zimbraMilterMaxIdleTime, -1L);
    }

    /**
     * Milter server drops client connection which are inactive for more than
     * zimbraMilterMaxIdleTIme seconds. postfix 2.11 has 2 timeouts which
     * affect whether to accept a message when DATA is coming slowly from the
     * remote system. One is every 300s (smtpd_timeout?) which fires when not
     * data arrives for that time.The other gets noticed once all data has
     * been read if more than 3600s (ipc_timeout?) has passed since the
     * connection was initiated and results in &#039;451 4.3.0 Error: queue
     * file write error&#039;.Commands are sent to milter for &quot;mail
     * from&quot; and &quot;rcpt to&quot; entries, then potentially no
     * further communication is made until all data for the message has been
     * read, so milter_max_idle_time needs to be long enough for that. The
     * value of milter_max_idle_time is to ensure we drop the connection if
     * there is a problem at the MTA end - hence the default value is
     * slightly longer than the max time the MTA should need. . Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @return zimbraMilterMaxIdleTime, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1769)
    public String getMilterMaxIdleTimeAsString() {
        return getAttr(Provisioning.A_zimbraMilterMaxIdleTime, null);
    }

    /**
     * Milter server drops client connection which are inactive for more than
     * zimbraMilterMaxIdleTIme seconds. postfix 2.11 has 2 timeouts which
     * affect whether to accept a message when DATA is coming slowly from the
     * remote system. One is every 300s (smtpd_timeout?) which fires when not
     * data arrives for that time.The other gets noticed once all data has
     * been read if more than 3600s (ipc_timeout?) has passed since the
     * connection was initiated and results in &#039;451 4.3.0 Error: queue
     * file write error&#039;.Commands are sent to milter for &quot;mail
     * from&quot; and &quot;rcpt to&quot; entries, then potentially no
     * further communication is made until all data for the message has been
     * read, so milter_max_idle_time needs to be long enough for that. The
     * value of milter_max_idle_time is to ensure we drop the connection if
     * there is a problem at the MTA end - hence the default value is
     * slightly longer than the max time the MTA should need. . Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraMilterMaxIdleTime new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1769)
    public void setMilterMaxIdleTime(String zimbraMilterMaxIdleTime) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMilterMaxIdleTime, zimbraMilterMaxIdleTime);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Milter server drops client connection which are inactive for more than
     * zimbraMilterMaxIdleTIme seconds. postfix 2.11 has 2 timeouts which
     * affect whether to accept a message when DATA is coming slowly from the
     * remote system. One is every 300s (smtpd_timeout?) which fires when not
     * data arrives for that time.The other gets noticed once all data has
     * been read if more than 3600s (ipc_timeout?) has passed since the
     * connection was initiated and results in &#039;451 4.3.0 Error: queue
     * file write error&#039;.Commands are sent to milter for &quot;mail
     * from&quot; and &quot;rcpt to&quot; entries, then potentially no
     * further communication is made until all data for the message has been
     * read, so milter_max_idle_time needs to be long enough for that. The
     * value of milter_max_idle_time is to ensure we drop the connection if
     * there is a problem at the MTA end - hence the default value is
     * slightly longer than the max time the MTA should need. . Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraMilterMaxIdleTime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1769)
    public Map<String,Object> setMilterMaxIdleTime(String zimbraMilterMaxIdleTime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMilterMaxIdleTime, zimbraMilterMaxIdleTime);
        return attrs;
    }

    /**
     * Milter server drops client connection which are inactive for more than
     * zimbraMilterMaxIdleTIme seconds. postfix 2.11 has 2 timeouts which
     * affect whether to accept a message when DATA is coming slowly from the
     * remote system. One is every 300s (smtpd_timeout?) which fires when not
     * data arrives for that time.The other gets noticed once all data has
     * been read if more than 3600s (ipc_timeout?) has passed since the
     * connection was initiated and results in &#039;451 4.3.0 Error: queue
     * file write error&#039;.Commands are sent to milter for &quot;mail
     * from&quot; and &quot;rcpt to&quot; entries, then potentially no
     * further communication is made until all data for the message has been
     * read, so milter_max_idle_time needs to be long enough for that. The
     * value of milter_max_idle_time is to ensure we drop the connection if
     * there is a problem at the MTA end - hence the default value is
     * slightly longer than the max time the MTA should need. . Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1769)
    public void unsetMilterMaxIdleTime() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMilterMaxIdleTime, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Milter server drops client connection which are inactive for more than
     * zimbraMilterMaxIdleTIme seconds. postfix 2.11 has 2 timeouts which
     * affect whether to accept a message when DATA is coming slowly from the
     * remote system. One is every 300s (smtpd_timeout?) which fires when not
     * data arrives for that time.The other gets noticed once all data has
     * been read if more than 3600s (ipc_timeout?) has passed since the
     * connection was initiated and results in &#039;451 4.3.0 Error: queue
     * file write error&#039;.Commands are sent to milter for &quot;mail
     * from&quot; and &quot;rcpt to&quot; entries, then potentially no
     * further communication is made until all data for the message has been
     * read, so milter_max_idle_time needs to be long enough for that. The
     * value of milter_max_idle_time is to ensure we drop the connection if
     * there is a problem at the MTA end - hence the default value is
     * slightly longer than the max time the MTA should need. . Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1769)
    public Map<String,Object> unsetMilterMaxIdleTime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMilterMaxIdleTime, "");
        return attrs;
    }

    /**
     * Milter server keep alive time in seconds for threads in threadpool.
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * <p>Use getMilterThreadKeepAliveTimeAsString to access value as a string.
     *
     * @see #getMilterThreadKeepAliveTimeAsString()
     *
     * @return zimbraMilterThreadKeepAliveTime in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1770)
    public long getMilterThreadKeepAliveTime() {
        return getTimeInterval(Provisioning.A_zimbraMilterThreadKeepAliveTime, -1L);
    }

    /**
     * Milter server keep alive time in seconds for threads in threadpool.
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @return zimbraMilterThreadKeepAliveTime, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1770)
    public String getMilterThreadKeepAliveTimeAsString() {
        return getAttr(Provisioning.A_zimbraMilterThreadKeepAliveTime, null);
    }

    /**
     * Milter server keep alive time in seconds for threads in threadpool.
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraMilterThreadKeepAliveTime new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1770)
    public void setMilterThreadKeepAliveTime(String zimbraMilterThreadKeepAliveTime) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMilterThreadKeepAliveTime, zimbraMilterThreadKeepAliveTime);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Milter server keep alive time in seconds for threads in threadpool.
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraMilterThreadKeepAliveTime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1770)
    public Map<String,Object> setMilterThreadKeepAliveTime(String zimbraMilterThreadKeepAliveTime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMilterThreadKeepAliveTime, zimbraMilterThreadKeepAliveTime);
        return attrs;
    }

    /**
     * Milter server keep alive time in seconds for threads in threadpool.
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1770)
    public void unsetMilterThreadKeepAliveTime() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMilterThreadKeepAliveTime, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Milter server keep alive time in seconds for threads in threadpool.
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1770)
    public Map<String,Object> unsetMilterThreadKeepAliveTime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMilterThreadKeepAliveTime, "");
        return attrs;
    }

    /**
     * Write chunk size in bytes for Milter server output stream
     *
     * @return zimbraMilterWriteChunkSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1771)
    public int getMilterWriteChunkSize() {
        return getIntAttr(Provisioning.A_zimbraMilterWriteChunkSize, -1);
    }

    /**
     * Write chunk size in bytes for Milter server output stream
     *
     * @param zimbraMilterWriteChunkSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1771)
    public void setMilterWriteChunkSize(int zimbraMilterWriteChunkSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMilterWriteChunkSize, Integer.toString(zimbraMilterWriteChunkSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Write chunk size in bytes for Milter server output stream
     *
     * @param zimbraMilterWriteChunkSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1771)
    public Map<String,Object> setMilterWriteChunkSize(int zimbraMilterWriteChunkSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMilterWriteChunkSize, Integer.toString(zimbraMilterWriteChunkSize));
        return attrs;
    }

    /**
     * Write chunk size in bytes for Milter server output stream
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1771)
    public void unsetMilterWriteChunkSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMilterWriteChunkSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Write chunk size in bytes for Milter server output stream
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1771)
    public Map<String,Object> unsetMilterWriteChunkSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMilterWriteChunkSize, "");
        return attrs;
    }

    /**
     * Write timeout in seconds for Milter server output stream. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * <p>Use getMilterWriteTimeoutAsString to access value as a string.
     *
     * @see #getMilterWriteTimeoutAsString()
     *
     * @return zimbraMilterWriteTimeout in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1772)
    public long getMilterWriteTimeout() {
        return getTimeInterval(Provisioning.A_zimbraMilterWriteTimeout, -1L);
    }

    /**
     * Write timeout in seconds for Milter server output stream. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @return zimbraMilterWriteTimeout, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1772)
    public String getMilterWriteTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraMilterWriteTimeout, null);
    }

    /**
     * Write timeout in seconds for Milter server output stream. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraMilterWriteTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1772)
    public void setMilterWriteTimeout(String zimbraMilterWriteTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMilterWriteTimeout, zimbraMilterWriteTimeout);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Write timeout in seconds for Milter server output stream. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraMilterWriteTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1772)
    public Map<String,Object> setMilterWriteTimeout(String zimbraMilterWriteTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMilterWriteTimeout, zimbraMilterWriteTimeout);
        return attrs;
    }

    /**
     * Write timeout in seconds for Milter server output stream. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1772)
    public void unsetMilterWriteTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMilterWriteTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Write timeout in seconds for Milter server output stream. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1772)
    public Map<String,Object> unsetMilterWriteTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMilterWriteTimeout, "");
        return attrs;
    }

    /**
     * Whether the UUENCODE decoder is run when parsing messages from the
     * store
     *
     * @return zimbraMimeConverterEnableUuencode, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1899)
    public boolean isMimeConverterEnableUuencode() {
        return getBooleanAttr(Provisioning.A_zimbraMimeConverterEnableUuencode, false);
    }

    /**
     * Whether the UUENCODE decoder is run when parsing messages from the
     * store
     *
     * @param zimbraMimeConverterEnableUuencode new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1899)
    public void setMimeConverterEnableUuencode(boolean zimbraMimeConverterEnableUuencode) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeConverterEnableUuencode, zimbraMimeConverterEnableUuencode ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether the UUENCODE decoder is run when parsing messages from the
     * store
     *
     * @param zimbraMimeConverterEnableUuencode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1899)
    public Map<String,Object> setMimeConverterEnableUuencode(boolean zimbraMimeConverterEnableUuencode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeConverterEnableUuencode, zimbraMimeConverterEnableUuencode ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether the UUENCODE decoder is run when parsing messages from the
     * store
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1899)
    public void unsetMimeConverterEnableUuencode() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeConverterEnableUuencode, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether the UUENCODE decoder is run when parsing messages from the
     * store
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1899)
    public Map<String,Object> unsetMimeConverterEnableUuencode(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeConverterEnableUuencode, "");
        return attrs;
    }

    /**
     * Whether the TNEF decoder is run when parsing messages from the store
     *
     * @return zimbraMimeConverterEnabledTnef, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1898)
    public boolean isMimeConverterEnabledTnef() {
        return getBooleanAttr(Provisioning.A_zimbraMimeConverterEnabledTnef, false);
    }

    /**
     * Whether the TNEF decoder is run when parsing messages from the store
     *
     * @param zimbraMimeConverterEnabledTnef new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1898)
    public void setMimeConverterEnabledTnef(boolean zimbraMimeConverterEnabledTnef) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeConverterEnabledTnef, zimbraMimeConverterEnabledTnef ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether the TNEF decoder is run when parsing messages from the store
     *
     * @param zimbraMimeConverterEnabledTnef new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1898)
    public Map<String,Object> setMimeConverterEnabledTnef(boolean zimbraMimeConverterEnabledTnef, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeConverterEnabledTnef, zimbraMimeConverterEnabledTnef ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Whether the TNEF decoder is run when parsing messages from the store
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1898)
    public void unsetMimeConverterEnabledTnef() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeConverterEnabledTnef, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Whether the TNEF decoder is run when parsing messages from the store
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1898)
    public Map<String,Object> unsetMimeConverterEnabledTnef(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeConverterEnabledTnef, "");
        return attrs;
    }

    /**
     * Recursive MIME part depth beneath which converters will not act
     *
     * @return zimbraMimeConverterMaxMimepartDepth, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1897)
    public int getMimeConverterMaxMimepartDepth() {
        return getIntAttr(Provisioning.A_zimbraMimeConverterMaxMimepartDepth, -1);
    }

    /**
     * Recursive MIME part depth beneath which converters will not act
     *
     * @param zimbraMimeConverterMaxMimepartDepth new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1897)
    public void setMimeConverterMaxMimepartDepth(int zimbraMimeConverterMaxMimepartDepth) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeConverterMaxMimepartDepth, Integer.toString(zimbraMimeConverterMaxMimepartDepth));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Recursive MIME part depth beneath which converters will not act
     *
     * @param zimbraMimeConverterMaxMimepartDepth new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1897)
    public Map<String,Object> setMimeConverterMaxMimepartDepth(int zimbraMimeConverterMaxMimepartDepth, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeConverterMaxMimepartDepth, Integer.toString(zimbraMimeConverterMaxMimepartDepth));
        return attrs;
    }

    /**
     * Recursive MIME part depth beneath which converters will not act
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1897)
    public void unsetMimeConverterMaxMimepartDepth() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeConverterMaxMimepartDepth, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Recursive MIME part depth beneath which converters will not act
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1897)
    public Map<String,Object> unsetMimeConverterMaxMimepartDepth(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeConverterMaxMimepartDepth, "");
        return attrs;
    }

    /**
     * Enable text extraction for the during mime parsing
     *
     * @return zimbraMimeEnableTextExtraction, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1853)
    public boolean isMimeEnableTextExtraction() {
        return getBooleanAttr(Provisioning.A_zimbraMimeEnableTextExtraction, false);
    }

    /**
     * Enable text extraction for the during mime parsing
     *
     * @param zimbraMimeEnableTextExtraction new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1853)
    public void setMimeEnableTextExtraction(boolean zimbraMimeEnableTextExtraction) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeEnableTextExtraction, zimbraMimeEnableTextExtraction ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enable text extraction for the during mime parsing
     *
     * @param zimbraMimeEnableTextExtraction new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1853)
    public Map<String,Object> setMimeEnableTextExtraction(boolean zimbraMimeEnableTextExtraction, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeEnableTextExtraction, zimbraMimeEnableTextExtraction ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Enable text extraction for the during mime parsing
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1853)
    public void unsetMimeEnableTextExtraction() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeEnableTextExtraction, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Enable text extraction for the during mime parsing
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1853)
    public Map<String,Object> unsetMimeEnableTextExtraction(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeEnableTextExtraction, "");
        return attrs;
    }

    /**
     * Flag to enable or disable encoding the missing blob in mime part
     *
     * @return zimbraMimeEncodeMissingBlob, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1763)
    public boolean isMimeEncodeMissingBlob() {
        return getBooleanAttr(Provisioning.A_zimbraMimeEncodeMissingBlob, false);
    }

    /**
     * Flag to enable or disable encoding the missing blob in mime part
     *
     * @param zimbraMimeEncodeMissingBlob new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1763)
    public void setMimeEncodeMissingBlob(boolean zimbraMimeEncodeMissingBlob) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeEncodeMissingBlob, zimbraMimeEncodeMissingBlob ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable or disable encoding the missing blob in mime part
     *
     * @param zimbraMimeEncodeMissingBlob new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1763)
    public Map<String,Object> setMimeEncodeMissingBlob(boolean zimbraMimeEncodeMissingBlob, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeEncodeMissingBlob, zimbraMimeEncodeMissingBlob ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Flag to enable or disable encoding the missing blob in mime part
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1763)
    public void unsetMimeEncodeMissingBlob() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeEncodeMissingBlob, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable or disable encoding the missing blob in mime part
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1763)
    public Map<String,Object> unsetMimeEncodeMissingBlob(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeEncodeMissingBlob, "");
        return attrs;
    }

    /**
     * Flag to disable empty content in mime message
     *
     * @return zimbraMimeExcludeEmptyContent, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1764)
    public boolean isMimeExcludeEmptyContent() {
        return getBooleanAttr(Provisioning.A_zimbraMimeExcludeEmptyContent, false);
    }

    /**
     * Flag to disable empty content in mime message
     *
     * @param zimbraMimeExcludeEmptyContent new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1764)
    public void setMimeExcludeEmptyContent(boolean zimbraMimeExcludeEmptyContent) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeExcludeEmptyContent, zimbraMimeExcludeEmptyContent ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to disable empty content in mime message
     *
     * @param zimbraMimeExcludeEmptyContent new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1764)
    public Map<String,Object> setMimeExcludeEmptyContent(boolean zimbraMimeExcludeEmptyContent, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeExcludeEmptyContent, zimbraMimeExcludeEmptyContent ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Flag to disable empty content in mime message
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1764)
    public void unsetMimeExcludeEmptyContent() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeExcludeEmptyContent, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to disable empty content in mime message
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1764)
    public Map<String,Object> unsetMimeExcludeEmptyContent(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeExcludeEmptyContent, "");
        return attrs;
    }

    /**
     * Maximum image size in bytes to resize
     *
     * @return zimbraMimeMaxImageSizeToResize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1801)
    public int getMimeMaxImageSizeToResize() {
        return getIntAttr(Provisioning.A_zimbraMimeMaxImageSizeToResize, -1);
    }

    /**
     * Maximum image size in bytes to resize
     *
     * @param zimbraMimeMaxImageSizeToResize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1801)
    public void setMimeMaxImageSizeToResize(int zimbraMimeMaxImageSizeToResize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeMaxImageSizeToResize, Integer.toString(zimbraMimeMaxImageSizeToResize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum image size in bytes to resize
     *
     * @param zimbraMimeMaxImageSizeToResize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1801)
    public Map<String,Object> setMimeMaxImageSizeToResize(int zimbraMimeMaxImageSizeToResize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeMaxImageSizeToResize, Integer.toString(zimbraMimeMaxImageSizeToResize));
        return attrs;
    }

    /**
     * Maximum image size in bytes to resize
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1801)
    public void unsetMimeMaxImageSizeToResize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeMaxImageSizeToResize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum image size in bytes to resize
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1801)
    public Map<String,Object> unsetMimeMaxImageSizeToResize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeMaxImageSizeToResize, "");
        return attrs;
    }

    /**
     * If true, always encode text attachments as base64
     *
     * @return zimbraMimeOverrideDefaultTransferEncodingToBase64, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1894)
    public boolean isMimeOverrideDefaultTransferEncodingToBase64() {
        return getBooleanAttr(Provisioning.A_zimbraMimeOverrideDefaultTransferEncodingToBase64, false);
    }

    /**
     * If true, always encode text attachments as base64
     *
     * @param zimbraMimeOverrideDefaultTransferEncodingToBase64 new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1894)
    public void setMimeOverrideDefaultTransferEncodingToBase64(boolean zimbraMimeOverrideDefaultTransferEncodingToBase64) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeOverrideDefaultTransferEncodingToBase64, zimbraMimeOverrideDefaultTransferEncodingToBase64 ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * If true, always encode text attachments as base64
     *
     * @param zimbraMimeOverrideDefaultTransferEncodingToBase64 new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1894)
    public Map<String,Object> setMimeOverrideDefaultTransferEncodingToBase64(boolean zimbraMimeOverrideDefaultTransferEncodingToBase64, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeOverrideDefaultTransferEncodingToBase64, zimbraMimeOverrideDefaultTransferEncodingToBase64 ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * If true, always encode text attachments as base64
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1894)
    public void unsetMimeOverrideDefaultTransferEncodingToBase64() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeOverrideDefaultTransferEncodingToBase64, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * If true, always encode text attachments as base64
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1894)
    public Map<String,Object> unsetMimeOverrideDefaultTransferEncodingToBase64(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMimeOverrideDefaultTransferEncodingToBase64, "");
        return attrs;
    }

    /**
     * Flag to enable streaming of jdbc results.
     *
     * @return zimbraMysqlJdbcResultStreamingEnabled, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1789)
    public boolean isMysqlJdbcResultStreamingEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraMysqlJdbcResultStreamingEnabled, false);
    }

    /**
     * Flag to enable streaming of jdbc results.
     *
     * @param zimbraMysqlJdbcResultStreamingEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1789)
    public void setMysqlJdbcResultStreamingEnabled(boolean zimbraMysqlJdbcResultStreamingEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMysqlJdbcResultStreamingEnabled, zimbraMysqlJdbcResultStreamingEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable streaming of jdbc results.
     *
     * @param zimbraMysqlJdbcResultStreamingEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1789)
    public Map<String,Object> setMysqlJdbcResultStreamingEnabled(boolean zimbraMysqlJdbcResultStreamingEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMysqlJdbcResultStreamingEnabled, zimbraMysqlJdbcResultStreamingEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Flag to enable streaming of jdbc results.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1789)
    public void unsetMysqlJdbcResultStreamingEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMysqlJdbcResultStreamingEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable streaming of jdbc results.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1789)
    public Map<String,Object> unsetMysqlJdbcResultStreamingEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMysqlJdbcResultStreamingEnabled, "");
        return attrs;
    }

    /**
     * Nio maximum write queue size in bytes
     *
     * @return zimbraNioMaxWriteQueueSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1803)
    public int getNioMaxWriteQueueSize() {
        return getIntAttr(Provisioning.A_zimbraNioMaxWriteQueueSize, -1);
    }

    /**
     * Nio maximum write queue size in bytes
     *
     * @param zimbraNioMaxWriteQueueSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1803)
    public void setNioMaxWriteQueueSize(int zimbraNioMaxWriteQueueSize) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNioMaxWriteQueueSize, Integer.toString(zimbraNioMaxWriteQueueSize));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Nio maximum write queue size in bytes
     *
     * @param zimbraNioMaxWriteQueueSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1803)
    public Map<String,Object> setNioMaxWriteQueueSize(int zimbraNioMaxWriteQueueSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNioMaxWriteQueueSize, Integer.toString(zimbraNioMaxWriteQueueSize));
        return attrs;
    }

    /**
     * Nio maximum write queue size in bytes
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1803)
    public void unsetNioMaxWriteQueueSize() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNioMaxWriteQueueSize, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Nio maximum write queue size in bytes
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1803)
    public Map<String,Object> unsetNioMaxWriteQueueSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNioMaxWriteQueueSize, "");
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
     * Maximum no of consecutive errors for retry
     *
     * @return zimbraPop3MaxConsecutiveError, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1805)
    public int getPop3MaxConsecutiveError() {
        return getIntAttr(Provisioning.A_zimbraPop3MaxConsecutiveError, -1);
    }

    /**
     * Maximum no of consecutive errors for retry
     *
     * @param zimbraPop3MaxConsecutiveError new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1805)
    public void setPop3MaxConsecutiveError(int zimbraPop3MaxConsecutiveError) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3MaxConsecutiveError, Integer.toString(zimbraPop3MaxConsecutiveError));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum no of consecutive errors for retry
     *
     * @param zimbraPop3MaxConsecutiveError new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1805)
    public Map<String,Object> setPop3MaxConsecutiveError(int zimbraPop3MaxConsecutiveError, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3MaxConsecutiveError, Integer.toString(zimbraPop3MaxConsecutiveError));
        return attrs;
    }

    /**
     * Maximum no of consecutive errors for retry
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1805)
    public void unsetPop3MaxConsecutiveError() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3MaxConsecutiveError, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum no of consecutive errors for retry
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1805)
    public Map<String,Object> unsetPop3MaxConsecutiveError(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3MaxConsecutiveError, "");
        return attrs;
    }

    /**
     * Maximum idle time in seconds. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * <p>Use getPop3MaxIdleTimeAsString to access value as a string.
     *
     * @see #getPop3MaxIdleTimeAsString()
     *
     * @return zimbraPop3MaxIdleTime in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1806)
    public long getPop3MaxIdleTime() {
        return getTimeInterval(Provisioning.A_zimbraPop3MaxIdleTime, -1L);
    }

    /**
     * Maximum idle time in seconds. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @return zimbraPop3MaxIdleTime, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1806)
    public String getPop3MaxIdleTimeAsString() {
        return getAttr(Provisioning.A_zimbraPop3MaxIdleTime, null);
    }

    /**
     * Maximum idle time in seconds. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param zimbraPop3MaxIdleTime new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1806)
    public void setPop3MaxIdleTime(String zimbraPop3MaxIdleTime) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3MaxIdleTime, zimbraPop3MaxIdleTime);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum idle time in seconds. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param zimbraPop3MaxIdleTime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1806)
    public Map<String,Object> setPop3MaxIdleTime(String zimbraPop3MaxIdleTime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3MaxIdleTime, zimbraPop3MaxIdleTime);
        return attrs;
    }

    /**
     * Maximum idle time in seconds. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1806)
    public void unsetPop3MaxIdleTime() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3MaxIdleTime, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Maximum idle time in seconds. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1806)
    public Map<String,Object> unsetPop3MaxIdleTime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3MaxIdleTime, "");
        return attrs;
    }

    /**
     * Flag to enable pop3 nio
     *
     * @return zimbraPop3NioEnabled, or false if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1794)
    public boolean isPop3NioEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPop3NioEnabled, false);
    }

    /**
     * Flag to enable pop3 nio
     *
     * @param zimbraPop3NioEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1794)
    public void setPop3NioEnabled(boolean zimbraPop3NioEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3NioEnabled, zimbraPop3NioEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable pop3 nio
     *
     * @param zimbraPop3NioEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1794)
    public Map<String,Object> setPop3NioEnabled(boolean zimbraPop3NioEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3NioEnabled, zimbraPop3NioEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * Flag to enable pop3 nio
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1794)
    public void unsetPop3NioEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3NioEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Flag to enable pop3 nio
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1794)
    public Map<String,Object> unsetPop3NioEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3NioEnabled, "");
        return attrs;
    }

    /**
     * Pop3 maximum keep alive time in seconds. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * <p>Use getPop3ThreadKeepAliveTimeAsString to access value as a string.
     *
     * @see #getPop3ThreadKeepAliveTimeAsString()
     *
     * @return zimbraPop3ThreadKeepAliveTime in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1808)
    public long getPop3ThreadKeepAliveTime() {
        return getTimeInterval(Provisioning.A_zimbraPop3ThreadKeepAliveTime, -1L);
    }

    /**
     * Pop3 maximum keep alive time in seconds. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @return zimbraPop3ThreadKeepAliveTime, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1808)
    public String getPop3ThreadKeepAliveTimeAsString() {
        return getAttr(Provisioning.A_zimbraPop3ThreadKeepAliveTime, null);
    }

    /**
     * Pop3 maximum keep alive time in seconds. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @param zimbraPop3ThreadKeepAliveTime new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1808)
    public void setPop3ThreadKeepAliveTime(String zimbraPop3ThreadKeepAliveTime) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ThreadKeepAliveTime, zimbraPop3ThreadKeepAliveTime);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Pop3 maximum keep alive time in seconds. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @param zimbraPop3ThreadKeepAliveTime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1808)
    public Map<String,Object> setPop3ThreadKeepAliveTime(String zimbraPop3ThreadKeepAliveTime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ThreadKeepAliveTime, zimbraPop3ThreadKeepAliveTime);
        return attrs;
    }

    /**
     * Pop3 maximum keep alive time in seconds. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1808)
    public void unsetPop3ThreadKeepAliveTime() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ThreadKeepAliveTime, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Pop3 maximum keep alive time in seconds. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1808)
    public Map<String,Object> unsetPop3ThreadKeepAliveTime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ThreadKeepAliveTime, "");
        return attrs;
    }

    /**
     * pop3 throttle account. Maximum allowed requests per second per account
     * .
     *
     * @return zimbraPop3ThrottleAcctLimit, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1810)
    public int getPop3ThrottleAcctLimit() {
        return getIntAttr(Provisioning.A_zimbraPop3ThrottleAcctLimit, -1);
    }

    /**
     * pop3 throttle account. Maximum allowed requests per second per account
     * .
     *
     * @param zimbraPop3ThrottleAcctLimit new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1810)
    public void setPop3ThrottleAcctLimit(int zimbraPop3ThrottleAcctLimit) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ThrottleAcctLimit, Integer.toString(zimbraPop3ThrottleAcctLimit));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * pop3 throttle account. Maximum allowed requests per second per account
     * .
     *
     * @param zimbraPop3ThrottleAcctLimit new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1810)
    public Map<String,Object> setPop3ThrottleAcctLimit(int zimbraPop3ThrottleAcctLimit, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ThrottleAcctLimit, Integer.toString(zimbraPop3ThrottleAcctLimit));
        return attrs;
    }

    /**
     * pop3 throttle account. Maximum allowed requests per second per account
     * .
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1810)
    public void unsetPop3ThrottleAcctLimit() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ThrottleAcctLimit, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * pop3 throttle account. Maximum allowed requests per second per account
     * .
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1810)
    public Map<String,Object> unsetPop3ThrottleAcctLimit(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ThrottleAcctLimit, "");
        return attrs;
    }

    /**
     * pop3 ip throttle. Maximum allowed requests per second per ip .
     *
     * @return zimbraPop3ThrottleIpLimit, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1809)
    public int getPop3ThrottleIpLimit() {
        return getIntAttr(Provisioning.A_zimbraPop3ThrottleIpLimit, -1);
    }

    /**
     * pop3 ip throttle. Maximum allowed requests per second per ip .
     *
     * @param zimbraPop3ThrottleIpLimit new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1809)
    public void setPop3ThrottleIpLimit(int zimbraPop3ThrottleIpLimit) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ThrottleIpLimit, Integer.toString(zimbraPop3ThrottleIpLimit));
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * pop3 ip throttle. Maximum allowed requests per second per ip .
     *
     * @param zimbraPop3ThrottleIpLimit new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1809)
    public Map<String,Object> setPop3ThrottleIpLimit(int zimbraPop3ThrottleIpLimit, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ThrottleIpLimit, Integer.toString(zimbraPop3ThrottleIpLimit));
        return attrs;
    }

    /**
     * pop3 ip throttle. Maximum allowed requests per second per ip .
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1809)
    public void unsetPop3ThrottleIpLimit() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ThrottleIpLimit, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * pop3 ip throttle. Maximum allowed requests per second per ip .
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1809)
    public Map<String,Object> unsetPop3ThrottleIpLimit(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3ThrottleIpLimit, "");
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
     * Pop3 write timeout in seconds. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * <p>Use getPop3WriteTimeoutAsString to access value as a string.
     *
     * @see #getPop3WriteTimeoutAsString()
     *
     * @return zimbraPop3WriteTimeout in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1807)
    public long getPop3WriteTimeout() {
        return getTimeInterval(Provisioning.A_zimbraPop3WriteTimeout, -1L);
    }

    /**
     * Pop3 write timeout in seconds. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @return zimbraPop3WriteTimeout, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1807)
    public String getPop3WriteTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraPop3WriteTimeout, null);
    }

    /**
     * Pop3 write timeout in seconds. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param zimbraPop3WriteTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1807)
    public void setPop3WriteTimeout(String zimbraPop3WriteTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3WriteTimeout, zimbraPop3WriteTimeout);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Pop3 write timeout in seconds. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param zimbraPop3WriteTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1807)
    public Map<String,Object> setPop3WriteTimeout(String zimbraPop3WriteTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3WriteTimeout, zimbraPop3WriteTimeout);
        return attrs;
    }

    /**
     * Pop3 write timeout in seconds. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1807)
    public void unsetPop3WriteTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3WriteTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Pop3 write timeout in seconds. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1807)
    public Map<String,Object> unsetPop3WriteTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3WriteTimeout, "");
        return attrs;
    }

    /**
     * Redis url
     *
     * @return zimbraRedisUrl, or empty array if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1788)
    public String[] getRedisUrl() {
        return getMultiAttr(Provisioning.A_zimbraRedisUrl);
    }

    /**
     * Redis url
     *
     * @param zimbraRedisUrl new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1788)
    public void setRedisUrl(String[] zimbraRedisUrl) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedisUrl, zimbraRedisUrl);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Redis url
     *
     * @param zimbraRedisUrl new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1788)
    public Map<String,Object> setRedisUrl(String[] zimbraRedisUrl, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedisUrl, zimbraRedisUrl);
        return attrs;
    }

    /**
     * Redis url
     *
     * @param zimbraRedisUrl new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1788)
    public void addRedisUrl(String zimbraRedisUrl) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraRedisUrl, zimbraRedisUrl);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Redis url
     *
     * @param zimbraRedisUrl new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1788)
    public Map<String,Object> addRedisUrl(String zimbraRedisUrl, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraRedisUrl, zimbraRedisUrl);
        return attrs;
    }

    /**
     * Redis url
     *
     * @param zimbraRedisUrl existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1788)
    public void removeRedisUrl(String zimbraRedisUrl) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraRedisUrl, zimbraRedisUrl);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Redis url
     *
     * @param zimbraRedisUrl existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1788)
    public Map<String,Object> removeRedisUrl(String zimbraRedisUrl, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraRedisUrl, zimbraRedisUrl);
        return attrs;
    }

    /**
     * Redis url
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1788)
    public void unsetRedisUrl() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedisUrl, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Redis url
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1788)
    public Map<String,Object> unsetRedisUrl(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraRedisUrl, "");
        return attrs;
    }

    /**
     * Maximum number of items to put into a single reindexing task.
     *
     * @return zimbraReindexBatchSize, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1834)
    public int getReindexBatchSize() {
        return getIntAttr(Provisioning.A_zimbraReindexBatchSize, -1);
    }

    /**
     * Maximum number of items to put into a single reindexing task.
     *
     * @param zimbraReindexBatchSize new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1834)
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
     * @since ZCS 9.0
     */
    @ZAttr(id=1834)
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
     * @since ZCS 9.0
     */
    @ZAttr(id=1834)
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
     * @since ZCS 9.0
     */
    @ZAttr(id=1834)
    public Map<String,Object> unsetReindexBatchSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraReindexBatchSize, "");
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
     * Timeout for zimlet deployment specified in seconds.. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * <p>Use getZimletDeployTimeoutAsString to access value as a string.
     *
     * @see #getZimletDeployTimeoutAsString()
     *
     * @return zimbraZimletDeployTimeout in millseconds, or -1 if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1858)
    public long getZimletDeployTimeout() {
        return getTimeInterval(Provisioning.A_zimbraZimletDeployTimeout, -1L);
    }

    /**
     * Timeout for zimlet deployment specified in seconds.. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @return zimbraZimletDeployTimeout, or null if unset
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1858)
    public String getZimletDeployTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraZimletDeployTimeout, null);
    }

    /**
     * Timeout for zimlet deployment specified in seconds.. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraZimletDeployTimeout new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1858)
    public void setZimletDeployTimeout(String zimbraZimletDeployTimeout) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZimletDeployTimeout, zimbraZimletDeployTimeout);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Timeout for zimlet deployment specified in seconds.. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param zimbraZimletDeployTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1858)
    public Map<String,Object> setZimletDeployTimeout(String zimbraZimletDeployTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZimletDeployTimeout, zimbraZimletDeployTimeout);
        return attrs;
    }

    /**
     * Timeout for zimlet deployment specified in seconds.. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1858)
    public void unsetZimletDeployTimeout() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZimletDeployTimeout, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Timeout for zimlet deployment specified in seconds.. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 9.0
     */
    @ZAttr(id=1858)
    public Map<String,Object> unsetZimletDeployTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZimletDeployTimeout, "");
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
