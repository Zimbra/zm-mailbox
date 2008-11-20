/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008 Zimbra, Inc.
 *
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account;

import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.StringUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * AUTO-GENERATED. DO NOT EDIT.
 *
 */

public class ZAttrAccount  extends MailTarget {

    public ZAttrAccount(String name, String id, Map<String,Object> attrs, Map<String, Object> defaults, Provisioning prov) {
        super(name, id, attrs, defaults, prov);
    }

    ///// BEGIN-AUTO-GEN-REPLACE

    /* build: 5.0 pshao 20081119-2159 */

    /**
     * RFC2256: ISO-3166 country 2-letter code
     *
     * @return c, or null unset
     */
    @ZAttr(id=-1)
    public String getC() {
        return getAttr(Provisioning.A_c);
    }

    /**
     * RFC2256: ISO-3166 country 2-letter code
     *
     * @param c new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setC(String c, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_c, c);
        return attrs;
    }

    /**
     * RFC2256: ISO-3166 country 2-letter code
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetC(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_c, "");
        return attrs;
    }

    /**
     * RFC2256: common name(s) for which the entity is known by
     *
     * @return cn, or null unset
     */
    @ZAttr(id=-1)
    public String getCn() {
        return getAttr(Provisioning.A_cn);
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
     * RFC1274: friendly country name
     *
     * @return co, or null unset
     */
    @ZAttr(id=-1)
    public String getCo() {
        return getAttr(Provisioning.A_co);
    }

    /**
     * RFC1274: friendly country name
     *
     * @param co new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setCo(String co, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_co, co);
        return attrs;
    }

    /**
     * RFC1274: friendly country name
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetCo(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_co, "");
        return attrs;
    }

    /**
     * From Microsoft Schema
     *
     * @return company, or null unset
     */
    @ZAttr(id=-1)
    public String getCompany() {
        return getAttr(Provisioning.A_company);
    }

    /**
     * From Microsoft Schema
     *
     * @param company new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setCompany(String company, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_company, company);
        return attrs;
    }

    /**
     * From Microsoft Schema
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetCompany(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_company, "");
        return attrs;
    }

    /**
     * RFC2256: descriptive information
     *
     * @return description, or null unset
     */
    @ZAttr(id=-1)
    public String getDescription() {
        return getAttr(Provisioning.A_description);
    }

    /**
     * RFC2256: descriptive information
     *
     * @param description new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setDescription(String description, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_description, description);
        return attrs;
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
     * @return displayName, or null unset
     */
    @ZAttr(id=-1)
    public String getDisplayName() {
        return getAttr(Provisioning.A_displayName);
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
     * RFC2256: first name(s) for which the entity is known by
     *
     * @return givenName, or null unset
     */
    @ZAttr(id=-1)
    public String getGivenName() {
        return getAttr(Provisioning.A_givenName);
    }

    /**
     * RFC2256: first name(s) for which the entity is known by
     *
     * @param givenName new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setGivenName(String givenName, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_givenName, givenName);
        return attrs;
    }

    /**
     * RFC2256: first name(s) for which the entity is known by
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetGivenName(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_givenName, "");
        return attrs;
    }

    /**
     * RFC2256: first name(s) for which the entity is known by
     *
     * @return gn, or null unset
     */
    @ZAttr(id=-1)
    public String getGn() {
        return getAttr(Provisioning.A_gn);
    }

    /**
     * RFC2256: first name(s) for which the entity is known by
     *
     * @param gn new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setGn(String gn, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_gn, gn);
        return attrs;
    }

    /**
     * RFC2256: first name(s) for which the entity is known by
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetGn(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_gn, "");
        return attrs;
    }

    /**
     * RFC2256: initials of some or all of names, but not the surname(s).
     *
     * @return initials, or null unset
     */
    @ZAttr(id=-1)
    public String getInitials() {
        return getAttr(Provisioning.A_initials);
    }

    /**
     * RFC2256: initials of some or all of names, but not the surname(s).
     *
     * @param initials new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setInitials(String initials, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_initials, initials);
        return attrs;
    }

    /**
     * RFC2256: initials of some or all of names, but not the surname(s).
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetInitials(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_initials, "");
        return attrs;
    }

    /**
     * RFC2256: locality which this object resides in
     *
     * @return l, or null unset
     */
    @ZAttr(id=-1)
    public String getL() {
        return getAttr(Provisioning.A_l);
    }

    /**
     * RFC2256: locality which this object resides in
     *
     * @param l new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setL(String l, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_l, l);
        return attrs;
    }

    /**
     * RFC2256: locality which this object resides in
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_l, "");
        return attrs;
    }

    /**
     * RFC1274: RFC822 Mailbox
     *
     * @return mail, or null unset
     */
    @ZAttr(id=-1)
    public String getMail() {
        return getAttr(Provisioning.A_mail);
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
     * RFC2256: organization this object belongs to
     *
     * @return o, or null unset
     */
    @ZAttr(id=-1)
    public String getO() {
        return getAttr(Provisioning.A_o);
    }

    /**
     * RFC2256: organization this object belongs to
     *
     * @param o new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setO(String o, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_o, o);
        return attrs;
    }

    /**
     * RFC2256: organization this object belongs to
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetO(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_o, "");
        return attrs;
    }

    /**
     * RFC2256: organizational unit this object belongs to
     *
     * @return ou, or null unset
     */
    @ZAttr(id=-1)
    public String getOu() {
        return getAttr(Provisioning.A_ou);
    }

    /**
     * RFC2256: organizational unit this object belongs to
     *
     * @param ou new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setOu(String ou, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_ou, ou);
        return attrs;
    }

    /**
     * RFC2256: organizational unit this object belongs to
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetOu(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_ou, "");
        return attrs;
    }

    /**
     * &#039;RFC2256: Physical Delivery Office Name
     *
     * @return physicalDeliveryOfficeName, or null unset
     */
    @ZAttr(id=-1)
    public String getPhysicalDeliveryOfficeName() {
        return getAttr(Provisioning.A_physicalDeliveryOfficeName);
    }

    /**
     * &#039;RFC2256: Physical Delivery Office Name
     *
     * @param physicalDeliveryOfficeName new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setPhysicalDeliveryOfficeName(String physicalDeliveryOfficeName, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_physicalDeliveryOfficeName, physicalDeliveryOfficeName);
        return attrs;
    }

    /**
     * &#039;RFC2256: Physical Delivery Office Name
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetPhysicalDeliveryOfficeName(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_physicalDeliveryOfficeName, "");
        return attrs;
    }

    /**
     * RFC2256: postal address
     *
     * @return postalAddress, or null unset
     */
    @ZAttr(id=-1)
    public String getPostalAddress() {
        return getAttr(Provisioning.A_postalAddress);
    }

    /**
     * RFC2256: postal address
     *
     * @param postalAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setPostalAddress(String postalAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_postalAddress, postalAddress);
        return attrs;
    }

    /**
     * RFC2256: postal address
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetPostalAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_postalAddress, "");
        return attrs;
    }

    /**
     * RFC2256: postal code
     *
     * @return postalCode, or null unset
     */
    @ZAttr(id=-1)
    public String getPostalCode() {
        return getAttr(Provisioning.A_postalCode);
    }

    /**
     * RFC2256: postal code
     *
     * @param postalCode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setPostalCode(String postalCode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_postalCode, postalCode);
        return attrs;
    }

    /**
     * RFC2256: postal code
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetPostalCode(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_postalCode, "");
        return attrs;
    }

    /**
     * RFC2256: last (family) name(s) for which the entity is known by
     *
     * @return sn, or null unset
     */
    @ZAttr(id=-1)
    public String getSn() {
        return getAttr(Provisioning.A_sn);
    }

    /**
     * RFC2256: last (family) name(s) for which the entity is known by
     *
     * @param sn new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setSn(String sn, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_sn, sn);
        return attrs;
    }

    /**
     * RFC2256: last (family) name(s) for which the entity is known by
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetSn(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_sn, "");
        return attrs;
    }

    /**
     * RFC2256: state or province which this object resides in
     *
     * @return st, or null unset
     */
    @ZAttr(id=-1)
    public String getSt() {
        return getAttr(Provisioning.A_st);
    }

    /**
     * RFC2256: state or province which this object resides in
     *
     * @param st new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setSt(String st, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_st, st);
        return attrs;
    }

    /**
     * RFC2256: state or province which this object resides in
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetSt(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_st, "");
        return attrs;
    }

    /**
     * RFC2256: Telephone Number
     *
     * @return telephoneNumber, or null unset
     */
    @ZAttr(id=-1)
    public String getTelephoneNumber() {
        return getAttr(Provisioning.A_telephoneNumber);
    }

    /**
     * RFC2256: Telephone Number
     *
     * @param telephoneNumber new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setTelephoneNumber(String telephoneNumber, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_telephoneNumber, telephoneNumber);
        return attrs;
    }

    /**
     * RFC2256: Telephone Number
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetTelephoneNumber(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_telephoneNumber, "");
        return attrs;
    }

    /**
     * RFC2256: title associated with the entity
     *
     * @return title, or null unset
     */
    @ZAttr(id=-1)
    public String getTitle() {
        return getAttr(Provisioning.A_title);
    }

    /**
     * RFC2256: title associated with the entity
     *
     * @param title new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setTitle(String title, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_title, title);
        return attrs;
    }

    /**
     * RFC2256: title associated with the entity
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetTitle(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_title, "");
        return attrs;
    }

    /**
     * RFC1274: user identifier
     *
     * @return uid, or null unset
     */
    @ZAttr(id=-1)
    public String getUid() {
        return getAttr(Provisioning.A_uid);
    }

    /**
     * RFC1274: user identifier
     *
     * @param uid new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setUid(String uid, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_uid, uid);
        return attrs;
    }

    /**
     * RFC1274: user identifier
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetUid(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_uid, "");
        return attrs;
    }

    /**
     * RFC2256/2307: password of user. Stored encoded as SSHA (salted-SHA1)
     *
     * @return userPassword, or null unset
     */
    @ZAttr(id=-1)
    public String getUserPassword() {
        return getAttr(Provisioning.A_userPassword);
    }

    /**
     * RFC2256/2307: password of user. Stored encoded as SSHA (salted-SHA1)
     *
     * @param userPassword new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> setUserPassword(String userPassword, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_userPassword, userPassword);
        return attrs;
    }

    /**
     * RFC2256/2307: password of user. Stored encoded as SSHA (salted-SHA1)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=-1)
    public Map<String,Object> unsetUserPassword(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_userPassword, "");
        return attrs;
    }

    /**
     * Zimbra access control list
     *
     * @return zimbraACE, or ampty array if unset
     */
    @ZAttr(id=659)
    public String[] getACE() {
        return getMultiAttr(Provisioning.A_zimbraACE);
    }

    /**
     * Zimbra access control list
     *
     * @param zimbraACE new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=659)
    public Map<String,Object> unsetACE(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraACE, "");
        return attrs;
    }

    /**
     * calendar user type - USER (default) or RESOURCE
     *
     * <p>Valid values: [USER, RESOURCE]
     *
     * @return zimbraAccountCalendarUserType, or null if unset and/or has invalid value
     */
    @ZAttr(id=313)
    public ZAttrProvisioning.AccountCalendarUserType getAccountCalendarUserType() {
        try { String v = getAttr(Provisioning.A_zimbraAccountCalendarUserType); return v == null ? null : ZAttrProvisioning.AccountCalendarUserType.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * calendar user type - USER (default) or RESOURCE
     *
     * <p>Valid values: [USER, RESOURCE]
     *
     * @return zimbraAccountCalendarUserType, or null unset
     */
    @ZAttr(id=313)
    public String getAccountCalendarUserTypeAsString() {
        return getAttr(Provisioning.A_zimbraAccountCalendarUserType);
    }

    /**
     * calendar user type - USER (default) or RESOURCE
     *
     * <p>Valid values: [USER, RESOURCE]
     *
     * @param zimbraAccountCalendarUserType new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=313)
    public Map<String,Object> setAccountCalendarUserType(ZAttrProvisioning.AccountCalendarUserType zimbraAccountCalendarUserType, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAccountCalendarUserType, zimbraAccountCalendarUserType.toString());
        return attrs;
    }

    /**
     * calendar user type - USER (default) or RESOURCE
     *
     * <p>Valid values: [USER, RESOURCE]
     *
     * @param zimbraAccountCalendarUserType new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=313)
    public Map<String,Object> setAccountCalendarUserTypeAsString(String zimbraAccountCalendarUserType, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAccountCalendarUserType, zimbraAccountCalendarUserType);
        return attrs;
    }

    /**
     * calendar user type - USER (default) or RESOURCE
     *
     * <p>Valid values: [USER, RESOURCE]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=313)
    public Map<String,Object> unsetAccountCalendarUserType(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAccountCalendarUserType, "");
        return attrs;
    }

    /**
     * account status
     *
     * <p>Valid values: [active, closed, lockout, locked, maintenance]
     *
     * @return zimbraAccountStatus, or null if unset and/or has invalid value
     */
    @ZAttr(id=2)
    public ZAttrProvisioning.AccountStatus getAccountStatus() {
        try { String v = getAttr(Provisioning.A_zimbraAccountStatus); return v == null ? null : ZAttrProvisioning.AccountStatus.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * account status
     *
     * <p>Valid values: [active, closed, lockout, locked, maintenance]
     *
     * @return zimbraAccountStatus, or null unset
     */
    @ZAttr(id=2)
    public String getAccountStatusAsString() {
        return getAttr(Provisioning.A_zimbraAccountStatus);
    }

    /**
     * account status
     *
     * <p>Valid values: [active, closed, lockout, locked, maintenance]
     *
     * @param zimbraAccountStatus new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=2)
    public Map<String,Object> setAccountStatus(ZAttrProvisioning.AccountStatus zimbraAccountStatus, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAccountStatus, zimbraAccountStatus.toString());
        return attrs;
    }

    /**
     * account status
     *
     * <p>Valid values: [active, closed, lockout, locked, maintenance]
     *
     * @param zimbraAccountStatus new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=2)
    public Map<String,Object> setAccountStatusAsString(String zimbraAccountStatus, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAccountStatus, zimbraAccountStatus);
        return attrs;
    }

    /**
     * account status
     *
     * <p>Valid values: [active, closed, lockout, locked, maintenance]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=2)
    public Map<String,Object> unsetAccountStatus(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAccountStatus, "");
        return attrs;
    }

    /**
     * lifetime (nnnnn[hmsd]) of newly created admin auth tokens
     *
     * <p>Use getAdminAuthTokenLifetimeAsString to access value as a string.
     *
     * @see #getAdminAuthTokenLifetimeAsString()
     *
     * @return zimbraAdminAuthTokenLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=109)
    public long getAdminAuthTokenLifetime() {
        return getTimeInterval(Provisioning.A_zimbraAdminAuthTokenLifetime, -1);
    }

    /**
     * lifetime (nnnnn[hmsd]) of newly created admin auth tokens
     *
     * @return zimbraAdminAuthTokenLifetime, or null unset
     */
    @ZAttr(id=109)
    public String getAdminAuthTokenLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraAdminAuthTokenLifetime);
    }

    /**
     * lifetime (nnnnn[hmsd]) of newly created admin auth tokens
     *
     * @param zimbraAdminAuthTokenLifetime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=109)
    public Map<String,Object> setAdminAuthTokenLifetime(String zimbraAdminAuthTokenLifetime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminAuthTokenLifetime, zimbraAdminAuthTokenLifetime);
        return attrs;
    }

    /**
     * lifetime (nnnnn[hmsd]) of newly created admin auth tokens
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=109)
    public Map<String,Object> unsetAdminAuthTokenLifetime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminAuthTokenLifetime, "");
        return attrs;
    }

    /**
     * UI components available for the authed admin in admin console
     *
     * @return zimbraAdminConsoleUIComponents, or ampty array if unset
     *
     * @since ZCS future
     */
    @ZAttr(id=761)
    public String[] getAdminConsoleUIComponents() {
        return getMultiAttr(Provisioning.A_zimbraAdminConsoleUIComponents);
    }

    /**
     * UI components available for the authed admin in admin console
     *
     * @param zimbraAdminConsoleUIComponents new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=761)
    public Map<String,Object> setAdminConsoleUIComponents(String[] zimbraAdminConsoleUIComponents, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleUIComponents, zimbraAdminConsoleUIComponents);
        return attrs;
    }

    /**
     * UI components available for the authed admin in admin console
     *
     * @param zimbraAdminConsoleUIComponents new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=761)
    public Map<String,Object> addAdminConsoleUIComponents(String zimbraAdminConsoleUIComponents, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraAdminConsoleUIComponents, zimbraAdminConsoleUIComponents);
        return attrs;
    }

    /**
     * UI components available for the authed admin in admin console
     *
     * @param zimbraAdminConsoleUIComponents existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=761)
    public Map<String,Object> removeAdminConsoleUIComponents(String zimbraAdminConsoleUIComponents, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraAdminConsoleUIComponents, zimbraAdminConsoleUIComponents);
        return attrs;
    }

    /**
     * UI components available for the authed admin in admin console
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=761)
    public Map<String,Object> unsetAdminConsoleUIComponents(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminConsoleUIComponents, "");
        return attrs;
    }

    /**
     * admin saved searches
     *
     * @return zimbraAdminSavedSearches, or ampty array if unset
     */
    @ZAttr(id=446)
    public String[] getAdminSavedSearches() {
        return getMultiAttr(Provisioning.A_zimbraAdminSavedSearches);
    }

    /**
     * admin saved searches
     *
     * @param zimbraAdminSavedSearches new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=446)
    public Map<String,Object> setAdminSavedSearches(String[] zimbraAdminSavedSearches, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminSavedSearches, zimbraAdminSavedSearches);
        return attrs;
    }

    /**
     * admin saved searches
     *
     * @param zimbraAdminSavedSearches new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=446)
    public Map<String,Object> addAdminSavedSearches(String zimbraAdminSavedSearches, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraAdminSavedSearches, zimbraAdminSavedSearches);
        return attrs;
    }

    /**
     * admin saved searches
     *
     * @param zimbraAdminSavedSearches existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=446)
    public Map<String,Object> removeAdminSavedSearches(String zimbraAdminSavedSearches, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraAdminSavedSearches, zimbraAdminSavedSearches);
        return attrs;
    }

    /**
     * admin saved searches
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=446)
    public Map<String,Object> unsetAdminSavedSearches(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAdminSavedSearches, "");
        return attrs;
    }

    /**
     * Whether this account can use any from address. Not changeable by
     * domain admin to allow arbitrary addresses
     *
     * @return zimbraAllowAnyFromAddress, or false if unset
     */
    @ZAttr(id=427)
    public boolean isAllowAnyFromAddress() {
        return getBooleanAttr(Provisioning.A_zimbraAllowAnyFromAddress, false);
    }

    /**
     * Whether this account can use any from address. Not changeable by
     * domain admin to allow arbitrary addresses
     *
     * @param zimbraAllowAnyFromAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=427)
    public Map<String,Object> setAllowAnyFromAddress(boolean zimbraAllowAnyFromAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAllowAnyFromAddress, Boolean.toString(zimbraAllowAnyFromAddress));
        return attrs;
    }

    /**
     * Whether this account can use any from address. Not changeable by
     * domain admin to allow arbitrary addresses
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=427)
    public Map<String,Object> unsetAllowAnyFromAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAllowAnyFromAddress, "");
        return attrs;
    }

    /**
     * Addresses that this account can as from address if
     * arbitrary-addresses-allowed setting is not set
     *
     * @return zimbraAllowFromAddress, or ampty array if unset
     */
    @ZAttr(id=428)
    public String[] getAllowFromAddress() {
        return getMultiAttr(Provisioning.A_zimbraAllowFromAddress);
    }

    /**
     * Addresses that this account can as from address if
     * arbitrary-addresses-allowed setting is not set
     *
     * @param zimbraAllowFromAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=428)
    public Map<String,Object> setAllowFromAddress(String[] zimbraAllowFromAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAllowFromAddress, zimbraAllowFromAddress);
        return attrs;
    }

    /**
     * Addresses that this account can as from address if
     * arbitrary-addresses-allowed setting is not set
     *
     * @param zimbraAllowFromAddress new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=428)
    public Map<String,Object> addAllowFromAddress(String zimbraAllowFromAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraAllowFromAddress, zimbraAllowFromAddress);
        return attrs;
    }

    /**
     * Addresses that this account can as from address if
     * arbitrary-addresses-allowed setting is not set
     *
     * @param zimbraAllowFromAddress existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=428)
    public Map<String,Object> removeAllowFromAddress(String zimbraAllowFromAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraAllowFromAddress, zimbraAllowFromAddress);
        return attrs;
    }

    /**
     * Addresses that this account can as from address if
     * arbitrary-addresses-allowed setting is not set
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=428)
    public Map<String,Object> unsetAllowFromAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAllowFromAddress, "");
        return attrs;
    }

    /**
     * Mailboxes in which the current account in archived. Multi-value attr
     * with eg values { user-2006@example.com.archive,
     * user-2007@example.com.archive } that tells us that user@example.com
     * has been archived into those two mailboxes.
     *
     * @return zimbraArchiveAccount, or ampty array if unset
     */
    @ZAttr(id=429)
    public String[] getArchiveAccount() {
        return getMultiAttr(Provisioning.A_zimbraArchiveAccount);
    }

    /**
     * Mailboxes in which the current account in archived. Multi-value attr
     * with eg values { user-2006@example.com.archive,
     * user-2007@example.com.archive } that tells us that user@example.com
     * has been archived into those two mailboxes.
     *
     * @param zimbraArchiveAccount new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=429)
    public Map<String,Object> setArchiveAccount(String[] zimbraArchiveAccount, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraArchiveAccount, zimbraArchiveAccount);
        return attrs;
    }

    /**
     * Mailboxes in which the current account in archived. Multi-value attr
     * with eg values { user-2006@example.com.archive,
     * user-2007@example.com.archive } that tells us that user@example.com
     * has been archived into those two mailboxes.
     *
     * @param zimbraArchiveAccount new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=429)
    public Map<String,Object> addArchiveAccount(String zimbraArchiveAccount, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraArchiveAccount, zimbraArchiveAccount);
        return attrs;
    }

    /**
     * Mailboxes in which the current account in archived. Multi-value attr
     * with eg values { user-2006@example.com.archive,
     * user-2007@example.com.archive } that tells us that user@example.com
     * has been archived into those two mailboxes.
     *
     * @param zimbraArchiveAccount existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=429)
    public Map<String,Object> removeArchiveAccount(String zimbraArchiveAccount, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraArchiveAccount, zimbraArchiveAccount);
        return attrs;
    }

    /**
     * Mailboxes in which the current account in archived. Multi-value attr
     * with eg values { user-2006@example.com.archive,
     * user-2007@example.com.archive } that tells us that user@example.com
     * has been archived into those two mailboxes.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=429)
    public Map<String,Object> unsetArchiveAccount(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraArchiveAccount, "");
        return attrs;
    }

    /**
     * An account or CoS setting that works with the name template that
     * allows you to dictate the date format used in the name template. This
     * is a Java SimpleDateFormat specifier. The default is an LDAP
     * generalized time format:
     *
     * @return zimbraArchiveAccountDateTemplate, or null unset
     */
    @ZAttr(id=432)
    public String getArchiveAccountDateTemplate() {
        return getAttr(Provisioning.A_zimbraArchiveAccountDateTemplate);
    }

    /**
     * An account or CoS setting that works with the name template that
     * allows you to dictate the date format used in the name template. This
     * is a Java SimpleDateFormat specifier. The default is an LDAP
     * generalized time format:
     *
     * @param zimbraArchiveAccountDateTemplate new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=432)
    public Map<String,Object> setArchiveAccountDateTemplate(String zimbraArchiveAccountDateTemplate, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraArchiveAccountDateTemplate, zimbraArchiveAccountDateTemplate);
        return attrs;
    }

    /**
     * An account or CoS setting that works with the name template that
     * allows you to dictate the date format used in the name template. This
     * is a Java SimpleDateFormat specifier. The default is an LDAP
     * generalized time format:
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=432)
    public Map<String,Object> unsetArchiveAccountDateTemplate(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraArchiveAccountDateTemplate, "");
        return attrs;
    }

    /**
     * An account or CoS setting - typically only in CoS - that tells the
     * archiving system how to derive the archive mailbox name. ID, USER,
     * DATE, and DOMAIN are expanded.
     *
     * @return zimbraArchiveAccountNameTemplate, or null unset
     */
    @ZAttr(id=431)
    public String getArchiveAccountNameTemplate() {
        return getAttr(Provisioning.A_zimbraArchiveAccountNameTemplate);
    }

    /**
     * An account or CoS setting - typically only in CoS - that tells the
     * archiving system how to derive the archive mailbox name. ID, USER,
     * DATE, and DOMAIN are expanded.
     *
     * @param zimbraArchiveAccountNameTemplate new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=431)
    public Map<String,Object> setArchiveAccountNameTemplate(String zimbraArchiveAccountNameTemplate, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraArchiveAccountNameTemplate, zimbraArchiveAccountNameTemplate);
        return attrs;
    }

    /**
     * An account or CoS setting - typically only in CoS - that tells the
     * archiving system how to derive the archive mailbox name. ID, USER,
     * DATE, and DOMAIN are expanded.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=431)
    public Map<String,Object> unsetArchiveAccountNameTemplate(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraArchiveAccountNameTemplate, "");
        return attrs;
    }

    /**
     * block all attachment downloading
     *
     * @return zimbraAttachmentsBlocked, or false if unset
     */
    @ZAttr(id=115)
    public boolean isAttachmentsBlocked() {
        return getBooleanAttr(Provisioning.A_zimbraAttachmentsBlocked, false);
    }

    /**
     * block all attachment downloading
     *
     * @param zimbraAttachmentsBlocked new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=115)
    public Map<String,Object> setAttachmentsBlocked(boolean zimbraAttachmentsBlocked, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsBlocked, Boolean.toString(zimbraAttachmentsBlocked));
        return attrs;
    }

    /**
     * block all attachment downloading
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=115)
    public Map<String,Object> unsetAttachmentsBlocked(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsBlocked, "");
        return attrs;
    }

    /**
     * whether or not to index attachemts
     *
     * @return zimbraAttachmentsIndexingEnabled, or false if unset
     */
    @ZAttr(id=173)
    public boolean isAttachmentsIndexingEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraAttachmentsIndexingEnabled, false);
    }

    /**
     * whether or not to index attachemts
     *
     * @param zimbraAttachmentsIndexingEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=173)
    public Map<String,Object> setAttachmentsIndexingEnabled(boolean zimbraAttachmentsIndexingEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsIndexingEnabled, Boolean.toString(zimbraAttachmentsIndexingEnabled));
        return attrs;
    }

    /**
     * whether or not to index attachemts
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=173)
    public Map<String,Object> unsetAttachmentsIndexingEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsIndexingEnabled, "");
        return attrs;
    }

    /**
     * view all attachments in html only
     *
     * @return zimbraAttachmentsViewInHtmlOnly, or false if unset
     */
    @ZAttr(id=116)
    public boolean isAttachmentsViewInHtmlOnly() {
        return getBooleanAttr(Provisioning.A_zimbraAttachmentsViewInHtmlOnly, false);
    }

    /**
     * view all attachments in html only
     *
     * @param zimbraAttachmentsViewInHtmlOnly new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=116)
    public Map<String,Object> setAttachmentsViewInHtmlOnly(boolean zimbraAttachmentsViewInHtmlOnly, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsViewInHtmlOnly, Boolean.toString(zimbraAttachmentsViewInHtmlOnly));
        return attrs;
    }

    /**
     * view all attachments in html only
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=116)
    public Map<String,Object> unsetAttachmentsViewInHtmlOnly(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAttachmentsViewInHtmlOnly, "");
        return attrs;
    }

    /**
     * explict mapping to an external LDAP dn for a given account
     *
     * @return zimbraAuthLdapExternalDn, or null unset
     */
    @ZAttr(id=256)
    public String getAuthLdapExternalDn() {
        return getAttr(Provisioning.A_zimbraAuthLdapExternalDn);
    }

    /**
     * explict mapping to an external LDAP dn for a given account
     *
     * @param zimbraAuthLdapExternalDn new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=256)
    public Map<String,Object> setAuthLdapExternalDn(String zimbraAuthLdapExternalDn, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapExternalDn, zimbraAuthLdapExternalDn);
        return attrs;
    }

    /**
     * explict mapping to an external LDAP dn for a given account
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=256)
    public Map<String,Object> unsetAuthLdapExternalDn(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthLdapExternalDn, "");
        return attrs;
    }

    /**
     * lifetime (nnnnn[hmsd]) of newly created auth tokens
     *
     * <p>Use getAuthTokenLifetimeAsString to access value as a string.
     *
     * @see #getAuthTokenLifetimeAsString()
     *
     * @return zimbraAuthTokenLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=108)
    public long getAuthTokenLifetime() {
        return getTimeInterval(Provisioning.A_zimbraAuthTokenLifetime, -1);
    }

    /**
     * lifetime (nnnnn[hmsd]) of newly created auth tokens
     *
     * @return zimbraAuthTokenLifetime, or null unset
     */
    @ZAttr(id=108)
    public String getAuthTokenLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraAuthTokenLifetime);
    }

    /**
     * lifetime (nnnnn[hmsd]) of newly created auth tokens
     *
     * @param zimbraAuthTokenLifetime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=108)
    public Map<String,Object> setAuthTokenLifetime(String zimbraAuthTokenLifetime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthTokenLifetime, zimbraAuthTokenLifetime);
        return attrs;
    }

    /**
     * lifetime (nnnnn[hmsd]) of newly created auth tokens
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=108)
    public Map<String,Object> unsetAuthTokenLifetime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAuthTokenLifetime, "");
        return attrs;
    }

    /**
     * Locales available for this account
     *
     * @return zimbraAvailableLocale, or ampty array if unset
     */
    @ZAttr(id=487)
    public String[] getAvailableLocale() {
        return getMultiAttr(Provisioning.A_zimbraAvailableLocale);
    }

    /**
     * Locales available for this account
     *
     * @param zimbraAvailableLocale new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=487)
    public Map<String,Object> setAvailableLocale(String[] zimbraAvailableLocale, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAvailableLocale, zimbraAvailableLocale);
        return attrs;
    }

    /**
     * Locales available for this account
     *
     * @param zimbraAvailableLocale new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=487)
    public Map<String,Object> addAvailableLocale(String zimbraAvailableLocale, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraAvailableLocale, zimbraAvailableLocale);
        return attrs;
    }

    /**
     * Locales available for this account
     *
     * @param zimbraAvailableLocale existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=487)
    public Map<String,Object> removeAvailableLocale(String zimbraAvailableLocale, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraAvailableLocale, zimbraAvailableLocale);
        return attrs;
    }

    /**
     * Locales available for this account
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=487)
    public Map<String,Object> unsetAvailableLocale(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAvailableLocale, "");
        return attrs;
    }

    /**
     * Skins available for this account. Fallback order is: 1. the normal
     * account/cos inheritance 2. if not set on account/cos, use the value on
     * the domain of the account
     *
     * @return zimbraAvailableSkin, or ampty array if unset
     */
    @ZAttr(id=364)
    public String[] getAvailableSkin() {
        return getMultiAttr(Provisioning.A_zimbraAvailableSkin);
    }

    /**
     * Skins available for this account. Fallback order is: 1. the normal
     * account/cos inheritance 2. if not set on account/cos, use the value on
     * the domain of the account
     *
     * @param zimbraAvailableSkin new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=364)
    public Map<String,Object> setAvailableSkin(String[] zimbraAvailableSkin, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAvailableSkin, zimbraAvailableSkin);
        return attrs;
    }

    /**
     * Skins available for this account. Fallback order is: 1. the normal
     * account/cos inheritance 2. if not set on account/cos, use the value on
     * the domain of the account
     *
     * @param zimbraAvailableSkin new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=364)
    public Map<String,Object> addAvailableSkin(String zimbraAvailableSkin, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraAvailableSkin, zimbraAvailableSkin);
        return attrs;
    }

    /**
     * Skins available for this account. Fallback order is: 1. the normal
     * account/cos inheritance 2. if not set on account/cos, use the value on
     * the domain of the account
     *
     * @param zimbraAvailableSkin existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=364)
    public Map<String,Object> removeAvailableSkin(String zimbraAvailableSkin, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraAvailableSkin, zimbraAvailableSkin);
        return attrs;
    }

    /**
     * Skins available for this account. Fallback order is: 1. the normal
     * account/cos inheritance 2. if not set on account/cos, use the value on
     * the domain of the account
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=364)
    public Map<String,Object> unsetAvailableSkin(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraAvailableSkin, "");
        return attrs;
    }

    /**
     * Batch size to use when indexing data
     *
     * @return zimbraBatchedIndexingSize, or -1 if unset
     */
    @ZAttr(id=619)
    public int getBatchedIndexingSize() {
        return getIntAttr(Provisioning.A_zimbraBatchedIndexingSize, -1);
    }

    /**
     * Batch size to use when indexing data
     *
     * @param zimbraBatchedIndexingSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=619)
    public Map<String,Object> setBatchedIndexingSize(int zimbraBatchedIndexingSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBatchedIndexingSize, Integer.toString(zimbraBatchedIndexingSize));
        return attrs;
    }

    /**
     * Batch size to use when indexing data
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=619)
    public Map<String,Object> unsetBatchedIndexingSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraBatchedIndexingSize, "");
        return attrs;
    }

    /**
     * COS zimbraID
     *
     * @return zimbraCOSId, or null unset
     */
    @ZAttr(id=14)
    public String getCOSId() {
        return getAttr(Provisioning.A_zimbraCOSId);
    }

    /**
     * COS zimbraID
     *
     * @param zimbraCOSId new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=14)
    public Map<String,Object> setCOSId(String zimbraCOSId, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCOSId, zimbraCOSId);
        return attrs;
    }

    /**
     * COS zimbraID
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=14)
    public Map<String,Object> unsetCOSId(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCOSId, "");
        return attrs;
    }

    /**
     * maximum number of revisions to keep for calendar items (appointments
     * and tasks). 0 means unlimited.
     *
     * @return zimbraCalendarMaxRevisions, or -1 if unset
     */
    @ZAttr(id=709)
    public int getCalendarMaxRevisions() {
        return getIntAttr(Provisioning.A_zimbraCalendarMaxRevisions, -1);
    }

    /**
     * maximum number of revisions to keep for calendar items (appointments
     * and tasks). 0 means unlimited.
     *
     * @param zimbraCalendarMaxRevisions new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=709)
    public Map<String,Object> setCalendarMaxRevisions(int zimbraCalendarMaxRevisions, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarMaxRevisions, Integer.toString(zimbraCalendarMaxRevisions));
        return attrs;
    }

    /**
     * maximum number of revisions to keep for calendar items (appointments
     * and tasks). 0 means unlimited.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=709)
    public Map<String,Object> unsetCalendarMaxRevisions(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCalendarMaxRevisions, "");
        return attrs;
    }

    /**
     * zimbraId of child accounts
     *
     * @return zimbraChildAccount, or ampty array if unset
     */
    @ZAttr(id=449)
    public String[] getChildAccount() {
        return getMultiAttr(Provisioning.A_zimbraChildAccount);
    }

    /**
     * zimbraId of child accounts
     *
     * @param zimbraChildAccount new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=449)
    public Map<String,Object> setChildAccount(String[] zimbraChildAccount, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraChildAccount, zimbraChildAccount);
        return attrs;
    }

    /**
     * zimbraId of child accounts
     *
     * @param zimbraChildAccount new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=449)
    public Map<String,Object> addChildAccount(String zimbraChildAccount, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraChildAccount, zimbraChildAccount);
        return attrs;
    }

    /**
     * zimbraId of child accounts
     *
     * @param zimbraChildAccount existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=449)
    public Map<String,Object> removeChildAccount(String zimbraChildAccount, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraChildAccount, zimbraChildAccount);
        return attrs;
    }

    /**
     * zimbraId of child accounts
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=449)
    public Map<String,Object> unsetChildAccount(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraChildAccount, "");
        return attrs;
    }

    /**
     * Deprecated since: 5.0 D4. deprecated in favor of user-settable
     * attribute zimbraPrefChildVisibleAccount . Orig desc: zimbraId of
     * visible child accounts
     *
     * @return zimbraChildVisibleAccount, or ampty array if unset
     */
    @ZAttr(id=450)
    public String[] getChildVisibleAccount() {
        return getMultiAttr(Provisioning.A_zimbraChildVisibleAccount);
    }

    /**
     * Deprecated since: 5.0 D4. deprecated in favor of user-settable
     * attribute zimbraPrefChildVisibleAccount . Orig desc: zimbraId of
     * visible child accounts
     *
     * @param zimbraChildVisibleAccount new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=450)
    public Map<String,Object> setChildVisibleAccount(String[] zimbraChildVisibleAccount, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraChildVisibleAccount, zimbraChildVisibleAccount);
        return attrs;
    }

    /**
     * Deprecated since: 5.0 D4. deprecated in favor of user-settable
     * attribute zimbraPrefChildVisibleAccount . Orig desc: zimbraId of
     * visible child accounts
     *
     * @param zimbraChildVisibleAccount new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=450)
    public Map<String,Object> addChildVisibleAccount(String zimbraChildVisibleAccount, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraChildVisibleAccount, zimbraChildVisibleAccount);
        return attrs;
    }

    /**
     * Deprecated since: 5.0 D4. deprecated in favor of user-settable
     * attribute zimbraPrefChildVisibleAccount . Orig desc: zimbraId of
     * visible child accounts
     *
     * @param zimbraChildVisibleAccount existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=450)
    public Map<String,Object> removeChildVisibleAccount(String zimbraChildVisibleAccount, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraChildVisibleAccount, zimbraChildVisibleAccount);
        return attrs;
    }

    /**
     * Deprecated since: 5.0 D4. deprecated in favor of user-settable
     * attribute zimbraPrefChildVisibleAccount . Orig desc: zimbraId of
     * visible child accounts
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=450)
    public Map<String,Object> unsetChildVisibleAccount(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraChildVisibleAccount, "");
        return attrs;
    }

    /**
     * Comma separates list of attributes in contact object to search for
     * email addresses when generating auto-complete contact list. The same
     * set of fields are used for GAL contacts as well because LDAP
     * attributes for GAL objects are mapped to Contact compatible attributes
     * via zimbraGalLdapAttrMap.
     *
     * @return zimbraContactAutoCompleteEmailFields, or null unset
     *
     * @since ZCS future
     */
    @ZAttr(id=760)
    public String getContactAutoCompleteEmailFields() {
        return getAttr(Provisioning.A_zimbraContactAutoCompleteEmailFields);
    }

    /**
     * Comma separates list of attributes in contact object to search for
     * email addresses when generating auto-complete contact list. The same
     * set of fields are used for GAL contacts as well because LDAP
     * attributes for GAL objects are mapped to Contact compatible attributes
     * via zimbraGalLdapAttrMap.
     *
     * @param zimbraContactAutoCompleteEmailFields new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=760)
    public Map<String,Object> setContactAutoCompleteEmailFields(String zimbraContactAutoCompleteEmailFields, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraContactAutoCompleteEmailFields, zimbraContactAutoCompleteEmailFields);
        return attrs;
    }

    /**
     * Comma separates list of attributes in contact object to search for
     * email addresses when generating auto-complete contact list. The same
     * set of fields are used for GAL contacts as well because LDAP
     * attributes for GAL objects are mapped to Contact compatible attributes
     * via zimbraGalLdapAttrMap.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=760)
    public Map<String,Object> unsetContactAutoCompleteEmailFields(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraContactAutoCompleteEmailFields, "");
        return attrs;
    }

    /**
     * Comma separates list of folder Ids that should be used to search for
     * contacts when generating auto-complete contact list. Folder id of 0 is
     * used to include GAL contacts in the response.
     *
     * @return zimbraContactAutoCompleteFolderIds, or null unset
     *
     * @since ZCS future
     */
    @ZAttr(id=759)
    public String getContactAutoCompleteFolderIds() {
        return getAttr(Provisioning.A_zimbraContactAutoCompleteFolderIds);
    }

    /**
     * Comma separates list of folder Ids that should be used to search for
     * contacts when generating auto-complete contact list. Folder id of 0 is
     * used to include GAL contacts in the response.
     *
     * @param zimbraContactAutoCompleteFolderIds new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=759)
    public Map<String,Object> setContactAutoCompleteFolderIds(String zimbraContactAutoCompleteFolderIds, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraContactAutoCompleteFolderIds, zimbraContactAutoCompleteFolderIds);
        return attrs;
    }

    /**
     * Comma separates list of folder Ids that should be used to search for
     * contacts when generating auto-complete contact list. Folder id of 0 is
     * used to include GAL contacts in the response.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=759)
    public Map<String,Object> unsetContactAutoCompleteFolderIds(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraContactAutoCompleteFolderIds, "");
        return attrs;
    }

    /**
     * Maximum number of contacts allowed in mailbox. 0 means no limit.
     *
     * @return zimbraContactMaxNumEntries, or -1 if unset
     */
    @ZAttr(id=107)
    public int getContactMaxNumEntries() {
        return getIntAttr(Provisioning.A_zimbraContactMaxNumEntries, -1);
    }

    /**
     * Maximum number of contacts allowed in mailbox. 0 means no limit.
     *
     * @param zimbraContactMaxNumEntries new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=107)
    public Map<String,Object> setContactMaxNumEntries(int zimbraContactMaxNumEntries, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraContactMaxNumEntries, Integer.toString(zimbraContactMaxNumEntries));
        return attrs;
    }

    /**
     * Maximum number of contacts allowed in mailbox. 0 means no limit.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=107)
    public Map<String,Object> unsetContactMaxNumEntries(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraContactMaxNumEntries, "");
        return attrs;
    }

    /**
     * Size of the contact ranking table. Ranking table is used to keep track
     * of most heavily used contacts in outgoing email. Contacts in the
     * ranking table are given the priority when generating the auto-complete
     * contact list.
     *
     * @return zimbraContactRankingTableSize, or -1 if unset
     *
     * @since ZCS future
     */
    @ZAttr(id=758)
    public int getContactRankingTableSize() {
        return getIntAttr(Provisioning.A_zimbraContactRankingTableSize, -1);
    }

    /**
     * Size of the contact ranking table. Ranking table is used to keep track
     * of most heavily used contacts in outgoing email. Contacts in the
     * ranking table are given the priority when generating the auto-complete
     * contact list.
     *
     * @param zimbraContactRankingTableSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=758)
    public Map<String,Object> setContactRankingTableSize(int zimbraContactRankingTableSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraContactRankingTableSize, Integer.toString(zimbraContactRankingTableSize));
        return attrs;
    }

    /**
     * Size of the contact ranking table. Ranking table is used to keep track
     * of most heavily used contacts in outgoing email. Contacts in the
     * ranking table are given the priority when generating the auto-complete
     * contact list.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=758)
    public Map<String,Object> unsetContactRankingTableSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraContactRankingTableSize, "");
        return attrs;
    }

    /**
     * set to 1 or 3 to specify customer care account tier level
     *
     * @return zimbraCustomerCareTier, or -1 if unset
     */
    @ZAttr(id=605)
    public int getCustomerCareTier() {
        return getIntAttr(Provisioning.A_zimbraCustomerCareTier, -1);
    }

    /**
     * set to 1 or 3 to specify customer care account tier level
     *
     * @param zimbraCustomerCareTier new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=605)
    public Map<String,Object> setCustomerCareTier(int zimbraCustomerCareTier, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCustomerCareTier, Integer.toString(zimbraCustomerCareTier));
        return attrs;
    }

    /**
     * set to 1 or 3 to specify customer care account tier level
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=605)
    public Map<String,Object> unsetCustomerCareTier(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraCustomerCareTier, "");
        return attrs;
    }

    /**
     * Maximum number of data sources allowed on an account
     *
     * @return zimbraDataSourceMaxNumEntries, or -1 if unset
     */
    @ZAttr(id=426)
    public int getDataSourceMaxNumEntries() {
        return getIntAttr(Provisioning.A_zimbraDataSourceMaxNumEntries, -1);
    }

    /**
     * Maximum number of data sources allowed on an account
     *
     * @param zimbraDataSourceMaxNumEntries new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=426)
    public Map<String,Object> setDataSourceMaxNumEntries(int zimbraDataSourceMaxNumEntries, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDataSourceMaxNumEntries, Integer.toString(zimbraDataSourceMaxNumEntries));
        return attrs;
    }

    /**
     * Maximum number of data sources allowed on an account
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=426)
    public Map<String,Object> unsetDataSourceMaxNumEntries(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDataSourceMaxNumEntries, "");
        return attrs;
    }

    /**
     * Shortest allowed duration for zimbraDataSourcePollingInterval.
     *
     * <p>Use getDataSourceMinPollingIntervalAsString to access value as a string.
     *
     * @see #getDataSourceMinPollingIntervalAsString()
     *
     * @return zimbraDataSourceMinPollingInterval in millseconds, or -1 if unset
     */
    @ZAttr(id=525)
    public long getDataSourceMinPollingInterval() {
        return getTimeInterval(Provisioning.A_zimbraDataSourceMinPollingInterval, -1);
    }

    /**
     * Shortest allowed duration for zimbraDataSourcePollingInterval.
     *
     * @return zimbraDataSourceMinPollingInterval, or null unset
     */
    @ZAttr(id=525)
    public String getDataSourceMinPollingIntervalAsString() {
        return getAttr(Provisioning.A_zimbraDataSourceMinPollingInterval);
    }

    /**
     * Shortest allowed duration for zimbraDataSourcePollingInterval.
     *
     * @param zimbraDataSourceMinPollingInterval new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=525)
    public Map<String,Object> setDataSourceMinPollingInterval(String zimbraDataSourceMinPollingInterval, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDataSourceMinPollingInterval, zimbraDataSourceMinPollingInterval);
        return attrs;
    }

    /**
     * Shortest allowed duration for zimbraDataSourcePollingInterval.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=525)
    public Map<String,Object> unsetDataSourceMinPollingInterval(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDataSourceMinPollingInterval, "");
        return attrs;
    }

    /**
     * The time interval between automated data imports for a data source, or
     * all data sources owned by an account. If unset or 0, the data source
     * will not be scheduled for automated polling.
     *
     * <p>Use getDataSourcePollingIntervalAsString to access value as a string.
     *
     * @see #getDataSourcePollingIntervalAsString()
     *
     * @return zimbraDataSourcePollingInterval in millseconds, or -1 if unset
     */
    @ZAttr(id=455)
    public long getDataSourcePollingInterval() {
        return getTimeInterval(Provisioning.A_zimbraDataSourcePollingInterval, -1);
    }

    /**
     * The time interval between automated data imports for a data source, or
     * all data sources owned by an account. If unset or 0, the data source
     * will not be scheduled for automated polling.
     *
     * @return zimbraDataSourcePollingInterval, or null unset
     */
    @ZAttr(id=455)
    public String getDataSourcePollingIntervalAsString() {
        return getAttr(Provisioning.A_zimbraDataSourcePollingInterval);
    }

    /**
     * The time interval between automated data imports for a data source, or
     * all data sources owned by an account. If unset or 0, the data source
     * will not be scheduled for automated polling.
     *
     * @param zimbraDataSourcePollingInterval new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=455)
    public Map<String,Object> setDataSourcePollingInterval(String zimbraDataSourcePollingInterval, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDataSourcePollingInterval, zimbraDataSourcePollingInterval);
        return attrs;
    }

    /**
     * The time interval between automated data imports for a data source, or
     * all data sources owned by an account. If unset or 0, the data source
     * will not be scheduled for automated polling.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=455)
    public Map<String,Object> unsetDataSourcePollingInterval(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDataSourcePollingInterval, "");
        return attrs;
    }

    /**
     * For selective enabling of debug logging
     *
     * @return zimbraDebugInfo, or ampty array if unset
     */
    @ZAttr(id=365)
    public String[] getDebugInfo() {
        return getMultiAttr(Provisioning.A_zimbraDebugInfo);
    }

    /**
     * For selective enabling of debug logging
     *
     * @param zimbraDebugInfo new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=365)
    public Map<String,Object> setDebugInfo(String[] zimbraDebugInfo, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDebugInfo, zimbraDebugInfo);
        return attrs;
    }

    /**
     * For selective enabling of debug logging
     *
     * @param zimbraDebugInfo new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=365)
    public Map<String,Object> addDebugInfo(String zimbraDebugInfo, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraDebugInfo, zimbraDebugInfo);
        return attrs;
    }

    /**
     * For selective enabling of debug logging
     *
     * @param zimbraDebugInfo existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=365)
    public Map<String,Object> removeDebugInfo(String zimbraDebugInfo, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraDebugInfo, zimbraDebugInfo);
        return attrs;
    }

    /**
     * For selective enabling of debug logging
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=365)
    public Map<String,Object> unsetDebugInfo(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDebugInfo, "");
        return attrs;
    }

    /**
     * maximum amount of mail quota a domain admin can set on a user
     *
     * @return zimbraDomainAdminMaxMailQuota, or -1 if unset
     */
    @ZAttr(id=398)
    public long getDomainAdminMaxMailQuota() {
        return getLongAttr(Provisioning.A_zimbraDomainAdminMaxMailQuota, -1);
    }

    /**
     * maximum amount of mail quota a domain admin can set on a user
     *
     * @param zimbraDomainAdminMaxMailQuota new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=398)
    public Map<String,Object> setDomainAdminMaxMailQuota(long zimbraDomainAdminMaxMailQuota, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAdminMaxMailQuota, Long.toString(zimbraDomainAdminMaxMailQuota));
        return attrs;
    }

    /**
     * maximum amount of mail quota a domain admin can set on a user
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=398)
    public Map<String,Object> unsetDomainAdminMaxMailQuota(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraDomainAdminMaxMailQuota, "");
        return attrs;
    }

    /**
     * Indicates the account should be excluded from Crossmailbox searchers.
     *
     * @return zimbraExcludeFromCMBSearch, or false if unset
     */
    @ZAttr(id=501)
    public boolean isExcludeFromCMBSearch() {
        return getBooleanAttr(Provisioning.A_zimbraExcludeFromCMBSearch, false);
    }

    /**
     * Indicates the account should be excluded from Crossmailbox searchers.
     *
     * @param zimbraExcludeFromCMBSearch new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=501)
    public Map<String,Object> setExcludeFromCMBSearch(boolean zimbraExcludeFromCMBSearch, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExcludeFromCMBSearch, Boolean.toString(zimbraExcludeFromCMBSearch));
        return attrs;
    }

    /**
     * Indicates the account should be excluded from Crossmailbox searchers.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=501)
    public Map<String,Object> unsetExcludeFromCMBSearch(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraExcludeFromCMBSearch, "");
        return attrs;
    }

    /**
     * advanced search button enabled
     *
     * @return zimbraFeatureAdvancedSearchEnabled, or false if unset
     */
    @ZAttr(id=138)
    public boolean isFeatureAdvancedSearchEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureAdvancedSearchEnabled, false);
    }

    /**
     * advanced search button enabled
     *
     * @param zimbraFeatureAdvancedSearchEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=138)
    public Map<String,Object> setFeatureAdvancedSearchEnabled(boolean zimbraFeatureAdvancedSearchEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureAdvancedSearchEnabled, Boolean.toString(zimbraFeatureAdvancedSearchEnabled));
        return attrs;
    }

    /**
     * advanced search button enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=138)
    public Map<String,Object> unsetFeatureAdvancedSearchEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureAdvancedSearchEnabled, "");
        return attrs;
    }

    /**
     * whether to allow use of briefcase feature
     *
     * @return zimbraFeatureBriefcasesEnabled, or false if unset
     */
    @ZAttr(id=498)
    public boolean isFeatureBriefcasesEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureBriefcasesEnabled, false);
    }

    /**
     * whether to allow use of briefcase feature
     *
     * @param zimbraFeatureBriefcasesEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=498)
    public Map<String,Object> setFeatureBriefcasesEnabled(boolean zimbraFeatureBriefcasesEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureBriefcasesEnabled, Boolean.toString(zimbraFeatureBriefcasesEnabled));
        return attrs;
    }

    /**
     * whether to allow use of briefcase feature
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=498)
    public Map<String,Object> unsetFeatureBriefcasesEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureBriefcasesEnabled, "");
        return attrs;
    }

    /**
     * calendar features
     *
     * @return zimbraFeatureCalendarEnabled, or false if unset
     */
    @ZAttr(id=136)
    public boolean isFeatureCalendarEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureCalendarEnabled, false);
    }

    /**
     * calendar features
     *
     * @param zimbraFeatureCalendarEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=136)
    public Map<String,Object> setFeatureCalendarEnabled(boolean zimbraFeatureCalendarEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureCalendarEnabled, Boolean.toString(zimbraFeatureCalendarEnabled));
        return attrs;
    }

    /**
     * calendar features
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=136)
    public Map<String,Object> unsetFeatureCalendarEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureCalendarEnabled, "");
        return attrs;
    }

    /**
     * calendar upsell enabled
     *
     * @return zimbraFeatureCalendarUpsellEnabled, or false if unset
     */
    @ZAttr(id=531)
    public boolean isFeatureCalendarUpsellEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureCalendarUpsellEnabled, false);
    }

    /**
     * calendar upsell enabled
     *
     * @param zimbraFeatureCalendarUpsellEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=531)
    public Map<String,Object> setFeatureCalendarUpsellEnabled(boolean zimbraFeatureCalendarUpsellEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureCalendarUpsellEnabled, Boolean.toString(zimbraFeatureCalendarUpsellEnabled));
        return attrs;
    }

    /**
     * calendar upsell enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=531)
    public Map<String,Object> unsetFeatureCalendarUpsellEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureCalendarUpsellEnabled, "");
        return attrs;
    }

    /**
     * calendar upsell URL
     *
     * @return zimbraFeatureCalendarUpsellURL, or null unset
     */
    @ZAttr(id=532)
    public String getFeatureCalendarUpsellURL() {
        return getAttr(Provisioning.A_zimbraFeatureCalendarUpsellURL);
    }

    /**
     * calendar upsell URL
     *
     * @param zimbraFeatureCalendarUpsellURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=532)
    public Map<String,Object> setFeatureCalendarUpsellURL(String zimbraFeatureCalendarUpsellURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureCalendarUpsellURL, zimbraFeatureCalendarUpsellURL);
        return attrs;
    }

    /**
     * calendar upsell URL
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=532)
    public Map<String,Object> unsetFeatureCalendarUpsellURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureCalendarUpsellURL, "");
        return attrs;
    }

    /**
     * password changing
     *
     * @return zimbraFeatureChangePasswordEnabled, or false if unset
     */
    @ZAttr(id=141)
    public boolean isFeatureChangePasswordEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureChangePasswordEnabled, false);
    }

    /**
     * password changing
     *
     * @param zimbraFeatureChangePasswordEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=141)
    public Map<String,Object> setFeatureChangePasswordEnabled(boolean zimbraFeatureChangePasswordEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureChangePasswordEnabled, Boolean.toString(zimbraFeatureChangePasswordEnabled));
        return attrs;
    }

    /**
     * password changing
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=141)
    public Map<String,Object> unsetFeatureChangePasswordEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureChangePasswordEnabled, "");
        return attrs;
    }

    /**
     * whether or not compose messages in a new windows is allowed
     *
     * @return zimbraFeatureComposeInNewWindowEnabled, or false if unset
     */
    @ZAttr(id=584)
    public boolean isFeatureComposeInNewWindowEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureComposeInNewWindowEnabled, false);
    }

    /**
     * whether or not compose messages in a new windows is allowed
     *
     * @param zimbraFeatureComposeInNewWindowEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=584)
    public Map<String,Object> setFeatureComposeInNewWindowEnabled(boolean zimbraFeatureComposeInNewWindowEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureComposeInNewWindowEnabled, Boolean.toString(zimbraFeatureComposeInNewWindowEnabled));
        return attrs;
    }

    /**
     * whether or not compose messages in a new windows is allowed
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=584)
    public Map<String,Object> unsetFeatureComposeInNewWindowEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureComposeInNewWindowEnabled, "");
        return attrs;
    }

    /**
     * contact features
     *
     * @return zimbraFeatureContactsEnabled, or false if unset
     */
    @ZAttr(id=135)
    public boolean isFeatureContactsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureContactsEnabled, false);
    }

    /**
     * contact features
     *
     * @param zimbraFeatureContactsEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=135)
    public Map<String,Object> setFeatureContactsEnabled(boolean zimbraFeatureContactsEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureContactsEnabled, Boolean.toString(zimbraFeatureContactsEnabled));
        return attrs;
    }

    /**
     * contact features
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=135)
    public Map<String,Object> unsetFeatureContactsEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureContactsEnabled, "");
        return attrs;
    }

    /**
     * address book upsell enabled
     *
     * @return zimbraFeatureContactsUpsellEnabled, or false if unset
     */
    @ZAttr(id=529)
    public boolean isFeatureContactsUpsellEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureContactsUpsellEnabled, false);
    }

    /**
     * address book upsell enabled
     *
     * @param zimbraFeatureContactsUpsellEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=529)
    public Map<String,Object> setFeatureContactsUpsellEnabled(boolean zimbraFeatureContactsUpsellEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureContactsUpsellEnabled, Boolean.toString(zimbraFeatureContactsUpsellEnabled));
        return attrs;
    }

    /**
     * address book upsell enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=529)
    public Map<String,Object> unsetFeatureContactsUpsellEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureContactsUpsellEnabled, "");
        return attrs;
    }

    /**
     * address book upsell URL
     *
     * @return zimbraFeatureContactsUpsellURL, or null unset
     */
    @ZAttr(id=530)
    public String getFeatureContactsUpsellURL() {
        return getAttr(Provisioning.A_zimbraFeatureContactsUpsellURL);
    }

    /**
     * address book upsell URL
     *
     * @param zimbraFeatureContactsUpsellURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=530)
    public Map<String,Object> setFeatureContactsUpsellURL(String zimbraFeatureContactsUpsellURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureContactsUpsellURL, zimbraFeatureContactsUpsellURL);
        return attrs;
    }

    /**
     * address book upsell URL
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=530)
    public Map<String,Object> unsetFeatureContactsUpsellURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureContactsUpsellURL, "");
        return attrs;
    }

    /**
     * conversations
     *
     * @return zimbraFeatureConversationsEnabled, or false if unset
     */
    @ZAttr(id=140)
    public boolean isFeatureConversationsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureConversationsEnabled, false);
    }

    /**
     * conversations
     *
     * @param zimbraFeatureConversationsEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=140)
    public Map<String,Object> setFeatureConversationsEnabled(boolean zimbraFeatureConversationsEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureConversationsEnabled, Boolean.toString(zimbraFeatureConversationsEnabled));
        return attrs;
    }

    /**
     * conversations
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=140)
    public Map<String,Object> unsetFeatureConversationsEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureConversationsEnabled, "");
        return attrs;
    }

    /**
     * filter prefs enabled
     *
     * @return zimbraFeatureFiltersEnabled, or false if unset
     */
    @ZAttr(id=143)
    public boolean isFeatureFiltersEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureFiltersEnabled, false);
    }

    /**
     * filter prefs enabled
     *
     * @param zimbraFeatureFiltersEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=143)
    public Map<String,Object> setFeatureFiltersEnabled(boolean zimbraFeatureFiltersEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureFiltersEnabled, Boolean.toString(zimbraFeatureFiltersEnabled));
        return attrs;
    }

    /**
     * filter prefs enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=143)
    public Map<String,Object> unsetFeatureFiltersEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureFiltersEnabled, "");
        return attrs;
    }

    /**
     * whether to allow use of flagging feature
     *
     * @return zimbraFeatureFlaggingEnabled, or false if unset
     */
    @ZAttr(id=499)
    public boolean isFeatureFlaggingEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureFlaggingEnabled, false);
    }

    /**
     * whether to allow use of flagging feature
     *
     * @param zimbraFeatureFlaggingEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=499)
    public Map<String,Object> setFeatureFlaggingEnabled(boolean zimbraFeatureFlaggingEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureFlaggingEnabled, Boolean.toString(zimbraFeatureFlaggingEnabled));
        return attrs;
    }

    /**
     * whether to allow use of flagging feature
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=499)
    public Map<String,Object> unsetFeatureFlaggingEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureFlaggingEnabled, "");
        return attrs;
    }

    /**
     * enable auto-completion from the GAL, zimbraFeatureGalEnabled also has
     * to be enabled for the auto-completion feature
     *
     * @return zimbraFeatureGalAutoCompleteEnabled, or false if unset
     */
    @ZAttr(id=359)
    public boolean isFeatureGalAutoCompleteEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureGalAutoCompleteEnabled, false);
    }

    /**
     * enable auto-completion from the GAL, zimbraFeatureGalEnabled also has
     * to be enabled for the auto-completion feature
     *
     * @param zimbraFeatureGalAutoCompleteEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=359)
    public Map<String,Object> setFeatureGalAutoCompleteEnabled(boolean zimbraFeatureGalAutoCompleteEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureGalAutoCompleteEnabled, Boolean.toString(zimbraFeatureGalAutoCompleteEnabled));
        return attrs;
    }

    /**
     * enable auto-completion from the GAL, zimbraFeatureGalEnabled also has
     * to be enabled for the auto-completion feature
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=359)
    public Map<String,Object> unsetFeatureGalAutoCompleteEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureGalAutoCompleteEnabled, "");
        return attrs;
    }

    /**
     * whether GAL features are enabled
     *
     * @return zimbraFeatureGalEnabled, or false if unset
     */
    @ZAttr(id=149)
    public boolean isFeatureGalEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureGalEnabled, false);
    }

    /**
     * whether GAL features are enabled
     *
     * @param zimbraFeatureGalEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=149)
    public Map<String,Object> setFeatureGalEnabled(boolean zimbraFeatureGalEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureGalEnabled, Boolean.toString(zimbraFeatureGalEnabled));
        return attrs;
    }

    /**
     * whether GAL features are enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=149)
    public Map<String,Object> unsetFeatureGalEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureGalEnabled, "");
        return attrs;
    }

    /**
     * whether GAL sync feature is enabled
     *
     * @return zimbraFeatureGalSyncEnabled, or false if unset
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=711)
    public boolean isFeatureGalSyncEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureGalSyncEnabled, false);
    }

    /**
     * whether GAL sync feature is enabled
     *
     * @param zimbraFeatureGalSyncEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=711)
    public Map<String,Object> setFeatureGalSyncEnabled(boolean zimbraFeatureGalSyncEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureGalSyncEnabled, Boolean.toString(zimbraFeatureGalSyncEnabled));
        return attrs;
    }

    /**
     * whether GAL sync feature is enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=711)
    public Map<String,Object> unsetFeatureGalSyncEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureGalSyncEnabled, "");
        return attrs;
    }

    /**
     * group calendar features. if set to FALSE, calendar works as a personal
     * calendar and attendees and scheduling etc are turned off in web UI
     *
     * @return zimbraFeatureGroupCalendarEnabled, or false if unset
     */
    @ZAttr(id=481)
    public boolean isFeatureGroupCalendarEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureGroupCalendarEnabled, false);
    }

    /**
     * group calendar features. if set to FALSE, calendar works as a personal
     * calendar and attendees and scheduling etc are turned off in web UI
     *
     * @param zimbraFeatureGroupCalendarEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=481)
    public Map<String,Object> setFeatureGroupCalendarEnabled(boolean zimbraFeatureGroupCalendarEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureGroupCalendarEnabled, Boolean.toString(zimbraFeatureGroupCalendarEnabled));
        return attrs;
    }

    /**
     * group calendar features. if set to FALSE, calendar works as a personal
     * calendar and attendees and scheduling etc are turned off in web UI
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=481)
    public Map<String,Object> unsetFeatureGroupCalendarEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureGroupCalendarEnabled, "");
        return attrs;
    }

    /**
     * enabled html composing
     *
     * @return zimbraFeatureHtmlComposeEnabled, or false if unset
     */
    @ZAttr(id=219)
    public boolean isFeatureHtmlComposeEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureHtmlComposeEnabled, false);
    }

    /**
     * enabled html composing
     *
     * @param zimbraFeatureHtmlComposeEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=219)
    public Map<String,Object> setFeatureHtmlComposeEnabled(boolean zimbraFeatureHtmlComposeEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureHtmlComposeEnabled, Boolean.toString(zimbraFeatureHtmlComposeEnabled));
        return attrs;
    }

    /**
     * enabled html composing
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=219)
    public Map<String,Object> unsetFeatureHtmlComposeEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureHtmlComposeEnabled, "");
        return attrs;
    }

    /**
     * IM features
     *
     * @return zimbraFeatureIMEnabled, or false if unset
     */
    @ZAttr(id=305)
    public boolean isFeatureIMEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureIMEnabled, false);
    }

    /**
     * IM features
     *
     * @param zimbraFeatureIMEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=305)
    public Map<String,Object> setFeatureIMEnabled(boolean zimbraFeatureIMEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureIMEnabled, Boolean.toString(zimbraFeatureIMEnabled));
        return attrs;
    }

    /**
     * IM features
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=305)
    public Map<String,Object> unsetFeatureIMEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureIMEnabled, "");
        return attrs;
    }

    /**
     * whether to allow use of identities feature
     *
     * @return zimbraFeatureIdentitiesEnabled, or false if unset
     */
    @ZAttr(id=415)
    public boolean isFeatureIdentitiesEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureIdentitiesEnabled, false);
    }

    /**
     * whether to allow use of identities feature
     *
     * @param zimbraFeatureIdentitiesEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=415)
    public Map<String,Object> setFeatureIdentitiesEnabled(boolean zimbraFeatureIdentitiesEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureIdentitiesEnabled, Boolean.toString(zimbraFeatureIdentitiesEnabled));
        return attrs;
    }

    /**
     * whether to allow use of identities feature
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=415)
    public Map<String,Object> unsetFeatureIdentitiesEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureIdentitiesEnabled, "");
        return attrs;
    }

    /**
     * whether user is allowed to retrieve mail from an external IMAP data
     * source
     *
     * @return zimbraFeatureImapDataSourceEnabled, or false if unset
     */
    @ZAttr(id=568)
    public boolean isFeatureImapDataSourceEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureImapDataSourceEnabled, false);
    }

    /**
     * whether user is allowed to retrieve mail from an external IMAP data
     * source
     *
     * @param zimbraFeatureImapDataSourceEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=568)
    public Map<String,Object> setFeatureImapDataSourceEnabled(boolean zimbraFeatureImapDataSourceEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureImapDataSourceEnabled, Boolean.toString(zimbraFeatureImapDataSourceEnabled));
        return attrs;
    }

    /**
     * whether user is allowed to retrieve mail from an external IMAP data
     * source
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=568)
    public Map<String,Object> unsetFeatureImapDataSourceEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureImapDataSourceEnabled, "");
        return attrs;
    }

    /**
     * whether import export folder feature is enabled
     *
     * @return zimbraFeatureImportExportFolderEnabled, or false if unset
     *
     * @since ZCS future
     */
    @ZAttr(id=750)
    public boolean isFeatureImportExportFolderEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureImportExportFolderEnabled, false);
    }

    /**
     * whether import export folder feature is enabled
     *
     * @param zimbraFeatureImportExportFolderEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=750)
    public Map<String,Object> setFeatureImportExportFolderEnabled(boolean zimbraFeatureImportExportFolderEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureImportExportFolderEnabled, Boolean.toString(zimbraFeatureImportExportFolderEnabled));
        return attrs;
    }

    /**
     * whether import export folder feature is enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=750)
    public Map<String,Object> unsetFeatureImportExportFolderEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureImportExportFolderEnabled, "");
        return attrs;
    }

    /**
     * preference to set initial search
     *
     * @return zimbraFeatureInitialSearchPreferenceEnabled, or false if unset
     */
    @ZAttr(id=142)
    public boolean isFeatureInitialSearchPreferenceEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureInitialSearchPreferenceEnabled, false);
    }

    /**
     * preference to set initial search
     *
     * @param zimbraFeatureInitialSearchPreferenceEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=142)
    public Map<String,Object> setFeatureInitialSearchPreferenceEnabled(boolean zimbraFeatureInitialSearchPreferenceEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureInitialSearchPreferenceEnabled, Boolean.toString(zimbraFeatureInitialSearchPreferenceEnabled));
        return attrs;
    }

    /**
     * preference to set initial search
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=142)
    public Map<String,Object> unsetFeatureInitialSearchPreferenceEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureInitialSearchPreferenceEnabled, "");
        return attrs;
    }

    /**
     * Enable instant notifications
     *
     * @return zimbraFeatureInstantNotify, or false if unset
     */
    @ZAttr(id=521)
    public boolean isFeatureInstantNotify() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureInstantNotify, false);
    }

    /**
     * Enable instant notifications
     *
     * @param zimbraFeatureInstantNotify new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=521)
    public Map<String,Object> setFeatureInstantNotify(boolean zimbraFeatureInstantNotify, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureInstantNotify, Boolean.toString(zimbraFeatureInstantNotify));
        return attrs;
    }

    /**
     * Enable instant notifications
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=521)
    public Map<String,Object> unsetFeatureInstantNotify(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureInstantNotify, "");
        return attrs;
    }

    /**
     * email features enabled
     *
     * @return zimbraFeatureMailEnabled, or false if unset
     */
    @ZAttr(id=489)
    public boolean isFeatureMailEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureMailEnabled, false);
    }

    /**
     * email features enabled
     *
     * @param zimbraFeatureMailEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=489)
    public Map<String,Object> setFeatureMailEnabled(boolean zimbraFeatureMailEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureMailEnabled, Boolean.toString(zimbraFeatureMailEnabled));
        return attrs;
    }

    /**
     * email features enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=489)
    public Map<String,Object> unsetFeatureMailEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureMailEnabled, "");
        return attrs;
    }

    /**
     * enable end-user mail forwarding features
     *
     * @return zimbraFeatureMailForwardingEnabled, or false if unset
     */
    @ZAttr(id=342)
    public boolean isFeatureMailForwardingEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureMailForwardingEnabled, false);
    }

    /**
     * enable end-user mail forwarding features
     *
     * @param zimbraFeatureMailForwardingEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=342)
    public Map<String,Object> setFeatureMailForwardingEnabled(boolean zimbraFeatureMailForwardingEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureMailForwardingEnabled, Boolean.toString(zimbraFeatureMailForwardingEnabled));
        return attrs;
    }

    /**
     * enable end-user mail forwarding features
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=342)
    public Map<String,Object> unsetFeatureMailForwardingEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureMailForwardingEnabled, "");
        return attrs;
    }

    /**
     * enable end-user mail forwarding defined in mail filters features
     *
     * @return zimbraFeatureMailForwardingInFiltersEnabled, or false if unset
     */
    @ZAttr(id=704)
    public boolean isFeatureMailForwardingInFiltersEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureMailForwardingInFiltersEnabled, false);
    }

    /**
     * enable end-user mail forwarding defined in mail filters features
     *
     * @param zimbraFeatureMailForwardingInFiltersEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=704)
    public Map<String,Object> setFeatureMailForwardingInFiltersEnabled(boolean zimbraFeatureMailForwardingInFiltersEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureMailForwardingInFiltersEnabled, Boolean.toString(zimbraFeatureMailForwardingInFiltersEnabled));
        return attrs;
    }

    /**
     * enable end-user mail forwarding defined in mail filters features
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=704)
    public Map<String,Object> unsetFeatureMailForwardingInFiltersEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureMailForwardingInFiltersEnabled, "");
        return attrs;
    }

    /**
     * Deprecated since: 5.0. done via skin template overrides. Orig desc:
     * whether user is allowed to set mail polling interval
     *
     * @return zimbraFeatureMailPollingIntervalPreferenceEnabled, or false if unset
     */
    @ZAttr(id=441)
    public boolean isFeatureMailPollingIntervalPreferenceEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureMailPollingIntervalPreferenceEnabled, false);
    }

    /**
     * Deprecated since: 5.0. done via skin template overrides. Orig desc:
     * whether user is allowed to set mail polling interval
     *
     * @param zimbraFeatureMailPollingIntervalPreferenceEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=441)
    public Map<String,Object> setFeatureMailPollingIntervalPreferenceEnabled(boolean zimbraFeatureMailPollingIntervalPreferenceEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureMailPollingIntervalPreferenceEnabled, Boolean.toString(zimbraFeatureMailPollingIntervalPreferenceEnabled));
        return attrs;
    }

    /**
     * Deprecated since: 5.0. done via skin template overrides. Orig desc:
     * whether user is allowed to set mail polling interval
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=441)
    public Map<String,Object> unsetFeatureMailPollingIntervalPreferenceEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureMailPollingIntervalPreferenceEnabled, "");
        return attrs;
    }

    /**
     * mail priority feature
     *
     * @return zimbraFeatureMailPriorityEnabled, or false if unset
     */
    @ZAttr(id=566)
    public boolean isFeatureMailPriorityEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureMailPriorityEnabled, false);
    }

    /**
     * mail priority feature
     *
     * @param zimbraFeatureMailPriorityEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=566)
    public Map<String,Object> setFeatureMailPriorityEnabled(boolean zimbraFeatureMailPriorityEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureMailPriorityEnabled, Boolean.toString(zimbraFeatureMailPriorityEnabled));
        return attrs;
    }

    /**
     * mail priority feature
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=566)
    public Map<String,Object> unsetFeatureMailPriorityEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureMailPriorityEnabled, "");
        return attrs;
    }

    /**
     * email upsell enabled
     *
     * @return zimbraFeatureMailUpsellEnabled, or false if unset
     */
    @ZAttr(id=527)
    public boolean isFeatureMailUpsellEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureMailUpsellEnabled, false);
    }

    /**
     * email upsell enabled
     *
     * @param zimbraFeatureMailUpsellEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=527)
    public Map<String,Object> setFeatureMailUpsellEnabled(boolean zimbraFeatureMailUpsellEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureMailUpsellEnabled, Boolean.toString(zimbraFeatureMailUpsellEnabled));
        return attrs;
    }

    /**
     * email upsell enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=527)
    public Map<String,Object> unsetFeatureMailUpsellEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureMailUpsellEnabled, "");
        return attrs;
    }

    /**
     * email upsell URL
     *
     * @return zimbraFeatureMailUpsellURL, or null unset
     */
    @ZAttr(id=528)
    public String getFeatureMailUpsellURL() {
        return getAttr(Provisioning.A_zimbraFeatureMailUpsellURL);
    }

    /**
     * email upsell URL
     *
     * @param zimbraFeatureMailUpsellURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=528)
    public Map<String,Object> setFeatureMailUpsellURL(String zimbraFeatureMailUpsellURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureMailUpsellURL, zimbraFeatureMailUpsellURL);
        return attrs;
    }

    /**
     * email upsell URL
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=528)
    public Map<String,Object> unsetFeatureMailUpsellURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureMailUpsellURL, "");
        return attrs;
    }

    /**
     * whether to permit mobile sync
     *
     * @return zimbraFeatureMobileSyncEnabled, or false if unset
     */
    @ZAttr(id=347)
    public boolean isFeatureMobileSyncEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureMobileSyncEnabled, false);
    }

    /**
     * whether to permit mobile sync
     *
     * @param zimbraFeatureMobileSyncEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=347)
    public Map<String,Object> setFeatureMobileSyncEnabled(boolean zimbraFeatureMobileSyncEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureMobileSyncEnabled, Boolean.toString(zimbraFeatureMobileSyncEnabled));
        return attrs;
    }

    /**
     * whether to permit mobile sync
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=347)
    public Map<String,Object> unsetFeatureMobileSyncEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureMobileSyncEnabled, "");
        return attrs;
    }

    /**
     * Whether user can create address books
     *
     * @return zimbraFeatureNewAddrBookEnabled, or false if unset
     */
    @ZAttr(id=631)
    public boolean isFeatureNewAddrBookEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureNewAddrBookEnabled, false);
    }

    /**
     * Whether user can create address books
     *
     * @param zimbraFeatureNewAddrBookEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=631)
    public Map<String,Object> setFeatureNewAddrBookEnabled(boolean zimbraFeatureNewAddrBookEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureNewAddrBookEnabled, Boolean.toString(zimbraFeatureNewAddrBookEnabled));
        return attrs;
    }

    /**
     * Whether user can create address books
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=631)
    public Map<String,Object> unsetFeatureNewAddrBookEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureNewAddrBookEnabled, "");
        return attrs;
    }

    /**
     * Whether new mail notification feature should be allowed for this
     * account or in this cos
     *
     * @return zimbraFeatureNewMailNotificationEnabled, or false if unset
     */
    @ZAttr(id=367)
    public boolean isFeatureNewMailNotificationEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureNewMailNotificationEnabled, false);
    }

    /**
     * Whether new mail notification feature should be allowed for this
     * account or in this cos
     *
     * @param zimbraFeatureNewMailNotificationEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=367)
    public Map<String,Object> setFeatureNewMailNotificationEnabled(boolean zimbraFeatureNewMailNotificationEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureNewMailNotificationEnabled, Boolean.toString(zimbraFeatureNewMailNotificationEnabled));
        return attrs;
    }

    /**
     * Whether new mail notification feature should be allowed for this
     * account or in this cos
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=367)
    public Map<String,Object> unsetFeatureNewMailNotificationEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureNewMailNotificationEnabled, "");
        return attrs;
    }

    /**
     * Whether notebook feature should be allowed for this account or in this
     * cos
     *
     * @return zimbraFeatureNotebookEnabled, or false if unset
     */
    @ZAttr(id=356)
    public boolean isFeatureNotebookEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureNotebookEnabled, false);
    }

    /**
     * Whether notebook feature should be allowed for this account or in this
     * cos
     *
     * @param zimbraFeatureNotebookEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=356)
    public Map<String,Object> setFeatureNotebookEnabled(boolean zimbraFeatureNotebookEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureNotebookEnabled, Boolean.toString(zimbraFeatureNotebookEnabled));
        return attrs;
    }

    /**
     * Whether notebook feature should be allowed for this account or in this
     * cos
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=356)
    public Map<String,Object> unsetFeatureNotebookEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureNotebookEnabled, "");
        return attrs;
    }

    /**
     * whether or not open a new msg/conv in a new windows is allowed
     *
     * @return zimbraFeatureOpenMailInNewWindowEnabled, or false if unset
     */
    @ZAttr(id=585)
    public boolean isFeatureOpenMailInNewWindowEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureOpenMailInNewWindowEnabled, false);
    }

    /**
     * whether or not open a new msg/conv in a new windows is allowed
     *
     * @param zimbraFeatureOpenMailInNewWindowEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=585)
    public Map<String,Object> setFeatureOpenMailInNewWindowEnabled(boolean zimbraFeatureOpenMailInNewWindowEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureOpenMailInNewWindowEnabled, Boolean.toString(zimbraFeatureOpenMailInNewWindowEnabled));
        return attrs;
    }

    /**
     * whether or not open a new msg/conv in a new windows is allowed
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=585)
    public Map<String,Object> unsetFeatureOpenMailInNewWindowEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureOpenMailInNewWindowEnabled, "");
        return attrs;
    }

    /**
     * whether an account can modify its zimbraPref* attributes
     *
     * @return zimbraFeatureOptionsEnabled, or false if unset
     */
    @ZAttr(id=451)
    public boolean isFeatureOptionsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureOptionsEnabled, false);
    }

    /**
     * whether an account can modify its zimbraPref* attributes
     *
     * @param zimbraFeatureOptionsEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=451)
    public Map<String,Object> setFeatureOptionsEnabled(boolean zimbraFeatureOptionsEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureOptionsEnabled, Boolean.toString(zimbraFeatureOptionsEnabled));
        return attrs;
    }

    /**
     * whether an account can modify its zimbraPref* attributes
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=451)
    public Map<String,Object> unsetFeatureOptionsEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureOptionsEnabled, "");
        return attrs;
    }

    /**
     * Whether out of office reply feature should be allowed for this account
     * or in this cos
     *
     * @return zimbraFeatureOutOfOfficeReplyEnabled, or false if unset
     */
    @ZAttr(id=366)
    public boolean isFeatureOutOfOfficeReplyEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureOutOfOfficeReplyEnabled, false);
    }

    /**
     * Whether out of office reply feature should be allowed for this account
     * or in this cos
     *
     * @param zimbraFeatureOutOfOfficeReplyEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=366)
    public Map<String,Object> setFeatureOutOfOfficeReplyEnabled(boolean zimbraFeatureOutOfOfficeReplyEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureOutOfOfficeReplyEnabled, Boolean.toString(zimbraFeatureOutOfOfficeReplyEnabled));
        return attrs;
    }

    /**
     * Whether out of office reply feature should be allowed for this account
     * or in this cos
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=366)
    public Map<String,Object> unsetFeatureOutOfOfficeReplyEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureOutOfOfficeReplyEnabled, "");
        return attrs;
    }

    /**
     * whether user is allowed to retrieve mail from an external POP3 data
     * source
     *
     * @return zimbraFeaturePop3DataSourceEnabled, or false if unset
     */
    @ZAttr(id=416)
    public boolean isFeaturePop3DataSourceEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeaturePop3DataSourceEnabled, false);
    }

    /**
     * whether user is allowed to retrieve mail from an external POP3 data
     * source
     *
     * @param zimbraFeaturePop3DataSourceEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=416)
    public Map<String,Object> setFeaturePop3DataSourceEnabled(boolean zimbraFeaturePop3DataSourceEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeaturePop3DataSourceEnabled, Boolean.toString(zimbraFeaturePop3DataSourceEnabled));
        return attrs;
    }

    /**
     * whether user is allowed to retrieve mail from an external POP3 data
     * source
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=416)
    public Map<String,Object> unsetFeaturePop3DataSourceEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeaturePop3DataSourceEnabled, "");
        return attrs;
    }

    /**
     * portal features
     *
     * @return zimbraFeaturePortalEnabled, or false if unset
     */
    @ZAttr(id=447)
    public boolean isFeaturePortalEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeaturePortalEnabled, false);
    }

    /**
     * portal features
     *
     * @param zimbraFeaturePortalEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=447)
    public Map<String,Object> setFeaturePortalEnabled(boolean zimbraFeaturePortalEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeaturePortalEnabled, Boolean.toString(zimbraFeaturePortalEnabled));
        return attrs;
    }

    /**
     * portal features
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=447)
    public Map<String,Object> unsetFeaturePortalEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeaturePortalEnabled, "");
        return attrs;
    }

    /**
     * saved search feature
     *
     * @return zimbraFeatureSavedSearchesEnabled, or false if unset
     */
    @ZAttr(id=139)
    public boolean isFeatureSavedSearchesEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureSavedSearchesEnabled, false);
    }

    /**
     * saved search feature
     *
     * @param zimbraFeatureSavedSearchesEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=139)
    public Map<String,Object> setFeatureSavedSearchesEnabled(boolean zimbraFeatureSavedSearchesEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureSavedSearchesEnabled, Boolean.toString(zimbraFeatureSavedSearchesEnabled));
        return attrs;
    }

    /**
     * saved search feature
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=139)
    public Map<String,Object> unsetFeatureSavedSearchesEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureSavedSearchesEnabled, "");
        return attrs;
    }

    /**
     * enabled sharing
     *
     * @return zimbraFeatureSharingEnabled, or false if unset
     */
    @ZAttr(id=335)
    public boolean isFeatureSharingEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureSharingEnabled, false);
    }

    /**
     * enabled sharing
     *
     * @param zimbraFeatureSharingEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=335)
    public Map<String,Object> setFeatureSharingEnabled(boolean zimbraFeatureSharingEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureSharingEnabled, Boolean.toString(zimbraFeatureSharingEnabled));
        return attrs;
    }

    /**
     * enabled sharing
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=335)
    public Map<String,Object> unsetFeatureSharingEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureSharingEnabled, "");
        return attrs;
    }

    /**
     * keyboard shortcuts aliases features
     *
     * @return zimbraFeatureShortcutAliasesEnabled, or false if unset
     */
    @ZAttr(id=452)
    public boolean isFeatureShortcutAliasesEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureShortcutAliasesEnabled, false);
    }

    /**
     * keyboard shortcuts aliases features
     *
     * @param zimbraFeatureShortcutAliasesEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=452)
    public Map<String,Object> setFeatureShortcutAliasesEnabled(boolean zimbraFeatureShortcutAliasesEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureShortcutAliasesEnabled, Boolean.toString(zimbraFeatureShortcutAliasesEnabled));
        return attrs;
    }

    /**
     * keyboard shortcuts aliases features
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=452)
    public Map<String,Object> unsetFeatureShortcutAliasesEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureShortcutAliasesEnabled, "");
        return attrs;
    }

    /**
     * whether to allow use of signature feature
     *
     * @return zimbraFeatureSignaturesEnabled, or false if unset
     */
    @ZAttr(id=494)
    public boolean isFeatureSignaturesEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureSignaturesEnabled, false);
    }

    /**
     * whether to allow use of signature feature
     *
     * @param zimbraFeatureSignaturesEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=494)
    public Map<String,Object> setFeatureSignaturesEnabled(boolean zimbraFeatureSignaturesEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureSignaturesEnabled, Boolean.toString(zimbraFeatureSignaturesEnabled));
        return attrs;
    }

    /**
     * whether to allow use of signature feature
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=494)
    public Map<String,Object> unsetFeatureSignaturesEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureSignaturesEnabled, "");
        return attrs;
    }

    /**
     * Whether changing skin is allowed for this account or in this cos
     *
     * @return zimbraFeatureSkinChangeEnabled, or false if unset
     */
    @ZAttr(id=354)
    public boolean isFeatureSkinChangeEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureSkinChangeEnabled, false);
    }

    /**
     * Whether changing skin is allowed for this account or in this cos
     *
     * @param zimbraFeatureSkinChangeEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=354)
    public Map<String,Object> setFeatureSkinChangeEnabled(boolean zimbraFeatureSkinChangeEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureSkinChangeEnabled, Boolean.toString(zimbraFeatureSkinChangeEnabled));
        return attrs;
    }

    /**
     * Whether changing skin is allowed for this account or in this cos
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=354)
    public Map<String,Object> unsetFeatureSkinChangeEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureSkinChangeEnabled, "");
        return attrs;
    }

    /**
     * tagging feature
     *
     * @return zimbraFeatureTaggingEnabled, or false if unset
     */
    @ZAttr(id=137)
    public boolean isFeatureTaggingEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureTaggingEnabled, false);
    }

    /**
     * tagging feature
     *
     * @param zimbraFeatureTaggingEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=137)
    public Map<String,Object> setFeatureTaggingEnabled(boolean zimbraFeatureTaggingEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureTaggingEnabled, Boolean.toString(zimbraFeatureTaggingEnabled));
        return attrs;
    }

    /**
     * tagging feature
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=137)
    public Map<String,Object> unsetFeatureTaggingEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureTaggingEnabled, "");
        return attrs;
    }

    /**
     * whether to allow use of tasks feature
     *
     * @return zimbraFeatureTasksEnabled, or false if unset
     */
    @ZAttr(id=436)
    public boolean isFeatureTasksEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureTasksEnabled, false);
    }

    /**
     * whether to allow use of tasks feature
     *
     * @param zimbraFeatureTasksEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=436)
    public Map<String,Object> setFeatureTasksEnabled(boolean zimbraFeatureTasksEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureTasksEnabled, Boolean.toString(zimbraFeatureTasksEnabled));
        return attrs;
    }

    /**
     * whether to allow use of tasks feature
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=436)
    public Map<String,Object> unsetFeatureTasksEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureTasksEnabled, "");
        return attrs;
    }

    /**
     * option to view attachments in html
     *
     * @return zimbraFeatureViewInHtmlEnabled, or false if unset
     */
    @ZAttr(id=312)
    public boolean isFeatureViewInHtmlEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureViewInHtmlEnabled, false);
    }

    /**
     * option to view attachments in html
     *
     * @param zimbraFeatureViewInHtmlEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=312)
    public Map<String,Object> setFeatureViewInHtmlEnabled(boolean zimbraFeatureViewInHtmlEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureViewInHtmlEnabled, Boolean.toString(zimbraFeatureViewInHtmlEnabled));
        return attrs;
    }

    /**
     * option to view attachments in html
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=312)
    public Map<String,Object> unsetFeatureViewInHtmlEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureViewInHtmlEnabled, "");
        return attrs;
    }

    /**
     * Voicemail features enabled
     *
     * @return zimbraFeatureVoiceEnabled, or false if unset
     */
    @ZAttr(id=445)
    public boolean isFeatureVoiceEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureVoiceEnabled, false);
    }

    /**
     * Voicemail features enabled
     *
     * @param zimbraFeatureVoiceEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=445)
    public Map<String,Object> setFeatureVoiceEnabled(boolean zimbraFeatureVoiceEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureVoiceEnabled, Boolean.toString(zimbraFeatureVoiceEnabled));
        return attrs;
    }

    /**
     * Voicemail features enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=445)
    public Map<String,Object> unsetFeatureVoiceEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureVoiceEnabled, "");
        return attrs;
    }

    /**
     * voice upsell enabled
     *
     * @return zimbraFeatureVoiceUpsellEnabled, or false if unset
     */
    @ZAttr(id=533)
    public boolean isFeatureVoiceUpsellEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureVoiceUpsellEnabled, false);
    }

    /**
     * voice upsell enabled
     *
     * @param zimbraFeatureVoiceUpsellEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=533)
    public Map<String,Object> setFeatureVoiceUpsellEnabled(boolean zimbraFeatureVoiceUpsellEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureVoiceUpsellEnabled, Boolean.toString(zimbraFeatureVoiceUpsellEnabled));
        return attrs;
    }

    /**
     * voice upsell enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=533)
    public Map<String,Object> unsetFeatureVoiceUpsellEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureVoiceUpsellEnabled, "");
        return attrs;
    }

    /**
     * voice upsell URL
     *
     * @return zimbraFeatureVoiceUpsellURL, or null unset
     */
    @ZAttr(id=534)
    public String getFeatureVoiceUpsellURL() {
        return getAttr(Provisioning.A_zimbraFeatureVoiceUpsellURL);
    }

    /**
     * voice upsell URL
     *
     * @param zimbraFeatureVoiceUpsellURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=534)
    public Map<String,Object> setFeatureVoiceUpsellURL(String zimbraFeatureVoiceUpsellURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureVoiceUpsellURL, zimbraFeatureVoiceUpsellURL);
        return attrs;
    }

    /**
     * voice upsell URL
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=534)
    public Map<String,Object> unsetFeatureVoiceUpsellURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureVoiceUpsellURL, "");
        return attrs;
    }

    /**
     * whether web search feature is enabled
     *
     * @return zimbraFeatureWebSearchEnabled, or false if unset
     */
    @ZAttr(id=602)
    public boolean isFeatureWebSearchEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureWebSearchEnabled, false);
    }

    /**
     * whether web search feature is enabled
     *
     * @param zimbraFeatureWebSearchEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=602)
    public Map<String,Object> setFeatureWebSearchEnabled(boolean zimbraFeatureWebSearchEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureWebSearchEnabled, Boolean.toString(zimbraFeatureWebSearchEnabled));
        return attrs;
    }

    /**
     * whether web search feature is enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=602)
    public Map<String,Object> unsetFeatureWebSearchEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureWebSearchEnabled, "");
        return attrs;
    }

    /**
     * Zimbra Assistant enabled
     *
     * @return zimbraFeatureZimbraAssistantEnabled, or false if unset
     */
    @ZAttr(id=544)
    public boolean isFeatureZimbraAssistantEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraFeatureZimbraAssistantEnabled, false);
    }

    /**
     * Zimbra Assistant enabled
     *
     * @param zimbraFeatureZimbraAssistantEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=544)
    public Map<String,Object> setFeatureZimbraAssistantEnabled(boolean zimbraFeatureZimbraAssistantEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureZimbraAssistantEnabled, Boolean.toString(zimbraFeatureZimbraAssistantEnabled));
        return attrs;
    }

    /**
     * Zimbra Assistant enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=544)
    public Map<String,Object> unsetFeatureZimbraAssistantEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFeatureZimbraAssistantEnabled, "");
        return attrs;
    }

    /**
     * mapping to foreign principal identifier
     *
     * @return zimbraForeignPrincipal, or ampty array if unset
     */
    @ZAttr(id=295)
    public String[] getForeignPrincipal() {
        return getMultiAttr(Provisioning.A_zimbraForeignPrincipal);
    }

    /**
     * mapping to foreign principal identifier
     *
     * @param zimbraForeignPrincipal new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=295)
    public Map<String,Object> setForeignPrincipal(String[] zimbraForeignPrincipal, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraForeignPrincipal, zimbraForeignPrincipal);
        return attrs;
    }

    /**
     * mapping to foreign principal identifier
     *
     * @param zimbraForeignPrincipal new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=295)
    public Map<String,Object> addForeignPrincipal(String zimbraForeignPrincipal, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraForeignPrincipal, zimbraForeignPrincipal);
        return attrs;
    }

    /**
     * mapping to foreign principal identifier
     *
     * @param zimbraForeignPrincipal existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=295)
    public Map<String,Object> removeForeignPrincipal(String zimbraForeignPrincipal, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraForeignPrincipal, zimbraForeignPrincipal);
        return attrs;
    }

    /**
     * mapping to foreign principal identifier
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=295)
    public Map<String,Object> unsetForeignPrincipal(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraForeignPrincipal, "");
        return attrs;
    }

    /**
     * Exchange user password for free/busy lookup and propagation
     *
     * @return zimbraFreebusyExchangeAuthPassword, or null unset
     */
    @ZAttr(id=609)
    public String getFreebusyExchangeAuthPassword() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeAuthPassword);
    }

    /**
     * Exchange user password for free/busy lookup and propagation
     *
     * @param zimbraFreebusyExchangeAuthPassword new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=609)
    public Map<String,Object> setFreebusyExchangeAuthPassword(String zimbraFreebusyExchangeAuthPassword, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeAuthPassword, zimbraFreebusyExchangeAuthPassword);
        return attrs;
    }

    /**
     * Exchange user password for free/busy lookup and propagation
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=609)
    public Map<String,Object> unsetFreebusyExchangeAuthPassword(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeAuthPassword, "");
        return attrs;
    }

    /**
     * auth scheme to use
     *
     * <p>Valid values: [basic, form]
     *
     * @return zimbraFreebusyExchangeAuthScheme, or null if unset and/or has invalid value
     */
    @ZAttr(id=611)
    public ZAttrProvisioning.FreebusyExchangeAuthScheme getFreebusyExchangeAuthScheme() {
        try { String v = getAttr(Provisioning.A_zimbraFreebusyExchangeAuthScheme); return v == null ? null : ZAttrProvisioning.FreebusyExchangeAuthScheme.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * auth scheme to use
     *
     * <p>Valid values: [basic, form]
     *
     * @return zimbraFreebusyExchangeAuthScheme, or null unset
     */
    @ZAttr(id=611)
    public String getFreebusyExchangeAuthSchemeAsString() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeAuthScheme);
    }

    /**
     * auth scheme to use
     *
     * <p>Valid values: [basic, form]
     *
     * @param zimbraFreebusyExchangeAuthScheme new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=611)
    public Map<String,Object> setFreebusyExchangeAuthScheme(ZAttrProvisioning.FreebusyExchangeAuthScheme zimbraFreebusyExchangeAuthScheme, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeAuthScheme, zimbraFreebusyExchangeAuthScheme.toString());
        return attrs;
    }

    /**
     * auth scheme to use
     *
     * <p>Valid values: [basic, form]
     *
     * @param zimbraFreebusyExchangeAuthScheme new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=611)
    public Map<String,Object> setFreebusyExchangeAuthSchemeAsString(String zimbraFreebusyExchangeAuthScheme, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeAuthScheme, zimbraFreebusyExchangeAuthScheme);
        return attrs;
    }

    /**
     * auth scheme to use
     *
     * <p>Valid values: [basic, form]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=611)
    public Map<String,Object> unsetFreebusyExchangeAuthScheme(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeAuthScheme, "");
        return attrs;
    }

    /**
     * Exchange username for free/busy lookup and propagation
     *
     * @return zimbraFreebusyExchangeAuthUsername, or null unset
     */
    @ZAttr(id=608)
    public String getFreebusyExchangeAuthUsername() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeAuthUsername);
    }

    /**
     * Exchange username for free/busy lookup and propagation
     *
     * @param zimbraFreebusyExchangeAuthUsername new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=608)
    public Map<String,Object> setFreebusyExchangeAuthUsername(String zimbraFreebusyExchangeAuthUsername, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeAuthUsername, zimbraFreebusyExchangeAuthUsername);
        return attrs;
    }

    /**
     * Exchange username for free/busy lookup and propagation
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=608)
    public Map<String,Object> unsetFreebusyExchangeAuthUsername(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeAuthUsername, "");
        return attrs;
    }

    /**
     * The duration of f/b block pushed to Exchange server.
     *
     * <p>Use getFreebusyExchangeCachedIntervalAsString to access value as a string.
     *
     * @see #getFreebusyExchangeCachedIntervalAsString()
     *
     * @return zimbraFreebusyExchangeCachedInterval in millseconds, or -1 if unset
     */
    @ZAttr(id=621)
    public long getFreebusyExchangeCachedInterval() {
        return getTimeInterval(Provisioning.A_zimbraFreebusyExchangeCachedInterval, -1);
    }

    /**
     * The duration of f/b block pushed to Exchange server.
     *
     * @return zimbraFreebusyExchangeCachedInterval, or null unset
     */
    @ZAttr(id=621)
    public String getFreebusyExchangeCachedIntervalAsString() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeCachedInterval);
    }

    /**
     * The duration of f/b block pushed to Exchange server.
     *
     * @param zimbraFreebusyExchangeCachedInterval new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=621)
    public Map<String,Object> setFreebusyExchangeCachedInterval(String zimbraFreebusyExchangeCachedInterval, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeCachedInterval, zimbraFreebusyExchangeCachedInterval);
        return attrs;
    }

    /**
     * The duration of f/b block pushed to Exchange server.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=621)
    public Map<String,Object> unsetFreebusyExchangeCachedInterval(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeCachedInterval, "");
        return attrs;
    }

    /**
     * The value of duration is used to indicate the start date (in the past
     * relative to today) of the f/b interval pushed to Exchange server.
     *
     * <p>Use getFreebusyExchangeCachedIntervalStartAsString to access value as a string.
     *
     * @see #getFreebusyExchangeCachedIntervalStartAsString()
     *
     * @return zimbraFreebusyExchangeCachedIntervalStart in millseconds, or -1 if unset
     */
    @ZAttr(id=620)
    public long getFreebusyExchangeCachedIntervalStart() {
        return getTimeInterval(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart, -1);
    }

    /**
     * The value of duration is used to indicate the start date (in the past
     * relative to today) of the f/b interval pushed to Exchange server.
     *
     * @return zimbraFreebusyExchangeCachedIntervalStart, or null unset
     */
    @ZAttr(id=620)
    public String getFreebusyExchangeCachedIntervalStartAsString() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart);
    }

    /**
     * The value of duration is used to indicate the start date (in the past
     * relative to today) of the f/b interval pushed to Exchange server.
     *
     * @param zimbraFreebusyExchangeCachedIntervalStart new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=620)
    public Map<String,Object> setFreebusyExchangeCachedIntervalStart(String zimbraFreebusyExchangeCachedIntervalStart, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart, zimbraFreebusyExchangeCachedIntervalStart);
        return attrs;
    }

    /**
     * The value of duration is used to indicate the start date (in the past
     * relative to today) of the f/b interval pushed to Exchange server.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=620)
    public Map<String,Object> unsetFreebusyExchangeCachedIntervalStart(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeCachedIntervalStart, "");
        return attrs;
    }

    /**
     * URL to Exchange server for free/busy lookup and propagation
     *
     * @return zimbraFreebusyExchangeURL, or null unset
     */
    @ZAttr(id=607)
    public String getFreebusyExchangeURL() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeURL);
    }

    /**
     * URL to Exchange server for free/busy lookup and propagation
     *
     * @param zimbraFreebusyExchangeURL new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=607)
    public Map<String,Object> setFreebusyExchangeURL(String zimbraFreebusyExchangeURL, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeURL, zimbraFreebusyExchangeURL);
        return attrs;
    }

    /**
     * URL to Exchange server for free/busy lookup and propagation
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=607)
    public Map<String,Object> unsetFreebusyExchangeURL(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeURL, "");
        return attrs;
    }

    /**
     * O and OU used in legacyExchangeDN attribute
     *
     * @return zimbraFreebusyExchangeUserOrg, or null unset
     */
    @ZAttr(id=610)
    public String getFreebusyExchangeUserOrg() {
        return getAttr(Provisioning.A_zimbraFreebusyExchangeUserOrg);
    }

    /**
     * O and OU used in legacyExchangeDN attribute
     *
     * @param zimbraFreebusyExchangeUserOrg new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=610)
    public Map<String,Object> setFreebusyExchangeUserOrg(String zimbraFreebusyExchangeUserOrg, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeUserOrg, zimbraFreebusyExchangeUserOrg);
        return attrs;
    }

    /**
     * O and OU used in legacyExchangeDN attribute
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=610)
    public Map<String,Object> unsetFreebusyExchangeUserOrg(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyExchangeUserOrg, "");
        return attrs;
    }

    /**
     * when set to TRUE, free/busy for the account is not calculated from
     * local mailbox.
     *
     * @return zimbraFreebusyLocalMailboxNotActive, or false if unset
     *
     * @since ZCS 5.0.11
     */
    @ZAttr(id=752)
    public boolean isFreebusyLocalMailboxNotActive() {
        return getBooleanAttr(Provisioning.A_zimbraFreebusyLocalMailboxNotActive, false);
    }

    /**
     * when set to TRUE, free/busy for the account is not calculated from
     * local mailbox.
     *
     * @param zimbraFreebusyLocalMailboxNotActive new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.11
     */
    @ZAttr(id=752)
    public Map<String,Object> setFreebusyLocalMailboxNotActive(boolean zimbraFreebusyLocalMailboxNotActive, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyLocalMailboxNotActive, Boolean.toString(zimbraFreebusyLocalMailboxNotActive));
        return attrs;
    }

    /**
     * when set to TRUE, free/busy for the account is not calculated from
     * local mailbox.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS 5.0.11
     */
    @ZAttr(id=752)
    public Map<String,Object> unsetFreebusyLocalMailboxNotActive(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraFreebusyLocalMailboxNotActive, "");
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=353)
    public Map<String,Object> setHideInGal(boolean zimbraHideInGal, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraHideInGal, Boolean.toString(zimbraHideInGal));
        return attrs;
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
     * available IM interop gateways
     *
     * @return zimbraIMAvailableInteropGateways, or ampty array if unset
     */
    @ZAttr(id=571)
    public String[] getIMAvailableInteropGateways() {
        return getMultiAttr(Provisioning.A_zimbraIMAvailableInteropGateways);
    }

    /**
     * available IM interop gateways
     *
     * @param zimbraIMAvailableInteropGateways new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=571)
    public Map<String,Object> setIMAvailableInteropGateways(String[] zimbraIMAvailableInteropGateways, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIMAvailableInteropGateways, zimbraIMAvailableInteropGateways);
        return attrs;
    }

    /**
     * available IM interop gateways
     *
     * @param zimbraIMAvailableInteropGateways new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=571)
    public Map<String,Object> addIMAvailableInteropGateways(String zimbraIMAvailableInteropGateways, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraIMAvailableInteropGateways, zimbraIMAvailableInteropGateways);
        return attrs;
    }

    /**
     * available IM interop gateways
     *
     * @param zimbraIMAvailableInteropGateways existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=571)
    public Map<String,Object> removeIMAvailableInteropGateways(String zimbraIMAvailableInteropGateways, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraIMAvailableInteropGateways, zimbraIMAvailableInteropGateways);
        return attrs;
    }

    /**
     * available IM interop gateways
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=571)
    public Map<String,Object> unsetIMAvailableInteropGateways(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIMAvailableInteropGateways, "");
        return attrs;
    }

    /**
     * IM service
     *
     * <p>Valid values: [yahoo, zimbra]
     *
     * @return zimbraIMService, or null if unset and/or has invalid value
     *
     * @since ZCS future
     */
    @ZAttr(id=762)
    public ZAttrProvisioning.IMService getIMService() {
        try { String v = getAttr(Provisioning.A_zimbraIMService); return v == null ? null : ZAttrProvisioning.IMService.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * IM service
     *
     * <p>Valid values: [yahoo, zimbra]
     *
     * @return zimbraIMService, or null unset
     *
     * @since ZCS future
     */
    @ZAttr(id=762)
    public String getIMServiceAsString() {
        return getAttr(Provisioning.A_zimbraIMService);
    }

    /**
     * IM service
     *
     * <p>Valid values: [yahoo, zimbra]
     *
     * @param zimbraIMService new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=762)
    public Map<String,Object> setIMService(ZAttrProvisioning.IMService zimbraIMService, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIMService, zimbraIMService.toString());
        return attrs;
    }

    /**
     * IM service
     *
     * <p>Valid values: [yahoo, zimbra]
     *
     * @param zimbraIMService new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=762)
    public Map<String,Object> setIMServiceAsString(String zimbraIMService, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIMService, zimbraIMService);
        return attrs;
    }

    /**
     * IM service
     *
     * <p>Valid values: [yahoo, zimbra]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=762)
    public Map<String,Object> unsetIMService(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIMService, "");
        return attrs;
    }

    /**
     * Zimbra Systems Unique ID
     *
     * @return zimbraId, or null unset
     */
    @ZAttr(id=1)
    public String getId() {
        return getAttr(Provisioning.A_zimbraId);
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
     * maximum number of identities allowed on an account
     *
     * @return zimbraIdentityMaxNumEntries, or -1 if unset
     */
    @ZAttr(id=414)
    public int getIdentityMaxNumEntries() {
        return getIntAttr(Provisioning.A_zimbraIdentityMaxNumEntries, -1);
    }

    /**
     * maximum number of identities allowed on an account
     *
     * @param zimbraIdentityMaxNumEntries new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=414)
    public Map<String,Object> setIdentityMaxNumEntries(int zimbraIdentityMaxNumEntries, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIdentityMaxNumEntries, Integer.toString(zimbraIdentityMaxNumEntries));
        return attrs;
    }

    /**
     * maximum number of identities allowed on an account
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=414)
    public Map<String,Object> unsetIdentityMaxNumEntries(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIdentityMaxNumEntries, "");
        return attrs;
    }

    /**
     * whether IMAP is enabled for an account
     *
     * @return zimbraImapEnabled, or false if unset
     */
    @ZAttr(id=174)
    public boolean isImapEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraImapEnabled, false);
    }

    /**
     * whether IMAP is enabled for an account
     *
     * @param zimbraImapEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=174)
    public Map<String,Object> setImapEnabled(boolean zimbraImapEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapEnabled, Boolean.toString(zimbraImapEnabled));
        return attrs;
    }

    /**
     * whether IMAP is enabled for an account
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=174)
    public Map<String,Object> unsetImapEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraImapEnabled, "");
        return attrs;
    }

    /**
     * The address to which legal intercept messages will be sent.
     *
     * @return zimbraInterceptAddress, or ampty array if unset
     */
    @ZAttr(id=614)
    public String[] getInterceptAddress() {
        return getMultiAttr(Provisioning.A_zimbraInterceptAddress);
    }

    /**
     * The address to which legal intercept messages will be sent.
     *
     * @param zimbraInterceptAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=614)
    public Map<String,Object> setInterceptAddress(String[] zimbraInterceptAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraInterceptAddress, zimbraInterceptAddress);
        return attrs;
    }

    /**
     * The address to which legal intercept messages will be sent.
     *
     * @param zimbraInterceptAddress new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=614)
    public Map<String,Object> addInterceptAddress(String zimbraInterceptAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraInterceptAddress, zimbraInterceptAddress);
        return attrs;
    }

    /**
     * The address to which legal intercept messages will be sent.
     *
     * @param zimbraInterceptAddress existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=614)
    public Map<String,Object> removeInterceptAddress(String zimbraInterceptAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraInterceptAddress, zimbraInterceptAddress);
        return attrs;
    }

    /**
     * The address to which legal intercept messages will be sent.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=614)
    public Map<String,Object> unsetInterceptAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraInterceptAddress, "");
        return attrs;
    }

    /**
     * Template used to construct the body of a legal intercept message.
     *
     * @return zimbraInterceptBody, or null unset
     */
    @ZAttr(id=618)
    public String getInterceptBody() {
        return getAttr(Provisioning.A_zimbraInterceptBody);
    }

    /**
     * Template used to construct the body of a legal intercept message.
     *
     * @param zimbraInterceptBody new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=618)
    public Map<String,Object> setInterceptBody(String zimbraInterceptBody, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraInterceptBody, zimbraInterceptBody);
        return attrs;
    }

    /**
     * Template used to construct the body of a legal intercept message.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=618)
    public Map<String,Object> unsetInterceptBody(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraInterceptBody, "");
        return attrs;
    }

    /**
     * Template used to construct the sender of a legal intercept message.
     *
     * @return zimbraInterceptFrom, or null unset
     */
    @ZAttr(id=616)
    public String getInterceptFrom() {
        return getAttr(Provisioning.A_zimbraInterceptFrom);
    }

    /**
     * Template used to construct the sender of a legal intercept message.
     *
     * @param zimbraInterceptFrom new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=616)
    public Map<String,Object> setInterceptFrom(String zimbraInterceptFrom, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraInterceptFrom, zimbraInterceptFrom);
        return attrs;
    }

    /**
     * Template used to construct the sender of a legal intercept message.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=616)
    public Map<String,Object> unsetInterceptFrom(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraInterceptFrom, "");
        return attrs;
    }

    /**
     * Specifies whether legal intercept messages should contain the entire
     * original message or just the headers.
     *
     * @return zimbraInterceptSendHeadersOnly, or false if unset
     */
    @ZAttr(id=615)
    public boolean isInterceptSendHeadersOnly() {
        return getBooleanAttr(Provisioning.A_zimbraInterceptSendHeadersOnly, false);
    }

    /**
     * Specifies whether legal intercept messages should contain the entire
     * original message or just the headers.
     *
     * @param zimbraInterceptSendHeadersOnly new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=615)
    public Map<String,Object> setInterceptSendHeadersOnly(boolean zimbraInterceptSendHeadersOnly, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraInterceptSendHeadersOnly, Boolean.toString(zimbraInterceptSendHeadersOnly));
        return attrs;
    }

    /**
     * Specifies whether legal intercept messages should contain the entire
     * original message or just the headers.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=615)
    public Map<String,Object> unsetInterceptSendHeadersOnly(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraInterceptSendHeadersOnly, "");
        return attrs;
    }

    /**
     * Template used to construct the subject of a legal intercept message.
     *
     * @return zimbraInterceptSubject, or null unset
     */
    @ZAttr(id=617)
    public String getInterceptSubject() {
        return getAttr(Provisioning.A_zimbraInterceptSubject);
    }

    /**
     * Template used to construct the subject of a legal intercept message.
     *
     * @param zimbraInterceptSubject new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=617)
    public Map<String,Object> setInterceptSubject(String zimbraInterceptSubject, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraInterceptSubject, zimbraInterceptSubject);
        return attrs;
    }

    /**
     * Template used to construct the subject of a legal intercept message.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=617)
    public Map<String,Object> unsetInterceptSubject(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraInterceptSubject, "");
        return attrs;
    }

    /**
     * set to true for admin accounts
     *
     * @return zimbraIsAdminAccount, or false if unset
     */
    @ZAttr(id=31)
    public boolean isIsAdminAccount() {
        return getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false);
    }

    /**
     * set to true for admin accounts
     *
     * @param zimbraIsAdminAccount new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=31)
    public Map<String,Object> setIsAdminAccount(boolean zimbraIsAdminAccount, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIsAdminAccount, Boolean.toString(zimbraIsAdminAccount));
        return attrs;
    }

    /**
     * set to true for admin accounts
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=31)
    public Map<String,Object> unsetIsAdminAccount(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIsAdminAccount, "");
        return attrs;
    }

    /**
     * set to true for customer care accounts
     *
     * @return zimbraIsCustomerCareAccount, or false if unset
     */
    @ZAttr(id=601)
    public boolean isIsCustomerCareAccount() {
        return getBooleanAttr(Provisioning.A_zimbraIsCustomerCareAccount, false);
    }

    /**
     * set to true for customer care accounts
     *
     * @param zimbraIsCustomerCareAccount new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=601)
    public Map<String,Object> setIsCustomerCareAccount(boolean zimbraIsCustomerCareAccount, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIsCustomerCareAccount, Boolean.toString(zimbraIsCustomerCareAccount));
        return attrs;
    }

    /**
     * set to true for customer care accounts
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=601)
    public Map<String,Object> unsetIsCustomerCareAccount(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIsCustomerCareAccount, "");
        return attrs;
    }

    /**
     * set to true for domain admin accounts
     *
     * @return zimbraIsDomainAdminAccount, or false if unset
     */
    @ZAttr(id=298)
    public boolean isIsDomainAdminAccount() {
        return getBooleanAttr(Provisioning.A_zimbraIsDomainAdminAccount, false);
    }

    /**
     * set to true for domain admin accounts
     *
     * @param zimbraIsDomainAdminAccount new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=298)
    public Map<String,Object> setIsDomainAdminAccount(boolean zimbraIsDomainAdminAccount, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIsDomainAdminAccount, Boolean.toString(zimbraIsDomainAdminAccount));
        return attrs;
    }

    /**
     * set to true for domain admin accounts
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=298)
    public Map<String,Object> unsetIsDomainAdminAccount(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "");
        return attrs;
    }

    /**
     * Indicates the account is a resource used by the system such as spam
     * accounts or Notebook accounts.
     *
     * @return zimbraIsSystemResource, or false if unset
     */
    @ZAttr(id=376)
    public boolean isIsSystemResource() {
        return getBooleanAttr(Provisioning.A_zimbraIsSystemResource, false);
    }

    /**
     * Indicates the account is a resource used by the system such as spam
     * accounts or Notebook accounts.
     *
     * @param zimbraIsSystemResource new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=376)
    public Map<String,Object> setIsSystemResource(boolean zimbraIsSystemResource, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIsSystemResource, Boolean.toString(zimbraIsSystemResource));
        return attrs;
    }

    /**
     * Indicates the account is a resource used by the system such as spam
     * accounts or Notebook accounts.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=376)
    public Map<String,Object> unsetIsSystemResource(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraIsSystemResource, "");
        return attrs;
    }

    /**
     * Whether to index junk messages
     *
     * @return zimbraJunkMessagesIndexingEnabled, or false if unset
     */
    @ZAttr(id=579)
    public boolean isJunkMessagesIndexingEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraJunkMessagesIndexingEnabled, false);
    }

    /**
     * Whether to index junk messages
     *
     * @param zimbraJunkMessagesIndexingEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=579)
    public Map<String,Object> setJunkMessagesIndexingEnabled(boolean zimbraJunkMessagesIndexingEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraJunkMessagesIndexingEnabled, Boolean.toString(zimbraJunkMessagesIndexingEnabled));
        return attrs;
    }

    /**
     * Whether to index junk messages
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=579)
    public Map<String,Object> unsetJunkMessagesIndexingEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraJunkMessagesIndexingEnabled, "");
        return attrs;
    }

    /**
     * rough estimate of when the user last logged in. see
     * zimbraLastLogonTimestampFrequency
     *
     * <p>Use getLastLogonTimestampAsString to access value as a string.
     *
     * @see #getLastLogonTimestampAsString()
     *
     * @return zimbraLastLogonTimestamp as Date, null if unset or unable to parse
     */
    @ZAttr(id=113)
    public Date getLastLogonTimestamp() {
        return getGeneralizedTimeAttr(Provisioning.A_zimbraLastLogonTimestamp, null);
    }

    /**
     * rough estimate of when the user last logged in. see
     * zimbraLastLogonTimestampFrequency
     *
     * @return zimbraLastLogonTimestamp, or null unset
     */
    @ZAttr(id=113)
    public String getLastLogonTimestampAsString() {
        return getAttr(Provisioning.A_zimbraLastLogonTimestamp);
    }

    /**
     * rough estimate of when the user last logged in. see
     * zimbraLastLogonTimestampFrequency
     *
     * @param zimbraLastLogonTimestamp new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=113)
    public Map<String,Object> setLastLogonTimestamp(Date zimbraLastLogonTimestamp, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLastLogonTimestamp, DateUtil.toGeneralizedTime(zimbraLastLogonTimestamp));
        return attrs;
    }

    /**
     * rough estimate of when the user last logged in. see
     * zimbraLastLogonTimestampFrequency
     *
     * @param zimbraLastLogonTimestamp new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=113)
    public Map<String,Object> setLastLogonTimestampAsString(String zimbraLastLogonTimestamp, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLastLogonTimestamp, zimbraLastLogonTimestamp);
        return attrs;
    }

    /**
     * rough estimate of when the user last logged in. see
     * zimbraLastLogonTimestampFrequency
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=113)
    public Map<String,Object> unsetLastLogonTimestamp(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraLastLogonTimestamp, "");
        return attrs;
    }

    /**
     * locale of entry, e.g. en_US
     *
     * @return zimbraLocale, or null unset
     */
    @ZAttr(id=345)
    public String getLocaleAsString() {
        return getAttr(Provisioning.A_zimbraLocale);
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
     * @return zimbraMailAddress, or ampty array if unset
     */
    @ZAttr(id=3)
    public String[] getMailAddress() {
        return getMultiAttr(Provisioning.A_zimbraMailAddress);
    }

    /**
     * RFC822 email address of this recipient for accepting mail
     *
     * @param zimbraMailAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=3)
    public Map<String,Object> setMailAddress(String[] zimbraMailAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailAddress, zimbraMailAddress);
        return attrs;
    }

    /**
     * RFC822 email address of this recipient for accepting mail
     *
     * @param zimbraMailAddress new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=3)
    public Map<String,Object> addMailAddress(String zimbraMailAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMailAddress, zimbraMailAddress);
        return attrs;
    }

    /**
     * RFC822 email address of this recipient for accepting mail
     *
     * @param zimbraMailAddress existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=3)
    public Map<String,Object> removeMailAddress(String zimbraMailAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMailAddress, zimbraMailAddress);
        return attrs;
    }

    /**
     * RFC822 email address of this recipient for accepting mail
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=3)
    public Map<String,Object> unsetMailAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailAddress, "");
        return attrs;
    }

    /**
     * RFC822 email address of this recipient for accepting mail
     *
     * @return zimbraMailAlias, or ampty array if unset
     */
    @ZAttr(id=20)
    public String[] getMailAlias() {
        return getMultiAttr(Provisioning.A_zimbraMailAlias);
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
     * RFC822 email address for senders outbound messages
     *
     * @return zimbraMailCanonicalAddress, or null unset
     */
    @ZAttr(id=213)
    public String getMailCanonicalAddress() {
        return getAttr(Provisioning.A_zimbraMailCanonicalAddress);
    }

    /**
     * RFC822 email address for senders outbound messages
     *
     * @param zimbraMailCanonicalAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=213)
    public Map<String,Object> setMailCanonicalAddress(String zimbraMailCanonicalAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailCanonicalAddress, zimbraMailCanonicalAddress);
        return attrs;
    }

    /**
     * RFC822 email address for senders outbound messages
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=213)
    public Map<String,Object> unsetMailCanonicalAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailCanonicalAddress, "");
        return attrs;
    }

    /**
     * Address to catch all messages to specified domain
     *
     * @return zimbraMailCatchAllAddress, or ampty array if unset
     */
    @ZAttr(id=214)
    public String[] getMailCatchAllAddress() {
        return getMultiAttr(Provisioning.A_zimbraMailCatchAllAddress);
    }

    /**
     * Address to catch all messages to specified domain
     *
     * @param zimbraMailCatchAllAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=214)
    public Map<String,Object> setMailCatchAllAddress(String[] zimbraMailCatchAllAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailCatchAllAddress, zimbraMailCatchAllAddress);
        return attrs;
    }

    /**
     * Address to catch all messages to specified domain
     *
     * @param zimbraMailCatchAllAddress new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=214)
    public Map<String,Object> addMailCatchAllAddress(String zimbraMailCatchAllAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMailCatchAllAddress, zimbraMailCatchAllAddress);
        return attrs;
    }

    /**
     * Address to catch all messages to specified domain
     *
     * @param zimbraMailCatchAllAddress existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=214)
    public Map<String,Object> removeMailCatchAllAddress(String zimbraMailCatchAllAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMailCatchAllAddress, zimbraMailCatchAllAddress);
        return attrs;
    }

    /**
     * Address to catch all messages to specified domain
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=214)
    public Map<String,Object> unsetMailCatchAllAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailCatchAllAddress, "");
        return attrs;
    }

    /**
     * Catch all address to rewrite to
     *
     * @return zimbraMailCatchAllCanonicalAddress, or null unset
     */
    @ZAttr(id=216)
    public String getMailCatchAllCanonicalAddress() {
        return getAttr(Provisioning.A_zimbraMailCatchAllCanonicalAddress);
    }

    /**
     * Catch all address to rewrite to
     *
     * @param zimbraMailCatchAllCanonicalAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=216)
    public Map<String,Object> setMailCatchAllCanonicalAddress(String zimbraMailCatchAllCanonicalAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailCatchAllCanonicalAddress, zimbraMailCatchAllCanonicalAddress);
        return attrs;
    }

    /**
     * Catch all address to rewrite to
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=216)
    public Map<String,Object> unsetMailCatchAllCanonicalAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailCatchAllCanonicalAddress, "");
        return attrs;
    }

    /**
     * Address to deliver catch all messages to
     *
     * @return zimbraMailCatchAllForwardingAddress, or null unset
     */
    @ZAttr(id=215)
    public String getMailCatchAllForwardingAddress() {
        return getAttr(Provisioning.A_zimbraMailCatchAllForwardingAddress);
    }

    /**
     * Address to deliver catch all messages to
     *
     * @param zimbraMailCatchAllForwardingAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=215)
    public Map<String,Object> setMailCatchAllForwardingAddress(String zimbraMailCatchAllForwardingAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailCatchAllForwardingAddress, zimbraMailCatchAllForwardingAddress);
        return attrs;
    }

    /**
     * Address to deliver catch all messages to
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=215)
    public Map<String,Object> unsetMailCatchAllForwardingAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailCatchAllForwardingAddress, "");
        return attrs;
    }

    /**
     * RFC822 email address of this recipient for local delivery
     *
     * @return zimbraMailDeliveryAddress, or ampty array if unset
     */
    @ZAttr(id=13)
    public String[] getMailDeliveryAddress() {
        return getMultiAttr(Provisioning.A_zimbraMailDeliveryAddress);
    }

    /**
     * RFC822 email address of this recipient for local delivery
     *
     * @param zimbraMailDeliveryAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=13)
    public Map<String,Object> setMailDeliveryAddress(String[] zimbraMailDeliveryAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailDeliveryAddress, zimbraMailDeliveryAddress);
        return attrs;
    }

    /**
     * RFC822 email address of this recipient for local delivery
     *
     * @param zimbraMailDeliveryAddress new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=13)
    public Map<String,Object> addMailDeliveryAddress(String zimbraMailDeliveryAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMailDeliveryAddress, zimbraMailDeliveryAddress);
        return attrs;
    }

    /**
     * RFC822 email address of this recipient for local delivery
     *
     * @param zimbraMailDeliveryAddress existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=13)
    public Map<String,Object> removeMailDeliveryAddress(String zimbraMailDeliveryAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMailDeliveryAddress, zimbraMailDeliveryAddress);
        return attrs;
    }

    /**
     * RFC822 email address of this recipient for local delivery
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=13)
    public Map<String,Object> unsetMailDeliveryAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailDeliveryAddress, "");
        return attrs;
    }

    /**
     * RFC822 forwarding address for an account
     *
     * @return zimbraMailForwardingAddress, or ampty array if unset
     */
    @ZAttr(id=12)
    public String[] getMailForwardingAddress() {
        return getMultiAttr(Provisioning.A_zimbraMailForwardingAddress);
    }

    /**
     * RFC822 forwarding address for an account
     *
     * @param zimbraMailForwardingAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=12)
    public Map<String,Object> setMailForwardingAddress(String[] zimbraMailForwardingAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailForwardingAddress, zimbraMailForwardingAddress);
        return attrs;
    }

    /**
     * RFC822 forwarding address for an account
     *
     * @param zimbraMailForwardingAddress new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=12)
    public Map<String,Object> addMailForwardingAddress(String zimbraMailForwardingAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMailForwardingAddress, zimbraMailForwardingAddress);
        return attrs;
    }

    /**
     * RFC822 forwarding address for an account
     *
     * @param zimbraMailForwardingAddress existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=12)
    public Map<String,Object> removeMailForwardingAddress(String zimbraMailForwardingAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMailForwardingAddress, zimbraMailForwardingAddress);
        return attrs;
    }

    /**
     * RFC822 forwarding address for an account
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=12)
    public Map<String,Object> unsetMailForwardingAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailForwardingAddress, "");
        return attrs;
    }

    /**
     * the server hosting the accounts mailbox
     *
     * @return zimbraMailHost, or null unset
     */
    @ZAttr(id=4)
    public String getMailHost() {
        return getAttr(Provisioning.A_zimbraMailHost);
    }

    /**
     * the server hosting the accounts mailbox
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
     * the server hosting the accounts mailbox
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
     * idle timeout (nnnnn[hmsd])
     *
     * <p>Use getMailIdleSessionTimeoutAsString to access value as a string.
     *
     * @see #getMailIdleSessionTimeoutAsString()
     *
     * @return zimbraMailIdleSessionTimeout in millseconds, or -1 if unset
     */
    @ZAttr(id=147)
    public long getMailIdleSessionTimeout() {
        return getTimeInterval(Provisioning.A_zimbraMailIdleSessionTimeout, -1);
    }

    /**
     * idle timeout (nnnnn[hmsd])
     *
     * @return zimbraMailIdleSessionTimeout, or null unset
     */
    @ZAttr(id=147)
    public String getMailIdleSessionTimeoutAsString() {
        return getAttr(Provisioning.A_zimbraMailIdleSessionTimeout);
    }

    /**
     * idle timeout (nnnnn[hmsd])
     *
     * @param zimbraMailIdleSessionTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=147)
    public Map<String,Object> setMailIdleSessionTimeout(String zimbraMailIdleSessionTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailIdleSessionTimeout, zimbraMailIdleSessionTimeout);
        return attrs;
    }

    /**
     * idle timeout (nnnnn[hmsd])
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=147)
    public Map<String,Object> unsetMailIdleSessionTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailIdleSessionTimeout, "");
        return attrs;
    }

    /**
     * lifetime (nnnnn[hmsd]) of a mail message regardless of location
     *
     * <p>Use getMailMessageLifetimeAsString to access value as a string.
     *
     * @see #getMailMessageLifetimeAsString()
     *
     * @return zimbraMailMessageLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=106)
    public long getMailMessageLifetime() {
        return getTimeInterval(Provisioning.A_zimbraMailMessageLifetime, -1);
    }

    /**
     * lifetime (nnnnn[hmsd]) of a mail message regardless of location
     *
     * @return zimbraMailMessageLifetime, or null unset
     */
    @ZAttr(id=106)
    public String getMailMessageLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraMailMessageLifetime);
    }

    /**
     * lifetime (nnnnn[hmsd]) of a mail message regardless of location
     *
     * @param zimbraMailMessageLifetime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=106)
    public Map<String,Object> setMailMessageLifetime(String zimbraMailMessageLifetime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailMessageLifetime, zimbraMailMessageLifetime);
        return attrs;
    }

    /**
     * lifetime (nnnnn[hmsd]) of a mail message regardless of location
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=106)
    public Map<String,Object> unsetMailMessageLifetime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailMessageLifetime, "");
        return attrs;
    }

    /**
     * minimum allowed value for zimbraPrefMailPollingInterval (nnnnn[hmsd])
     *
     * <p>Use getMailMinPollingIntervalAsString to access value as a string.
     *
     * @see #getMailMinPollingIntervalAsString()
     *
     * @return zimbraMailMinPollingInterval in millseconds, or -1 if unset
     */
    @ZAttr(id=110)
    public long getMailMinPollingInterval() {
        return getTimeInterval(Provisioning.A_zimbraMailMinPollingInterval, -1);
    }

    /**
     * minimum allowed value for zimbraPrefMailPollingInterval (nnnnn[hmsd])
     *
     * @return zimbraMailMinPollingInterval, or null unset
     */
    @ZAttr(id=110)
    public String getMailMinPollingIntervalAsString() {
        return getAttr(Provisioning.A_zimbraMailMinPollingInterval);
    }

    /**
     * minimum allowed value for zimbraPrefMailPollingInterval (nnnnn[hmsd])
     *
     * @param zimbraMailMinPollingInterval new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=110)
    public Map<String,Object> setMailMinPollingInterval(String zimbraMailMinPollingInterval, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailMinPollingInterval, zimbraMailMinPollingInterval);
        return attrs;
    }

    /**
     * minimum allowed value for zimbraPrefMailPollingInterval (nnnnn[hmsd])
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=110)
    public Map<String,Object> unsetMailMinPollingInterval(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailMinPollingInterval, "");
        return attrs;
    }

    /**
     * If TRUE, a message is purged from trash based on the date that it was
     * moved to the Trash folder. If FALSE, a message is purged from Trash
     * based on the date that it was added to the mailbox.
     *
     * @return zimbraMailPurgeUseChangeDateForTrash, or false if unset
     *
     * @since ZCS future
     */
    @ZAttr(id=748)
    public boolean isMailPurgeUseChangeDateForTrash() {
        return getBooleanAttr(Provisioning.A_zimbraMailPurgeUseChangeDateForTrash, false);
    }

    /**
     * If TRUE, a message is purged from trash based on the date that it was
     * moved to the Trash folder. If FALSE, a message is purged from Trash
     * based on the date that it was added to the mailbox.
     *
     * @param zimbraMailPurgeUseChangeDateForTrash new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=748)
    public Map<String,Object> setMailPurgeUseChangeDateForTrash(boolean zimbraMailPurgeUseChangeDateForTrash, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailPurgeUseChangeDateForTrash, Boolean.toString(zimbraMailPurgeUseChangeDateForTrash));
        return attrs;
    }

    /**
     * If TRUE, a message is purged from trash based on the date that it was
     * moved to the Trash folder. If FALSE, a message is purged from Trash
     * based on the date that it was added to the mailbox.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=748)
    public Map<String,Object> unsetMailPurgeUseChangeDateForTrash(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailPurgeUseChangeDateForTrash, "");
        return attrs;
    }

    /**
     * mail quota in bytes
     *
     * @return zimbraMailQuota, or -1 if unset
     */
    @ZAttr(id=16)
    public long getMailQuota() {
        return getLongAttr(Provisioning.A_zimbraMailQuota, -1);
    }

    /**
     * mail quota in bytes
     *
     * @param zimbraMailQuota new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=16)
    public Map<String,Object> setMailQuota(long zimbraMailQuota, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailQuota, Long.toString(zimbraMailQuota));
        return attrs;
    }

    /**
     * mail quota in bytes
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=16)
    public Map<String,Object> unsetMailQuota(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailQuota, "");
        return attrs;
    }

    /**
     * sieve script generated from user filter rules
     *
     * @return zimbraMailSieveScript, or null unset
     */
    @ZAttr(id=32)
    public String getMailSieveScript() {
        return getAttr(Provisioning.A_zimbraMailSieveScript);
    }

    /**
     * sieve script generated from user filter rules
     *
     * @param zimbraMailSieveScript new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=32)
    public Map<String,Object> setMailSieveScript(String zimbraMailSieveScript, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailSieveScript, zimbraMailSieveScript);
        return attrs;
    }

    /**
     * sieve script generated from user filter rules
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=32)
    public Map<String,Object> unsetMailSieveScript(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailSieveScript, "");
        return attrs;
    }

    /**
     * maximum length of mail signature, 0 means unlimited. If not set,
     * default is 1024
     *
     * @return zimbraMailSignatureMaxLength, or -1 if unset
     */
    @ZAttr(id=454)
    public long getMailSignatureMaxLength() {
        return getLongAttr(Provisioning.A_zimbraMailSignatureMaxLength, -1);
    }

    /**
     * maximum length of mail signature, 0 means unlimited. If not set,
     * default is 1024
     *
     * @param zimbraMailSignatureMaxLength new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=454)
    public Map<String,Object> setMailSignatureMaxLength(long zimbraMailSignatureMaxLength, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailSignatureMaxLength, Long.toString(zimbraMailSignatureMaxLength));
        return attrs;
    }

    /**
     * maximum length of mail signature, 0 means unlimited. If not set,
     * default is 1024
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=454)
    public Map<String,Object> unsetMailSignatureMaxLength(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailSignatureMaxLength, "");
        return attrs;
    }

    /**
     * Retention period of messages in the Junk folder. 0 means that all
     * messages will be retained. This admin-modifiable attribute works in
     * conjunction with zimbraPrefJunkLifetime, which is user-modifiable. The
     * shorter duration is used.
     *
     * <p>Use getMailSpamLifetimeAsString to access value as a string.
     *
     * @see #getMailSpamLifetimeAsString()
     *
     * @return zimbraMailSpamLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=105)
    public long getMailSpamLifetime() {
        return getTimeInterval(Provisioning.A_zimbraMailSpamLifetime, -1);
    }

    /**
     * Retention period of messages in the Junk folder. 0 means that all
     * messages will be retained. This admin-modifiable attribute works in
     * conjunction with zimbraPrefJunkLifetime, which is user-modifiable. The
     * shorter duration is used.
     *
     * @return zimbraMailSpamLifetime, or null unset
     */
    @ZAttr(id=105)
    public String getMailSpamLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraMailSpamLifetime);
    }

    /**
     * Retention period of messages in the Junk folder. 0 means that all
     * messages will be retained. This admin-modifiable attribute works in
     * conjunction with zimbraPrefJunkLifetime, which is user-modifiable. The
     * shorter duration is used.
     *
     * @param zimbraMailSpamLifetime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=105)
    public Map<String,Object> setMailSpamLifetime(String zimbraMailSpamLifetime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailSpamLifetime, zimbraMailSpamLifetime);
        return attrs;
    }

    /**
     * Retention period of messages in the Junk folder. 0 means that all
     * messages will be retained. This admin-modifiable attribute works in
     * conjunction with zimbraPrefJunkLifetime, which is user-modifiable. The
     * shorter duration is used.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=105)
    public Map<String,Object> unsetMailSpamLifetime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailSpamLifetime, "");
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
     * @return zimbraMailStatus, or null unset
     */
    @ZAttr(id=15)
    public String getMailStatusAsString() {
        return getAttr(Provisioning.A_zimbraMailStatus);
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
     * where to deliver parameter for use in postfix transport_maps
     *
     * @return zimbraMailTransport, or null unset
     */
    @ZAttr(id=247)
    public String getMailTransport() {
        return getAttr(Provisioning.A_zimbraMailTransport);
    }

    /**
     * where to deliver parameter for use in postfix transport_maps
     *
     * @param zimbraMailTransport new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=247)
    public Map<String,Object> setMailTransport(String zimbraMailTransport, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailTransport, zimbraMailTransport);
        return attrs;
    }

    /**
     * where to deliver parameter for use in postfix transport_maps
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=247)
    public Map<String,Object> unsetMailTransport(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailTransport, "");
        return attrs;
    }

    /**
     * Retention period of messages in the Trash folder. 0 means that all
     * messages will be retained. This admin-modifiable attribute works in
     * conjunction with zimbraPrefTrashLifetime, which is user-modifiable.
     * The shorter duration is used.
     *
     * <p>Use getMailTrashLifetimeAsString to access value as a string.
     *
     * @see #getMailTrashLifetimeAsString()
     *
     * @return zimbraMailTrashLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=104)
    public long getMailTrashLifetime() {
        return getTimeInterval(Provisioning.A_zimbraMailTrashLifetime, -1);
    }

    /**
     * Retention period of messages in the Trash folder. 0 means that all
     * messages will be retained. This admin-modifiable attribute works in
     * conjunction with zimbraPrefTrashLifetime, which is user-modifiable.
     * The shorter duration is used.
     *
     * @return zimbraMailTrashLifetime, or null unset
     */
    @ZAttr(id=104)
    public String getMailTrashLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraMailTrashLifetime);
    }

    /**
     * Retention period of messages in the Trash folder. 0 means that all
     * messages will be retained. This admin-modifiable attribute works in
     * conjunction with zimbraPrefTrashLifetime, which is user-modifiable.
     * The shorter duration is used.
     *
     * @param zimbraMailTrashLifetime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=104)
    public Map<String,Object> setMailTrashLifetime(String zimbraMailTrashLifetime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailTrashLifetime, zimbraMailTrashLifetime);
        return attrs;
    }

    /**
     * Retention period of messages in the Trash folder. 0 means that all
     * messages will be retained. This admin-modifiable attribute works in
     * conjunction with zimbraPrefTrashLifetime, which is user-modifiable.
     * The shorter duration is used.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=104)
    public Map<String,Object> unsetMailTrashLifetime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailTrashLifetime, "");
        return attrs;
    }

    /**
     * serverId:mboxId of mailbox before being moved
     *
     * @return zimbraMailboxLocationBeforeMove, or null unset
     */
    @ZAttr(id=346)
    public String getMailboxLocationBeforeMove() {
        return getAttr(Provisioning.A_zimbraMailboxLocationBeforeMove);
    }

    /**
     * serverId:mboxId of mailbox before being moved
     *
     * @param zimbraMailboxLocationBeforeMove new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=346)
    public Map<String,Object> setMailboxLocationBeforeMove(String zimbraMailboxLocationBeforeMove, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxLocationBeforeMove, zimbraMailboxLocationBeforeMove);
        return attrs;
    }

    /**
     * serverId:mboxId of mailbox before being moved
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=346)
    public Map<String,Object> unsetMailboxLocationBeforeMove(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMailboxLocationBeforeMove, "");
        return attrs;
    }

    /**
     * Deprecated since: 3.2.0. greatly simplify dl/group model. Orig desc:
     * for group membership, included with person object
     *
     * @return zimbraMemberOf, or ampty array if unset
     */
    @ZAttr(id=11)
    public String[] getMemberOf() {
        return getMultiAttr(Provisioning.A_zimbraMemberOf);
    }

    /**
     * Deprecated since: 3.2.0. greatly simplify dl/group model. Orig desc:
     * for group membership, included with person object
     *
     * @param zimbraMemberOf new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=11)
    public Map<String,Object> setMemberOf(String[] zimbraMemberOf, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMemberOf, zimbraMemberOf);
        return attrs;
    }

    /**
     * Deprecated since: 3.2.0. greatly simplify dl/group model. Orig desc:
     * for group membership, included with person object
     *
     * @param zimbraMemberOf new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=11)
    public Map<String,Object> addMemberOf(String zimbraMemberOf, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraMemberOf, zimbraMemberOf);
        return attrs;
    }

    /**
     * Deprecated since: 3.2.0. greatly simplify dl/group model. Orig desc:
     * for group membership, included with person object
     *
     * @param zimbraMemberOf existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=11)
    public Map<String,Object> removeMemberOf(String zimbraMemberOf, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraMemberOf, zimbraMemberOf);
        return attrs;
    }

    /**
     * Deprecated since: 3.2.0. greatly simplify dl/group model. Orig desc:
     * for group membership, included with person object
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=11)
    public Map<String,Object> unsetMemberOf(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraMemberOf, "");
        return attrs;
    }

    /**
     * template used to construct the body of an email notification message
     *
     * @return zimbraNewMailNotificationBody, or null unset
     */
    @ZAttr(id=152)
    public String getNewMailNotificationBody() {
        return getAttr(Provisioning.A_zimbraNewMailNotificationBody);
    }

    /**
     * template used to construct the body of an email notification message
     *
     * @param zimbraNewMailNotificationBody new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=152)
    public Map<String,Object> setNewMailNotificationBody(String zimbraNewMailNotificationBody, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNewMailNotificationBody, zimbraNewMailNotificationBody);
        return attrs;
    }

    /**
     * template used to construct the body of an email notification message
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=152)
    public Map<String,Object> unsetNewMailNotificationBody(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNewMailNotificationBody, "");
        return attrs;
    }

    /**
     * template used to construct the sender of an email notification message
     *
     * @return zimbraNewMailNotificationFrom, or null unset
     */
    @ZAttr(id=150)
    public String getNewMailNotificationFrom() {
        return getAttr(Provisioning.A_zimbraNewMailNotificationFrom);
    }

    /**
     * template used to construct the sender of an email notification message
     *
     * @param zimbraNewMailNotificationFrom new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=150)
    public Map<String,Object> setNewMailNotificationFrom(String zimbraNewMailNotificationFrom, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNewMailNotificationFrom, zimbraNewMailNotificationFrom);
        return attrs;
    }

    /**
     * template used to construct the sender of an email notification message
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=150)
    public Map<String,Object> unsetNewMailNotificationFrom(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNewMailNotificationFrom, "");
        return attrs;
    }

    /**
     * template used to construct the subject of an email notification
     * message
     *
     * @return zimbraNewMailNotificationSubject, or null unset
     */
    @ZAttr(id=151)
    public String getNewMailNotificationSubject() {
        return getAttr(Provisioning.A_zimbraNewMailNotificationSubject);
    }

    /**
     * template used to construct the subject of an email notification
     * message
     *
     * @param zimbraNewMailNotificationSubject new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=151)
    public Map<String,Object> setNewMailNotificationSubject(String zimbraNewMailNotificationSubject, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNewMailNotificationSubject, zimbraNewMailNotificationSubject);
        return attrs;
    }

    /**
     * template used to construct the subject of an email notification
     * message
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=151)
    public Map<String,Object> unsetNewMailNotificationSubject(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNewMailNotificationSubject, "");
        return attrs;
    }

    /**
     * maximum number of revisions to keep for wiki pages and documents. 0
     * means unlimited.
     *
     * @return zimbraNotebookMaxRevisions, or -1 if unset
     */
    @ZAttr(id=482)
    public int getNotebookMaxRevisions() {
        return getIntAttr(Provisioning.A_zimbraNotebookMaxRevisions, -1);
    }

    /**
     * maximum number of revisions to keep for wiki pages and documents. 0
     * means unlimited.
     *
     * @param zimbraNotebookMaxRevisions new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=482)
    public Map<String,Object> setNotebookMaxRevisions(int zimbraNotebookMaxRevisions, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotebookMaxRevisions, Integer.toString(zimbraNotebookMaxRevisions));
        return attrs;
    }

    /**
     * maximum number of revisions to keep for wiki pages and documents. 0
     * means unlimited.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=482)
    public Map<String,Object> unsetNotebookMaxRevisions(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotebookMaxRevisions, "");
        return attrs;
    }

    /**
     * whether to strip off potentially harming HTML tags in Wiki and HTML
     * Documents.
     *
     * @return zimbraNotebookSanitizeHtml, or false if unset
     */
    @ZAttr(id=646)
    public boolean isNotebookSanitizeHtml() {
        return getBooleanAttr(Provisioning.A_zimbraNotebookSanitizeHtml, false);
    }

    /**
     * whether to strip off potentially harming HTML tags in Wiki and HTML
     * Documents.
     *
     * @param zimbraNotebookSanitizeHtml new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=646)
    public Map<String,Object> setNotebookSanitizeHtml(boolean zimbraNotebookSanitizeHtml, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotebookSanitizeHtml, Boolean.toString(zimbraNotebookSanitizeHtml));
        return attrs;
    }

    /**
     * whether to strip off potentially harming HTML tags in Wiki and HTML
     * Documents.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=646)
    public Map<String,Object> unsetNotebookSanitizeHtml(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotebookSanitizeHtml, "");
        return attrs;
    }

    /**
     * administrative notes
     *
     * @return zimbraNotes, or null unset
     */
    @ZAttr(id=9)
    public String getNotes() {
        return getAttr(Provisioning.A_zimbraNotes);
    }

    /**
     * administrative notes
     *
     * @param zimbraNotes new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=9)
    public Map<String,Object> setNotes(String zimbraNotes, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotes, zimbraNotes);
        return attrs;
    }

    /**
     * administrative notes
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=9)
    public Map<String,Object> unsetNotes(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraNotes, "");
        return attrs;
    }

    /**
     * whether or not to enforce password history. Number of unique passwords
     * a user must have before being allowed to re-use an old one. A value of
     * 0 means no password history.
     *
     * @return zimbraPasswordEnforceHistory, or -1 if unset
     */
    @ZAttr(id=37)
    public int getPasswordEnforceHistory() {
        return getIntAttr(Provisioning.A_zimbraPasswordEnforceHistory, -1);
    }

    /**
     * whether or not to enforce password history. Number of unique passwords
     * a user must have before being allowed to re-use an old one. A value of
     * 0 means no password history.
     *
     * @param zimbraPasswordEnforceHistory new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=37)
    public Map<String,Object> setPasswordEnforceHistory(int zimbraPasswordEnforceHistory, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordEnforceHistory, Integer.toString(zimbraPasswordEnforceHistory));
        return attrs;
    }

    /**
     * whether or not to enforce password history. Number of unique passwords
     * a user must have before being allowed to re-use an old one. A value of
     * 0 means no password history.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=37)
    public Map<String,Object> unsetPasswordEnforceHistory(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordEnforceHistory, "");
        return attrs;
    }

    /**
     * historical password values
     *
     * @return zimbraPasswordHistory, or ampty array if unset
     */
    @ZAttr(id=38)
    public String[] getPasswordHistory() {
        return getMultiAttr(Provisioning.A_zimbraPasswordHistory);
    }

    /**
     * historical password values
     *
     * @param zimbraPasswordHistory new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=38)
    public Map<String,Object> setPasswordHistory(String[] zimbraPasswordHistory, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordHistory, zimbraPasswordHistory);
        return attrs;
    }

    /**
     * historical password values
     *
     * @param zimbraPasswordHistory new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=38)
    public Map<String,Object> addPasswordHistory(String zimbraPasswordHistory, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraPasswordHistory, zimbraPasswordHistory);
        return attrs;
    }

    /**
     * historical password values
     *
     * @param zimbraPasswordHistory existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=38)
    public Map<String,Object> removePasswordHistory(String zimbraPasswordHistory, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraPasswordHistory, zimbraPasswordHistory);
        return attrs;
    }

    /**
     * historical password values
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=38)
    public Map<String,Object> unsetPasswordHistory(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordHistory, "");
        return attrs;
    }

    /**
     * user is unable to change password
     *
     * @return zimbraPasswordLocked, or false if unset
     */
    @ZAttr(id=45)
    public boolean isPasswordLocked() {
        return getBooleanAttr(Provisioning.A_zimbraPasswordLocked, false);
    }

    /**
     * user is unable to change password
     *
     * @param zimbraPasswordLocked new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=45)
    public Map<String,Object> setPasswordLocked(boolean zimbraPasswordLocked, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordLocked, Boolean.toString(zimbraPasswordLocked));
        return attrs;
    }

    /**
     * user is unable to change password
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=45)
    public Map<String,Object> unsetPasswordLocked(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordLocked, "");
        return attrs;
    }

    /**
     * how long an account is locked out. Use 0 to lockout an account until
     * admin resets it
     *
     * <p>Use getPasswordLockoutDurationAsString to access value as a string.
     *
     * @see #getPasswordLockoutDurationAsString()
     *
     * @return zimbraPasswordLockoutDuration in millseconds, or -1 if unset
     */
    @ZAttr(id=379)
    public long getPasswordLockoutDuration() {
        return getTimeInterval(Provisioning.A_zimbraPasswordLockoutDuration, -1);
    }

    /**
     * how long an account is locked out. Use 0 to lockout an account until
     * admin resets it
     *
     * @return zimbraPasswordLockoutDuration, or null unset
     */
    @ZAttr(id=379)
    public String getPasswordLockoutDurationAsString() {
        return getAttr(Provisioning.A_zimbraPasswordLockoutDuration);
    }

    /**
     * how long an account is locked out. Use 0 to lockout an account until
     * admin resets it
     *
     * @param zimbraPasswordLockoutDuration new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=379)
    public Map<String,Object> setPasswordLockoutDuration(String zimbraPasswordLockoutDuration, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordLockoutDuration, zimbraPasswordLockoutDuration);
        return attrs;
    }

    /**
     * how long an account is locked out. Use 0 to lockout an account until
     * admin resets it
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=379)
    public Map<String,Object> unsetPasswordLockoutDuration(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordLockoutDuration, "");
        return attrs;
    }

    /**
     * whether or not account lockout is enabled.
     *
     * @return zimbraPasswordLockoutEnabled, or false if unset
     */
    @ZAttr(id=378)
    public boolean isPasswordLockoutEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPasswordLockoutEnabled, false);
    }

    /**
     * whether or not account lockout is enabled.
     *
     * @param zimbraPasswordLockoutEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=378)
    public Map<String,Object> setPasswordLockoutEnabled(boolean zimbraPasswordLockoutEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordLockoutEnabled, Boolean.toString(zimbraPasswordLockoutEnabled));
        return attrs;
    }

    /**
     * whether or not account lockout is enabled.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=378)
    public Map<String,Object> unsetPasswordLockoutEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordLockoutEnabled, "");
        return attrs;
    }

    /**
     * the duration after which old consecutive failed login attempts are
     * purged from the list, even though no successful authentication has
     * occurred
     *
     * <p>Use getPasswordLockoutFailureLifetimeAsString to access value as a string.
     *
     * @see #getPasswordLockoutFailureLifetimeAsString()
     *
     * @return zimbraPasswordLockoutFailureLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=381)
    public long getPasswordLockoutFailureLifetime() {
        return getTimeInterval(Provisioning.A_zimbraPasswordLockoutFailureLifetime, -1);
    }

    /**
     * the duration after which old consecutive failed login attempts are
     * purged from the list, even though no successful authentication has
     * occurred
     *
     * @return zimbraPasswordLockoutFailureLifetime, or null unset
     */
    @ZAttr(id=381)
    public String getPasswordLockoutFailureLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraPasswordLockoutFailureLifetime);
    }

    /**
     * the duration after which old consecutive failed login attempts are
     * purged from the list, even though no successful authentication has
     * occurred
     *
     * @param zimbraPasswordLockoutFailureLifetime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=381)
    public Map<String,Object> setPasswordLockoutFailureLifetime(String zimbraPasswordLockoutFailureLifetime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordLockoutFailureLifetime, zimbraPasswordLockoutFailureLifetime);
        return attrs;
    }

    /**
     * the duration after which old consecutive failed login attempts are
     * purged from the list, even though no successful authentication has
     * occurred
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=381)
    public Map<String,Object> unsetPasswordLockoutFailureLifetime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordLockoutFailureLifetime, "");
        return attrs;
    }

    /**
     * this attribute contains the timestamps of each of the consecutive
     * authentication failures made on an account
     *
     * <p>Use getPasswordLockoutFailureTimeAsString to access value as a string.
     *
     * @see #getPasswordLockoutFailureTimeAsString()
     *
     * @return zimbraPasswordLockoutFailureTime as Date, null if unset or unable to parse
     */
    @ZAttr(id=383)
    public Date getPasswordLockoutFailureTime() {
        return getGeneralizedTimeAttr(Provisioning.A_zimbraPasswordLockoutFailureTime, null);
    }

    /**
     * this attribute contains the timestamps of each of the consecutive
     * authentication failures made on an account
     *
     * @return zimbraPasswordLockoutFailureTime, or ampty array if unset
     */
    @ZAttr(id=383)
    public String[] getPasswordLockoutFailureTimeAsString() {
        return getMultiAttr(Provisioning.A_zimbraPasswordLockoutFailureTime);
    }

    /**
     * this attribute contains the timestamps of each of the consecutive
     * authentication failures made on an account
     *
     * @param zimbraPasswordLockoutFailureTime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=383)
    public Map<String,Object> setPasswordLockoutFailureTime(Date zimbraPasswordLockoutFailureTime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordLockoutFailureTime, DateUtil.toGeneralizedTime(zimbraPasswordLockoutFailureTime));
        return attrs;
    }

    /**
     * this attribute contains the timestamps of each of the consecutive
     * authentication failures made on an account
     *
     * @param zimbraPasswordLockoutFailureTime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=383)
    public Map<String,Object> setPasswordLockoutFailureTimeAsString(String[] zimbraPasswordLockoutFailureTime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordLockoutFailureTime, zimbraPasswordLockoutFailureTime);
        return attrs;
    }

    /**
     * this attribute contains the timestamps of each of the consecutive
     * authentication failures made on an account
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=383)
    public Map<String,Object> unsetPasswordLockoutFailureTime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordLockoutFailureTime, "");
        return attrs;
    }

    /**
     * the time at which an account was locked
     *
     * <p>Use getPasswordLockoutLockedTimeAsString to access value as a string.
     *
     * @see #getPasswordLockoutLockedTimeAsString()
     *
     * @return zimbraPasswordLockoutLockedTime as Date, null if unset or unable to parse
     */
    @ZAttr(id=382)
    public Date getPasswordLockoutLockedTime() {
        return getGeneralizedTimeAttr(Provisioning.A_zimbraPasswordLockoutLockedTime, null);
    }

    /**
     * the time at which an account was locked
     *
     * @return zimbraPasswordLockoutLockedTime, or null unset
     */
    @ZAttr(id=382)
    public String getPasswordLockoutLockedTimeAsString() {
        return getAttr(Provisioning.A_zimbraPasswordLockoutLockedTime);
    }

    /**
     * the time at which an account was locked
     *
     * @param zimbraPasswordLockoutLockedTime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=382)
    public Map<String,Object> setPasswordLockoutLockedTime(Date zimbraPasswordLockoutLockedTime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordLockoutLockedTime, DateUtil.toGeneralizedTime(zimbraPasswordLockoutLockedTime));
        return attrs;
    }

    /**
     * the time at which an account was locked
     *
     * @param zimbraPasswordLockoutLockedTime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=382)
    public Map<String,Object> setPasswordLockoutLockedTimeAsString(String zimbraPasswordLockoutLockedTime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordLockoutLockedTime, zimbraPasswordLockoutLockedTime);
        return attrs;
    }

    /**
     * the time at which an account was locked
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=382)
    public Map<String,Object> unsetPasswordLockoutLockedTime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordLockoutLockedTime, "");
        return attrs;
    }

    /**
     * number of consecutive failed login attempts until an account is locked
     * out
     *
     * @return zimbraPasswordLockoutMaxFailures, or -1 if unset
     */
    @ZAttr(id=380)
    public int getPasswordLockoutMaxFailures() {
        return getIntAttr(Provisioning.A_zimbraPasswordLockoutMaxFailures, -1);
    }

    /**
     * number of consecutive failed login attempts until an account is locked
     * out
     *
     * @param zimbraPasswordLockoutMaxFailures new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=380)
    public Map<String,Object> setPasswordLockoutMaxFailures(int zimbraPasswordLockoutMaxFailures, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordLockoutMaxFailures, Integer.toString(zimbraPasswordLockoutMaxFailures));
        return attrs;
    }

    /**
     * number of consecutive failed login attempts until an account is locked
     * out
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=380)
    public Map<String,Object> unsetPasswordLockoutMaxFailures(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordLockoutMaxFailures, "");
        return attrs;
    }

    /**
     * maximum days between password changes
     *
     * @return zimbraPasswordMaxAge, or -1 if unset
     */
    @ZAttr(id=36)
    public int getPasswordMaxAge() {
        return getIntAttr(Provisioning.A_zimbraPasswordMaxAge, -1);
    }

    /**
     * maximum days between password changes
     *
     * @param zimbraPasswordMaxAge new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=36)
    public Map<String,Object> setPasswordMaxAge(int zimbraPasswordMaxAge, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordMaxAge, Integer.toString(zimbraPasswordMaxAge));
        return attrs;
    }

    /**
     * maximum days between password changes
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=36)
    public Map<String,Object> unsetPasswordMaxAge(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordMaxAge, "");
        return attrs;
    }

    /**
     * max length of a password
     *
     * @return zimbraPasswordMaxLength, or -1 if unset
     */
    @ZAttr(id=34)
    public int getPasswordMaxLength() {
        return getIntAttr(Provisioning.A_zimbraPasswordMaxLength, -1);
    }

    /**
     * max length of a password
     *
     * @param zimbraPasswordMaxLength new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=34)
    public Map<String,Object> setPasswordMaxLength(int zimbraPasswordMaxLength, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordMaxLength, Integer.toString(zimbraPasswordMaxLength));
        return attrs;
    }

    /**
     * max length of a password
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=34)
    public Map<String,Object> unsetPasswordMaxLength(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordMaxLength, "");
        return attrs;
    }

    /**
     * minimum days between password changes
     *
     * @return zimbraPasswordMinAge, or -1 if unset
     */
    @ZAttr(id=35)
    public int getPasswordMinAge() {
        return getIntAttr(Provisioning.A_zimbraPasswordMinAge, -1);
    }

    /**
     * minimum days between password changes
     *
     * @param zimbraPasswordMinAge new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=35)
    public Map<String,Object> setPasswordMinAge(int zimbraPasswordMinAge, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordMinAge, Integer.toString(zimbraPasswordMinAge));
        return attrs;
    }

    /**
     * minimum days between password changes
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=35)
    public Map<String,Object> unsetPasswordMinAge(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordMinAge, "");
        return attrs;
    }

    /**
     * minimum length of a password
     *
     * @return zimbraPasswordMinLength, or -1 if unset
     */
    @ZAttr(id=33)
    public int getPasswordMinLength() {
        return getIntAttr(Provisioning.A_zimbraPasswordMinLength, -1);
    }

    /**
     * minimum length of a password
     *
     * @param zimbraPasswordMinLength new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=33)
    public Map<String,Object> setPasswordMinLength(int zimbraPasswordMinLength, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordMinLength, Integer.toString(zimbraPasswordMinLength));
        return attrs;
    }

    /**
     * minimum length of a password
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=33)
    public Map<String,Object> unsetPasswordMinLength(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordMinLength, "");
        return attrs;
    }

    /**
     * minimum number of lower case characters required in a password
     *
     * @return zimbraPasswordMinLowerCaseChars, or -1 if unset
     */
    @ZAttr(id=390)
    public int getPasswordMinLowerCaseChars() {
        return getIntAttr(Provisioning.A_zimbraPasswordMinLowerCaseChars, -1);
    }

    /**
     * minimum number of lower case characters required in a password
     *
     * @param zimbraPasswordMinLowerCaseChars new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=390)
    public Map<String,Object> setPasswordMinLowerCaseChars(int zimbraPasswordMinLowerCaseChars, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordMinLowerCaseChars, Integer.toString(zimbraPasswordMinLowerCaseChars));
        return attrs;
    }

    /**
     * minimum number of lower case characters required in a password
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=390)
    public Map<String,Object> unsetPasswordMinLowerCaseChars(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordMinLowerCaseChars, "");
        return attrs;
    }

    /**
     * minimum number of numeric characters required in a password
     *
     * @return zimbraPasswordMinNumericChars, or -1 if unset
     */
    @ZAttr(id=392)
    public int getPasswordMinNumericChars() {
        return getIntAttr(Provisioning.A_zimbraPasswordMinNumericChars, -1);
    }

    /**
     * minimum number of numeric characters required in a password
     *
     * @param zimbraPasswordMinNumericChars new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=392)
    public Map<String,Object> setPasswordMinNumericChars(int zimbraPasswordMinNumericChars, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordMinNumericChars, Integer.toString(zimbraPasswordMinNumericChars));
        return attrs;
    }

    /**
     * minimum number of numeric characters required in a password
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=392)
    public Map<String,Object> unsetPasswordMinNumericChars(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordMinNumericChars, "");
        return attrs;
    }

    /**
     * minimum number of ascii punctuation characters required in a password
     *
     * @return zimbraPasswordMinPunctuationChars, or -1 if unset
     */
    @ZAttr(id=391)
    public int getPasswordMinPunctuationChars() {
        return getIntAttr(Provisioning.A_zimbraPasswordMinPunctuationChars, -1);
    }

    /**
     * minimum number of ascii punctuation characters required in a password
     *
     * @param zimbraPasswordMinPunctuationChars new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=391)
    public Map<String,Object> setPasswordMinPunctuationChars(int zimbraPasswordMinPunctuationChars, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordMinPunctuationChars, Integer.toString(zimbraPasswordMinPunctuationChars));
        return attrs;
    }

    /**
     * minimum number of ascii punctuation characters required in a password
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=391)
    public Map<String,Object> unsetPasswordMinPunctuationChars(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordMinPunctuationChars, "");
        return attrs;
    }

    /**
     * minimum number of upper case characters required in a password
     *
     * @return zimbraPasswordMinUpperCaseChars, or -1 if unset
     */
    @ZAttr(id=389)
    public int getPasswordMinUpperCaseChars() {
        return getIntAttr(Provisioning.A_zimbraPasswordMinUpperCaseChars, -1);
    }

    /**
     * minimum number of upper case characters required in a password
     *
     * @param zimbraPasswordMinUpperCaseChars new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=389)
    public Map<String,Object> setPasswordMinUpperCaseChars(int zimbraPasswordMinUpperCaseChars, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordMinUpperCaseChars, Integer.toString(zimbraPasswordMinUpperCaseChars));
        return attrs;
    }

    /**
     * minimum number of upper case characters required in a password
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=389)
    public Map<String,Object> unsetPasswordMinUpperCaseChars(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordMinUpperCaseChars, "");
        return attrs;
    }

    /**
     * time password was last changed
     *
     * <p>Use getPasswordModifiedTimeAsString to access value as a string.
     *
     * @see #getPasswordModifiedTimeAsString()
     *
     * @return zimbraPasswordModifiedTime as Date, null if unset or unable to parse
     */
    @ZAttr(id=39)
    public Date getPasswordModifiedTime() {
        return getGeneralizedTimeAttr(Provisioning.A_zimbraPasswordModifiedTime, null);
    }

    /**
     * time password was last changed
     *
     * @return zimbraPasswordModifiedTime, or null unset
     */
    @ZAttr(id=39)
    public String getPasswordModifiedTimeAsString() {
        return getAttr(Provisioning.A_zimbraPasswordModifiedTime);
    }

    /**
     * time password was last changed
     *
     * @param zimbraPasswordModifiedTime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=39)
    public Map<String,Object> setPasswordModifiedTime(Date zimbraPasswordModifiedTime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordModifiedTime, DateUtil.toGeneralizedTime(zimbraPasswordModifiedTime));
        return attrs;
    }

    /**
     * time password was last changed
     *
     * @param zimbraPasswordModifiedTime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=39)
    public Map<String,Object> setPasswordModifiedTimeAsString(String zimbraPasswordModifiedTime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordModifiedTime, zimbraPasswordModifiedTime);
        return attrs;
    }

    /**
     * time password was last changed
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=39)
    public Map<String,Object> unsetPasswordModifiedTime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordModifiedTime, "");
        return attrs;
    }

    /**
     * must change password on auth
     *
     * @return zimbraPasswordMustChange, or false if unset
     */
    @ZAttr(id=41)
    public boolean isPasswordMustChange() {
        return getBooleanAttr(Provisioning.A_zimbraPasswordMustChange, false);
    }

    /**
     * must change password on auth
     *
     * @param zimbraPasswordMustChange new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=41)
    public Map<String,Object> setPasswordMustChange(boolean zimbraPasswordMustChange, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordMustChange, Boolean.toString(zimbraPasswordMustChange));
        return attrs;
    }

    /**
     * must change password on auth
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=41)
    public Map<String,Object> unsetPasswordMustChange(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPasswordMustChange, "");
        return attrs;
    }

    /**
     * whether POP3 is enabled for an account
     *
     * @return zimbraPop3Enabled, or false if unset
     */
    @ZAttr(id=175)
    public boolean isPop3Enabled() {
        return getBooleanAttr(Provisioning.A_zimbraPop3Enabled, false);
    }

    /**
     * whether POP3 is enabled for an account
     *
     * @param zimbraPop3Enabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=175)
    public Map<String,Object> setPop3Enabled(boolean zimbraPop3Enabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3Enabled, Boolean.toString(zimbraPop3Enabled));
        return attrs;
    }

    /**
     * whether POP3 is enabled for an account
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=175)
    public Map<String,Object> unsetPop3Enabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPop3Enabled, "");
        return attrs;
    }

    /**
     * portal name
     *
     * @return zimbraPortalName, or null unset
     */
    @ZAttr(id=448)
    public String getPortalName() {
        return getAttr(Provisioning.A_zimbraPortalName);
    }

    /**
     * portal name
     *
     * @param zimbraPortalName new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=448)
    public Map<String,Object> setPortalName(String zimbraPortalName, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPortalName, zimbraPortalName);
        return attrs;
    }

    /**
     * portal name
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=448)
    public Map<String,Object> unsetPortalName(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPortalName, "");
        return attrs;
    }

    /**
     * After login, whether the advanced client should enforce minimum
     * display resolution
     *
     * @return zimbraPrefAdvancedClientEnforceMinDisplay, or false if unset
     */
    @ZAttr(id=678)
    public boolean isPrefAdvancedClientEnforceMinDisplay() {
        return getBooleanAttr(Provisioning.A_zimbraPrefAdvancedClientEnforceMinDisplay, false);
    }

    /**
     * After login, whether the advanced client should enforce minimum
     * display resolution
     *
     * @param zimbraPrefAdvancedClientEnforceMinDisplay new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=678)
    public Map<String,Object> setPrefAdvancedClientEnforceMinDisplay(boolean zimbraPrefAdvancedClientEnforceMinDisplay, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefAdvancedClientEnforceMinDisplay, Boolean.toString(zimbraPrefAdvancedClientEnforceMinDisplay));
        return attrs;
    }

    /**
     * After login, whether the advanced client should enforce minimum
     * display resolution
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=678)
    public Map<String,Object> unsetPrefAdvancedClientEnforceMinDisplay(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefAdvancedClientEnforceMinDisplay, "");
        return attrs;
    }

    /**
     * whether or not new address in outgoing email are auto added to address
     * book
     *
     * @return zimbraPrefAutoAddAddressEnabled, or false if unset
     */
    @ZAttr(id=131)
    public boolean isPrefAutoAddAddressEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefAutoAddAddressEnabled, false);
    }

    /**
     * whether or not new address in outgoing email are auto added to address
     * book
     *
     * @param zimbraPrefAutoAddAddressEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=131)
    public Map<String,Object> setPrefAutoAddAddressEnabled(boolean zimbraPrefAutoAddAddressEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefAutoAddAddressEnabled, Boolean.toString(zimbraPrefAutoAddAddressEnabled));
        return attrs;
    }

    /**
     * whether or not new address in outgoing email are auto added to address
     * book
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=131)
    public Map<String,Object> unsetPrefAutoAddAddressEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefAutoAddAddressEnabled, "");
        return attrs;
    }

    /**
     * time to wait before auto saving a draft(nnnnn[hmsd])
     *
     * <p>Use getPrefAutoSaveDraftIntervalAsString to access value as a string.
     *
     * @see #getPrefAutoSaveDraftIntervalAsString()
     *
     * @return zimbraPrefAutoSaveDraftInterval in millseconds, or -1 if unset
     */
    @ZAttr(id=561)
    public long getPrefAutoSaveDraftInterval() {
        return getTimeInterval(Provisioning.A_zimbraPrefAutoSaveDraftInterval, -1);
    }

    /**
     * time to wait before auto saving a draft(nnnnn[hmsd])
     *
     * @return zimbraPrefAutoSaveDraftInterval, or null unset
     */
    @ZAttr(id=561)
    public String getPrefAutoSaveDraftIntervalAsString() {
        return getAttr(Provisioning.A_zimbraPrefAutoSaveDraftInterval);
    }

    /**
     * time to wait before auto saving a draft(nnnnn[hmsd])
     *
     * @param zimbraPrefAutoSaveDraftInterval new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=561)
    public Map<String,Object> setPrefAutoSaveDraftInterval(String zimbraPrefAutoSaveDraftInterval, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefAutoSaveDraftInterval, zimbraPrefAutoSaveDraftInterval);
        return attrs;
    }

    /**
     * time to wait before auto saving a draft(nnnnn[hmsd])
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=561)
    public Map<String,Object> unsetPrefAutoSaveDraftInterval(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefAutoSaveDraftInterval, "");
        return attrs;
    }

    /**
     * address that we will bcc when using sending mail with this identity
     * (deprecatedSince 5.0 in identity)
     *
     * @return zimbraPrefBccAddress, or null unset
     */
    @ZAttr(id=411)
    public String getPrefBccAddress() {
        return getAttr(Provisioning.A_zimbraPrefBccAddress);
    }

    /**
     * address that we will bcc when using sending mail with this identity
     * (deprecatedSince 5.0 in identity)
     *
     * @param zimbraPrefBccAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=411)
    public Map<String,Object> setPrefBccAddress(String zimbraPrefBccAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefBccAddress, zimbraPrefBccAddress);
        return attrs;
    }

    /**
     * address that we will bcc when using sending mail with this identity
     * (deprecatedSince 5.0 in identity)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=411)
    public Map<String,Object> unsetPrefBccAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefBccAddress, "");
        return attrs;
    }

    /**
     * whether to allow a cancel email sent to organizer of appointment
     *
     * @return zimbraPrefCalendarAllowCancelEmailToSelf, or false if unset
     */
    @ZAttr(id=702)
    public boolean isPrefCalendarAllowCancelEmailToSelf() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarAllowCancelEmailToSelf, false);
    }

    /**
     * whether to allow a cancel email sent to organizer of appointment
     *
     * @param zimbraPrefCalendarAllowCancelEmailToSelf new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=702)
    public Map<String,Object> setPrefCalendarAllowCancelEmailToSelf(boolean zimbraPrefCalendarAllowCancelEmailToSelf, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarAllowCancelEmailToSelf, Boolean.toString(zimbraPrefCalendarAllowCancelEmailToSelf));
        return attrs;
    }

    /**
     * whether to allow a cancel email sent to organizer of appointment
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=702)
    public Map<String,Object> unsetPrefCalendarAllowCancelEmailToSelf(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarAllowCancelEmailToSelf, "");
        return attrs;
    }

    /**
     * whether calendar invite part in a forwarded email is auto-added to
     * calendar
     *
     * @return zimbraPrefCalendarAllowForwardedInvite, or false if unset
     */
    @ZAttr(id=686)
    public boolean isPrefCalendarAllowForwardedInvite() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarAllowForwardedInvite, false);
    }

    /**
     * whether calendar invite part in a forwarded email is auto-added to
     * calendar
     *
     * @param zimbraPrefCalendarAllowForwardedInvite new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=686)
    public Map<String,Object> setPrefCalendarAllowForwardedInvite(boolean zimbraPrefCalendarAllowForwardedInvite, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarAllowForwardedInvite, Boolean.toString(zimbraPrefCalendarAllowForwardedInvite));
        return attrs;
    }

    /**
     * whether calendar invite part in a forwarded email is auto-added to
     * calendar
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=686)
    public Map<String,Object> unsetPrefCalendarAllowForwardedInvite(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarAllowForwardedInvite, "");
        return attrs;
    }

    /**
     * whether calendar invite part without method parameter in Content-Type
     * header is auto-added to calendar
     *
     * @return zimbraPrefCalendarAllowMethodlessInvite, or false if unset
     */
    @ZAttr(id=687)
    public boolean isPrefCalendarAllowMethodlessInvite() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarAllowMethodlessInvite, false);
    }

    /**
     * whether calendar invite part without method parameter in Content-Type
     * header is auto-added to calendar
     *
     * @param zimbraPrefCalendarAllowMethodlessInvite new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=687)
    public Map<String,Object> setPrefCalendarAllowMethodlessInvite(boolean zimbraPrefCalendarAllowMethodlessInvite, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarAllowMethodlessInvite, Boolean.toString(zimbraPrefCalendarAllowMethodlessInvite));
        return attrs;
    }

    /**
     * whether calendar invite part without method parameter in Content-Type
     * header is auto-added to calendar
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=687)
    public Map<String,Object> unsetPrefCalendarAllowMethodlessInvite(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarAllowMethodlessInvite, "");
        return attrs;
    }

    /**
     * whether calendar invite part with PUBLISH method is auto-added to
     * calendar
     *
     * @return zimbraPrefCalendarAllowPublishMethodInvite, or false if unset
     */
    @ZAttr(id=688)
    public boolean isPrefCalendarAllowPublishMethodInvite() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarAllowPublishMethodInvite, false);
    }

    /**
     * whether calendar invite part with PUBLISH method is auto-added to
     * calendar
     *
     * @param zimbraPrefCalendarAllowPublishMethodInvite new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=688)
    public Map<String,Object> setPrefCalendarAllowPublishMethodInvite(boolean zimbraPrefCalendarAllowPublishMethodInvite, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarAllowPublishMethodInvite, Boolean.toString(zimbraPrefCalendarAllowPublishMethodInvite));
        return attrs;
    }

    /**
     * whether calendar invite part with PUBLISH method is auto-added to
     * calendar
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=688)
    public Map<String,Object> unsetPrefCalendarAllowPublishMethodInvite(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarAllowPublishMethodInvite, "");
        return attrs;
    }

    /**
     * always show the mini calendar
     *
     * @return zimbraPrefCalendarAlwaysShowMiniCal, or false if unset
     */
    @ZAttr(id=276)
    public boolean isPrefCalendarAlwaysShowMiniCal() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarAlwaysShowMiniCal, false);
    }

    /**
     * always show the mini calendar
     *
     * @param zimbraPrefCalendarAlwaysShowMiniCal new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=276)
    public Map<String,Object> setPrefCalendarAlwaysShowMiniCal(boolean zimbraPrefCalendarAlwaysShowMiniCal, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarAlwaysShowMiniCal, Boolean.toString(zimbraPrefCalendarAlwaysShowMiniCal));
        return attrs;
    }

    /**
     * always show the mini calendar
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=276)
    public Map<String,Object> unsetPrefCalendarAlwaysShowMiniCal(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarAlwaysShowMiniCal, "");
        return attrs;
    }

    /**
     * number of minutes (0 = never) before appt to show reminder dialog
     *
     * @return zimbraPrefCalendarApptReminderWarningTime, or -1 if unset
     */
    @ZAttr(id=341)
    public int getPrefCalendarApptReminderWarningTime() {
        return getIntAttr(Provisioning.A_zimbraPrefCalendarApptReminderWarningTime, -1);
    }

    /**
     * number of minutes (0 = never) before appt to show reminder dialog
     *
     * @param zimbraPrefCalendarApptReminderWarningTime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=341)
    public Map<String,Object> setPrefCalendarApptReminderWarningTime(int zimbraPrefCalendarApptReminderWarningTime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarApptReminderWarningTime, Integer.toString(zimbraPrefCalendarApptReminderWarningTime));
        return attrs;
    }

    /**
     * number of minutes (0 = never) before appt to show reminder dialog
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=341)
    public Map<String,Object> unsetPrefCalendarApptReminderWarningTime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarApptReminderWarningTime, "");
        return attrs;
    }

    /**
     * hour of day that the day view should end at, non-inclusive (16=4pm, 24
     * = midnight, etc)
     *
     * @return zimbraPrefCalendarDayHourEnd, or -1 if unset
     */
    @ZAttr(id=440)
    public int getPrefCalendarDayHourEnd() {
        return getIntAttr(Provisioning.A_zimbraPrefCalendarDayHourEnd, -1);
    }

    /**
     * hour of day that the day view should end at, non-inclusive (16=4pm, 24
     * = midnight, etc)
     *
     * @param zimbraPrefCalendarDayHourEnd new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=440)
    public Map<String,Object> setPrefCalendarDayHourEnd(int zimbraPrefCalendarDayHourEnd, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarDayHourEnd, Integer.toString(zimbraPrefCalendarDayHourEnd));
        return attrs;
    }

    /**
     * hour of day that the day view should end at, non-inclusive (16=4pm, 24
     * = midnight, etc)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=440)
    public Map<String,Object> unsetPrefCalendarDayHourEnd(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarDayHourEnd, "");
        return attrs;
    }

    /**
     * hour of day that the day view should start at (1=1 AM, 8=8 AM, etc)
     *
     * @return zimbraPrefCalendarDayHourStart, or -1 if unset
     */
    @ZAttr(id=439)
    public int getPrefCalendarDayHourStart() {
        return getIntAttr(Provisioning.A_zimbraPrefCalendarDayHourStart, -1);
    }

    /**
     * hour of day that the day view should start at (1=1 AM, 8=8 AM, etc)
     *
     * @param zimbraPrefCalendarDayHourStart new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=439)
    public Map<String,Object> setPrefCalendarDayHourStart(int zimbraPrefCalendarDayHourStart, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarDayHourStart, Integer.toString(zimbraPrefCalendarDayHourStart));
        return attrs;
    }

    /**
     * hour of day that the day view should start at (1=1 AM, 8=8 AM, etc)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=439)
    public Map<String,Object> unsetPrefCalendarDayHourStart(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarDayHourStart, "");
        return attrs;
    }

    /**
     * first day of week to show in calendar (0=sunday, 6=saturday)
     *
     * @return zimbraPrefCalendarFirstDayOfWeek, or -1 if unset
     */
    @ZAttr(id=261)
    public int getPrefCalendarFirstDayOfWeek() {
        return getIntAttr(Provisioning.A_zimbraPrefCalendarFirstDayOfWeek, -1);
    }

    /**
     * first day of week to show in calendar (0=sunday, 6=saturday)
     *
     * @param zimbraPrefCalendarFirstDayOfWeek new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=261)
    public Map<String,Object> setPrefCalendarFirstDayOfWeek(int zimbraPrefCalendarFirstDayOfWeek, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarFirstDayOfWeek, Integer.toString(zimbraPrefCalendarFirstDayOfWeek));
        return attrs;
    }

    /**
     * first day of week to show in calendar (0=sunday, 6=saturday)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=261)
    public Map<String,Object> unsetPrefCalendarFirstDayOfWeek(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarFirstDayOfWeek, "");
        return attrs;
    }

    /**
     * comma-sep list of calendars that are initially checked
     *
     * @return zimbraPrefCalendarInitialCheckedCalendars, or null unset
     */
    @ZAttr(id=275)
    public String getPrefCalendarInitialCheckedCalendars() {
        return getAttr(Provisioning.A_zimbraPrefCalendarInitialCheckedCalendars);
    }

    /**
     * comma-sep list of calendars that are initially checked
     *
     * @param zimbraPrefCalendarInitialCheckedCalendars new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=275)
    public Map<String,Object> setPrefCalendarInitialCheckedCalendars(String zimbraPrefCalendarInitialCheckedCalendars, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarInitialCheckedCalendars, zimbraPrefCalendarInitialCheckedCalendars);
        return attrs;
    }

    /**
     * comma-sep list of calendars that are initially checked
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=275)
    public Map<String,Object> unsetPrefCalendarInitialCheckedCalendars(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarInitialCheckedCalendars, "");
        return attrs;
    }

    /**
     * initial calendar view to use
     *
     * <p>Valid values: [list, month, schedule, day, workWeek, week]
     *
     * @return zimbraPrefCalendarInitialView, or null if unset and/or has invalid value
     */
    @ZAttr(id=240)
    public ZAttrProvisioning.PrefCalendarInitialView getPrefCalendarInitialView() {
        try { String v = getAttr(Provisioning.A_zimbraPrefCalendarInitialView); return v == null ? null : ZAttrProvisioning.PrefCalendarInitialView.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * initial calendar view to use
     *
     * <p>Valid values: [list, month, schedule, day, workWeek, week]
     *
     * @return zimbraPrefCalendarInitialView, or null unset
     */
    @ZAttr(id=240)
    public String getPrefCalendarInitialViewAsString() {
        return getAttr(Provisioning.A_zimbraPrefCalendarInitialView);
    }

    /**
     * initial calendar view to use
     *
     * <p>Valid values: [list, month, schedule, day, workWeek, week]
     *
     * @param zimbraPrefCalendarInitialView new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=240)
    public Map<String,Object> setPrefCalendarInitialView(ZAttrProvisioning.PrefCalendarInitialView zimbraPrefCalendarInitialView, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarInitialView, zimbraPrefCalendarInitialView.toString());
        return attrs;
    }

    /**
     * initial calendar view to use
     *
     * <p>Valid values: [list, month, schedule, day, workWeek, week]
     *
     * @param zimbraPrefCalendarInitialView new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=240)
    public Map<String,Object> setPrefCalendarInitialViewAsString(String zimbraPrefCalendarInitialView, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarInitialView, zimbraPrefCalendarInitialView);
        return attrs;
    }

    /**
     * initial calendar view to use
     *
     * <p>Valid values: [list, month, schedule, day, workWeek, week]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=240)
    public Map<String,Object> unsetPrefCalendarInitialView(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarInitialView, "");
        return attrs;
    }

    /**
     * If set to true, user is notified by email of changes made to her
     * calendar by others via delegated calendar access.
     *
     * @return zimbraPrefCalendarNotifyDelegatedChanges, or false if unset
     */
    @ZAttr(id=273)
    public boolean isPrefCalendarNotifyDelegatedChanges() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarNotifyDelegatedChanges, false);
    }

    /**
     * If set to true, user is notified by email of changes made to her
     * calendar by others via delegated calendar access.
     *
     * @param zimbraPrefCalendarNotifyDelegatedChanges new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=273)
    public Map<String,Object> setPrefCalendarNotifyDelegatedChanges(boolean zimbraPrefCalendarNotifyDelegatedChanges, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarNotifyDelegatedChanges, Boolean.toString(zimbraPrefCalendarNotifyDelegatedChanges));
        return attrs;
    }

    /**
     * If set to true, user is notified by email of changes made to her
     * calendar by others via delegated calendar access.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=273)
    public Map<String,Object> unsetPrefCalendarNotifyDelegatedChanges(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarNotifyDelegatedChanges, "");
        return attrs;
    }

    /**
     * When to send the first reminder for an event.
     *
     * @return zimbraPrefCalendarReminderDuration1, or null unset
     */
    @ZAttr(id=573)
    public String getPrefCalendarReminderDuration1() {
        return getAttr(Provisioning.A_zimbraPrefCalendarReminderDuration1);
    }

    /**
     * When to send the first reminder for an event.
     *
     * @param zimbraPrefCalendarReminderDuration1 new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=573)
    public Map<String,Object> setPrefCalendarReminderDuration1(String zimbraPrefCalendarReminderDuration1, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarReminderDuration1, zimbraPrefCalendarReminderDuration1);
        return attrs;
    }

    /**
     * When to send the first reminder for an event.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=573)
    public Map<String,Object> unsetPrefCalendarReminderDuration1(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarReminderDuration1, "");
        return attrs;
    }

    /**
     * When to send the second reminder for an event.
     *
     * @return zimbraPrefCalendarReminderDuration2, or null unset
     */
    @ZAttr(id=574)
    public String getPrefCalendarReminderDuration2() {
        return getAttr(Provisioning.A_zimbraPrefCalendarReminderDuration2);
    }

    /**
     * When to send the second reminder for an event.
     *
     * @param zimbraPrefCalendarReminderDuration2 new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=574)
    public Map<String,Object> setPrefCalendarReminderDuration2(String zimbraPrefCalendarReminderDuration2, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarReminderDuration2, zimbraPrefCalendarReminderDuration2);
        return attrs;
    }

    /**
     * When to send the second reminder for an event.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=574)
    public Map<String,Object> unsetPrefCalendarReminderDuration2(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarReminderDuration2, "");
        return attrs;
    }

    /**
     * The email the reminder goes to.
     *
     * @return zimbraPrefCalendarReminderEmail, or null unset
     */
    @ZAttr(id=575)
    public String getPrefCalendarReminderEmail() {
        return getAttr(Provisioning.A_zimbraPrefCalendarReminderEmail);
    }

    /**
     * The email the reminder goes to.
     *
     * @param zimbraPrefCalendarReminderEmail new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=575)
    public Map<String,Object> setPrefCalendarReminderEmail(String zimbraPrefCalendarReminderEmail, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarReminderEmail, zimbraPrefCalendarReminderEmail);
        return attrs;
    }

    /**
     * The email the reminder goes to.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=575)
    public Map<String,Object> unsetPrefCalendarReminderEmail(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarReminderEmail, "");
        return attrs;
    }

    /**
     * Flash title when on appointment remimnder notification
     *
     * @return zimbraPrefCalendarReminderFlashTitle, or false if unset
     */
    @ZAttr(id=682)
    public boolean isPrefCalendarReminderFlashTitle() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarReminderFlashTitle, false);
    }

    /**
     * Flash title when on appointment remimnder notification
     *
     * @param zimbraPrefCalendarReminderFlashTitle new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=682)
    public Map<String,Object> setPrefCalendarReminderFlashTitle(boolean zimbraPrefCalendarReminderFlashTitle, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarReminderFlashTitle, Boolean.toString(zimbraPrefCalendarReminderFlashTitle));
        return attrs;
    }

    /**
     * Flash title when on appointment remimnder notification
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=682)
    public Map<String,Object> unsetPrefCalendarReminderFlashTitle(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarReminderFlashTitle, "");
        return attrs;
    }

    /**
     * The mobile device (phone) the reminder goes to.
     *
     * @return zimbraPrefCalendarReminderMobile, or false if unset
     */
    @ZAttr(id=577)
    public boolean isPrefCalendarReminderMobile() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarReminderMobile, false);
    }

    /**
     * The mobile device (phone) the reminder goes to.
     *
     * @param zimbraPrefCalendarReminderMobile new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=577)
    public Map<String,Object> setPrefCalendarReminderMobile(boolean zimbraPrefCalendarReminderMobile, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarReminderMobile, Boolean.toString(zimbraPrefCalendarReminderMobile));
        return attrs;
    }

    /**
     * The mobile device (phone) the reminder goes to.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=577)
    public Map<String,Object> unsetPrefCalendarReminderMobile(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarReminderMobile, "");
        return attrs;
    }

    /**
     * To send email or to not send email is the question.
     *
     * @return zimbraPrefCalendarReminderSendEmail, or false if unset
     */
    @ZAttr(id=576)
    public boolean isPrefCalendarReminderSendEmail() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarReminderSendEmail, false);
    }

    /**
     * To send email or to not send email is the question.
     *
     * @param zimbraPrefCalendarReminderSendEmail new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=576)
    public Map<String,Object> setPrefCalendarReminderSendEmail(boolean zimbraPrefCalendarReminderSendEmail, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarReminderSendEmail, Boolean.toString(zimbraPrefCalendarReminderSendEmail));
        return attrs;
    }

    /**
     * To send email or to not send email is the question.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=576)
    public Map<String,Object> unsetPrefCalendarReminderSendEmail(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarReminderSendEmail, "");
        return attrs;
    }

    /**
     * whether audible alert is enabled when appointment notification is
     * played
     *
     * @return zimbraPrefCalendarReminderSoundsEnabled, or false if unset
     */
    @ZAttr(id=667)
    public boolean isPrefCalendarReminderSoundsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarReminderSoundsEnabled, false);
    }

    /**
     * whether audible alert is enabled when appointment notification is
     * played
     *
     * @param zimbraPrefCalendarReminderSoundsEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=667)
    public Map<String,Object> setPrefCalendarReminderSoundsEnabled(boolean zimbraPrefCalendarReminderSoundsEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarReminderSoundsEnabled, Boolean.toString(zimbraPrefCalendarReminderSoundsEnabled));
        return attrs;
    }

    /**
     * whether audible alert is enabled when appointment notification is
     * played
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=667)
    public Map<String,Object> unsetPrefCalendarReminderSoundsEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarReminderSoundsEnabled, "");
        return attrs;
    }

    /**
     * Send a reminder via YIM
     *
     * @return zimbraPrefCalendarReminderYMessenger, or false if unset
     */
    @ZAttr(id=578)
    public boolean isPrefCalendarReminderYMessenger() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarReminderYMessenger, false);
    }

    /**
     * Send a reminder via YIM
     *
     * @param zimbraPrefCalendarReminderYMessenger new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=578)
    public Map<String,Object> setPrefCalendarReminderYMessenger(boolean zimbraPrefCalendarReminderYMessenger, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarReminderYMessenger, Boolean.toString(zimbraPrefCalendarReminderYMessenger));
        return attrs;
    }

    /**
     * Send a reminder via YIM
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=578)
    public Map<String,Object> unsetPrefCalendarReminderYMessenger(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarReminderYMessenger, "");
        return attrs;
    }

    /**
     * whether or not use quick add dialog or go into full appt edit view
     *
     * @return zimbraPrefCalendarUseQuickAdd, or false if unset
     */
    @ZAttr(id=274)
    public boolean isPrefCalendarUseQuickAdd() {
        return getBooleanAttr(Provisioning.A_zimbraPrefCalendarUseQuickAdd, false);
    }

    /**
     * whether or not use quick add dialog or go into full appt edit view
     *
     * @param zimbraPrefCalendarUseQuickAdd new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=274)
    public Map<String,Object> setPrefCalendarUseQuickAdd(boolean zimbraPrefCalendarUseQuickAdd, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarUseQuickAdd, Boolean.toString(zimbraPrefCalendarUseQuickAdd));
        return attrs;
    }

    /**
     * whether or not use quick add dialog or go into full appt edit view
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=274)
    public Map<String,Object> unsetPrefCalendarUseQuickAdd(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefCalendarUseQuickAdd, "");
        return attrs;
    }

    /**
     * zimbraId of visible child accounts
     *
     * @return zimbraPrefChildVisibleAccount, or ampty array if unset
     */
    @ZAttr(id=553)
    public String[] getPrefChildVisibleAccount() {
        return getMultiAttr(Provisioning.A_zimbraPrefChildVisibleAccount);
    }

    /**
     * zimbraId of visible child accounts
     *
     * @param zimbraPrefChildVisibleAccount new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=553)
    public Map<String,Object> setPrefChildVisibleAccount(String[] zimbraPrefChildVisibleAccount, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefChildVisibleAccount, zimbraPrefChildVisibleAccount);
        return attrs;
    }

    /**
     * zimbraId of visible child accounts
     *
     * @param zimbraPrefChildVisibleAccount new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=553)
    public Map<String,Object> addPrefChildVisibleAccount(String zimbraPrefChildVisibleAccount, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraPrefChildVisibleAccount, zimbraPrefChildVisibleAccount);
        return attrs;
    }

    /**
     * zimbraId of visible child accounts
     *
     * @param zimbraPrefChildVisibleAccount existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=553)
    public Map<String,Object> removePrefChildVisibleAccount(String zimbraPrefChildVisibleAccount, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraPrefChildVisibleAccount, zimbraPrefChildVisibleAccount);
        return attrs;
    }

    /**
     * zimbraId of visible child accounts
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=553)
    public Map<String,Object> unsetPrefChildVisibleAccount(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefChildVisibleAccount, "");
        return attrs;
    }

    /**
     * user preference of client type
     *
     * <p>Valid values: [standard, advanced]
     *
     * @return zimbraPrefClientType, or null if unset and/or has invalid value
     */
    @ZAttr(id=453)
    public ZAttrProvisioning.PrefClientType getPrefClientType() {
        try { String v = getAttr(Provisioning.A_zimbraPrefClientType); return v == null ? null : ZAttrProvisioning.PrefClientType.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * user preference of client type
     *
     * <p>Valid values: [standard, advanced]
     *
     * @return zimbraPrefClientType, or null unset
     */
    @ZAttr(id=453)
    public String getPrefClientTypeAsString() {
        return getAttr(Provisioning.A_zimbraPrefClientType);
    }

    /**
     * user preference of client type
     *
     * <p>Valid values: [standard, advanced]
     *
     * @param zimbraPrefClientType new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=453)
    public Map<String,Object> setPrefClientType(ZAttrProvisioning.PrefClientType zimbraPrefClientType, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefClientType, zimbraPrefClientType.toString());
        return attrs;
    }

    /**
     * user preference of client type
     *
     * <p>Valid values: [standard, advanced]
     *
     * @param zimbraPrefClientType new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=453)
    public Map<String,Object> setPrefClientTypeAsString(String zimbraPrefClientType, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefClientType, zimbraPrefClientType);
        return attrs;
    }

    /**
     * user preference of client type
     *
     * <p>Valid values: [standard, advanced]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=453)
    public Map<String,Object> unsetPrefClientType(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefClientType, "");
        return attrs;
    }

    /**
     * whether or not to compose in html or text.
     *
     * <p>Valid values: [html, text]
     *
     * @return zimbraPrefComposeFormat, or null if unset and/or has invalid value
     */
    @ZAttr(id=217)
    public ZAttrProvisioning.PrefComposeFormat getPrefComposeFormat() {
        try { String v = getAttr(Provisioning.A_zimbraPrefComposeFormat); return v == null ? null : ZAttrProvisioning.PrefComposeFormat.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * whether or not to compose in html or text.
     *
     * <p>Valid values: [html, text]
     *
     * @return zimbraPrefComposeFormat, or null unset
     */
    @ZAttr(id=217)
    public String getPrefComposeFormatAsString() {
        return getAttr(Provisioning.A_zimbraPrefComposeFormat);
    }

    /**
     * whether or not to compose in html or text.
     *
     * <p>Valid values: [html, text]
     *
     * @param zimbraPrefComposeFormat new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=217)
    public Map<String,Object> setPrefComposeFormat(ZAttrProvisioning.PrefComposeFormat zimbraPrefComposeFormat, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefComposeFormat, zimbraPrefComposeFormat.toString());
        return attrs;
    }

    /**
     * whether or not to compose in html or text.
     *
     * <p>Valid values: [html, text]
     *
     * @param zimbraPrefComposeFormat new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=217)
    public Map<String,Object> setPrefComposeFormatAsString(String zimbraPrefComposeFormat, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefComposeFormat, zimbraPrefComposeFormat);
        return attrs;
    }

    /**
     * whether or not to compose in html or text.
     *
     * <p>Valid values: [html, text]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=217)
    public Map<String,Object> unsetPrefComposeFormat(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefComposeFormat, "");
        return attrs;
    }

    /**
     * whether or not compose messages in a new windows by default
     *
     * @return zimbraPrefComposeInNewWindow, or false if unset
     */
    @ZAttr(id=209)
    public boolean isPrefComposeInNewWindow() {
        return getBooleanAttr(Provisioning.A_zimbraPrefComposeInNewWindow, false);
    }

    /**
     * whether or not compose messages in a new windows by default
     *
     * @param zimbraPrefComposeInNewWindow new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=209)
    public Map<String,Object> setPrefComposeInNewWindow(boolean zimbraPrefComposeInNewWindow, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefComposeInNewWindow, Boolean.toString(zimbraPrefComposeInNewWindow));
        return attrs;
    }

    /**
     * whether or not compose messages in a new windows by default
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=209)
    public Map<String,Object> unsetPrefComposeInNewWindow(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefComposeInNewWindow, "");
        return attrs;
    }

    /**
     * initial contact view to use
     *
     * <p>Valid values: [list, cards]
     *
     * @return zimbraPrefContactsInitialView, or null if unset and/or has invalid value
     */
    @ZAttr(id=167)
    public ZAttrProvisioning.PrefContactsInitialView getPrefContactsInitialView() {
        try { String v = getAttr(Provisioning.A_zimbraPrefContactsInitialView); return v == null ? null : ZAttrProvisioning.PrefContactsInitialView.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * initial contact view to use
     *
     * <p>Valid values: [list, cards]
     *
     * @return zimbraPrefContactsInitialView, or null unset
     */
    @ZAttr(id=167)
    public String getPrefContactsInitialViewAsString() {
        return getAttr(Provisioning.A_zimbraPrefContactsInitialView);
    }

    /**
     * initial contact view to use
     *
     * <p>Valid values: [list, cards]
     *
     * @param zimbraPrefContactsInitialView new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=167)
    public Map<String,Object> setPrefContactsInitialView(ZAttrProvisioning.PrefContactsInitialView zimbraPrefContactsInitialView, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefContactsInitialView, zimbraPrefContactsInitialView.toString());
        return attrs;
    }

    /**
     * initial contact view to use
     *
     * <p>Valid values: [list, cards]
     *
     * @param zimbraPrefContactsInitialView new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=167)
    public Map<String,Object> setPrefContactsInitialViewAsString(String zimbraPrefContactsInitialView, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefContactsInitialView, zimbraPrefContactsInitialView);
        return attrs;
    }

    /**
     * initial contact view to use
     *
     * <p>Valid values: [list, cards]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=167)
    public Map<String,Object> unsetPrefContactsInitialView(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefContactsInitialView, "");
        return attrs;
    }

    /**
     * number of contacts per page
     *
     * @return zimbraPrefContactsPerPage, or -1 if unset
     */
    @ZAttr(id=148)
    public int getPrefContactsPerPage() {
        return getIntAttr(Provisioning.A_zimbraPrefContactsPerPage, -1);
    }

    /**
     * number of contacts per page
     *
     * @param zimbraPrefContactsPerPage new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=148)
    public Map<String,Object> setPrefContactsPerPage(int zimbraPrefContactsPerPage, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefContactsPerPage, Integer.toString(zimbraPrefContactsPerPage));
        return attrs;
    }

    /**
     * number of contacts per page
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=148)
    public Map<String,Object> unsetPrefContactsPerPage(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefContactsPerPage, "");
        return attrs;
    }

    /**
     * dedupeNone|secondCopyIfOnToOrCC|moveSentMessageToInbox|dedupeAll
     *
     * <p>Valid values: [dedupeAll, dedupeNone, secondCopyifOnToOrCC]
     *
     * @return zimbraPrefDedupeMessagesSentToSelf, or null if unset and/or has invalid value
     */
    @ZAttr(id=144)
    public ZAttrProvisioning.PrefDedupeMessagesSentToSelf getPrefDedupeMessagesSentToSelf() {
        try { String v = getAttr(Provisioning.A_zimbraPrefDedupeMessagesSentToSelf); return v == null ? null : ZAttrProvisioning.PrefDedupeMessagesSentToSelf.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * dedupeNone|secondCopyIfOnToOrCC|moveSentMessageToInbox|dedupeAll
     *
     * <p>Valid values: [dedupeAll, dedupeNone, secondCopyifOnToOrCC]
     *
     * @return zimbraPrefDedupeMessagesSentToSelf, or null unset
     */
    @ZAttr(id=144)
    public String getPrefDedupeMessagesSentToSelfAsString() {
        return getAttr(Provisioning.A_zimbraPrefDedupeMessagesSentToSelf);
    }

    /**
     * dedupeNone|secondCopyIfOnToOrCC|moveSentMessageToInbox|dedupeAll
     *
     * <p>Valid values: [dedupeAll, dedupeNone, secondCopyifOnToOrCC]
     *
     * @param zimbraPrefDedupeMessagesSentToSelf new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=144)
    public Map<String,Object> setPrefDedupeMessagesSentToSelf(ZAttrProvisioning.PrefDedupeMessagesSentToSelf zimbraPrefDedupeMessagesSentToSelf, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefDedupeMessagesSentToSelf, zimbraPrefDedupeMessagesSentToSelf.toString());
        return attrs;
    }

    /**
     * dedupeNone|secondCopyIfOnToOrCC|moveSentMessageToInbox|dedupeAll
     *
     * <p>Valid values: [dedupeAll, dedupeNone, secondCopyifOnToOrCC]
     *
     * @param zimbraPrefDedupeMessagesSentToSelf new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=144)
    public Map<String,Object> setPrefDedupeMessagesSentToSelfAsString(String zimbraPrefDedupeMessagesSentToSelf, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefDedupeMessagesSentToSelf, zimbraPrefDedupeMessagesSentToSelf);
        return attrs;
    }

    /**
     * dedupeNone|secondCopyIfOnToOrCC|moveSentMessageToInbox|dedupeAll
     *
     * <p>Valid values: [dedupeAll, dedupeNone, secondCopyifOnToOrCC]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=144)
    public Map<String,Object> unsetPrefDedupeMessagesSentToSelf(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefDedupeMessagesSentToSelf, "");
        return attrs;
    }

    /**
     * default mail signature for account/identity/dataSource
     *
     * @return zimbraPrefDefaultSignatureId, or null unset
     */
    @ZAttr(id=492)
    public String getPrefDefaultSignatureId() {
        return getAttr(Provisioning.A_zimbraPrefDefaultSignatureId);
    }

    /**
     * default mail signature for account/identity/dataSource
     *
     * @param zimbraPrefDefaultSignatureId new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=492)
    public Map<String,Object> setPrefDefaultSignatureId(String zimbraPrefDefaultSignatureId, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefDefaultSignatureId, zimbraPrefDefaultSignatureId);
        return attrs;
    }

    /**
     * default mail signature for account/identity/dataSource
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=492)
    public Map<String,Object> unsetPrefDefaultSignatureId(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefDefaultSignatureId, "");
        return attrs;
    }

    /**
     * whether meeting invite emails are moved to Trash folder upon
     * accept/decline
     *
     * @return zimbraPrefDeleteInviteOnReply, or false if unset
     */
    @ZAttr(id=470)
    public boolean isPrefDeleteInviteOnReply() {
        return getBooleanAttr(Provisioning.A_zimbraPrefDeleteInviteOnReply, false);
    }

    /**
     * whether meeting invite emails are moved to Trash folder upon
     * accept/decline
     *
     * @param zimbraPrefDeleteInviteOnReply new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=470)
    public Map<String,Object> setPrefDeleteInviteOnReply(boolean zimbraPrefDeleteInviteOnReply, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefDeleteInviteOnReply, Boolean.toString(zimbraPrefDeleteInviteOnReply));
        return attrs;
    }

    /**
     * whether meeting invite emails are moved to Trash folder upon
     * accept/decline
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=470)
    public Map<String,Object> unsetPrefDeleteInviteOnReply(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefDeleteInviteOnReply, "");
        return attrs;
    }

    /**
     * whether to display external images in HTML mail
     *
     * @return zimbraPrefDisplayExternalImages, or false if unset
     */
    @ZAttr(id=511)
    public boolean isPrefDisplayExternalImages() {
        return getBooleanAttr(Provisioning.A_zimbraPrefDisplayExternalImages, false);
    }

    /**
     * whether to display external images in HTML mail
     *
     * @param zimbraPrefDisplayExternalImages new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=511)
    public Map<String,Object> setPrefDisplayExternalImages(boolean zimbraPrefDisplayExternalImages, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefDisplayExternalImages, Boolean.toString(zimbraPrefDisplayExternalImages));
        return attrs;
    }

    /**
     * whether to display external images in HTML mail
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=511)
    public Map<String,Object> unsetPrefDisplayExternalImages(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefDisplayExternalImages, "");
        return attrs;
    }

    /**
     * whether or not folder tree is expanded
     *
     * @return zimbraPrefFolderTreeOpen, or false if unset
     */
    @ZAttr(id=637)
    public boolean isPrefFolderTreeOpen() {
        return getBooleanAttr(Provisioning.A_zimbraPrefFolderTreeOpen, false);
    }

    /**
     * whether or not folder tree is expanded
     *
     * @param zimbraPrefFolderTreeOpen new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=637)
    public Map<String,Object> setPrefFolderTreeOpen(boolean zimbraPrefFolderTreeOpen, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefFolderTreeOpen, Boolean.toString(zimbraPrefFolderTreeOpen));
        return attrs;
    }

    /**
     * whether or not folder tree is expanded
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=637)
    public Map<String,Object> unsetPrefFolderTreeOpen(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefFolderTreeOpen, "");
        return attrs;
    }

    /**
     * what part of the original message to include during forwards
     * (deprecatedSince 5.0 in identity)
     *
     * <p>Valid values: [includeAsAttachment, includeBodyAndHeadersWithPrefix, includeBody, includeBodyWithPrefix]
     *
     * @return zimbraPrefForwardIncludeOriginalText, or null if unset and/or has invalid value
     */
    @ZAttr(id=134)
    public ZAttrProvisioning.PrefForwardIncludeOriginalText getPrefForwardIncludeOriginalText() {
        try { String v = getAttr(Provisioning.A_zimbraPrefForwardIncludeOriginalText); return v == null ? null : ZAttrProvisioning.PrefForwardIncludeOriginalText.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * what part of the original message to include during forwards
     * (deprecatedSince 5.0 in identity)
     *
     * <p>Valid values: [includeAsAttachment, includeBodyAndHeadersWithPrefix, includeBody, includeBodyWithPrefix]
     *
     * @return zimbraPrefForwardIncludeOriginalText, or null unset
     */
    @ZAttr(id=134)
    public String getPrefForwardIncludeOriginalTextAsString() {
        return getAttr(Provisioning.A_zimbraPrefForwardIncludeOriginalText);
    }

    /**
     * what part of the original message to include during forwards
     * (deprecatedSince 5.0 in identity)
     *
     * <p>Valid values: [includeAsAttachment, includeBodyAndHeadersWithPrefix, includeBody, includeBodyWithPrefix]
     *
     * @param zimbraPrefForwardIncludeOriginalText new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=134)
    public Map<String,Object> setPrefForwardIncludeOriginalText(ZAttrProvisioning.PrefForwardIncludeOriginalText zimbraPrefForwardIncludeOriginalText, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefForwardIncludeOriginalText, zimbraPrefForwardIncludeOriginalText.toString());
        return attrs;
    }

    /**
     * what part of the original message to include during forwards
     * (deprecatedSince 5.0 in identity)
     *
     * <p>Valid values: [includeAsAttachment, includeBodyAndHeadersWithPrefix, includeBody, includeBodyWithPrefix]
     *
     * @param zimbraPrefForwardIncludeOriginalText new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=134)
    public Map<String,Object> setPrefForwardIncludeOriginalTextAsString(String zimbraPrefForwardIncludeOriginalText, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefForwardIncludeOriginalText, zimbraPrefForwardIncludeOriginalText);
        return attrs;
    }

    /**
     * what part of the original message to include during forwards
     * (deprecatedSince 5.0 in identity)
     *
     * <p>Valid values: [includeAsAttachment, includeBodyAndHeadersWithPrefix, includeBody, includeBodyWithPrefix]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=134)
    public Map<String,Object> unsetPrefForwardIncludeOriginalText(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefForwardIncludeOriginalText, "");
        return attrs;
    }

    /**
     * what format we reply/forward messages in (deprecatedSince 5.0 in
     * identity)
     *
     * <p>Valid values: [same, html, text]
     *
     * @return zimbraPrefForwardReplyFormat, or null if unset and/or has invalid value
     */
    @ZAttr(id=413)
    public ZAttrProvisioning.PrefForwardReplyFormat getPrefForwardReplyFormat() {
        try { String v = getAttr(Provisioning.A_zimbraPrefForwardReplyFormat); return v == null ? null : ZAttrProvisioning.PrefForwardReplyFormat.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * what format we reply/forward messages in (deprecatedSince 5.0 in
     * identity)
     *
     * <p>Valid values: [same, html, text]
     *
     * @return zimbraPrefForwardReplyFormat, or null unset
     */
    @ZAttr(id=413)
    public String getPrefForwardReplyFormatAsString() {
        return getAttr(Provisioning.A_zimbraPrefForwardReplyFormat);
    }

    /**
     * what format we reply/forward messages in (deprecatedSince 5.0 in
     * identity)
     *
     * <p>Valid values: [same, html, text]
     *
     * @param zimbraPrefForwardReplyFormat new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=413)
    public Map<String,Object> setPrefForwardReplyFormat(ZAttrProvisioning.PrefForwardReplyFormat zimbraPrefForwardReplyFormat, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefForwardReplyFormat, zimbraPrefForwardReplyFormat.toString());
        return attrs;
    }

    /**
     * what format we reply/forward messages in (deprecatedSince 5.0 in
     * identity)
     *
     * <p>Valid values: [same, html, text]
     *
     * @param zimbraPrefForwardReplyFormat new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=413)
    public Map<String,Object> setPrefForwardReplyFormatAsString(String zimbraPrefForwardReplyFormat, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefForwardReplyFormat, zimbraPrefForwardReplyFormat);
        return attrs;
    }

    /**
     * what format we reply/forward messages in (deprecatedSince 5.0 in
     * identity)
     *
     * <p>Valid values: [same, html, text]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=413)
    public Map<String,Object> unsetPrefForwardReplyFormat(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefForwardReplyFormat, "");
        return attrs;
    }

    /**
     * Deprecated since: 4.5. Deprecated in favor of
     * zimbraPrefForwardReplyFormat. Orig desc: whether or not to use same
     * format (text or html) of message we are replying to
     *
     * @return zimbraPrefForwardReplyInOriginalFormat, or false if unset
     */
    @ZAttr(id=218)
    public boolean isPrefForwardReplyInOriginalFormat() {
        return getBooleanAttr(Provisioning.A_zimbraPrefForwardReplyInOriginalFormat, false);
    }

    /**
     * Deprecated since: 4.5. Deprecated in favor of
     * zimbraPrefForwardReplyFormat. Orig desc: whether or not to use same
     * format (text or html) of message we are replying to
     *
     * @param zimbraPrefForwardReplyInOriginalFormat new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=218)
    public Map<String,Object> setPrefForwardReplyInOriginalFormat(boolean zimbraPrefForwardReplyInOriginalFormat, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefForwardReplyInOriginalFormat, Boolean.toString(zimbraPrefForwardReplyInOriginalFormat));
        return attrs;
    }

    /**
     * Deprecated since: 4.5. Deprecated in favor of
     * zimbraPrefForwardReplyFormat. Orig desc: whether or not to use same
     * format (text or html) of message we are replying to
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=218)
    public Map<String,Object> unsetPrefForwardReplyInOriginalFormat(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefForwardReplyInOriginalFormat, "");
        return attrs;
    }

    /**
     * prefix character to use during forward/reply (deprecatedSince 5.0 in
     * identity)
     *
     * @return zimbraPrefForwardReplyPrefixChar, or null unset
     */
    @ZAttr(id=130)
    public String getPrefForwardReplyPrefixChar() {
        return getAttr(Provisioning.A_zimbraPrefForwardReplyPrefixChar);
    }

    /**
     * prefix character to use during forward/reply (deprecatedSince 5.0 in
     * identity)
     *
     * @param zimbraPrefForwardReplyPrefixChar new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=130)
    public Map<String,Object> setPrefForwardReplyPrefixChar(String zimbraPrefForwardReplyPrefixChar, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefForwardReplyPrefixChar, zimbraPrefForwardReplyPrefixChar);
        return attrs;
    }

    /**
     * prefix character to use during forward/reply (deprecatedSince 5.0 in
     * identity)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=130)
    public Map<String,Object> unsetPrefForwardReplyPrefixChar(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefForwardReplyPrefixChar, "");
        return attrs;
    }

    /**
     * email address to put in from header
     *
     * @return zimbraPrefFromAddress, or null unset
     */
    @ZAttr(id=403)
    public String getPrefFromAddress() {
        return getAttr(Provisioning.A_zimbraPrefFromAddress);
    }

    /**
     * email address to put in from header
     *
     * @param zimbraPrefFromAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=403)
    public Map<String,Object> setPrefFromAddress(String zimbraPrefFromAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefFromAddress, zimbraPrefFromAddress);
        return attrs;
    }

    /**
     * email address to put in from header
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=403)
    public Map<String,Object> unsetPrefFromAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefFromAddress, "");
        return attrs;
    }

    /**
     * personal part of email address put in from header
     *
     * @return zimbraPrefFromDisplay, or null unset
     */
    @ZAttr(id=402)
    public String getPrefFromDisplay() {
        return getAttr(Provisioning.A_zimbraPrefFromDisplay);
    }

    /**
     * personal part of email address put in from header
     *
     * @param zimbraPrefFromDisplay new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=402)
    public Map<String,Object> setPrefFromDisplay(String zimbraPrefFromDisplay, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefFromDisplay, zimbraPrefFromDisplay);
        return attrs;
    }

    /**
     * personal part of email address put in from header
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=402)
    public Map<String,Object> unsetPrefFromDisplay(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefFromDisplay, "");
        return attrs;
    }

    /**
     * whether end-user wants auto-complete from GAL. Feature must also be
     * enabled.
     *
     * @return zimbraPrefGalAutoCompleteEnabled, or false if unset
     */
    @ZAttr(id=372)
    public boolean isPrefGalAutoCompleteEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefGalAutoCompleteEnabled, false);
    }

    /**
     * whether end-user wants auto-complete from GAL. Feature must also be
     * enabled.
     *
     * @param zimbraPrefGalAutoCompleteEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=372)
    public Map<String,Object> setPrefGalAutoCompleteEnabled(boolean zimbraPrefGalAutoCompleteEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefGalAutoCompleteEnabled, Boolean.toString(zimbraPrefGalAutoCompleteEnabled));
        return attrs;
    }

    /**
     * whether end-user wants auto-complete from GAL. Feature must also be
     * enabled.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=372)
    public Map<String,Object> unsetPrefGalAutoCompleteEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefGalAutoCompleteEnabled, "");
        return attrs;
    }

    /**
     * whether end-user wants search from GAL. Feature must also be enabled
     *
     * @return zimbraPrefGalSearchEnabled, or false if unset
     */
    @ZAttr(id=635)
    public boolean isPrefGalSearchEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefGalSearchEnabled, false);
    }

    /**
     * whether end-user wants search from GAL. Feature must also be enabled
     *
     * @param zimbraPrefGalSearchEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=635)
    public Map<String,Object> setPrefGalSearchEnabled(boolean zimbraPrefGalSearchEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefGalSearchEnabled, Boolean.toString(zimbraPrefGalSearchEnabled));
        return attrs;
    }

    /**
     * whether end-user wants search from GAL. Feature must also be enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=635)
    public Map<String,Object> unsetPrefGalSearchEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefGalSearchEnabled, "");
        return attrs;
    }

    /**
     * how to group mail by default
     *
     * <p>Valid values: [conversation, message]
     *
     * @return zimbraPrefGroupMailBy, or null if unset and/or has invalid value
     */
    @ZAttr(id=54)
    public ZAttrProvisioning.PrefGroupMailBy getPrefGroupMailBy() {
        try { String v = getAttr(Provisioning.A_zimbraPrefGroupMailBy); return v == null ? null : ZAttrProvisioning.PrefGroupMailBy.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * how to group mail by default
     *
     * <p>Valid values: [conversation, message]
     *
     * @return zimbraPrefGroupMailBy, or null unset
     */
    @ZAttr(id=54)
    public String getPrefGroupMailByAsString() {
        return getAttr(Provisioning.A_zimbraPrefGroupMailBy);
    }

    /**
     * how to group mail by default
     *
     * <p>Valid values: [conversation, message]
     *
     * @param zimbraPrefGroupMailBy new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=54)
    public Map<String,Object> setPrefGroupMailBy(ZAttrProvisioning.PrefGroupMailBy zimbraPrefGroupMailBy, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefGroupMailBy, zimbraPrefGroupMailBy.toString());
        return attrs;
    }

    /**
     * how to group mail by default
     *
     * <p>Valid values: [conversation, message]
     *
     * @param zimbraPrefGroupMailBy new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=54)
    public Map<String,Object> setPrefGroupMailByAsString(String zimbraPrefGroupMailBy, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefGroupMailBy, zimbraPrefGroupMailBy);
        return attrs;
    }

    /**
     * how to group mail by default
     *
     * <p>Valid values: [conversation, message]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=54)
    public Map<String,Object> unsetPrefGroupMailBy(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefGroupMailBy, "");
        return attrs;
    }

    /**
     * default font color
     *
     * @return zimbraPrefHtmlEditorDefaultFontColor, or null unset
     */
    @ZAttr(id=260)
    public String getPrefHtmlEditorDefaultFontColor() {
        return getAttr(Provisioning.A_zimbraPrefHtmlEditorDefaultFontColor);
    }

    /**
     * default font color
     *
     * @param zimbraPrefHtmlEditorDefaultFontColor new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=260)
    public Map<String,Object> setPrefHtmlEditorDefaultFontColor(String zimbraPrefHtmlEditorDefaultFontColor, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefHtmlEditorDefaultFontColor, zimbraPrefHtmlEditorDefaultFontColor);
        return attrs;
    }

    /**
     * default font color
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=260)
    public Map<String,Object> unsetPrefHtmlEditorDefaultFontColor(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefHtmlEditorDefaultFontColor, "");
        return attrs;
    }

    /**
     * default font family
     *
     * @return zimbraPrefHtmlEditorDefaultFontFamily, or null unset
     */
    @ZAttr(id=258)
    public String getPrefHtmlEditorDefaultFontFamily() {
        return getAttr(Provisioning.A_zimbraPrefHtmlEditorDefaultFontFamily);
    }

    /**
     * default font family
     *
     * @param zimbraPrefHtmlEditorDefaultFontFamily new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=258)
    public Map<String,Object> setPrefHtmlEditorDefaultFontFamily(String zimbraPrefHtmlEditorDefaultFontFamily, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefHtmlEditorDefaultFontFamily, zimbraPrefHtmlEditorDefaultFontFamily);
        return attrs;
    }

    /**
     * default font family
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=258)
    public Map<String,Object> unsetPrefHtmlEditorDefaultFontFamily(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefHtmlEditorDefaultFontFamily, "");
        return attrs;
    }

    /**
     * default font size
     *
     * @return zimbraPrefHtmlEditorDefaultFontSize, or null unset
     */
    @ZAttr(id=259)
    public String getPrefHtmlEditorDefaultFontSize() {
        return getAttr(Provisioning.A_zimbraPrefHtmlEditorDefaultFontSize);
    }

    /**
     * default font size
     *
     * @param zimbraPrefHtmlEditorDefaultFontSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=259)
    public Map<String,Object> setPrefHtmlEditorDefaultFontSize(String zimbraPrefHtmlEditorDefaultFontSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefHtmlEditorDefaultFontSize, zimbraPrefHtmlEditorDefaultFontSize);
        return attrs;
    }

    /**
     * default font size
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=259)
    public Map<String,Object> unsetPrefHtmlEditorDefaultFontSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefHtmlEditorDefaultFontSize, "");
        return attrs;
    }

    /**
     * whether to login to the IM client automatically
     *
     * @return zimbraPrefIMAutoLogin, or false if unset
     */
    @ZAttr(id=488)
    public boolean isPrefIMAutoLogin() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMAutoLogin, false);
    }

    /**
     * whether to login to the IM client automatically
     *
     * @param zimbraPrefIMAutoLogin new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=488)
    public Map<String,Object> setPrefIMAutoLogin(boolean zimbraPrefIMAutoLogin, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMAutoLogin, Boolean.toString(zimbraPrefIMAutoLogin));
        return attrs;
    }

    /**
     * whether to login to the IM client automatically
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=488)
    public Map<String,Object> unsetPrefIMAutoLogin(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMAutoLogin, "");
        return attrs;
    }

    /**
     * IM buddy list sort order
     *
     * @return zimbraPrefIMBuddyListSort, or null unset
     */
    @ZAttr(id=705)
    public String getPrefIMBuddyListSort() {
        return getAttr(Provisioning.A_zimbraPrefIMBuddyListSort);
    }

    /**
     * IM buddy list sort order
     *
     * @param zimbraPrefIMBuddyListSort new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=705)
    public Map<String,Object> setPrefIMBuddyListSort(String zimbraPrefIMBuddyListSort, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMBuddyListSort, zimbraPrefIMBuddyListSort);
        return attrs;
    }

    /**
     * IM buddy list sort order
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=705)
    public Map<String,Object> unsetPrefIMBuddyListSort(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMBuddyListSort, "");
        return attrs;
    }

    /**
     * Custom IM status messages
     *
     * @return zimbraPrefIMCustomStatusMessage, or ampty array if unset
     */
    @ZAttr(id=645)
    public String[] getPrefIMCustomStatusMessage() {
        return getMultiAttr(Provisioning.A_zimbraPrefIMCustomStatusMessage);
    }

    /**
     * Custom IM status messages
     *
     * @param zimbraPrefIMCustomStatusMessage new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=645)
    public Map<String,Object> setPrefIMCustomStatusMessage(String[] zimbraPrefIMCustomStatusMessage, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMCustomStatusMessage, zimbraPrefIMCustomStatusMessage);
        return attrs;
    }

    /**
     * Custom IM status messages
     *
     * @param zimbraPrefIMCustomStatusMessage new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=645)
    public Map<String,Object> addPrefIMCustomStatusMessage(String zimbraPrefIMCustomStatusMessage, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraPrefIMCustomStatusMessage, zimbraPrefIMCustomStatusMessage);
        return attrs;
    }

    /**
     * Custom IM status messages
     *
     * @param zimbraPrefIMCustomStatusMessage existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=645)
    public Map<String,Object> removePrefIMCustomStatusMessage(String zimbraPrefIMCustomStatusMessage, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraPrefIMCustomStatusMessage, zimbraPrefIMCustomStatusMessage);
        return attrs;
    }

    /**
     * Custom IM status messages
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=645)
    public Map<String,Object> unsetPrefIMCustomStatusMessage(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMCustomStatusMessage, "");
        return attrs;
    }

    /**
     * Flash IM icon on new messages
     *
     * @return zimbraPrefIMFlashIcon, or false if unset
     */
    @ZAttr(id=462)
    public boolean isPrefIMFlashIcon() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMFlashIcon, false);
    }

    /**
     * Flash IM icon on new messages
     *
     * @param zimbraPrefIMFlashIcon new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=462)
    public Map<String,Object> setPrefIMFlashIcon(boolean zimbraPrefIMFlashIcon, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMFlashIcon, Boolean.toString(zimbraPrefIMFlashIcon));
        return attrs;
    }

    /**
     * Flash IM icon on new messages
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=462)
    public Map<String,Object> unsetPrefIMFlashIcon(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMFlashIcon, "");
        return attrs;
    }

    /**
     * Flash title bar when a new IM arrives
     *
     * @return zimbraPrefIMFlashTitle, or false if unset
     */
    @ZAttr(id=679)
    public boolean isPrefIMFlashTitle() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMFlashTitle, false);
    }

    /**
     * Flash title bar when a new IM arrives
     *
     * @param zimbraPrefIMFlashTitle new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=679)
    public Map<String,Object> setPrefIMFlashTitle(boolean zimbraPrefIMFlashTitle, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMFlashTitle, Boolean.toString(zimbraPrefIMFlashTitle));
        return attrs;
    }

    /**
     * Flash title bar when a new IM arrives
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=679)
    public Map<String,Object> unsetPrefIMFlashTitle(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMFlashTitle, "");
        return attrs;
    }

    /**
     * whether to hide IM blocked buddies
     *
     * @return zimbraPrefIMHideBlockedBuddies, or false if unset
     */
    @ZAttr(id=707)
    public boolean isPrefIMHideBlockedBuddies() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMHideBlockedBuddies, false);
    }

    /**
     * whether to hide IM blocked buddies
     *
     * @param zimbraPrefIMHideBlockedBuddies new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=707)
    public Map<String,Object> setPrefIMHideBlockedBuddies(boolean zimbraPrefIMHideBlockedBuddies, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMHideBlockedBuddies, Boolean.toString(zimbraPrefIMHideBlockedBuddies));
        return attrs;
    }

    /**
     * whether to hide IM blocked buddies
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=707)
    public Map<String,Object> unsetPrefIMHideBlockedBuddies(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMHideBlockedBuddies, "");
        return attrs;
    }

    /**
     * whether to hide IM offline buddies
     *
     * @return zimbraPrefIMHideOfflineBuddies, or false if unset
     */
    @ZAttr(id=706)
    public boolean isPrefIMHideOfflineBuddies() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMHideOfflineBuddies, false);
    }

    /**
     * whether to hide IM offline buddies
     *
     * @param zimbraPrefIMHideOfflineBuddies new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=706)
    public Map<String,Object> setPrefIMHideOfflineBuddies(boolean zimbraPrefIMHideOfflineBuddies, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMHideOfflineBuddies, Boolean.toString(zimbraPrefIMHideOfflineBuddies));
        return attrs;
    }

    /**
     * whether to hide IM offline buddies
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=706)
    public Map<String,Object> unsetPrefIMHideOfflineBuddies(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMHideOfflineBuddies, "");
        return attrs;
    }

    /**
     * IM idle status
     *
     * <p>Valid values: [away, xa, offline, invisible]
     *
     * @return zimbraPrefIMIdleStatus, or null if unset and/or has invalid value
     */
    @ZAttr(id=560)
    public ZAttrProvisioning.PrefIMIdleStatus getPrefIMIdleStatus() {
        try { String v = getAttr(Provisioning.A_zimbraPrefIMIdleStatus); return v == null ? null : ZAttrProvisioning.PrefIMIdleStatus.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * IM idle status
     *
     * <p>Valid values: [away, xa, offline, invisible]
     *
     * @return zimbraPrefIMIdleStatus, or null unset
     */
    @ZAttr(id=560)
    public String getPrefIMIdleStatusAsString() {
        return getAttr(Provisioning.A_zimbraPrefIMIdleStatus);
    }

    /**
     * IM idle status
     *
     * <p>Valid values: [away, xa, offline, invisible]
     *
     * @param zimbraPrefIMIdleStatus new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=560)
    public Map<String,Object> setPrefIMIdleStatus(ZAttrProvisioning.PrefIMIdleStatus zimbraPrefIMIdleStatus, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMIdleStatus, zimbraPrefIMIdleStatus.toString());
        return attrs;
    }

    /**
     * IM idle status
     *
     * <p>Valid values: [away, xa, offline, invisible]
     *
     * @param zimbraPrefIMIdleStatus new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=560)
    public Map<String,Object> setPrefIMIdleStatusAsString(String zimbraPrefIMIdleStatus, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMIdleStatus, zimbraPrefIMIdleStatus);
        return attrs;
    }

    /**
     * IM idle status
     *
     * <p>Valid values: [away, xa, offline, invisible]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=560)
    public Map<String,Object> unsetPrefIMIdleStatus(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMIdleStatus, "");
        return attrs;
    }

    /**
     * IM session idle timeout in minutes
     *
     * @return zimbraPrefIMIdleTimeout, or -1 if unset
     */
    @ZAttr(id=559)
    public int getPrefIMIdleTimeout() {
        return getIntAttr(Provisioning.A_zimbraPrefIMIdleTimeout, -1);
    }

    /**
     * IM session idle timeout in minutes
     *
     * @param zimbraPrefIMIdleTimeout new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=559)
    public Map<String,Object> setPrefIMIdleTimeout(int zimbraPrefIMIdleTimeout, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMIdleTimeout, Integer.toString(zimbraPrefIMIdleTimeout));
        return attrs;
    }

    /**
     * IM session idle timeout in minutes
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=559)
    public Map<String,Object> unsetPrefIMIdleTimeout(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMIdleTimeout, "");
        return attrs;
    }

    /**
     * Enable instant notifications
     *
     * @return zimbraPrefIMInstantNotify, or false if unset
     */
    @ZAttr(id=517)
    public boolean isPrefIMInstantNotify() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMInstantNotify, false);
    }

    /**
     * Enable instant notifications
     *
     * @param zimbraPrefIMInstantNotify new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=517)
    public Map<String,Object> setPrefIMInstantNotify(boolean zimbraPrefIMInstantNotify, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMInstantNotify, Boolean.toString(zimbraPrefIMInstantNotify));
        return attrs;
    }

    /**
     * Enable instant notifications
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=517)
    public Map<String,Object> unsetPrefIMInstantNotify(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMInstantNotify, "");
        return attrs;
    }

    /**
     * whether to log IM chats to the Chats folder
     *
     * @return zimbraPrefIMLogChats, or false if unset
     */
    @ZAttr(id=556)
    public boolean isPrefIMLogChats() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMLogChats, false);
    }

    /**
     * whether to log IM chats to the Chats folder
     *
     * @param zimbraPrefIMLogChats new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=556)
    public Map<String,Object> setPrefIMLogChats(boolean zimbraPrefIMLogChats, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMLogChats, Boolean.toString(zimbraPrefIMLogChats));
        return attrs;
    }

    /**
     * whether to log IM chats to the Chats folder
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=556)
    public Map<String,Object> unsetPrefIMLogChats(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMLogChats, "");
        return attrs;
    }

    /**
     * whether IM log chats is enabled
     *
     * @return zimbraPrefIMLogChatsEnabled, or false if unset
     */
    @ZAttr(id=552)
    public boolean isPrefIMLogChatsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMLogChatsEnabled, false);
    }

    /**
     * whether IM log chats is enabled
     *
     * @param zimbraPrefIMLogChatsEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=552)
    public Map<String,Object> setPrefIMLogChatsEnabled(boolean zimbraPrefIMLogChatsEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMLogChatsEnabled, Boolean.toString(zimbraPrefIMLogChatsEnabled));
        return attrs;
    }

    /**
     * whether IM log chats is enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=552)
    public Map<String,Object> unsetPrefIMLogChatsEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMLogChatsEnabled, "");
        return attrs;
    }

    /**
     * Notify for presence modifications
     *
     * @return zimbraPrefIMNotifyPresence, or false if unset
     */
    @ZAttr(id=463)
    public boolean isPrefIMNotifyPresence() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMNotifyPresence, false);
    }

    /**
     * Notify for presence modifications
     *
     * @param zimbraPrefIMNotifyPresence new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=463)
    public Map<String,Object> setPrefIMNotifyPresence(boolean zimbraPrefIMNotifyPresence, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMNotifyPresence, Boolean.toString(zimbraPrefIMNotifyPresence));
        return attrs;
    }

    /**
     * Notify for presence modifications
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=463)
    public Map<String,Object> unsetPrefIMNotifyPresence(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMNotifyPresence, "");
        return attrs;
    }

    /**
     * Notify for status change
     *
     * @return zimbraPrefIMNotifyStatus, or false if unset
     */
    @ZAttr(id=464)
    public boolean isPrefIMNotifyStatus() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMNotifyStatus, false);
    }

    /**
     * Notify for status change
     *
     * @param zimbraPrefIMNotifyStatus new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=464)
    public Map<String,Object> setPrefIMNotifyStatus(boolean zimbraPrefIMNotifyStatus, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMNotifyStatus, Boolean.toString(zimbraPrefIMNotifyStatus));
        return attrs;
    }

    /**
     * Notify for status change
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=464)
    public Map<String,Object> unsetPrefIMNotifyStatus(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMNotifyStatus, "");
        return attrs;
    }

    /**
     * whether to report IM idle status
     *
     * @return zimbraPrefIMReportIdle, or false if unset
     */
    @ZAttr(id=558)
    public boolean isPrefIMReportIdle() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMReportIdle, false);
    }

    /**
     * whether to report IM idle status
     *
     * @param zimbraPrefIMReportIdle new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=558)
    public Map<String,Object> setPrefIMReportIdle(boolean zimbraPrefIMReportIdle, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMReportIdle, Boolean.toString(zimbraPrefIMReportIdle));
        return attrs;
    }

    /**
     * whether to report IM idle status
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=558)
    public Map<String,Object> unsetPrefIMReportIdle(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMReportIdle, "");
        return attrs;
    }

    /**
     * whether sounds is enabled in IM
     *
     * @return zimbraPrefIMSoundsEnabled, or false if unset
     */
    @ZAttr(id=570)
    public boolean isPrefIMSoundsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIMSoundsEnabled, false);
    }

    /**
     * whether sounds is enabled in IM
     *
     * @param zimbraPrefIMSoundsEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=570)
    public Map<String,Object> setPrefIMSoundsEnabled(boolean zimbraPrefIMSoundsEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMSoundsEnabled, Boolean.toString(zimbraPrefIMSoundsEnabled));
        return attrs;
    }

    /**
     * whether sounds is enabled in IM
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=570)
    public Map<String,Object> unsetPrefIMSoundsEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMSoundsEnabled, "");
        return attrs;
    }

    /**
     * last used yahoo id
     *
     * @return zimbraPrefIMYahooId, or null unset
     *
     * @since ZCS future
     */
    @ZAttr(id=757)
    public String getPrefIMYahooId() {
        return getAttr(Provisioning.A_zimbraPrefIMYahooId);
    }

    /**
     * last used yahoo id
     *
     * @param zimbraPrefIMYahooId new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=757)
    public Map<String,Object> setPrefIMYahooId(String zimbraPrefIMYahooId, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMYahooId, zimbraPrefIMYahooId);
        return attrs;
    }

    /**
     * last used yahoo id
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=757)
    public Map<String,Object> unsetPrefIMYahooId(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIMYahooId, "");
        return attrs;
    }

    /**
     * name of the identity
     *
     * @return zimbraPrefIdentityName, or null unset
     */
    @ZAttr(id=412)
    public String getPrefIdentityName() {
        return getAttr(Provisioning.A_zimbraPrefIdentityName);
    }

    /**
     * name of the identity
     *
     * @param zimbraPrefIdentityName new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=412)
    public Map<String,Object> setPrefIdentityName(String zimbraPrefIdentityName, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIdentityName, zimbraPrefIdentityName);
        return attrs;
    }

    /**
     * name of the identity
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=412)
    public Map<String,Object> unsetPrefIdentityName(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIdentityName, "");
        return attrs;
    }

    /**
     * whether or not the IMAP server exports search folders
     *
     * @return zimbraPrefImapSearchFoldersEnabled, or false if unset
     */
    @ZAttr(id=241)
    public boolean isPrefImapSearchFoldersEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefImapSearchFoldersEnabled, false);
    }

    /**
     * whether or not the IMAP server exports search folders
     *
     * @param zimbraPrefImapSearchFoldersEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=241)
    public Map<String,Object> setPrefImapSearchFoldersEnabled(boolean zimbraPrefImapSearchFoldersEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefImapSearchFoldersEnabled, Boolean.toString(zimbraPrefImapSearchFoldersEnabled));
        return attrs;
    }

    /**
     * whether or not the IMAP server exports search folders
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=241)
    public Map<String,Object> unsetPrefImapSearchFoldersEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefImapSearchFoldersEnabled, "");
        return attrs;
    }

    /**
     * Retention period of read messages in the Inbox folder. 0 means that
     * all messages will be retained.
     *
     * <p>Use getPrefInboxReadLifetimeAsString to access value as a string.
     *
     * @see #getPrefInboxReadLifetimeAsString()
     *
     * @return zimbraPrefInboxReadLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=538)
    public long getPrefInboxReadLifetime() {
        return getTimeInterval(Provisioning.A_zimbraPrefInboxReadLifetime, -1);
    }

    /**
     * Retention period of read messages in the Inbox folder. 0 means that
     * all messages will be retained.
     *
     * @return zimbraPrefInboxReadLifetime, or null unset
     */
    @ZAttr(id=538)
    public String getPrefInboxReadLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraPrefInboxReadLifetime);
    }

    /**
     * Retention period of read messages in the Inbox folder. 0 means that
     * all messages will be retained.
     *
     * @param zimbraPrefInboxReadLifetime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=538)
    public Map<String,Object> setPrefInboxReadLifetime(String zimbraPrefInboxReadLifetime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefInboxReadLifetime, zimbraPrefInboxReadLifetime);
        return attrs;
    }

    /**
     * Retention period of read messages in the Inbox folder. 0 means that
     * all messages will be retained.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=538)
    public Map<String,Object> unsetPrefInboxReadLifetime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefInboxReadLifetime, "");
        return attrs;
    }

    /**
     * Retention period of unread messages in the Inbox folder. 0 means that
     * all messages will be retained.
     *
     * <p>Use getPrefInboxUnreadLifetimeAsString to access value as a string.
     *
     * @see #getPrefInboxUnreadLifetimeAsString()
     *
     * @return zimbraPrefInboxUnreadLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=537)
    public long getPrefInboxUnreadLifetime() {
        return getTimeInterval(Provisioning.A_zimbraPrefInboxUnreadLifetime, -1);
    }

    /**
     * Retention period of unread messages in the Inbox folder. 0 means that
     * all messages will be retained.
     *
     * @return zimbraPrefInboxUnreadLifetime, or null unset
     */
    @ZAttr(id=537)
    public String getPrefInboxUnreadLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraPrefInboxUnreadLifetime);
    }

    /**
     * Retention period of unread messages in the Inbox folder. 0 means that
     * all messages will be retained.
     *
     * @param zimbraPrefInboxUnreadLifetime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=537)
    public Map<String,Object> setPrefInboxUnreadLifetime(String zimbraPrefInboxUnreadLifetime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefInboxUnreadLifetime, zimbraPrefInboxUnreadLifetime);
        return attrs;
    }

    /**
     * Retention period of unread messages in the Inbox folder. 0 means that
     * all messages will be retained.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=537)
    public Map<String,Object> unsetPrefInboxUnreadLifetime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefInboxUnreadLifetime, "");
        return attrs;
    }

    /**
     * whether or not to include spam in search by default
     *
     * @return zimbraPrefIncludeSpamInSearch, or false if unset
     */
    @ZAttr(id=55)
    public boolean isPrefIncludeSpamInSearch() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIncludeSpamInSearch, false);
    }

    /**
     * whether or not to include spam in search by default
     *
     * @param zimbraPrefIncludeSpamInSearch new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=55)
    public Map<String,Object> setPrefIncludeSpamInSearch(boolean zimbraPrefIncludeSpamInSearch, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIncludeSpamInSearch, Boolean.toString(zimbraPrefIncludeSpamInSearch));
        return attrs;
    }

    /**
     * whether or not to include spam in search by default
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=55)
    public Map<String,Object> unsetPrefIncludeSpamInSearch(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIncludeSpamInSearch, "");
        return attrs;
    }

    /**
     * whether or not to include trash in search by default
     *
     * @return zimbraPrefIncludeTrashInSearch, or false if unset
     */
    @ZAttr(id=56)
    public boolean isPrefIncludeTrashInSearch() {
        return getBooleanAttr(Provisioning.A_zimbraPrefIncludeTrashInSearch, false);
    }

    /**
     * whether or not to include trash in search by default
     *
     * @param zimbraPrefIncludeTrashInSearch new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=56)
    public Map<String,Object> setPrefIncludeTrashInSearch(boolean zimbraPrefIncludeTrashInSearch, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIncludeTrashInSearch, Boolean.toString(zimbraPrefIncludeTrashInSearch));
        return attrs;
    }

    /**
     * whether or not to include trash in search by default
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=56)
    public Map<String,Object> unsetPrefIncludeTrashInSearch(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefIncludeTrashInSearch, "");
        return attrs;
    }

    /**
     * Retention period of messages in the Junk folder. 0 means that all
     * messages will be retained. This user-modifiable attribute works in
     * conjunction with zimbraMailSpamLifetime, which is admin-modifiable.
     * The shorter duration is used.
     *
     * <p>Use getPrefJunkLifetimeAsString to access value as a string.
     *
     * @see #getPrefJunkLifetimeAsString()
     *
     * @return zimbraPrefJunkLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=540)
    public long getPrefJunkLifetime() {
        return getTimeInterval(Provisioning.A_zimbraPrefJunkLifetime, -1);
    }

    /**
     * Retention period of messages in the Junk folder. 0 means that all
     * messages will be retained. This user-modifiable attribute works in
     * conjunction with zimbraMailSpamLifetime, which is admin-modifiable.
     * The shorter duration is used.
     *
     * @return zimbraPrefJunkLifetime, or null unset
     */
    @ZAttr(id=540)
    public String getPrefJunkLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraPrefJunkLifetime);
    }

    /**
     * Retention period of messages in the Junk folder. 0 means that all
     * messages will be retained. This user-modifiable attribute works in
     * conjunction with zimbraMailSpamLifetime, which is admin-modifiable.
     * The shorter duration is used.
     *
     * @param zimbraPrefJunkLifetime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=540)
    public Map<String,Object> setPrefJunkLifetime(String zimbraPrefJunkLifetime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefJunkLifetime, zimbraPrefJunkLifetime);
        return attrs;
    }

    /**
     * Retention period of messages in the Junk folder. 0 means that all
     * messages will be retained. This user-modifiable attribute works in
     * conjunction with zimbraMailSpamLifetime, which is admin-modifiable.
     * The shorter duration is used.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=540)
    public Map<String,Object> unsetPrefJunkLifetime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefJunkLifetime, "");
        return attrs;
    }

    /**
     * optional account descriptive label
     *
     * @return zimbraPrefLabel, or null unset
     */
    @ZAttr(id=603)
    public String getPrefLabel() {
        return getAttr(Provisioning.A_zimbraPrefLabel);
    }

    /**
     * optional account descriptive label
     *
     * @param zimbraPrefLabel new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=603)
    public Map<String,Object> setPrefLabel(String zimbraPrefLabel, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefLabel, zimbraPrefLabel);
        return attrs;
    }

    /**
     * optional account descriptive label
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=603)
    public Map<String,Object> unsetPrefLabel(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefLabel, "");
        return attrs;
    }

    /**
     * list view columns in web client
     *
     * @return zimbraPrefListViewColumns, or null unset
     */
    @ZAttr(id=694)
    public String getPrefListViewColumns() {
        return getAttr(Provisioning.A_zimbraPrefListViewColumns);
    }

    /**
     * list view columns in web client
     *
     * @param zimbraPrefListViewColumns new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=694)
    public Map<String,Object> setPrefListViewColumns(String zimbraPrefListViewColumns, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefListViewColumns, zimbraPrefListViewColumns);
        return attrs;
    }

    /**
     * list view columns in web client
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=694)
    public Map<String,Object> unsetPrefListViewColumns(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefListViewColumns, "");
        return attrs;
    }

    /**
     * user locale preference, e.g. en_US Whenever the server looks for the
     * user locale, it will first look for zimbraPrefLocale, if it is not set
     * then it will fallback to the current mechanism of looking for
     * zimbraLocale in the various places for a user. zimbraLocale is the non
     * end-user attribute that specifies which locale an object defaults to,
     * it is not an end-user setting.
     *
     * @return zimbraPrefLocale, or null unset
     */
    @ZAttr(id=442)
    public String getPrefLocale() {
        return getAttr(Provisioning.A_zimbraPrefLocale);
    }

    /**
     * user locale preference, e.g. en_US Whenever the server looks for the
     * user locale, it will first look for zimbraPrefLocale, if it is not set
     * then it will fallback to the current mechanism of looking for
     * zimbraLocale in the various places for a user. zimbraLocale is the non
     * end-user attribute that specifies which locale an object defaults to,
     * it is not an end-user setting.
     *
     * @param zimbraPrefLocale new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=442)
    public Map<String,Object> setPrefLocale(String zimbraPrefLocale, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefLocale, zimbraPrefLocale);
        return attrs;
    }

    /**
     * user locale preference, e.g. en_US Whenever the server looks for the
     * user locale, it will first look for zimbraPrefLocale, if it is not set
     * then it will fallback to the current mechanism of looking for
     * zimbraLocale in the various places for a user. zimbraLocale is the non
     * end-user attribute that specifies which locale an object defaults to,
     * it is not an end-user setting.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=442)
    public Map<String,Object> unsetPrefLocale(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefLocale, "");
        return attrs;
    }

    /**
     * Default Charset for mail composing and parsing text
     *
     * @return zimbraPrefMailDefaultCharset, or null unset
     */
    @ZAttr(id=469)
    public String getPrefMailDefaultCharset() {
        return getAttr(Provisioning.A_zimbraPrefMailDefaultCharset);
    }

    /**
     * Default Charset for mail composing and parsing text
     *
     * @param zimbraPrefMailDefaultCharset new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=469)
    public Map<String,Object> setPrefMailDefaultCharset(String zimbraPrefMailDefaultCharset, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailDefaultCharset, zimbraPrefMailDefaultCharset);
        return attrs;
    }

    /**
     * Default Charset for mail composing and parsing text
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=469)
    public Map<String,Object> unsetPrefMailDefaultCharset(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailDefaultCharset, "");
        return attrs;
    }

    /**
     * Flash icon when a new email arrives
     *
     * @return zimbraPrefMailFlashIcon, or false if unset
     */
    @ZAttr(id=681)
    public boolean isPrefMailFlashIcon() {
        return getBooleanAttr(Provisioning.A_zimbraPrefMailFlashIcon, false);
    }

    /**
     * Flash icon when a new email arrives
     *
     * @param zimbraPrefMailFlashIcon new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=681)
    public Map<String,Object> setPrefMailFlashIcon(boolean zimbraPrefMailFlashIcon, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailFlashIcon, Boolean.toString(zimbraPrefMailFlashIcon));
        return attrs;
    }

    /**
     * Flash icon when a new email arrives
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=681)
    public Map<String,Object> unsetPrefMailFlashIcon(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailFlashIcon, "");
        return attrs;
    }

    /**
     * Flash title bar when a new email arrives
     *
     * @return zimbraPrefMailFlashTitle, or false if unset
     */
    @ZAttr(id=680)
    public boolean isPrefMailFlashTitle() {
        return getBooleanAttr(Provisioning.A_zimbraPrefMailFlashTitle, false);
    }

    /**
     * Flash title bar when a new email arrives
     *
     * @param zimbraPrefMailFlashTitle new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=680)
    public Map<String,Object> setPrefMailFlashTitle(boolean zimbraPrefMailFlashTitle, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailFlashTitle, Boolean.toString(zimbraPrefMailFlashTitle));
        return attrs;
    }

    /**
     * Flash title bar when a new email arrives
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=680)
    public Map<String,Object> unsetPrefMailFlashTitle(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailFlashTitle, "");
        return attrs;
    }

    /**
     * RFC822 forwarding address for an account
     *
     * @return zimbraPrefMailForwardingAddress, or null unset
     */
    @ZAttr(id=343)
    public String getPrefMailForwardingAddress() {
        return getAttr(Provisioning.A_zimbraPrefMailForwardingAddress);
    }

    /**
     * RFC822 forwarding address for an account
     *
     * @param zimbraPrefMailForwardingAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=343)
    public Map<String,Object> setPrefMailForwardingAddress(String zimbraPrefMailForwardingAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailForwardingAddress, zimbraPrefMailForwardingAddress);
        return attrs;
    }

    /**
     * RFC822 forwarding address for an account
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=343)
    public Map<String,Object> unsetPrefMailForwardingAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailForwardingAddress, "");
        return attrs;
    }

    /**
     * initial search done by dhtml client
     *
     * @return zimbraPrefMailInitialSearch, or null unset
     */
    @ZAttr(id=102)
    public String getPrefMailInitialSearch() {
        return getAttr(Provisioning.A_zimbraPrefMailInitialSearch);
    }

    /**
     * initial search done by dhtml client
     *
     * @param zimbraPrefMailInitialSearch new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=102)
    public Map<String,Object> setPrefMailInitialSearch(String zimbraPrefMailInitialSearch, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailInitialSearch, zimbraPrefMailInitialSearch);
        return attrs;
    }

    /**
     * initial search done by dhtml client
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=102)
    public Map<String,Object> unsetPrefMailInitialSearch(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailInitialSearch, "");
        return attrs;
    }

    /**
     * number of messages/conversations per page
     *
     * @return zimbraPrefMailItemsPerPage, or -1 if unset
     */
    @ZAttr(id=57)
    public int getPrefMailItemsPerPage() {
        return getIntAttr(Provisioning.A_zimbraPrefMailItemsPerPage, -1);
    }

    /**
     * number of messages/conversations per page
     *
     * @param zimbraPrefMailItemsPerPage new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=57)
    public Map<String,Object> setPrefMailItemsPerPage(int zimbraPrefMailItemsPerPage, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailItemsPerPage, Integer.toString(zimbraPrefMailItemsPerPage));
        return attrs;
    }

    /**
     * number of messages/conversations per page
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=57)
    public Map<String,Object> unsetPrefMailItemsPerPage(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailItemsPerPage, "");
        return attrs;
    }

    /**
     * whether or not to deliver mail locally
     *
     * @return zimbraPrefMailLocalDeliveryDisabled, or false if unset
     */
    @ZAttr(id=344)
    public boolean isPrefMailLocalDeliveryDisabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefMailLocalDeliveryDisabled, false);
    }

    /**
     * whether or not to deliver mail locally
     *
     * @param zimbraPrefMailLocalDeliveryDisabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=344)
    public Map<String,Object> setPrefMailLocalDeliveryDisabled(boolean zimbraPrefMailLocalDeliveryDisabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailLocalDeliveryDisabled, Boolean.toString(zimbraPrefMailLocalDeliveryDisabled));
        return attrs;
    }

    /**
     * whether or not to deliver mail locally
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=344)
    public Map<String,Object> unsetPrefMailLocalDeliveryDisabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailLocalDeliveryDisabled, "");
        return attrs;
    }

    /**
     * interval at which the web client polls the server for new messages
     * (nnnnn[hmsd])
     *
     * <p>Use getPrefMailPollingIntervalAsString to access value as a string.
     *
     * @see #getPrefMailPollingIntervalAsString()
     *
     * @return zimbraPrefMailPollingInterval in millseconds, or -1 if unset
     */
    @ZAttr(id=111)
    public long getPrefMailPollingInterval() {
        return getTimeInterval(Provisioning.A_zimbraPrefMailPollingInterval, -1);
    }

    /**
     * interval at which the web client polls the server for new messages
     * (nnnnn[hmsd])
     *
     * @return zimbraPrefMailPollingInterval, or null unset
     */
    @ZAttr(id=111)
    public String getPrefMailPollingIntervalAsString() {
        return getAttr(Provisioning.A_zimbraPrefMailPollingInterval);
    }

    /**
     * interval at which the web client polls the server for new messages
     * (nnnnn[hmsd])
     *
     * @param zimbraPrefMailPollingInterval new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=111)
    public Map<String,Object> setPrefMailPollingInterval(String zimbraPrefMailPollingInterval, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailPollingInterval, zimbraPrefMailPollingInterval);
        return attrs;
    }

    /**
     * interval at which the web client polls the server for new messages
     * (nnnnn[hmsd])
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=111)
    public Map<String,Object> unsetPrefMailPollingInterval(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailPollingInterval, "");
        return attrs;
    }

    /**
     * mail text signature (deprecatedSince 5.0 in identity)
     *
     * @return zimbraPrefMailSignature, or null unset
     */
    @ZAttr(id=17)
    public String getPrefMailSignature() {
        return getAttr(Provisioning.A_zimbraPrefMailSignature);
    }

    /**
     * mail text signature (deprecatedSince 5.0 in identity)
     *
     * @param zimbraPrefMailSignature new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=17)
    public Map<String,Object> setPrefMailSignature(String zimbraPrefMailSignature, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailSignature, zimbraPrefMailSignature);
        return attrs;
    }

    /**
     * mail text signature (deprecatedSince 5.0 in identity)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=17)
    public Map<String,Object> unsetPrefMailSignature(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailSignature, "");
        return attrs;
    }

    /**
     * mail signature enabled (deprecatedSince 5.0 in identity)
     *
     * @return zimbraPrefMailSignatureEnabled, or false if unset
     */
    @ZAttr(id=18)
    public boolean isPrefMailSignatureEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefMailSignatureEnabled, false);
    }

    /**
     * mail signature enabled (deprecatedSince 5.0 in identity)
     *
     * @param zimbraPrefMailSignatureEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=18)
    public Map<String,Object> setPrefMailSignatureEnabled(boolean zimbraPrefMailSignatureEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailSignatureEnabled, Boolean.toString(zimbraPrefMailSignatureEnabled));
        return attrs;
    }

    /**
     * mail signature enabled (deprecatedSince 5.0 in identity)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=18)
    public Map<String,Object> unsetPrefMailSignatureEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailSignatureEnabled, "");
        return attrs;
    }

    /**
     * mail html signature
     *
     * @return zimbraPrefMailSignatureHTML, or null unset
     */
    @ZAttr(id=516)
    public String getPrefMailSignatureHTML() {
        return getAttr(Provisioning.A_zimbraPrefMailSignatureHTML);
    }

    /**
     * mail html signature
     *
     * @param zimbraPrefMailSignatureHTML new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=516)
    public Map<String,Object> setPrefMailSignatureHTML(String zimbraPrefMailSignatureHTML, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailSignatureHTML, zimbraPrefMailSignatureHTML);
        return attrs;
    }

    /**
     * mail html signature
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=516)
    public Map<String,Object> unsetPrefMailSignatureHTML(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailSignatureHTML, "");
        return attrs;
    }

    /**
     * mail signature style outlook|internet (deprecatedSince 5.0 in
     * identity)
     *
     * <p>Valid values: [outlook, internet]
     *
     * @return zimbraPrefMailSignatureStyle, or null if unset and/or has invalid value
     */
    @ZAttr(id=156)
    public ZAttrProvisioning.PrefMailSignatureStyle getPrefMailSignatureStyle() {
        try { String v = getAttr(Provisioning.A_zimbraPrefMailSignatureStyle); return v == null ? null : ZAttrProvisioning.PrefMailSignatureStyle.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * mail signature style outlook|internet (deprecatedSince 5.0 in
     * identity)
     *
     * <p>Valid values: [outlook, internet]
     *
     * @return zimbraPrefMailSignatureStyle, or null unset
     */
    @ZAttr(id=156)
    public String getPrefMailSignatureStyleAsString() {
        return getAttr(Provisioning.A_zimbraPrefMailSignatureStyle);
    }

    /**
     * mail signature style outlook|internet (deprecatedSince 5.0 in
     * identity)
     *
     * <p>Valid values: [outlook, internet]
     *
     * @param zimbraPrefMailSignatureStyle new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=156)
    public Map<String,Object> setPrefMailSignatureStyle(ZAttrProvisioning.PrefMailSignatureStyle zimbraPrefMailSignatureStyle, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailSignatureStyle, zimbraPrefMailSignatureStyle.toString());
        return attrs;
    }

    /**
     * mail signature style outlook|internet (deprecatedSince 5.0 in
     * identity)
     *
     * <p>Valid values: [outlook, internet]
     *
     * @param zimbraPrefMailSignatureStyle new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=156)
    public Map<String,Object> setPrefMailSignatureStyleAsString(String zimbraPrefMailSignatureStyle, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailSignatureStyle, zimbraPrefMailSignatureStyle);
        return attrs;
    }

    /**
     * mail signature style outlook|internet (deprecatedSince 5.0 in
     * identity)
     *
     * <p>Valid values: [outlook, internet]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=156)
    public Map<String,Object> unsetPrefMailSignatureStyle(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailSignatureStyle, "");
        return attrs;
    }

    /**
     * whether audible alert is enabled when a new email arrives
     *
     * @return zimbraPrefMailSoundsEnabled, or false if unset
     */
    @ZAttr(id=666)
    public boolean isPrefMailSoundsEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefMailSoundsEnabled, false);
    }

    /**
     * whether audible alert is enabled when a new email arrives
     *
     * @param zimbraPrefMailSoundsEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=666)
    public Map<String,Object> setPrefMailSoundsEnabled(boolean zimbraPrefMailSoundsEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailSoundsEnabled, Boolean.toString(zimbraPrefMailSoundsEnabled));
        return attrs;
    }

    /**
     * whether audible alert is enabled when a new email arrives
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=666)
    public Map<String,Object> unsetPrefMailSoundsEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMailSoundsEnabled, "");
        return attrs;
    }

    /**
     * whether mandatory spell check is enabled
     *
     * @return zimbraPrefMandatorySpellCheckEnabled, or false if unset
     *
     * @since ZCS future
     */
    @ZAttr(id=749)
    public boolean isPrefMandatorySpellCheckEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefMandatorySpellCheckEnabled, false);
    }

    /**
     * whether mandatory spell check is enabled
     *
     * @param zimbraPrefMandatorySpellCheckEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=749)
    public Map<String,Object> setPrefMandatorySpellCheckEnabled(boolean zimbraPrefMandatorySpellCheckEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMandatorySpellCheckEnabled, Boolean.toString(zimbraPrefMandatorySpellCheckEnabled));
        return attrs;
    }

    /**
     * whether mandatory spell check is enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     *
     * @since ZCS future
     */
    @ZAttr(id=749)
    public Map<String,Object> unsetPrefMandatorySpellCheckEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMandatorySpellCheckEnabled, "");
        return attrs;
    }

    /**
     * whether and mark a message as read -1: Do not mark read 0: Mark read
     * 1..n: Mark read after this many seconds
     *
     * @return zimbraPrefMarkMsgRead, or -1 if unset
     */
    @ZAttr(id=650)
    public int getPrefMarkMsgRead() {
        return getIntAttr(Provisioning.A_zimbraPrefMarkMsgRead, -1);
    }

    /**
     * whether and mark a message as read -1: Do not mark read 0: Mark read
     * 1..n: Mark read after this many seconds
     *
     * @param zimbraPrefMarkMsgRead new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=650)
    public Map<String,Object> setPrefMarkMsgRead(int zimbraPrefMarkMsgRead, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMarkMsgRead, Integer.toString(zimbraPrefMarkMsgRead));
        return attrs;
    }

    /**
     * whether and mark a message as read -1: Do not mark read 0: Mark read
     * 1..n: Mark read after this many seconds
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=650)
    public Map<String,Object> unsetPrefMarkMsgRead(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMarkMsgRead, "");
        return attrs;
    }

    /**
     * whether client prefers text/html or text/plain
     *
     * @return zimbraPrefMessageViewHtmlPreferred, or false if unset
     */
    @ZAttr(id=145)
    public boolean isPrefMessageViewHtmlPreferred() {
        return getBooleanAttr(Provisioning.A_zimbraPrefMessageViewHtmlPreferred, false);
    }

    /**
     * whether client prefers text/html or text/plain
     *
     * @param zimbraPrefMessageViewHtmlPreferred new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=145)
    public Map<String,Object> setPrefMessageViewHtmlPreferred(boolean zimbraPrefMessageViewHtmlPreferred, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMessageViewHtmlPreferred, Boolean.toString(zimbraPrefMessageViewHtmlPreferred));
        return attrs;
    }

    /**
     * whether client prefers text/html or text/plain
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=145)
    public Map<String,Object> unsetPrefMessageViewHtmlPreferred(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefMessageViewHtmlPreferred, "");
        return attrs;
    }

    /**
     * RFC822 email address for email notifications
     *
     * @return zimbraPrefNewMailNotificationAddress, or null unset
     */
    @ZAttr(id=127)
    public String getPrefNewMailNotificationAddress() {
        return getAttr(Provisioning.A_zimbraPrefNewMailNotificationAddress);
    }

    /**
     * RFC822 email address for email notifications
     *
     * @param zimbraPrefNewMailNotificationAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=127)
    public Map<String,Object> setPrefNewMailNotificationAddress(String zimbraPrefNewMailNotificationAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefNewMailNotificationAddress, zimbraPrefNewMailNotificationAddress);
        return attrs;
    }

    /**
     * RFC822 email address for email notifications
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=127)
    public Map<String,Object> unsetPrefNewMailNotificationAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefNewMailNotificationAddress, "");
        return attrs;
    }

    /**
     * whether or not new mail notification is enabled
     *
     * @return zimbraPrefNewMailNotificationEnabled, or false if unset
     */
    @ZAttr(id=126)
    public boolean isPrefNewMailNotificationEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefNewMailNotificationEnabled, false);
    }

    /**
     * whether or not new mail notification is enabled
     *
     * @param zimbraPrefNewMailNotificationEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=126)
    public Map<String,Object> setPrefNewMailNotificationEnabled(boolean zimbraPrefNewMailNotificationEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefNewMailNotificationEnabled, Boolean.toString(zimbraPrefNewMailNotificationEnabled));
        return attrs;
    }

    /**
     * whether or not new mail notification is enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=126)
    public Map<String,Object> unsetPrefNewMailNotificationEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefNewMailNotificationEnabled, "");
        return attrs;
    }

    /**
     * whether or not the client opens a new msg/conv in a new window (via
     * dbl-click)
     *
     * @return zimbraPrefOpenMailInNewWindow, or false if unset
     */
    @ZAttr(id=500)
    public boolean isPrefOpenMailInNewWindow() {
        return getBooleanAttr(Provisioning.A_zimbraPrefOpenMailInNewWindow, false);
    }

    /**
     * whether or not the client opens a new msg/conv in a new window (via
     * dbl-click)
     *
     * @param zimbraPrefOpenMailInNewWindow new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=500)
    public Map<String,Object> setPrefOpenMailInNewWindow(boolean zimbraPrefOpenMailInNewWindow, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefOpenMailInNewWindow, Boolean.toString(zimbraPrefOpenMailInNewWindow));
        return attrs;
    }

    /**
     * whether or not the client opens a new msg/conv in a new window (via
     * dbl-click)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=500)
    public Map<String,Object> unsetPrefOpenMailInNewWindow(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefOpenMailInNewWindow, "");
        return attrs;
    }

    /**
     * server remembers addresses to which notifications have been sent for
     * this interval, and does not send duplicate notifications in this
     * interval
     *
     * <p>Use getPrefOutOfOfficeCacheDurationAsString to access value as a string.
     *
     * @see #getPrefOutOfOfficeCacheDurationAsString()
     *
     * @return zimbraPrefOutOfOfficeCacheDuration in millseconds, or -1 if unset
     */
    @ZAttr(id=386)
    public long getPrefOutOfOfficeCacheDuration() {
        return getTimeInterval(Provisioning.A_zimbraPrefOutOfOfficeCacheDuration, -1);
    }

    /**
     * server remembers addresses to which notifications have been sent for
     * this interval, and does not send duplicate notifications in this
     * interval
     *
     * @return zimbraPrefOutOfOfficeCacheDuration, or null unset
     */
    @ZAttr(id=386)
    public String getPrefOutOfOfficeCacheDurationAsString() {
        return getAttr(Provisioning.A_zimbraPrefOutOfOfficeCacheDuration);
    }

    /**
     * server remembers addresses to which notifications have been sent for
     * this interval, and does not send duplicate notifications in this
     * interval
     *
     * @param zimbraPrefOutOfOfficeCacheDuration new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=386)
    public Map<String,Object> setPrefOutOfOfficeCacheDuration(String zimbraPrefOutOfOfficeCacheDuration, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeCacheDuration, zimbraPrefOutOfOfficeCacheDuration);
        return attrs;
    }

    /**
     * server remembers addresses to which notifications have been sent for
     * this interval, and does not send duplicate notifications in this
     * interval
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=386)
    public Map<String,Object> unsetPrefOutOfOfficeCacheDuration(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeCacheDuration, "");
        return attrs;
    }

    /**
     * per RFC 3834 no out of office notifications are sent if recipients
     * address is not directly specified in the To/CC headers - for this
     * check, we check to see if To/CC contained accounts address, aliases,
     * canonical address. But when external accounts are forwarded to Zimbra,
     * and you want notifications sent to messages that contain their
     * external address in To/Cc, add those address, then you can specify
     * those external addresses here.
     *
     * @return zimbraPrefOutOfOfficeDirectAddress, or ampty array if unset
     */
    @ZAttr(id=387)
    public String[] getPrefOutOfOfficeDirectAddress() {
        return getMultiAttr(Provisioning.A_zimbraPrefOutOfOfficeDirectAddress);
    }

    /**
     * per RFC 3834 no out of office notifications are sent if recipients
     * address is not directly specified in the To/CC headers - for this
     * check, we check to see if To/CC contained accounts address, aliases,
     * canonical address. But when external accounts are forwarded to Zimbra,
     * and you want notifications sent to messages that contain their
     * external address in To/Cc, add those address, then you can specify
     * those external addresses here.
     *
     * @param zimbraPrefOutOfOfficeDirectAddress new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=387)
    public Map<String,Object> setPrefOutOfOfficeDirectAddress(String[] zimbraPrefOutOfOfficeDirectAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeDirectAddress, zimbraPrefOutOfOfficeDirectAddress);
        return attrs;
    }

    /**
     * per RFC 3834 no out of office notifications are sent if recipients
     * address is not directly specified in the To/CC headers - for this
     * check, we check to see if To/CC contained accounts address, aliases,
     * canonical address. But when external accounts are forwarded to Zimbra,
     * and you want notifications sent to messages that contain their
     * external address in To/Cc, add those address, then you can specify
     * those external addresses here.
     *
     * @param zimbraPrefOutOfOfficeDirectAddress new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=387)
    public Map<String,Object> addPrefOutOfOfficeDirectAddress(String zimbraPrefOutOfOfficeDirectAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraPrefOutOfOfficeDirectAddress, zimbraPrefOutOfOfficeDirectAddress);
        return attrs;
    }

    /**
     * per RFC 3834 no out of office notifications are sent if recipients
     * address is not directly specified in the To/CC headers - for this
     * check, we check to see if To/CC contained accounts address, aliases,
     * canonical address. But when external accounts are forwarded to Zimbra,
     * and you want notifications sent to messages that contain their
     * external address in To/Cc, add those address, then you can specify
     * those external addresses here.
     *
     * @param zimbraPrefOutOfOfficeDirectAddress existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=387)
    public Map<String,Object> removePrefOutOfOfficeDirectAddress(String zimbraPrefOutOfOfficeDirectAddress, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraPrefOutOfOfficeDirectAddress, zimbraPrefOutOfOfficeDirectAddress);
        return attrs;
    }

    /**
     * per RFC 3834 no out of office notifications are sent if recipients
     * address is not directly specified in the To/CC headers - for this
     * check, we check to see if To/CC contained accounts address, aliases,
     * canonical address. But when external accounts are forwarded to Zimbra,
     * and you want notifications sent to messages that contain their
     * external address in To/Cc, add those address, then you can specify
     * those external addresses here.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=387)
    public Map<String,Object> unsetPrefOutOfOfficeDirectAddress(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeDirectAddress, "");
        return attrs;
    }

    /**
     * out of office notifications (if enabled) are sent only if current date
     * is after this date
     *
     * <p>Use getPrefOutOfOfficeFromDateAsString to access value as a string.
     *
     * @see #getPrefOutOfOfficeFromDateAsString()
     *
     * @return zimbraPrefOutOfOfficeFromDate as Date, null if unset or unable to parse
     */
    @ZAttr(id=384)
    public Date getPrefOutOfOfficeFromDate() {
        return getGeneralizedTimeAttr(Provisioning.A_zimbraPrefOutOfOfficeFromDate, null);
    }

    /**
     * out of office notifications (if enabled) are sent only if current date
     * is after this date
     *
     * @return zimbraPrefOutOfOfficeFromDate, or null unset
     */
    @ZAttr(id=384)
    public String getPrefOutOfOfficeFromDateAsString() {
        return getAttr(Provisioning.A_zimbraPrefOutOfOfficeFromDate);
    }

    /**
     * out of office notifications (if enabled) are sent only if current date
     * is after this date
     *
     * @param zimbraPrefOutOfOfficeFromDate new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=384)
    public Map<String,Object> setPrefOutOfOfficeFromDate(Date zimbraPrefOutOfOfficeFromDate, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeFromDate, DateUtil.toGeneralizedTime(zimbraPrefOutOfOfficeFromDate));
        return attrs;
    }

    /**
     * out of office notifications (if enabled) are sent only if current date
     * is after this date
     *
     * @param zimbraPrefOutOfOfficeFromDate new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=384)
    public Map<String,Object> setPrefOutOfOfficeFromDateAsString(String zimbraPrefOutOfOfficeFromDate, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeFromDate, zimbraPrefOutOfOfficeFromDate);
        return attrs;
    }

    /**
     * out of office notifications (if enabled) are sent only if current date
     * is after this date
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=384)
    public Map<String,Object> unsetPrefOutOfOfficeFromDate(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeFromDate, "");
        return attrs;
    }

    /**
     * out of office message
     *
     * @return zimbraPrefOutOfOfficeReply, or null unset
     */
    @ZAttr(id=58)
    public String getPrefOutOfOfficeReply() {
        return getAttr(Provisioning.A_zimbraPrefOutOfOfficeReply);
    }

    /**
     * out of office message
     *
     * @param zimbraPrefOutOfOfficeReply new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=58)
    public Map<String,Object> setPrefOutOfOfficeReply(String zimbraPrefOutOfOfficeReply, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeReply, zimbraPrefOutOfOfficeReply);
        return attrs;
    }

    /**
     * out of office message
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=58)
    public Map<String,Object> unsetPrefOutOfOfficeReply(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeReply, "");
        return attrs;
    }

    /**
     * whether or not out of office reply is enabled
     *
     * @return zimbraPrefOutOfOfficeReplyEnabled, or false if unset
     */
    @ZAttr(id=59)
    public boolean isPrefOutOfOfficeReplyEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefOutOfOfficeReplyEnabled, false);
    }

    /**
     * whether or not out of office reply is enabled
     *
     * @param zimbraPrefOutOfOfficeReplyEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=59)
    public Map<String,Object> setPrefOutOfOfficeReplyEnabled(boolean zimbraPrefOutOfOfficeReplyEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeReplyEnabled, Boolean.toString(zimbraPrefOutOfOfficeReplyEnabled));
        return attrs;
    }

    /**
     * whether or not out of office reply is enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=59)
    public Map<String,Object> unsetPrefOutOfOfficeReplyEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeReplyEnabled, "");
        return attrs;
    }

    /**
     * out of office notifications (if enabled) are sent only if current date
     * is before this date
     *
     * <p>Use getPrefOutOfOfficeUntilDateAsString to access value as a string.
     *
     * @see #getPrefOutOfOfficeUntilDateAsString()
     *
     * @return zimbraPrefOutOfOfficeUntilDate as Date, null if unset or unable to parse
     */
    @ZAttr(id=385)
    public Date getPrefOutOfOfficeUntilDate() {
        return getGeneralizedTimeAttr(Provisioning.A_zimbraPrefOutOfOfficeUntilDate, null);
    }

    /**
     * out of office notifications (if enabled) are sent only if current date
     * is before this date
     *
     * @return zimbraPrefOutOfOfficeUntilDate, or null unset
     */
    @ZAttr(id=385)
    public String getPrefOutOfOfficeUntilDateAsString() {
        return getAttr(Provisioning.A_zimbraPrefOutOfOfficeUntilDate);
    }

    /**
     * out of office notifications (if enabled) are sent only if current date
     * is before this date
     *
     * @param zimbraPrefOutOfOfficeUntilDate new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=385)
    public Map<String,Object> setPrefOutOfOfficeUntilDate(Date zimbraPrefOutOfOfficeUntilDate, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeUntilDate, DateUtil.toGeneralizedTime(zimbraPrefOutOfOfficeUntilDate));
        return attrs;
    }

    /**
     * out of office notifications (if enabled) are sent only if current date
     * is before this date
     *
     * @param zimbraPrefOutOfOfficeUntilDate new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=385)
    public Map<String,Object> setPrefOutOfOfficeUntilDateAsString(String zimbraPrefOutOfOfficeUntilDate, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeUntilDate, zimbraPrefOutOfOfficeUntilDate);
        return attrs;
    }

    /**
     * out of office notifications (if enabled) are sent only if current date
     * is before this date
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=385)
    public Map<String,Object> unsetPrefOutOfOfficeUntilDate(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefOutOfOfficeUntilDate, "");
        return attrs;
    }

    /**
     * download pop3 messages since
     *
     * <p>Use getPrefPop3DownloadSinceAsString to access value as a string.
     *
     * @see #getPrefPop3DownloadSinceAsString()
     *
     * @return zimbraPrefPop3DownloadSince as Date, null if unset or unable to parse
     */
    @ZAttr(id=653)
    public Date getPrefPop3DownloadSince() {
        return getGeneralizedTimeAttr(Provisioning.A_zimbraPrefPop3DownloadSince, null);
    }

    /**
     * download pop3 messages since
     *
     * @return zimbraPrefPop3DownloadSince, or null unset
     */
    @ZAttr(id=653)
    public String getPrefPop3DownloadSinceAsString() {
        return getAttr(Provisioning.A_zimbraPrefPop3DownloadSince);
    }

    /**
     * download pop3 messages since
     *
     * @param zimbraPrefPop3DownloadSince new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=653)
    public Map<String,Object> setPrefPop3DownloadSince(Date zimbraPrefPop3DownloadSince, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefPop3DownloadSince, DateUtil.toGeneralizedTime(zimbraPrefPop3DownloadSince));
        return attrs;
    }

    /**
     * download pop3 messages since
     *
     * @param zimbraPrefPop3DownloadSince new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=653)
    public Map<String,Object> setPrefPop3DownloadSinceAsString(String zimbraPrefPop3DownloadSince, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefPop3DownloadSince, zimbraPrefPop3DownloadSince);
        return attrs;
    }

    /**
     * download pop3 messages since
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=653)
    public Map<String,Object> unsetPrefPop3DownloadSince(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefPop3DownloadSince, "");
        return attrs;
    }

    /**
     * whether reading pane is shown by default
     *
     * @return zimbraPrefReadingPaneEnabled, or false if unset
     */
    @ZAttr(id=394)
    public boolean isPrefReadingPaneEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefReadingPaneEnabled, false);
    }

    /**
     * whether reading pane is shown by default
     *
     * @param zimbraPrefReadingPaneEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=394)
    public Map<String,Object> setPrefReadingPaneEnabled(boolean zimbraPrefReadingPaneEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefReadingPaneEnabled, Boolean.toString(zimbraPrefReadingPaneEnabled));
        return attrs;
    }

    /**
     * whether reading pane is shown by default
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=394)
    public Map<String,Object> unsetPrefReadingPaneEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefReadingPaneEnabled, "");
        return attrs;
    }

    /**
     * what part of the original message to include during replies
     * (deprecatedSince 5.0 in identity)
     *
     * <p>Valid values: [includeAsAttachment, includeNone, includeBodyAndHeadersWithPrefix, includeSmart, includeBody, includeBodyWithPrefix]
     *
     * @return zimbraPrefReplyIncludeOriginalText, or null if unset and/or has invalid value
     */
    @ZAttr(id=133)
    public ZAttrProvisioning.PrefReplyIncludeOriginalText getPrefReplyIncludeOriginalText() {
        try { String v = getAttr(Provisioning.A_zimbraPrefReplyIncludeOriginalText); return v == null ? null : ZAttrProvisioning.PrefReplyIncludeOriginalText.fromString(v); } catch(com.zimbra.common.service.ServiceException e) { return null; }
    }

    /**
     * what part of the original message to include during replies
     * (deprecatedSince 5.0 in identity)
     *
     * <p>Valid values: [includeAsAttachment, includeNone, includeBodyAndHeadersWithPrefix, includeSmart, includeBody, includeBodyWithPrefix]
     *
     * @return zimbraPrefReplyIncludeOriginalText, or null unset
     */
    @ZAttr(id=133)
    public String getPrefReplyIncludeOriginalTextAsString() {
        return getAttr(Provisioning.A_zimbraPrefReplyIncludeOriginalText);
    }

    /**
     * what part of the original message to include during replies
     * (deprecatedSince 5.0 in identity)
     *
     * <p>Valid values: [includeAsAttachment, includeNone, includeBodyAndHeadersWithPrefix, includeSmart, includeBody, includeBodyWithPrefix]
     *
     * @param zimbraPrefReplyIncludeOriginalText new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=133)
    public Map<String,Object> setPrefReplyIncludeOriginalText(ZAttrProvisioning.PrefReplyIncludeOriginalText zimbraPrefReplyIncludeOriginalText, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefReplyIncludeOriginalText, zimbraPrefReplyIncludeOriginalText.toString());
        return attrs;
    }

    /**
     * what part of the original message to include during replies
     * (deprecatedSince 5.0 in identity)
     *
     * <p>Valid values: [includeAsAttachment, includeNone, includeBodyAndHeadersWithPrefix, includeSmart, includeBody, includeBodyWithPrefix]
     *
     * @param zimbraPrefReplyIncludeOriginalText new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=133)
    public Map<String,Object> setPrefReplyIncludeOriginalTextAsString(String zimbraPrefReplyIncludeOriginalText, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefReplyIncludeOriginalText, zimbraPrefReplyIncludeOriginalText);
        return attrs;
    }

    /**
     * what part of the original message to include during replies
     * (deprecatedSince 5.0 in identity)
     *
     * <p>Valid values: [includeAsAttachment, includeNone, includeBodyAndHeadersWithPrefix, includeSmart, includeBody, includeBodyWithPrefix]
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=133)
    public Map<String,Object> unsetPrefReplyIncludeOriginalText(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefReplyIncludeOriginalText, "");
        return attrs;
    }

    /**
     * address to put in reply-to header
     *
     * @return zimbraPrefReplyToAddress, or null unset
     */
    @ZAttr(id=60)
    public String getPrefReplyToAddress() {
        return getAttr(Provisioning.A_zimbraPrefReplyToAddress);
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
     * @return zimbraPrefReplyToDisplay, or null unset
     */
    @ZAttr(id=404)
    public String getPrefReplyToDisplay() {
        return getAttr(Provisioning.A_zimbraPrefReplyToDisplay);
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
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=405)
    public Map<String,Object> setPrefReplyToEnabled(boolean zimbraPrefReplyToEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefReplyToEnabled, Boolean.toString(zimbraPrefReplyToEnabled));
        return attrs;
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

    /**
     * whether or not to save outgoing mail (deprecatedSince 5.0 in identity)
     *
     * @return zimbraPrefSaveToSent, or false if unset
     */
    @ZAttr(id=22)
    public boolean isPrefSaveToSent() {
        return getBooleanAttr(Provisioning.A_zimbraPrefSaveToSent, false);
    }

    /**
     * whether or not to save outgoing mail (deprecatedSince 5.0 in identity)
     *
     * @param zimbraPrefSaveToSent new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=22)
    public Map<String,Object> setPrefSaveToSent(boolean zimbraPrefSaveToSent, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefSaveToSent, Boolean.toString(zimbraPrefSaveToSent));
        return attrs;
    }

    /**
     * whether or not to save outgoing mail (deprecatedSince 5.0 in identity)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=22)
    public Map<String,Object> unsetPrefSaveToSent(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefSaveToSent, "");
        return attrs;
    }

    /**
     * whether or not search tree is expanded
     *
     * @return zimbraPrefSearchTreeOpen, or false if unset
     */
    @ZAttr(id=634)
    public boolean isPrefSearchTreeOpen() {
        return getBooleanAttr(Provisioning.A_zimbraPrefSearchTreeOpen, false);
    }

    /**
     * whether or not search tree is expanded
     *
     * @param zimbraPrefSearchTreeOpen new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=634)
    public Map<String,Object> setPrefSearchTreeOpen(boolean zimbraPrefSearchTreeOpen, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefSearchTreeOpen, Boolean.toString(zimbraPrefSearchTreeOpen));
        return attrs;
    }

    /**
     * whether or not search tree is expanded
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=634)
    public Map<String,Object> unsetPrefSearchTreeOpen(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefSearchTreeOpen, "");
        return attrs;
    }

    /**
     * Retention period of messages in the Sent folder. 0 means that all
     * messages will be retained.
     *
     * <p>Use getPrefSentLifetimeAsString to access value as a string.
     *
     * @see #getPrefSentLifetimeAsString()
     *
     * @return zimbraPrefSentLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=539)
    public long getPrefSentLifetime() {
        return getTimeInterval(Provisioning.A_zimbraPrefSentLifetime, -1);
    }

    /**
     * Retention period of messages in the Sent folder. 0 means that all
     * messages will be retained.
     *
     * @return zimbraPrefSentLifetime, or null unset
     */
    @ZAttr(id=539)
    public String getPrefSentLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraPrefSentLifetime);
    }

    /**
     * Retention period of messages in the Sent folder. 0 means that all
     * messages will be retained.
     *
     * @param zimbraPrefSentLifetime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=539)
    public Map<String,Object> setPrefSentLifetime(String zimbraPrefSentLifetime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefSentLifetime, zimbraPrefSentLifetime);
        return attrs;
    }

    /**
     * Retention period of messages in the Sent folder. 0 means that all
     * messages will be retained.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=539)
    public Map<String,Object> unsetPrefSentLifetime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefSentLifetime, "");
        return attrs;
    }

    /**
     * name of folder to save sent mail in (deprecatedSince 5.0 in identity)
     *
     * @return zimbraPrefSentMailFolder, or null unset
     */
    @ZAttr(id=103)
    public String getPrefSentMailFolder() {
        return getAttr(Provisioning.A_zimbraPrefSentMailFolder);
    }

    /**
     * name of folder to save sent mail in (deprecatedSince 5.0 in identity)
     *
     * @param zimbraPrefSentMailFolder new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=103)
    public Map<String,Object> setPrefSentMailFolder(String zimbraPrefSentMailFolder, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefSentMailFolder, zimbraPrefSentMailFolder);
        return attrs;
    }

    /**
     * name of folder to save sent mail in (deprecatedSince 5.0 in identity)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=103)
    public Map<String,Object> unsetPrefSentMailFolder(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefSentMailFolder, "");
        return attrs;
    }

    /**
     * keyboard shortcuts
     *
     * @return zimbraPrefShortcuts, or null unset
     */
    @ZAttr(id=396)
    public String getPrefShortcuts() {
        return getAttr(Provisioning.A_zimbraPrefShortcuts);
    }

    /**
     * keyboard shortcuts
     *
     * @param zimbraPrefShortcuts new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=396)
    public Map<String,Object> setPrefShortcuts(String zimbraPrefShortcuts, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefShortcuts, zimbraPrefShortcuts);
        return attrs;
    }

    /**
     * keyboard shortcuts
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=396)
    public Map<String,Object> unsetPrefShortcuts(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefShortcuts, "");
        return attrs;
    }

    /**
     * show fragments in conversation and message lists
     *
     * @return zimbraPrefShowFragments, or false if unset
     */
    @ZAttr(id=192)
    public boolean isPrefShowFragments() {
        return getBooleanAttr(Provisioning.A_zimbraPrefShowFragments, false);
    }

    /**
     * show fragments in conversation and message lists
     *
     * @param zimbraPrefShowFragments new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=192)
    public Map<String,Object> setPrefShowFragments(boolean zimbraPrefShowFragments, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefShowFragments, Boolean.toString(zimbraPrefShowFragments));
        return attrs;
    }

    /**
     * show fragments in conversation and message lists
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=192)
    public Map<String,Object> unsetPrefShowFragments(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefShowFragments, "");
        return attrs;
    }

    /**
     * whether to show search box or not
     *
     * @return zimbraPrefShowSearchString, or false if unset
     */
    @ZAttr(id=222)
    public boolean isPrefShowSearchString() {
        return getBooleanAttr(Provisioning.A_zimbraPrefShowSearchString, false);
    }

    /**
     * whether to show search box or not
     *
     * @param zimbraPrefShowSearchString new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=222)
    public Map<String,Object> setPrefShowSearchString(boolean zimbraPrefShowSearchString, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefShowSearchString, Boolean.toString(zimbraPrefShowSearchString));
        return attrs;
    }

    /**
     * whether to show search box or not
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=222)
    public Map<String,Object> unsetPrefShowSearchString(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefShowSearchString, "");
        return attrs;
    }

    /**
     * show selection checkbox for selecting email, contact, voicemial items
     * in a list view for batch operations
     *
     * @return zimbraPrefShowSelectionCheckbox, or false if unset
     */
    @ZAttr(id=471)
    public boolean isPrefShowSelectionCheckbox() {
        return getBooleanAttr(Provisioning.A_zimbraPrefShowSelectionCheckbox, false);
    }

    /**
     * show selection checkbox for selecting email, contact, voicemial items
     * in a list view for batch operations
     *
     * @param zimbraPrefShowSelectionCheckbox new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=471)
    public Map<String,Object> setPrefShowSelectionCheckbox(boolean zimbraPrefShowSelectionCheckbox, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefShowSelectionCheckbox, Boolean.toString(zimbraPrefShowSelectionCheckbox));
        return attrs;
    }

    /**
     * show selection checkbox for selecting email, contact, voicemial items
     * in a list view for batch operations
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=471)
    public Map<String,Object> unsetPrefShowSelectionCheckbox(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefShowSelectionCheckbox, "");
        return attrs;
    }

    /**
     * Skin to use for this account
     *
     * @return zimbraPrefSkin, or null unset
     */
    @ZAttr(id=355)
    public String getPrefSkin() {
        return getAttr(Provisioning.A_zimbraPrefSkin);
    }

    /**
     * Skin to use for this account
     *
     * @param zimbraPrefSkin new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=355)
    public Map<String,Object> setPrefSkin(String zimbraPrefSkin, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefSkin, zimbraPrefSkin);
        return attrs;
    }

    /**
     * Skin to use for this account
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=355)
    public Map<String,Object> unsetPrefSkin(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefSkin, "");
        return attrs;
    }

    /**
     * whether standard client should operate in accessilbity Mode
     *
     * @return zimbraPrefStandardClientAccessilbityMode, or false if unset
     */
    @ZAttr(id=689)
    public boolean isPrefStandardClientAccessilbityMode() {
        return getBooleanAttr(Provisioning.A_zimbraPrefStandardClientAccessilbityMode, false);
    }

    /**
     * whether standard client should operate in accessilbity Mode
     *
     * @param zimbraPrefStandardClientAccessilbityMode new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=689)
    public Map<String,Object> setPrefStandardClientAccessilbityMode(boolean zimbraPrefStandardClientAccessilbityMode, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefStandardClientAccessilbityMode, Boolean.toString(zimbraPrefStandardClientAccessilbityMode));
        return attrs;
    }

    /**
     * whether standard client should operate in accessilbity Mode
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=689)
    public Map<String,Object> unsetPrefStandardClientAccessilbityMode(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefStandardClientAccessilbityMode, "");
        return attrs;
    }

    /**
     * whether or not tag tree is expanded
     *
     * @return zimbraPrefTagTreeOpen, or false if unset
     */
    @ZAttr(id=633)
    public boolean isPrefTagTreeOpen() {
        return getBooleanAttr(Provisioning.A_zimbraPrefTagTreeOpen, false);
    }

    /**
     * whether or not tag tree is expanded
     *
     * @param zimbraPrefTagTreeOpen new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=633)
    public Map<String,Object> setPrefTagTreeOpen(boolean zimbraPrefTagTreeOpen, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefTagTreeOpen, Boolean.toString(zimbraPrefTagTreeOpen));
        return attrs;
    }

    /**
     * whether or not tag tree is expanded
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=633)
    public Map<String,Object> unsetPrefTagTreeOpen(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefTagTreeOpen, "");
        return attrs;
    }

    /**
     * time zone of user or COS
     *
     * @return zimbraPrefTimeZoneId, or ampty array if unset
     */
    @ZAttr(id=235)
    public String[] getPrefTimeZoneId() {
        return getMultiAttr(Provisioning.A_zimbraPrefTimeZoneId);
    }

    /**
     * time zone of user or COS
     *
     * @param zimbraPrefTimeZoneId new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=235)
    public Map<String,Object> setPrefTimeZoneId(String[] zimbraPrefTimeZoneId, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefTimeZoneId, zimbraPrefTimeZoneId);
        return attrs;
    }

    /**
     * time zone of user or COS
     *
     * @param zimbraPrefTimeZoneId new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=235)
    public Map<String,Object> addPrefTimeZoneId(String zimbraPrefTimeZoneId, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraPrefTimeZoneId, zimbraPrefTimeZoneId);
        return attrs;
    }

    /**
     * time zone of user or COS
     *
     * @param zimbraPrefTimeZoneId existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=235)
    public Map<String,Object> removePrefTimeZoneId(String zimbraPrefTimeZoneId, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraPrefTimeZoneId, zimbraPrefTimeZoneId);
        return attrs;
    }

    /**
     * time zone of user or COS
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=235)
    public Map<String,Object> unsetPrefTimeZoneId(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefTimeZoneId, "");
        return attrs;
    }

    /**
     * Retention period of messages in the Trash folder. 0 means that all
     * messages will be retained. This user-modifiable attribute works in
     * conjunction with zimbraMailTrashLifetime, which is admin-modifiable.
     * The shorter duration is used.
     *
     * <p>Use getPrefTrashLifetimeAsString to access value as a string.
     *
     * @see #getPrefTrashLifetimeAsString()
     *
     * @return zimbraPrefTrashLifetime in millseconds, or -1 if unset
     */
    @ZAttr(id=541)
    public long getPrefTrashLifetime() {
        return getTimeInterval(Provisioning.A_zimbraPrefTrashLifetime, -1);
    }

    /**
     * Retention period of messages in the Trash folder. 0 means that all
     * messages will be retained. This user-modifiable attribute works in
     * conjunction with zimbraMailTrashLifetime, which is admin-modifiable.
     * The shorter duration is used.
     *
     * @return zimbraPrefTrashLifetime, or null unset
     */
    @ZAttr(id=541)
    public String getPrefTrashLifetimeAsString() {
        return getAttr(Provisioning.A_zimbraPrefTrashLifetime);
    }

    /**
     * Retention period of messages in the Trash folder. 0 means that all
     * messages will be retained. This user-modifiable attribute works in
     * conjunction with zimbraMailTrashLifetime, which is admin-modifiable.
     * The shorter duration is used.
     *
     * @param zimbraPrefTrashLifetime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=541)
    public Map<String,Object> setPrefTrashLifetime(String zimbraPrefTrashLifetime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefTrashLifetime, zimbraPrefTrashLifetime);
        return attrs;
    }

    /**
     * Retention period of messages in the Trash folder. 0 means that all
     * messages will be retained. This user-modifiable attribute works in
     * conjunction with zimbraMailTrashLifetime, which is admin-modifiable.
     * The shorter duration is used.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=541)
    public Map<String,Object> unsetPrefTrashLifetime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefTrashLifetime, "");
        return attrs;
    }

    /**
     * Deprecated since: 5.0. no longer used in account or identity. Orig
     * desc: TRUE if we this identity should get settings from the default
     * identity
     *
     * @return zimbraPrefUseDefaultIdentitySettings, or false if unset
     */
    @ZAttr(id=410)
    public boolean isPrefUseDefaultIdentitySettings() {
        return getBooleanAttr(Provisioning.A_zimbraPrefUseDefaultIdentitySettings, false);
    }

    /**
     * Deprecated since: 5.0. no longer used in account or identity. Orig
     * desc: TRUE if we this identity should get settings from the default
     * identity
     *
     * @param zimbraPrefUseDefaultIdentitySettings new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=410)
    public Map<String,Object> setPrefUseDefaultIdentitySettings(boolean zimbraPrefUseDefaultIdentitySettings, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefUseDefaultIdentitySettings, Boolean.toString(zimbraPrefUseDefaultIdentitySettings));
        return attrs;
    }

    /**
     * Deprecated since: 5.0. no longer used in account or identity. Orig
     * desc: TRUE if we this identity should get settings from the default
     * identity
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=410)
    public Map<String,Object> unsetPrefUseDefaultIdentitySettings(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefUseDefaultIdentitySettings, "");
        return attrs;
    }

    /**
     * whether or not keyboard shortcuts are enabled
     *
     * @return zimbraPrefUseKeyboardShortcuts, or false if unset
     */
    @ZAttr(id=61)
    public boolean isPrefUseKeyboardShortcuts() {
        return getBooleanAttr(Provisioning.A_zimbraPrefUseKeyboardShortcuts, false);
    }

    /**
     * whether or not keyboard shortcuts are enabled
     *
     * @param zimbraPrefUseKeyboardShortcuts new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=61)
    public Map<String,Object> setPrefUseKeyboardShortcuts(boolean zimbraPrefUseKeyboardShortcuts, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefUseKeyboardShortcuts, Boolean.toString(zimbraPrefUseKeyboardShortcuts));
        return attrs;
    }

    /**
     * whether or not keyboard shortcuts are enabled
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=61)
    public Map<String,Object> unsetPrefUseKeyboardShortcuts(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefUseKeyboardShortcuts, "");
        return attrs;
    }

    /**
     * When composing and sending mail, whether to use RFC 2231 MIME
     * parameter value encoding. If set to FALSE, then RFC 2047 style
     * encoding is used.
     *
     * @return zimbraPrefUseRfc2231, or false if unset
     */
    @ZAttr(id=395)
    public boolean isPrefUseRfc2231() {
        return getBooleanAttr(Provisioning.A_zimbraPrefUseRfc2231, false);
    }

    /**
     * When composing and sending mail, whether to use RFC 2231 MIME
     * parameter value encoding. If set to FALSE, then RFC 2047 style
     * encoding is used.
     *
     * @param zimbraPrefUseRfc2231 new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=395)
    public Map<String,Object> setPrefUseRfc2231(boolean zimbraPrefUseRfc2231, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefUseRfc2231, Boolean.toString(zimbraPrefUseRfc2231));
        return attrs;
    }

    /**
     * When composing and sending mail, whether to use RFC 2231 MIME
     * parameter value encoding. If set to FALSE, then RFC 2047 style
     * encoding is used.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=395)
    public Map<String,Object> unsetPrefUseRfc2231(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefUseRfc2231, "");
        return attrs;
    }

    /**
     * whether list of well known time zones is displayed in calendar UI
     *
     * @return zimbraPrefUseTimeZoneListInCalendar, or false if unset
     */
    @ZAttr(id=236)
    public boolean isPrefUseTimeZoneListInCalendar() {
        return getBooleanAttr(Provisioning.A_zimbraPrefUseTimeZoneListInCalendar, false);
    }

    /**
     * whether list of well known time zones is displayed in calendar UI
     *
     * @param zimbraPrefUseTimeZoneListInCalendar new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=236)
    public Map<String,Object> setPrefUseTimeZoneListInCalendar(boolean zimbraPrefUseTimeZoneListInCalendar, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefUseTimeZoneListInCalendar, Boolean.toString(zimbraPrefUseTimeZoneListInCalendar));
        return attrs;
    }

    /**
     * whether list of well known time zones is displayed in calendar UI
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=236)
    public Map<String,Object> unsetPrefUseTimeZoneListInCalendar(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefUseTimeZoneListInCalendar, "");
        return attrs;
    }

    /**
     * number of voice messages/call logs per page
     *
     * @return zimbraPrefVoiceItemsPerPage, or -1 if unset
     */
    @ZAttr(id=526)
    public int getPrefVoiceItemsPerPage() {
        return getIntAttr(Provisioning.A_zimbraPrefVoiceItemsPerPage, -1);
    }

    /**
     * number of voice messages/call logs per page
     *
     * @param zimbraPrefVoiceItemsPerPage new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=526)
    public Map<String,Object> setPrefVoiceItemsPerPage(int zimbraPrefVoiceItemsPerPage, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefVoiceItemsPerPage, Integer.toString(zimbraPrefVoiceItemsPerPage));
        return attrs;
    }

    /**
     * number of voice messages/call logs per page
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=526)
    public Map<String,Object> unsetPrefVoiceItemsPerPage(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefVoiceItemsPerPage, "");
        return attrs;
    }

    /**
     * whether to display a warning when users try to navigate away from ZCS
     *
     * @return zimbraPrefWarnOnExit, or false if unset
     */
    @ZAttr(id=456)
    public boolean isPrefWarnOnExit() {
        return getBooleanAttr(Provisioning.A_zimbraPrefWarnOnExit, false);
    }

    /**
     * whether to display a warning when users try to navigate away from ZCS
     *
     * @param zimbraPrefWarnOnExit new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=456)
    public Map<String,Object> setPrefWarnOnExit(boolean zimbraPrefWarnOnExit, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefWarnOnExit, Boolean.toString(zimbraPrefWarnOnExit));
        return attrs;
    }

    /**
     * whether to display a warning when users try to navigate away from ZCS
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=456)
    public Map<String,Object> unsetPrefWarnOnExit(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefWarnOnExit, "");
        return attrs;
    }

    /**
     * if replying/forwarding a message in this folder, use this identity
     * (deprecatedSince 5.0 in account)
     *
     * @return zimbraPrefWhenInFolderIds, or ampty array if unset
     */
    @ZAttr(id=409)
    public String[] getPrefWhenInFolderIds() {
        return getMultiAttr(Provisioning.A_zimbraPrefWhenInFolderIds);
    }

    /**
     * if replying/forwarding a message in this folder, use this identity
     * (deprecatedSince 5.0 in account)
     *
     * @param zimbraPrefWhenInFolderIds new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=409)
    public Map<String,Object> setPrefWhenInFolderIds(String[] zimbraPrefWhenInFolderIds, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefWhenInFolderIds, zimbraPrefWhenInFolderIds);
        return attrs;
    }

    /**
     * if replying/forwarding a message in this folder, use this identity
     * (deprecatedSince 5.0 in account)
     *
     * @param zimbraPrefWhenInFolderIds new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=409)
    public Map<String,Object> addPrefWhenInFolderIds(String zimbraPrefWhenInFolderIds, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraPrefWhenInFolderIds, zimbraPrefWhenInFolderIds);
        return attrs;
    }

    /**
     * if replying/forwarding a message in this folder, use this identity
     * (deprecatedSince 5.0 in account)
     *
     * @param zimbraPrefWhenInFolderIds existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=409)
    public Map<String,Object> removePrefWhenInFolderIds(String zimbraPrefWhenInFolderIds, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraPrefWhenInFolderIds, zimbraPrefWhenInFolderIds);
        return attrs;
    }

    /**
     * if replying/forwarding a message in this folder, use this identity
     * (deprecatedSince 5.0 in account)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=409)
    public Map<String,Object> unsetPrefWhenInFolderIds(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefWhenInFolderIds, "");
        return attrs;
    }

    /**
     * TRUE if we should look at zimbraPrefWhenInFolderIds (deprecatedSince
     * 5.0 in account)
     *
     * @return zimbraPrefWhenInFoldersEnabled, or false if unset
     */
    @ZAttr(id=408)
    public boolean isPrefWhenInFoldersEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefWhenInFoldersEnabled, false);
    }

    /**
     * TRUE if we should look at zimbraPrefWhenInFolderIds (deprecatedSince
     * 5.0 in account)
     *
     * @param zimbraPrefWhenInFoldersEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=408)
    public Map<String,Object> setPrefWhenInFoldersEnabled(boolean zimbraPrefWhenInFoldersEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefWhenInFoldersEnabled, Boolean.toString(zimbraPrefWhenInFoldersEnabled));
        return attrs;
    }

    /**
     * TRUE if we should look at zimbraPrefWhenInFolderIds (deprecatedSince
     * 5.0 in account)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=408)
    public Map<String,Object> unsetPrefWhenInFoldersEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefWhenInFoldersEnabled, "");
        return attrs;
    }

    /**
     * addresses that we will look at to see if we should use an identity
     * (deprecatedSince 5.0 in account)
     *
     * @return zimbraPrefWhenSentToAddresses, or ampty array if unset
     */
    @ZAttr(id=407)
    public String[] getPrefWhenSentToAddresses() {
        return getMultiAttr(Provisioning.A_zimbraPrefWhenSentToAddresses);
    }

    /**
     * addresses that we will look at to see if we should use an identity
     * (deprecatedSince 5.0 in account)
     *
     * @param zimbraPrefWhenSentToAddresses new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=407)
    public Map<String,Object> setPrefWhenSentToAddresses(String[] zimbraPrefWhenSentToAddresses, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefWhenSentToAddresses, zimbraPrefWhenSentToAddresses);
        return attrs;
    }

    /**
     * addresses that we will look at to see if we should use an identity
     * (deprecatedSince 5.0 in account)
     *
     * @param zimbraPrefWhenSentToAddresses new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=407)
    public Map<String,Object> addPrefWhenSentToAddresses(String zimbraPrefWhenSentToAddresses, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraPrefWhenSentToAddresses, zimbraPrefWhenSentToAddresses);
        return attrs;
    }

    /**
     * addresses that we will look at to see if we should use an identity
     * (deprecatedSince 5.0 in account)
     *
     * @param zimbraPrefWhenSentToAddresses existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=407)
    public Map<String,Object> removePrefWhenSentToAddresses(String zimbraPrefWhenSentToAddresses, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraPrefWhenSentToAddresses, zimbraPrefWhenSentToAddresses);
        return attrs;
    }

    /**
     * addresses that we will look at to see if we should use an identity
     * (deprecatedSince 5.0 in account)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=407)
    public Map<String,Object> unsetPrefWhenSentToAddresses(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefWhenSentToAddresses, "");
        return attrs;
    }

    /**
     * TRUE if we should look at zimbraPrefWhenSentToAddresses
     * (deprecatedSince 5.0 in account)
     *
     * @return zimbraPrefWhenSentToEnabled, or false if unset
     */
    @ZAttr(id=406)
    public boolean isPrefWhenSentToEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraPrefWhenSentToEnabled, false);
    }

    /**
     * TRUE if we should look at zimbraPrefWhenSentToAddresses
     * (deprecatedSince 5.0 in account)
     *
     * @param zimbraPrefWhenSentToEnabled new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=406)
    public Map<String,Object> setPrefWhenSentToEnabled(boolean zimbraPrefWhenSentToEnabled, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefWhenSentToEnabled, Boolean.toString(zimbraPrefWhenSentToEnabled));
        return attrs;
    }

    /**
     * TRUE if we should look at zimbraPrefWhenSentToAddresses
     * (deprecatedSince 5.0 in account)
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=406)
    public Map<String,Object> unsetPrefWhenSentToEnabled(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefWhenSentToEnabled, "");
        return attrs;
    }

    /**
     * whether or not zimlet tree is expanded
     *
     * @return zimbraPrefZimletTreeOpen, or false if unset
     */
    @ZAttr(id=638)
    public boolean isPrefZimletTreeOpen() {
        return getBooleanAttr(Provisioning.A_zimbraPrefZimletTreeOpen, false);
    }

    /**
     * whether or not zimlet tree is expanded
     *
     * @param zimbraPrefZimletTreeOpen new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=638)
    public Map<String,Object> setPrefZimletTreeOpen(boolean zimbraPrefZimletTreeOpen, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefZimletTreeOpen, Boolean.toString(zimbraPrefZimletTreeOpen));
        return attrs;
    }

    /**
     * whether or not zimlet tree is expanded
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=638)
    public Map<String,Object> unsetPrefZimletTreeOpen(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraPrefZimletTreeOpen, "");
        return attrs;
    }

    /**
     * Allowed domains for Proxy servlet
     *
     * @return zimbraProxyAllowedDomains, or ampty array if unset
     */
    @ZAttr(id=294)
    public String[] getProxyAllowedDomains() {
        return getMultiAttr(Provisioning.A_zimbraProxyAllowedDomains);
    }

    /**
     * Allowed domains for Proxy servlet
     *
     * @param zimbraProxyAllowedDomains new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=294)
    public Map<String,Object> setProxyAllowedDomains(String[] zimbraProxyAllowedDomains, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraProxyAllowedDomains, zimbraProxyAllowedDomains);
        return attrs;
    }

    /**
     * Allowed domains for Proxy servlet
     *
     * @param zimbraProxyAllowedDomains new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=294)
    public Map<String,Object> addProxyAllowedDomains(String zimbraProxyAllowedDomains, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraProxyAllowedDomains, zimbraProxyAllowedDomains);
        return attrs;
    }

    /**
     * Allowed domains for Proxy servlet
     *
     * @param zimbraProxyAllowedDomains existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=294)
    public Map<String,Object> removeProxyAllowedDomains(String zimbraProxyAllowedDomains, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraProxyAllowedDomains, zimbraProxyAllowedDomains);
        return attrs;
    }

    /**
     * Allowed domains for Proxy servlet
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=294)
    public Map<String,Object> unsetProxyAllowedDomains(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraProxyAllowedDomains, "");
        return attrs;
    }

    /**
     * Last time a quota warning was sent.
     *
     * <p>Use getQuotaLastWarnTimeAsString to access value as a string.
     *
     * @see #getQuotaLastWarnTimeAsString()
     *
     * @return zimbraQuotaLastWarnTime as Date, null if unset or unable to parse
     */
    @ZAttr(id=484)
    public Date getQuotaLastWarnTime() {
        return getGeneralizedTimeAttr(Provisioning.A_zimbraQuotaLastWarnTime, null);
    }

    /**
     * Last time a quota warning was sent.
     *
     * @return zimbraQuotaLastWarnTime, or null unset
     */
    @ZAttr(id=484)
    public String getQuotaLastWarnTimeAsString() {
        return getAttr(Provisioning.A_zimbraQuotaLastWarnTime);
    }

    /**
     * Last time a quota warning was sent.
     *
     * @param zimbraQuotaLastWarnTime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=484)
    public Map<String,Object> setQuotaLastWarnTime(Date zimbraQuotaLastWarnTime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraQuotaLastWarnTime, DateUtil.toGeneralizedTime(zimbraQuotaLastWarnTime));
        return attrs;
    }

    /**
     * Last time a quota warning was sent.
     *
     * @param zimbraQuotaLastWarnTime new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=484)
    public Map<String,Object> setQuotaLastWarnTimeAsString(String zimbraQuotaLastWarnTime, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraQuotaLastWarnTime, zimbraQuotaLastWarnTime);
        return attrs;
    }

    /**
     * Last time a quota warning was sent.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=484)
    public Map<String,Object> unsetQuotaLastWarnTime(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraQuotaLastWarnTime, "");
        return attrs;
    }

    /**
     * Minimum duration of time between quota warnings.
     *
     * <p>Use getQuotaWarnIntervalAsString to access value as a string.
     *
     * @see #getQuotaWarnIntervalAsString()
     *
     * @return zimbraQuotaWarnInterval in millseconds, or -1 if unset
     */
    @ZAttr(id=485)
    public long getQuotaWarnInterval() {
        return getTimeInterval(Provisioning.A_zimbraQuotaWarnInterval, -1);
    }

    /**
     * Minimum duration of time between quota warnings.
     *
     * @return zimbraQuotaWarnInterval, or null unset
     */
    @ZAttr(id=485)
    public String getQuotaWarnIntervalAsString() {
        return getAttr(Provisioning.A_zimbraQuotaWarnInterval);
    }

    /**
     * Minimum duration of time between quota warnings.
     *
     * @param zimbraQuotaWarnInterval new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=485)
    public Map<String,Object> setQuotaWarnInterval(String zimbraQuotaWarnInterval, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraQuotaWarnInterval, zimbraQuotaWarnInterval);
        return attrs;
    }

    /**
     * Minimum duration of time between quota warnings.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=485)
    public Map<String,Object> unsetQuotaWarnInterval(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraQuotaWarnInterval, "");
        return attrs;
    }

    /**
     * Quota warning message template.
     *
     * @return zimbraQuotaWarnMessage, or null unset
     */
    @ZAttr(id=486)
    public String getQuotaWarnMessage() {
        return getAttr(Provisioning.A_zimbraQuotaWarnMessage);
    }

    /**
     * Quota warning message template.
     *
     * @param zimbraQuotaWarnMessage new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=486)
    public Map<String,Object> setQuotaWarnMessage(String zimbraQuotaWarnMessage, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraQuotaWarnMessage, zimbraQuotaWarnMessage);
        return attrs;
    }

    /**
     * Quota warning message template.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=486)
    public Map<String,Object> unsetQuotaWarnMessage(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraQuotaWarnMessage, "");
        return attrs;
    }

    /**
     * Threshold for quota warning messages.
     *
     * @return zimbraQuotaWarnPercent, or -1 if unset
     */
    @ZAttr(id=483)
    public int getQuotaWarnPercent() {
        return getIntAttr(Provisioning.A_zimbraQuotaWarnPercent, -1);
    }

    /**
     * Threshold for quota warning messages.
     *
     * @param zimbraQuotaWarnPercent new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=483)
    public Map<String,Object> setQuotaWarnPercent(int zimbraQuotaWarnPercent, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraQuotaWarnPercent, Integer.toString(zimbraQuotaWarnPercent));
        return attrs;
    }

    /**
     * Threshold for quota warning messages.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=483)
    public Map<String,Object> unsetQuotaWarnPercent(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraQuotaWarnPercent, "");
        return attrs;
    }

    /**
     * items an account has shared
     *
     * @return zimbraShareInfo, or ampty array if unset
     */
    @ZAttr(id=357)
    public String[] getShareInfo() {
        return getMultiAttr(Provisioning.A_zimbraShareInfo);
    }

    /**
     * items an account has shared
     *
     * @param zimbraShareInfo new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=357)
    public Map<String,Object> setShareInfo(String[] zimbraShareInfo, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraShareInfo, zimbraShareInfo);
        return attrs;
    }

    /**
     * items an account has shared
     *
     * @param zimbraShareInfo new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=357)
    public Map<String,Object> addShareInfo(String zimbraShareInfo, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraShareInfo, zimbraShareInfo);
        return attrs;
    }

    /**
     * items an account has shared
     *
     * @param zimbraShareInfo existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=357)
    public Map<String,Object> removeShareInfo(String zimbraShareInfo, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraShareInfo, zimbraShareInfo);
        return attrs;
    }

    /**
     * items an account has shared
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=357)
    public Map<String,Object> unsetShareInfo(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraShareInfo, "");
        return attrs;
    }

    /**
     * Unique ID for an signature
     *
     * @return zimbraSignatureId, or null unset
     */
    @ZAttr(id=490)
    public String getSignatureId() {
        return getAttr(Provisioning.A_zimbraSignatureId);
    }

    /**
     * Unique ID for an signature
     *
     * @param zimbraSignatureId new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=490)
    public Map<String,Object> setSignatureId(String zimbraSignatureId, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSignatureId, zimbraSignatureId);
        return attrs;
    }

    /**
     * Unique ID for an signature
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=490)
    public Map<String,Object> unsetSignatureId(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSignatureId, "");
        return attrs;
    }

    /**
     * maximum number of signatures allowed on an account
     *
     * @return zimbraSignatureMaxNumEntries, or -1 if unset
     */
    @ZAttr(id=493)
    public int getSignatureMaxNumEntries() {
        return getIntAttr(Provisioning.A_zimbraSignatureMaxNumEntries, -1);
    }

    /**
     * maximum number of signatures allowed on an account
     *
     * @param zimbraSignatureMaxNumEntries new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=493)
    public Map<String,Object> setSignatureMaxNumEntries(int zimbraSignatureMaxNumEntries, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSignatureMaxNumEntries, Integer.toString(zimbraSignatureMaxNumEntries));
        return attrs;
    }

    /**
     * maximum number of signatures allowed on an account
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=493)
    public Map<String,Object> unsetSignatureMaxNumEntries(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSignatureMaxNumEntries, "");
        return attrs;
    }

    /**
     * minimum number of signatures allowed on an account, this is only used
     * in the client
     *
     * @return zimbraSignatureMinNumEntries, or -1 if unset
     */
    @ZAttr(id=523)
    public int getSignatureMinNumEntries() {
        return getIntAttr(Provisioning.A_zimbraSignatureMinNumEntries, -1);
    }

    /**
     * minimum number of signatures allowed on an account, this is only used
     * in the client
     *
     * @param zimbraSignatureMinNumEntries new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=523)
    public Map<String,Object> setSignatureMinNumEntries(int zimbraSignatureMinNumEntries, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSignatureMinNumEntries, Integer.toString(zimbraSignatureMinNumEntries));
        return attrs;
    }

    /**
     * minimum number of signatures allowed on an account, this is only used
     * in the client
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=523)
    public Map<String,Object> unsetSignatureMinNumEntries(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSignatureMinNumEntries, "");
        return attrs;
    }

    /**
     * name of the signature
     *
     * @return zimbraSignatureName, or null unset
     */
    @ZAttr(id=491)
    public String getSignatureName() {
        return getAttr(Provisioning.A_zimbraSignatureName);
    }

    /**
     * name of the signature
     *
     * @param zimbraSignatureName new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=491)
    public Map<String,Object> setSignatureName(String zimbraSignatureName, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSignatureName, zimbraSignatureName);
        return attrs;
    }

    /**
     * name of the signature
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=491)
    public Map<String,Object> unsetSignatureName(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSignatureName, "");
        return attrs;
    }

    /**
     * If TRUE, spam messages will be affected by user mail filters instead
     * of being automatically filed into the Junk folder. This attribute is
     * deprecated and will be removed in a future release. See bug 23886 for
     * details.
     *
     * @return zimbraSpamApplyUserFilters, or false if unset
     */
    @ZAttr(id=604)
    public boolean isSpamApplyUserFilters() {
        return getBooleanAttr(Provisioning.A_zimbraSpamApplyUserFilters, false);
    }

    /**
     * If TRUE, spam messages will be affected by user mail filters instead
     * of being automatically filed into the Junk folder. This attribute is
     * deprecated and will be removed in a future release. See bug 23886 for
     * details.
     *
     * @param zimbraSpamApplyUserFilters new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=604)
    public Map<String,Object> setSpamApplyUserFilters(boolean zimbraSpamApplyUserFilters, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamApplyUserFilters, Boolean.toString(zimbraSpamApplyUserFilters));
        return attrs;
    }

    /**
     * If TRUE, spam messages will be affected by user mail filters instead
     * of being automatically filed into the Junk folder. This attribute is
     * deprecated and will be removed in a future release. See bug 23886 for
     * details.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=604)
    public Map<String,Object> unsetSpamApplyUserFilters(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSpamApplyUserFilters, "");
        return attrs;
    }

    /**
     * The maximum batch size for each ZimbraSync transaction. Default value
     * of 0 means to follow client requested size. If set to any positive
     * integer, the value will be the maximum number of items to sync even if
     * client requests more. This setting affects all sync categories
     * including email, contacts, calendar and tasks.
     *
     * @return zimbraSyncWindowSize, or -1 if unset
     */
    @ZAttr(id=437)
    public int getSyncWindowSize() {
        return getIntAttr(Provisioning.A_zimbraSyncWindowSize, -1);
    }

    /**
     * The maximum batch size for each ZimbraSync transaction. Default value
     * of 0 means to follow client requested size. If set to any positive
     * integer, the value will be the maximum number of items to sync even if
     * client requests more. This setting affects all sync categories
     * including email, contacts, calendar and tasks.
     *
     * @param zimbraSyncWindowSize new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=437)
    public Map<String,Object> setSyncWindowSize(int zimbraSyncWindowSize, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSyncWindowSize, Integer.toString(zimbraSyncWindowSize));
        return attrs;
    }

    /**
     * The maximum batch size for each ZimbraSync transaction. Default value
     * of 0 means to follow client requested size. If set to any positive
     * integer, the value will be the maximum number of items to sync even if
     * client requests more. This setting affects all sync categories
     * including email, contacts, calendar and tasks.
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=437)
    public Map<String,Object> unsetSyncWindowSize(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraSyncWindowSize, "");
        return attrs;
    }

    /**
     * The registered name of the Zimbra Analyzer Extension for this account
     * to use
     *
     * @return zimbraTextAnalyzer, or null unset
     */
    @ZAttr(id=393)
    public String getTextAnalyzer() {
        return getAttr(Provisioning.A_zimbraTextAnalyzer);
    }

    /**
     * The registered name of the Zimbra Analyzer Extension for this account
     * to use
     *
     * @param zimbraTextAnalyzer new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=393)
    public Map<String,Object> setTextAnalyzer(String zimbraTextAnalyzer, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraTextAnalyzer, zimbraTextAnalyzer);
        return attrs;
    }

    /**
     * The registered name of the Zimbra Analyzer Extension for this account
     * to use
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=393)
    public Map<String,Object> unsetTextAnalyzer(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraTextAnalyzer, "");
        return attrs;
    }

    /**
     * account version information
     *
     * @return zimbraVersion, or -1 if unset
     */
    @ZAttr(id=399)
    public int getVersion() {
        return getIntAttr(Provisioning.A_zimbraVersion, -1);
    }

    /**
     * account version information
     *
     * @param zimbraVersion new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=399)
    public Map<String,Object> setVersion(int zimbraVersion, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVersion, Integer.toString(zimbraVersion));
        return attrs;
    }

    /**
     * account version information
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=399)
    public Map<String,Object> unsetVersion(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraVersion, "");
        return attrs;
    }

    /**
     * Yahoo ID
     *
     * @return zimbraYahooId, or null unset
     */
    @ZAttr(id=658)
    public String getYahooId() {
        return getAttr(Provisioning.A_zimbraYahooId);
    }

    /**
     * Yahoo ID
     *
     * @param zimbraYahooId new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=658)
    public Map<String,Object> setYahooId(String zimbraYahooId, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraYahooId, zimbraYahooId);
        return attrs;
    }

    /**
     * Yahoo ID
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=658)
    public Map<String,Object> unsetYahooId(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraYahooId, "");
        return attrs;
    }

    /**
     * List of Zimlets available to this COS
     *
     * @return zimbraZimletAvailableZimlets, or ampty array if unset
     */
    @ZAttr(id=291)
    public String[] getZimletAvailableZimlets() {
        return getMultiAttr(Provisioning.A_zimbraZimletAvailableZimlets);
    }

    /**
     * List of Zimlets available to this COS
     *
     * @param zimbraZimletAvailableZimlets new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=291)
    public Map<String,Object> setZimletAvailableZimlets(String[] zimbraZimletAvailableZimlets, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZimletAvailableZimlets, zimbraZimletAvailableZimlets);
        return attrs;
    }

    /**
     * List of Zimlets available to this COS
     *
     * @param zimbraZimletAvailableZimlets new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=291)
    public Map<String,Object> addZimletAvailableZimlets(String zimbraZimletAvailableZimlets, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraZimletAvailableZimlets, zimbraZimletAvailableZimlets);
        return attrs;
    }

    /**
     * List of Zimlets available to this COS
     *
     * @param zimbraZimletAvailableZimlets existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=291)
    public Map<String,Object> removeZimletAvailableZimlets(String zimbraZimletAvailableZimlets, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraZimletAvailableZimlets, zimbraZimletAvailableZimlets);
        return attrs;
    }

    /**
     * List of Zimlets available to this COS
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=291)
    public Map<String,Object> unsetZimletAvailableZimlets(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZimletAvailableZimlets, "");
        return attrs;
    }

    /**
     * User properties for Zimlets
     *
     * @return zimbraZimletUserProperties, or ampty array if unset
     */
    @ZAttr(id=296)
    public String[] getZimletUserProperties() {
        return getMultiAttr(Provisioning.A_zimbraZimletUserProperties);
    }

    /**
     * User properties for Zimlets
     *
     * @param zimbraZimletUserProperties new value
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=296)
    public Map<String,Object> setZimletUserProperties(String[] zimbraZimletUserProperties, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZimletUserProperties, zimbraZimletUserProperties);
        return attrs;
    }

    /**
     * User properties for Zimlets
     *
     * @param zimbraZimletUserProperties new to add to existing values
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=296)
    public Map<String,Object> addZimletUserProperties(String zimbraZimletUserProperties, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraZimletUserProperties, zimbraZimletUserProperties);
        return attrs;
    }

    /**
     * User properties for Zimlets
     *
     * @param zimbraZimletUserProperties existing value to remove
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=296)
    public Map<String,Object> removeZimletUserProperties(String zimbraZimletUserProperties, Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraZimletUserProperties, zimbraZimletUserProperties);
        return attrs;
    }

    /**
     * User properties for Zimlets
     *
     * @param attrs existing map to populate, or null to create a new map
     * @return populated map to pass into Provisioning.modifyAttrs
     */
    @ZAttr(id=296)
    public Map<String,Object> unsetZimletUserProperties(Map<String,Object> attrs) {
        if (attrs == null) attrs = new HashMap<String,Object>();
        attrs.put(Provisioning.A_zimbraZimletUserProperties, "");
        return attrs;
    }

    ///// END-AUTO-GEN-REPLACE

}
