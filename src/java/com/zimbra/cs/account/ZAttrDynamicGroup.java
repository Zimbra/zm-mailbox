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
public abstract class ZAttrDynamicGroup extends Group {

    protected ZAttrDynamicGroup(String name, String id, Map<String, Object> attrs, Provisioning prov) {
        super(name, id, attrs, prov);
    }

    ///// BEGIN-AUTO-GEN-REPLACE

    /* build: 8.0.0_BETA1_1111 pshao 20120313-1105 */

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
     * RFC2798: preferred name to be used when displaying entries
     *
     * @return displayName, or null if unset
     */
    @ZAttr(id=-1)
    public String getDisplayName() {
        return getAttr(Provisioning.A_displayName, null);
    }

    /**
     * RFC2798: preferred name to be used when displaying entries
     *
     * @param displayName new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=-1)
    public void setDisplayName(String displayName) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_displayName, displayName);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * RFC2798: preferred name to be used when displaying entries
     *
     * @param displayName new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setDisplayName(String displayName, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_displayName, displayName);
        return attrs;
    }

    /**
     * RFC2798: preferred name to be used when displaying entries
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=-1)
    public void unsetDisplayName() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_displayName, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * RFC2798: preferred name to be used when displaying entries
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetDisplayName(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_displayName, "");
        return attrs;
    }

    /**
     * RFC1274: RFC822 Mailbox
     *
     * @return mail, or null if unset
     */
    @ZAttr(id=-1)
    public String getMail() {
        return getAttr(Provisioning.A_mail, null);
    }

    /**
     * RFC1274: RFC822 Mailbox
     *
     * @param mail new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=-1)
    public void setMail(String mail) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_mail, mail);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * RFC1274: RFC822 Mailbox
     *
     * @param mail new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setMail(String mail, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_mail, mail);
        return attrs;
    }

    /**
     * RFC1274: RFC822 Mailbox
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=-1)
    public void unsetMail() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_mail, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * RFC1274: RFC822 Mailbox
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetMail(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_mail, "");
        return attrs;
    }

    /**
     * Identifies an URL associated with each member of a group
     *
     * @return memberURL, or null if unset
     */
    @ZAttr(id=-1)
    public String getMemberURL() {
        return getAttr(Provisioning.A_memberURL, null);
    }

