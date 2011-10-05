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
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"vcard", "attrs"})
public class ContactSpec {

    // Used when modifying a contact
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private Integer id;

    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folder;

    @Deprecated
    @XmlAttribute(name=MailConstants.A_TAGS /* t */, required=false)
    private String tags;

    @XmlAttribute(name=MailConstants.A_TAG_NAMES /* tn */, required=false)
    private String tagNames;

    // Either a vcard or attributes can be specified but not both.
    @XmlElement(name=MailConstants.E_VCARD, required=false)
    private VCardInfo vcard;

    @XmlElement(name=MailConstants.E_ATTRIBUTE, required=false)
    private List<NewContactAttr> attrs = Lists.newArrayList();

    public ContactSpec() {
    }

    public void setId(Integer id) { this.id = id; }
    public void setFolder(String folder) { this.folder = folder; }
    @Deprecated
    public void setTags(String tags) { this.tags = tags; }
    public void setTagNames(String tagNames) { this.tagNames = tagNames; }
    public void setVcard(VCardInfo vcard) { this.vcard = vcard; }
    public void setAttrs(Iterable <NewContactAttr> attrs) {
        this.attrs.clear();
        if (attrs != null) {
            Iterables.addAll(this.attrs,attrs);
        }
    }

    public ContactSpec addAttr(NewContactAttr attr) {
        this.attrs.add(attr);
        return this;
    }

    public Integer getId() { return id; }
    public String getFolder() { return folder; }
    @Deprecated
    public String getTags() { return tags; }
    public String getTagNames() { return tagNames; }
    public VCardInfo getVcard() { return vcard; }
    public List<NewContactAttr> getAttrs() {
        return Collections.unmodifiableList(attrs);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("id", id)
            .add("folder", folder)
            .add("tags", tags)
            .add("tagNames", tagNames)
            .add("vcard", vcard)
            .add("attrs", attrs)
            .toString();
    }
}
