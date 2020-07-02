/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.SmimeConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;

@XmlAccessorType(XmlAccessType.NONE)
public class CertificateDN {

    /**
     * @zm-api-field-tag email
     * @zm-api-field-description email address of subject or issuer.
     */
    @ZimbraJsonAttribute
    @XmlElement(name=SmimeConstants.E_EMAIL_ADDR, required=false)
    private String email;

    /**
     * @zm-api-field-tag country
     * @zm-api-field-description country of subject or issuer.
     */
    @ZimbraJsonAttribute
    @XmlElement(name=SmimeConstants.E_COUNTRY, required=false)
    private String country;

    /**
     * @zm-api-field-tag state
     * @zm-api-field-description state of subject or issuer.
     */
    @ZimbraJsonAttribute
    @XmlElement(name=SmimeConstants.E_STATE, required=false)
    private String state;

    /**
     * @zm-api-field-tag city
     * @zm-api-field-description city of subject or issuer.
     */
    @ZimbraJsonAttribute
    @XmlElement(name=SmimeConstants.E_CITY, required=false)
    private String city;

    /**
     * @zm-api-field-tag org
     * @zm-api-field-description organization of subject or issuer.
     */
    @ZimbraJsonAttribute
    @XmlElement(name=SmimeConstants.E_ORG, required=false)
    private String org;

    /**
     * @zm-api-field-tag orgunit
     * @zm-api-field-description organizational unit of subject or issuer.
     */
    @ZimbraJsonAttribute
    @XmlElement(name=SmimeConstants.E_ORG_UNIT, required=false)
    private String orgunit;

    /**
     * @zm-api-field-tag commonName
     * @zm-api-field-description common name of subject or issuer.
     */
    @ZimbraJsonAttribute
    @XmlElement(name=SmimeConstants.E_COMMON_NAME, required=false)
    private String commonName;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getOrg() {
        return org;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    public String getOrgunit() {
        return orgunit;
    }

    public void setOrgunit(String orgunit) {
        this.orgunit = orgunit;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper.add("email", email)
            .add("country", country)
            .add("state", state)
            .add("city", city)
            .add("org", org)
            .add("orgunit", orgunit)
            .add("commonName", commonName);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }

}