    /**
     * Identifies an URL associated with each member of a group
     *
     * @param memberURL new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=-1)
    public void setMemberURL(String memberURL) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_memberURL, memberURL);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Identifies an URL associated with each member of a group
     *
     * @param memberURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setMemberURL(String memberURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_memberURL, memberURL);
        return attrs;
    }

    /**
     * Identifies an URL associated with each member of a group
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=-1)
    public void unsetMemberURL() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_memberURL, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Identifies an URL associated with each member of a group
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetMemberURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_memberURL, "");
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
     * distribution subscription policy. ACCEPT: always accept, REJECT:
     * always reject, APPROVAL: require owners approval.
     *
     * <p>Valid values: [ACCEPT, APPROVAL, REJECT]
     *
     * @return zimbraDistributionListSubscriptionPolicy, or null if unset and/or has invalid value
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1275)
    public ZAttrProvisioning.DistributionListSubscriptionPolicy getDistributionListSubscriptionPolicy() {
        try { String v = getAttr(Provisioning.A_zimbraDistributionListSubscriptionPolicy); return v == null ? null : ZAttrProvisioning.DistributionListSubscriptionPolicy.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * distribution subscription policy. ACCEPT: always accept, REJECT:
     * always reject, APPROVAL: require owners approval.
     *
     * <p>Valid values: [ACCEPT, APPROVAL, REJECT]
     *
     * @return zimbraDistributionListSubscriptionPolicy, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1275)
    public String getDistributionListSubscriptionPolicyAsString() {
        return getAttr(Provisioning.A_zimbraDistributionListSubscriptionPolicy, null);
    }

    /**
     * distribution subscription policy. ACCEPT: always accept, REJECT:
     * always reject, APPROVAL: require owners approval.
     *
     * <p>Valid values: [ACCEPT, APPROVAL, REJECT]
     *
     * @param zimbraDistributionListSubscriptionPolicy new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1275)
    public void setDistributionListSubscriptionPolicy(ZAttrProvisioning.DistributionListSubscriptionPolicy zimbraDistributionListSubscriptionPolicy) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDistributionListSubscriptionPolicy, zimbraDistributionListSubscriptionPolicy.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * distribution subscription policy. ACCEPT: always accept, REJECT:
     * always reject, APPROVAL: require owners approval.
     *
     * <p>Valid values: [ACCEPT, APPROVAL, REJECT]
     *
     * @param zimbraDistributionListSubscriptionPolicy new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1275)
    public Map<String,Object> setDistributionListSubscriptionPolicy(ZAttrProvisioning.DistributionListSubscriptionPolicy zimbraDistributionListSubscriptionPolicy, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDistributionListSubscriptionPolicy, zimbraDistributionListSubscriptionPolicy.toString());
        return attrs;
    }

    /**
     * distribution subscription policy. ACCEPT: always accept, REJECT:
     * always reject, APPROVAL: require owners approval.
     *
     * <p>Valid values: [ACCEPT, APPROVAL, REJECT]
     *
     * @param zimbraDistributionListSubscriptionPolicy new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1275)
    public void setDistributionListSubscriptionPolicyAsString(String zimbraDistributionListSubscriptionPolicy) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDistributionListSubscriptionPolicy, zimbraDistributionListSubscriptionPolicy);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * distribution subscription policy. ACCEPT: always accept, REJECT:
     * always reject, APPROVAL: require owners approval.
     *
     * <p>Valid values: [ACCEPT, APPROVAL, REJECT]
     *
     * @param zimbraDistributionListSubscriptionPolicy new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1275)
    public Map<String,Object> setDistributionListSubscriptionPolicyAsString(String zimbraDistributionListSubscriptionPolicy, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDistributionListSubscriptionPolicy, zimbraDistributionListSubscriptionPolicy);
        return attrs;
    }

    /**
     * distribution subscription policy. ACCEPT: always accept, REJECT:
     * always reject, APPROVAL: require owners approval.
     *
     * <p>Valid values: [ACCEPT, APPROVAL, REJECT]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1275)
    public void unsetDistributionListSubscriptionPolicy() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDistributionListSubscriptionPolicy, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * distribution subscription policy. ACCEPT: always accept, REJECT:
     * always reject, APPROVAL: require owners approval.
     *
     * <p>Valid values: [ACCEPT, APPROVAL, REJECT]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1275)
    public Map<String,Object> unsetDistributionListSubscriptionPolicy(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDistributionListSubscriptionPolicy, "");
        return attrs;
    }

    /**
     * distribution subscription policy. ACCEPT: always accept, REJECT:
     * always reject, APPROVAL: require owners approval.
     *
     * <p>Valid values: [ACCEPT, APPROVAL, REJECT]
     *
     * @return zimbraDistributionListUnsubscriptionPolicy, or null if unset and/or has invalid value
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1276)
    public ZAttrProvisioning.DistributionListUnsubscriptionPolicy getDistributionListUnsubscriptionPolicy() {
        try { String v = getAttr(Provisioning.A_zimbraDistributionListUnsubscriptionPolicy); return v == null ? null : ZAttrProvisioning.DistributionListUnsubscriptionPolicy.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * distribution subscription policy. ACCEPT: always accept, REJECT:
     * always reject, APPROVAL: require owners approval.
     *
     * <p>Valid values: [ACCEPT, APPROVAL, REJECT]
     *
     * @return zimbraDistributionListUnsubscriptionPolicy, or null if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1276)
    public String getDistributionListUnsubscriptionPolicyAsString() {
        return getAttr(Provisioning.A_zimbraDistributionListUnsubscriptionPolicy, null);
    }

    /**
     * distribution subscription policy. ACCEPT: always accept, REJECT:
     * always reject, APPROVAL: require owners approval.
     *
     * <p>Valid values: [ACCEPT, APPROVAL, REJECT]
     *
     * @param zimbraDistributionListUnsubscriptionPolicy new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1276)
    public void setDistributionListUnsubscriptionPolicy(ZAttrProvisioning.DistributionListUnsubscriptionPolicy zimbraDistributionListUnsubscriptionPolicy) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDistributionListUnsubscriptionPolicy, zimbraDistributionListUnsubscriptionPolicy.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * distribution subscription policy. ACCEPT: always accept, REJECT:
     * always reject, APPROVAL: require owners approval.
     *
     * <p>Valid values: [ACCEPT, APPROVAL, REJECT]
     *
     * @param zimbraDistributionListUnsubscriptionPolicy new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1276)
    public Map<String,Object> setDistributionListUnsubscriptionPolicy(ZAttrProvisioning.DistributionListUnsubscriptionPolicy zimbraDistributionListUnsubscriptionPolicy, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDistributionListUnsubscriptionPolicy, zimbraDistributionListUnsubscriptionPolicy.toString());
        return attrs;
    }

    /**
     * distribution subscription policy. ACCEPT: always accept, REJECT:
     * always reject, APPROVAL: require owners approval.
     *
     * <p>Valid values: [ACCEPT, APPROVAL, REJECT]
     *
     * @param zimbraDistributionListUnsubscriptionPolicy new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1276)
    public void setDistributionListUnsubscriptionPolicyAsString(String zimbraDistributionListUnsubscriptionPolicy) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDistributionListUnsubscriptionPolicy, zimbraDistributionListUnsubscriptionPolicy);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * distribution subscription policy. ACCEPT: always accept, REJECT:
     * always reject, APPROVAL: require owners approval.
     *
     * <p>Valid values: [ACCEPT, APPROVAL, REJECT]
     *
     * @param zimbraDistributionListUnsubscriptionPolicy new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1276)
    public Map<String,Object> setDistributionListUnsubscriptionPolicyAsString(String zimbraDistributionListUnsubscriptionPolicy, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDistributionListUnsubscriptionPolicy, zimbraDistributionListUnsubscriptionPolicy);
        return attrs;
    }

    /**
     * distribution subscription policy. ACCEPT: always accept, REJECT:
     * always reject, APPROVAL: require owners approval.
     *
     * <p>Valid values: [ACCEPT, APPROVAL, REJECT]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1276)
    public void unsetDistributionListUnsubscriptionPolicy() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDistributionListUnsubscriptionPolicy, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * distribution subscription policy. ACCEPT: always accept, REJECT:
     * always reject, APPROVAL: require owners approval.
     *
     * <p>Valid values: [ACCEPT, APPROVAL, REJECT]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1276)
    public Map<String,Object> unsetDistributionListUnsubscriptionPolicy(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDistributionListUnsubscriptionPolicy, "");
        return attrs;
    }

    /**
     * hide entry in Global Address List
     *
     * @return zimbraHideInGal, or false if unset
     */
    @ZAttr(id=353)
    public boolean isHideInGal() {
        return getBooleanAttr(Provisioning.A_zimbraHideInGal, false);
    }

