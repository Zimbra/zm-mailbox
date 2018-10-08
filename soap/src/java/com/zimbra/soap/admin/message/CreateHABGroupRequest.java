/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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
 */package com.zimbra.soap.admin.message;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.Attr;

/**
  * @zm-api-command-auth-required true
  * @zm-api-command-admin-auth-required true
  * @zm-api-command-description Create a Hierarchical address book group
  * <br />
  * <b>Access</b>: domain admin sufficient
  */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_CREATE_HAB_GROUP_REQUEST)
public class CreateHABGroupRequest extends CreateDistributionListRequest {
     /**
      * @zm-api-field-tag hab-display-name
      * @zm-api-field-description Display Name for group in HAB
      */
     @XmlAttribute(name=AdminConstants.A_HAB_DISPLAY_NAME /* habDisplayName */, required=false)
     private String habDisplayName;

     /**
      * @zm-api-field-tag hab-orgunit-name
      * @zm-api-field-description organizational unit of the HAB Group
      */
     @XmlAttribute(name=AdminConstants.A_HAB_ORG_UNIT /* habOrgUnit */, required=true)
     private String habOrgUnit;

     /**
      * no-argument constructor wanted by JAXB
      */
     @SuppressWarnings("unused")
     public CreateHABGroupRequest() {
         this(null, null, null, (Collection<Attr>) null, false);
     }

    public CreateHABGroupRequest(String habOrgUnit, String name) {
         this(null, habOrgUnit, name, (Collection<Attr>) null, false);
     }

     public CreateHABGroupRequest(String habGroupName, String habOrgUnit, String name, Collection<Attr> attrs, boolean dynamic) {
         super(name, attrs, dynamic);
         this.habDisplayName = habGroupName;
         this.habOrgUnit = habOrgUnit;
     }

     public String getHabDisplayName() {
         return habDisplayName;
     }

     public String getHabOrgUnit() {
         return habOrgUnit;
     }

     public void setHabDisplayName(String habGroupName) {
         this.habDisplayName = habGroupName;
     }

     public void setHabOrgUnit(String habOrgUnit) {
         this.habOrgUnit = habOrgUnit;
     }
}
