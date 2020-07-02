/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.soap.mail.type;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.Id;

@XmlAccessorType(XmlAccessType.NONE)
public class ShareDetails {
    /**
     * @zm-api-field-tag item-id
     * @zm-api-field-description Shared item ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private String id;

    /**
     * @zm-api-field-description Grantees
     */
    @XmlElement(name=MailConstants.E_GRANTEE /* grantee */, required=false)
    private final List<ShareGrantee> grantees = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ShareDetails() {
        this((String) null);
    }

    public ShareDetails(String id) {
        setId(id);
    }

    public ShareDetails(Id id) {
        setId(id.getId());
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setGrantees(Iterable<ShareGrantee> grantees) {
        this.grantees.clear();
        if (grantees != null) {
            Iterables.addAll(this.grantees, grantees);
        }
    }

    public void addGrantee(ShareGrantee grantee) {
        this.grantees.add(grantee);
    }

    public String getId() {
        return id;
    }

    public List<ShareGrantee> getGrantees() {
        return Collections.unmodifiableList(grantees);
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("grantees", grantees);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }

}
