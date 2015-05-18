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
        return getIntAttr(Provisioning.A_zimbraExtensionBindPort, -1);
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
        return getAttr(Provisioning.A_zimbraExtensionBindPort, null);
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
        return getTimeInterval(Provisioning.A_zimbraShortTermAllEffectiveRightsCacheExpiration, -1L);
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
        return getAttr(Provisioning.A_zimbraShortTermAllEffectiveRightsCacheExpiration, null);
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
        return getIntAttr(Provisioning.A_zimbraShortTermAllEffectiveRightsCacheSize, -1);
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
        return getTimeInterval(Provisioning.A_zimbraShortTermGranteeCacheExpiration, -1L);
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
        return getAttr(Provisioning.A_zimbraShortTermGranteeCacheExpiration, null);
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
        return getIntAttr(Provisioning.A_zimbraShortTermGranteeCacheSize, -1);
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
     * list of host:port for zookeeper servers; set to empty value to disable
     * the use of zookeeper
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