    /**
     * hide entry in Global Address List
     *
     * @param zimbraHideInGal new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=353)
    public void setHideInGal(boolean zimbraHideInGal) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHideInGal, zimbraHideInGal ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * hide entry in Global Address List
     *
     * @param zimbraHideInGal new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=353)
    public Map<String,Object> setHideInGal(boolean zimbraHideInGal, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHideInGal, zimbraHideInGal ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * hide entry in Global Address List
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=353)
    public void unsetHideInGal() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHideInGal, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * hide entry in Global Address List
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=353)
    public Map<String,Object> unsetHideInGal(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHideInGal, "");
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
     * if the dynamic group can be a legitimate grantee for folder grantees;
     * and a legitimate grantee or target for delegated admin grants
     *
     * @return zimbraIsACLGroup, or false if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1242)
    public boolean isIsACLGroup() {
        return getBooleanAttr(Provisioning.A_zimbraIsACLGroup, false);
    }

    /**
     * if the dynamic group can be a legitimate grantee for folder grantees;
     * and a legitimate grantee or target for delegated admin grants
     *
     * @param zimbraIsACLGroup new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1242)
    public void setIsACLGroup(boolean zimbraIsACLGroup) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIsACLGroup, zimbraIsACLGroup ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * if the dynamic group can be a legitimate grantee for folder grantees;
     * and a legitimate grantee or target for delegated admin grants
     *
     * @param zimbraIsACLGroup new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1242)
    public Map<String,Object> setIsACLGroup(boolean zimbraIsACLGroup, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIsACLGroup, zimbraIsACLGroup ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * if the dynamic group can be a legitimate grantee for folder grantees;
     * and a legitimate grantee or target for delegated admin grants
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1242)
    public void unsetIsACLGroup() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIsACLGroup, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * if the dynamic group can be a legitimate grantee for folder grantees;
     * and a legitimate grantee or target for delegated admin grants
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1242)
    public Map<String,Object> unsetIsACLGroup(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIsACLGroup, "");
        return attrs;
    }

    /**
     * set to true for admin groups
     *
     * @return zimbraIsAdminGroup, or false if unset
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=802)
    public boolean isIsAdminGroup() {
        return getBooleanAttr(Provisioning.A_zimbraIsAdminGroup, false);
    }

    /**
     * set to true for admin groups
     *
     * @param zimbraIsAdminGroup new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=802)
    public void setIsAdminGroup(boolean zimbraIsAdminGroup) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIsAdminGroup, zimbraIsAdminGroup ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * set to true for admin groups
     *
     * @param zimbraIsAdminGroup new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=802)
    public Map<String,Object> setIsAdminGroup(boolean zimbraIsAdminGroup, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIsAdminGroup, zimbraIsAdminGroup ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * set to true for admin groups
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=802)
    public void unsetIsAdminGroup() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIsAdminGroup, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * set to true for admin groups
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=802)
    public Map<String,Object> unsetIsAdminGroup(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIsAdminGroup, "");
        return attrs;
    }

    /**
     * locale of entry, e.g. en_US
     *
     * @return zimbraLocale, or null if unset
     */
    @ZAttr(id=345)
    public String getLocaleAsString() {
        return getAttr(Provisioning.A_zimbraLocale, null);
    }

