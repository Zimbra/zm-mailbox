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
import com.zimbra.soap.type.SearchHit;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {})
public class MessageHitInfo extends MessageInfo implements SearchHit {

    @XmlAttribute(name=MailConstants.A_SORT_FIELD /* sf */, required=false)
    private String sortField;

    @XmlAttribute(name=MailConstants.A_CONTENTMATCHED /* cm */, required=false)
    private ZmBoolean contentMatched;

    @XmlElement(name=MailConstants.E_HIT_MIMEPART /* hp */, required=false)
    private List<Part> messagePartHits = Lists.newArrayList();

    public MessageHitInfo() {
        this(null);
    }
    public MessageHitInfo(String id) {
        super(id);
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }
    public void setContentMatched(Boolean contentMatched) {
        this.contentMatched = ZmBoolean.fromBool(contentMatched);
    }
    public void setMessagePartHits(Iterable <Part> messagePartHits) {
        this.messagePartHits.clear();
        if (messagePartHits != null) {
            Iterables.addAll(this.messagePartHits,messagePartHits);
        }
    }

    public MessageHitInfo addMessagePartHit(Part messagePartHit) {
        this.messagePartHits.add(messagePartHit);
        return this;
    }

    public String getSortField() { return sortField; }
    public Boolean getContentMatched() { return ZmBoolean.toBool(contentMatched); }
    public List<Part> getMessagePartHits() {
        return Collections.unmodifiableList(messagePartHits);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("sortField", sortField)
            .add("contentMatched", contentMatched)
            .add("messagePartHits", messagePartHits);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
