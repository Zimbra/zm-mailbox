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
 */
package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;

public abstract class HABMember {

    /**
     * @zm-api-field-tag member-email-address
     * @zm-api-field-description HAB Member name - an email address (user@domain)
     */
    @ZimbraJsonAttribute
    @XmlElement(name=AccountConstants.A_NAME /* name */, required=true)
    private String name;

    /**
     * @zm-api-field-tag seniorityIndex
     * @zm-api-field-description seniorityIndex of the HAB group member
     */
    @XmlAttribute(name = AccountConstants.A_HAB_SENIORITY_INDEX /* seniorityIndex */, required = false)
    private int seniorityIndex;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    public HABMember() {
        this((String) null);
    }

    public HABMember(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSeniorityIndex() {
        return seniorityIndex;
    }

    public void setSeniorityIndex(int seniorityIndex) {
        this.seniorityIndex = seniorityIndex;
    }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
         helper.add("name", name);
         if (seniorityIndex != 0) {
             helper.add("seniorityIndex", seniorityIndex);
         }
         return helper;
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
