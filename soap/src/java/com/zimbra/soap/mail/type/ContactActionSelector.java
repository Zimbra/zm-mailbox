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

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ContactActionSelector extends ActionSelector {

    /**
     * @zm-api-field-description New Contact attributes
     */
    @XmlElement(name=MailConstants.E_ATTRIBUTE, required=false)
    private final List<NewContactAttr> attrs = Lists.newArrayList();

    public ContactActionSelector() {
    }

    public ContactActionSelector(String ids, String operation) {
        super(ids, operation);
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
        return MoreObjects.toStringHelper(this)
            .add("attrs", attrs)
            .toString();
    }
}
