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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class WatcherInfo {

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
     * @zm-api-field-description List of items in the user's mailbox currently being watched by other users
     */
    @XmlElement(name=MailConstants.E_ITEM /* item */, required=false)
    private List<IntegerIdAttr> items = Lists.newArrayList();

    private WatcherInfo() {
    }

    private WatcherInfo(String id, String email, String name) {
        setId(id);
        setEmail(email);
        setName(name);
    }

    public static WatcherInfo createForIdEmailAndName(String id, String email, String name) {
        return new WatcherInfo(id, email, name);
    }

    public void setId(String id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setName(String name) { this.name = name; }
    public void setItems(Iterable <IntegerIdAttr> items) {
        this.items.clear();
        if (items != null) {
            Iterables.addAll(this.items,items);
        }
    }

    public void addItem(IntegerIdAttr item) {
        this.items.add(item);
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public List<IntegerIdAttr> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("email", email)
            .add("name", name)
            .add("items", items);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