    /**
     * locale of entry, e.g. en_US
     *
     * @param zimbraLocale new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=345)
    public void setLocale(String zimbraLocale) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLocale, zimbraLocale);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * locale of entry, e.g. en_US
     *
     * @param zimbraLocale new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=345)
    public Map<String,Object> setLocale(String zimbraLocale, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLocale, zimbraLocale);
        return attrs;
    }

    /**
     * locale of entry, e.g. en_US
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=345)
    public void unsetLocale() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLocale, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * locale of entry, e.g. en_US
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=345)
    public Map<String,Object> unsetLocale(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLocale, "");
        return attrs;
    }

    /**
     * RFC822 email address of this recipient for accepting mail
     *
     * @return zimbraMailAlias, or empty array if unset
     */
    @ZAttr(id=20)
    public String[] getMailAlias() {
        return getMultiAttr(Provisioning.A_zimbraMailAlias);
    }

    /**
     * RFC822 email address of this recipient for accepting mail
     *
     * @param zimbraMailAlias new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=20)
    public void setMailAlias(String[] zimbraMailAlias) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailAlias, zimbraMailAlias);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * RFC822 email address of this recipient for accepting mail
     *
     * @param zimbraMailAlias new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=20)
    public Map<String,Object> setMailAlias(String[] zimbraMailAlias, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailAlias, zimbraMailAlias);
        return attrs;
    }

    /**
     * RFC822 email address of this recipient for accepting mail
     *
     * @param zimbraMailAlias new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=20)
    public void addMailAlias(String zimbraMailAlias) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMailAlias, zimbraMailAlias);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * RFC822 email address of this recipient for accepting mail
     *
     * @param zimbraMailAlias new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=20)
    public Map<String,Object> addMailAlias(String zimbraMailAlias, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMailAlias, zimbraMailAlias);
        return attrs;
    }

    /**
     * RFC822 email address of this recipient for accepting mail
     *
     * @param zimbraMailAlias existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=20)
    public void removeMailAlias(String zimbraMailAlias) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMailAlias, zimbraMailAlias);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * RFC822 email address of this recipient for accepting mail
     *
     * @param zimbraMailAlias existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=20)
    public Map<String,Object> removeMailAlias(String zimbraMailAlias, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMailAlias, zimbraMailAlias);
        return attrs;
    }

    /**
     * RFC822 email address of this recipient for accepting mail
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=20)
    public void unsetMailAlias() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailAlias, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * RFC822 email address of this recipient for accepting mail
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=20)
    public Map<String,Object> unsetMailAlias(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailAlias, "");
        return attrs;
    }

    /**
     * the server hosting the account&#039;s mailbox
     *
     * @return zimbraMailHost, or null if unset
     */
    @ZAttr(id=4)
    public String getMailHost() {
        return getAttr(Provisioning.A_zimbraMailHost, null);
    }

