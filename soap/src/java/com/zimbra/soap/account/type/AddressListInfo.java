/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class AddressListInfo {

    /**
     * @zm-api-field-tag address-list-id
     * @zm-api-field-description Id of Address List
     */
    @XmlAttribute(name = AccountConstants.A_ID /* id */, required = true)
    private String id;

    /**
     * @zm-api-field-tag name
     * @zm-api-field-description Name of the address list
     */
    @XmlAttribute(name = AccountConstants.A_NAME /* name */, required = true)
    private String name;

    /**
     * @zm-api-field-tag description
     * @zm-api-field-description description of the address list
     */
    @XmlAttribute(name = AccountConstants.A_DESCRIPTION /* description */, required = false)
    private String description;

    /**
     * @zm-api-field-tag active
     * @zm-api-field-description address list is active or not
     */
    @XmlAttribute(name = AccountConstants.A_ACTIVE /* active */, required = false)
    private ZmBoolean active;

    /**
     * @zm-api-field-tag galFilter
     * @zm-api-field-description galFilter of the address list
     */
    @XmlElement(name = AdminConstants.E_GAL_FILTER /* galFilter */, required = false)
    private String galFilter;

    /**
     * @zm-api-field-tag ldapFilter
     * @zm-api-field-description ldapFilter of address list
     */
    @XmlElement(name = AdminConstants.E_LDAP_FILTER /* ldapFilter */, required = false)
    private String ldapFilter;

    /**
     * @zm-api-field-tag address-list-dn
     * @zm-api-field-description DN of Address List
     */
    @XmlAttribute(name = AdminConstants.A_DN /* dn */, required = false)
    private String dn;

    /**
     * no-argument constructor wanted by JAXB
     */
    public AddressListInfo() {
        this((String) null, (String) null, (String) null);
    }

    public AddressListInfo(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public AddressListInfo(String dn) {
        this.dn = dn;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return ZmBoolean.toBool(active, Boolean.FALSE);
    }

    public void setActive(boolean active) {
        this.active = ZmBoolean.fromBool(active, Boolean.FALSE);
    }

    public String getGalFilter() {
        return galFilter;
    }

    public String getLdapFilter() {
        return ldapFilter;
    }

    public void setGalFilter(String galFilter) {
        this.galFilter = galFilter;
    }

    public void setLdapFilter(String ldapFilter) {
        this.ldapFilter = ldapFilter;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getDn() {
        return dn;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper.add("id", id)
                     .add("name", name)
                     .add("description", description)
                     .add("active", active)
                     .add("galFilter", galFilter)
                     .add("ldapFilter", ldapFilter)
                     .add("dn", dn);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
