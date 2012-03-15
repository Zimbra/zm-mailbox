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
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ContactActionSelector extends ActionSelector {

    /**
     * @zm-api-field-description New Contact attributes
     */
    @XmlElement(name=MailConstants.E_ATTRIBUTE, required=false)
    private List<NewContactAttr> attrs = Lists.newArrayList();

    public ContactActionSelector() {
    }

    public void setAttrs(Iterable <NewContactAttr> attrs) {
        this.attrs.clear();
        if (attrs != null) {
            Iterables.addAll(this.attrs,attrs);
        }
    }

    public ContactActionSelector addAttr(NewContactAttr attr) {
        this.attrs.add(attr);
        return this;
    }

    public List<NewContactAttr> getAttrs() {
        return Collections.unmodifiableList(attrs);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("attrs", attrs)
            .toString();
    }
}
