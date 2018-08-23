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

package com.zimbra.soap.mail.type;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.Id;

@XmlAccessorType(XmlAccessType.NONE)
public class WatchingTarget {

    /**
     * @zm-api-field-tag account-id
     * @zm-api-field-description Account ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private String id;

    /**
     * @zm-api-field-tag email-address
     * @zm-api-field-description Email address
     */
    @XmlAttribute(name=MailConstants.A_EMAIL /* email */, required=true)
    private String email;

    /**
     * @zm-api-field-tag display-name
     * @zm-api-field-description Display name
     */
    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=true)
    private String name;

    /**
     * @zm-api-field-description Items the user is currently watching
     */
    @XmlElement(name=MailConstants.E_ITEM /* item */, required=false)
    private List<Id> items = Lists.newArrayList();

    public WatchingTarget() {
    }

    private WatchingTarget(String id, String email, String name) {
        setId(id);
        setEmail(email);
        setName(name);
    }

    public static WatchingTarget createForIdEmailAndName(String id, String email, String name) {
        return new WatchingTarget(id, email, name);
    }

    public void setId(String id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setName(String name) { this.name = name; }
    public void setItems(Iterable <Id> items) {
        this.items.clear();
        if (items != null) {
            Iterables.addAll(this.items,items);
        }
    }

    public void addItem(Id item) {
        this.items.add(item);
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public List<Id> getItems() {
        return Collections.unmodifiableList(items);
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("email", email)
            .add("name", name)
            .add("items", items);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