    /**
     * the server hosting the account&#039;s mailbox
     *
     * @param zimbraMailHost new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=4)
    public void setMailHost(String zimbraMailHost) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailHost, zimbraMailHost);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * the server hosting the account&#039;s mailbox
     *
     * @param zimbraMailHost new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=4)
    public Map<String,Object> setMailHost(String zimbraMailHost, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailHost, zimbraMailHost);
        return attrs;
    }

    /**
     * the server hosting the account&#039;s mailbox
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=4)
    public void unsetMailHost() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailHost, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * the server hosting the account&#039;s mailbox
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=4)
    public Map<String,Object> unsetMailHost(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailHost, "");
        return attrs;
    }

    /**
     * mail delivery status (enabled/disabled)
     *
     * <p>Valid values: [enabled, disabled]
     *
     * @return zimbraMailStatus, or null if unset and/or has invalid value
     */
    @ZAttr(id=15)
    public ZAttrProvisioning.MailStatus getMailStatus() {
        try { String v = getAttr(Provisioning.A_zimbraMailStatus); return v == null ? null : ZAttrProvisioning.MailStatus.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * mail delivery status (enabled/disabled)
     *
     * <p>Valid values: [enabled, disabled]
     *
     * @return zimbraMailStatus, or null if unset
     */
    @ZAttr(id=15)
    public String getMailStatusAsString() {
        return getAttr(Provisioning.A_zimbraMailStatus, null);
    }

    /**
     * mail delivery status (enabled/disabled)
     *
     * <p>Valid values: [enabled, disabled]
     *
     * @param zimbraMailStatus new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=15)
    public void setMailStatus(ZAttrProvisioning.MailStatus zimbraMailStatus) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailStatus, zimbraMailStatus.toString());
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * mail delivery status (enabled/disabled)
     *
     * <p>Valid values: [enabled, disabled]
     *
     * @param zimbraMailStatus new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=15)
    public Map<String,Object> setMailStatus(ZAttrProvisioning.MailStatus zimbraMailStatus, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailStatus, zimbraMailStatus.toString());
        return attrs;
    }

    /**
     * mail delivery status (enabled/disabled)
     *
     * <p>Valid values: [enabled, disabled]
     *
     * @param zimbraMailStatus new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=15)
    public void setMailStatusAsString(String zimbraMailStatus) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailStatus, zimbraMailStatus);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * mail delivery status (enabled/disabled)
     *
     * <p>Valid values: [enabled, disabled]
     *
     * @param zimbraMailStatus new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=15)
    public Map<String,Object> setMailStatusAsString(String zimbraMailStatus, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailStatus, zimbraMailStatus);
        return attrs;
    }

    /**
     * mail delivery status (enabled/disabled)
     *
     * <p>Valid values: [enabled, disabled]
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=15)
    public void unsetMailStatus() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailStatus, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * mail delivery status (enabled/disabled)
     *
     * <p>Valid values: [enabled, disabled]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=15)
    public Map<String,Object> unsetMailStatus(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailStatus, "");
        return attrs;
    }

    /**
     * Addresses of the account that can be used by allowed delegated senders
     * as From and Sender address.
     *
     * @return zimbraPrefAllowAddressForDelegatedSender, or empty array if unset
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1333)
    public String[] getPrefAllowAddressForDelegatedSender() {
        return getMultiAttr(Provisioning.A_zimbraPrefAllowAddressForDelegatedSender);
    }

    /**
     * Addresses of the account that can be used by allowed delegated senders
     * as From and Sender address.
     *
     * @param zimbraPrefAllowAddressForDelegatedSender new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1333)
    public void setPrefAllowAddressForDelegatedSender(String[] zimbraPrefAllowAddressForDelegatedSender) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefAllowAddressForDelegatedSender, zimbraPrefAllowAddressForDelegatedSender);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Addresses of the account that can be used by allowed delegated senders
     * as From and Sender address.
     *
     * @param zimbraPrefAllowAddressForDelegatedSender new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1333)
    public Map<String,Object> setPrefAllowAddressForDelegatedSender(String[] zimbraPrefAllowAddressForDelegatedSender, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefAllowAddressForDelegatedSender, zimbraPrefAllowAddressForDelegatedSender);
        return attrs;
    }

    /**
     * Addresses of the account that can be used by allowed delegated senders
     * as From and Sender address.
     *
     * @param zimbraPrefAllowAddressForDelegatedSender new to add to existing values
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1333)
    public void addPrefAllowAddressForDelegatedSender(String zimbraPrefAllowAddressForDelegatedSender) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraPrefAllowAddressForDelegatedSender, zimbraPrefAllowAddressForDelegatedSender);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Addresses of the account that can be used by allowed delegated senders
     * as From and Sender address.
     *
     * @param zimbraPrefAllowAddressForDelegatedSender new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1333)
    public Map<String,Object> addPrefAllowAddressForDelegatedSender(String zimbraPrefAllowAddressForDelegatedSender, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraPrefAllowAddressForDelegatedSender, zimbraPrefAllowAddressForDelegatedSender);
        return attrs;
    }

    /**
     * Addresses of the account that can be used by allowed delegated senders
     * as From and Sender address.
     *
     * @param zimbraPrefAllowAddressForDelegatedSender existing value to remove
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1333)
    public void removePrefAllowAddressForDelegatedSender(String zimbraPrefAllowAddressForDelegatedSender) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraPrefAllowAddressForDelegatedSender, zimbraPrefAllowAddressForDelegatedSender);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Addresses of the account that can be used by allowed delegated senders
     * as From and Sender address.
     *
     * @param zimbraPrefAllowAddressForDelegatedSender existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1333)
    public Map<String,Object> removePrefAllowAddressForDelegatedSender(String zimbraPrefAllowAddressForDelegatedSender, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraPrefAllowAddressForDelegatedSender, zimbraPrefAllowAddressForDelegatedSender);
        return attrs;
    }

    /**
     * Addresses of the account that can be used by allowed delegated senders
     * as From and Sender address.
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1333)
    public void unsetPrefAllowAddressForDelegatedSender() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefAllowAddressForDelegatedSender, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * Addresses of the account that can be used by allowed delegated senders
     * as From and Sender address.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1333)
    public Map<String,Object> unsetPrefAllowAddressForDelegatedSender(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefAllowAddressForDelegatedSender, "");
        return attrs;
    }

    /**
     * address to put in reply-to header
     *
     * @return zimbraPrefReplyToAddress, or null if unset
     */
    @ZAttr(id=60)
    public String getPrefReplyToAddress() {
        return getAttr(Provisioning.A_zimbraPrefReplyToAddress, null);
    }

    /**
     * address to put in reply-to header
     *
     * @param zimbraPrefReplyToAddress new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=60)
    public void setPrefReplyToAddress(String zimbraPrefReplyToAddress) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefReplyToAddress, zimbraPrefReplyToAddress);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * address to put in reply-to header
     *
     * @param zimbraPrefReplyToAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=60)
    public Map<String,Object> setPrefReplyToAddress(String zimbraPrefReplyToAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefReplyToAddress, zimbraPrefReplyToAddress);
        return attrs;
    }

    /**
     * address to put in reply-to header
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=60)
    public void unsetPrefReplyToAddress() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefReplyToAddress, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * address to put in reply-to header
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=60)
    public Map<String,Object> unsetPrefReplyToAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefReplyToAddress, "");
        return attrs;
    }

    /**
     * personal part of email address put in reply-to header
     *
     * @return zimbraPrefReplyToDisplay, or null if unset
     */
    @ZAttr(id=404)
    public String getPrefReplyToDisplay() {
        return getAttr(Provisioning.A_zimbraPrefReplyToDisplay, null);
    }

    /**
     * personal part of email address put in reply-to header
     *
     * @param zimbraPrefReplyToDisplay new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=404)
    public void setPrefReplyToDisplay(String zimbraPrefReplyToDisplay) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefReplyToDisplay, zimbraPrefReplyToDisplay);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * personal part of email address put in reply-to header
     *
     * @param zimbraPrefReplyToDisplay new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=404)
    public Map<String,Object> setPrefReplyToDisplay(String zimbraPrefReplyToDisplay, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefReplyToDisplay, zimbraPrefReplyToDisplay);
        return attrs;
    }

    /**
     * personal part of email address put in reply-to header
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=404)
    public void unsetPrefReplyToDisplay() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefReplyToDisplay, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * personal part of email address put in reply-to header
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=404)
    public Map<String,Object> unsetPrefReplyToDisplay(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefReplyToDisplay, "");
        return attrs;
    }

    /**
     * TRUE if we should set a reply-to header
     *
     * @return zimbraPrefReplyToEnabled, or false if unset
     */
    @ZAttr(id=405)
    public boolean isPrefReplyToEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefReplyToEnabled, false);
    }

    /**
     * TRUE if we should set a reply-to header
     *
     * @param zimbraPrefReplyToEnabled new value
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=405)
    public void setPrefReplyToEnabled(boolean zimbraPrefReplyToEnabled) throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefReplyToEnabled, zimbraPrefReplyToEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * TRUE if we should set a reply-to header
     *
     * @param zimbraPrefReplyToEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=405)
    public Map<String,Object> setPrefReplyToEnabled(boolean zimbraPrefReplyToEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefReplyToEnabled, zimbraPrefReplyToEnabled ? Provisioning.TRUE : Provisioning.FALSE);
        return attrs;
    }

    /**
     * TRUE if we should set a reply-to header
     *
     * @throws com.zimbra.common.service.ServiceException if error during update
     */
    @ZAttr(id=405)
    public void unsetPrefReplyToEnabled() throws com.zimbra.common.service.ServiceException {
        HashMap<String,Object> attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefReplyToEnabled, "");
        getProvisioning().modifyAttrs(this, attrs);
    }

    /**
     * TRUE if we should set a reply-to header
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=405)
    public Map<String,Object> unsetPrefReplyToEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefReplyToEnabled, "");
        return attrs;
    }

    ///// END-AUTO-GEN-REPLACE

}
