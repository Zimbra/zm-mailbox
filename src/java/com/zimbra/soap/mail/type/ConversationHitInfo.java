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
import com.zimbra.soap.type.SearchHit;

@XmlAccessorType(XmlAccessType.NONE)
public class ConversationHitInfo
extends ConversationSummary
implements SearchHit {

    /**
     * @zm-api-field-tag sort-field
     * @zm-api-field-description Sort field value
     */
    @XmlAttribute(name=MailConstants.A_SORT_FIELD /* sf */, required=false)
    private String sortField;

    /**
     * @zm-api-field-description Hits
     */
    @XmlElement(name=MailConstants.E_MSG /* m */, required=false)
    private List<ConversationMsgHitInfo> messageHits = Lists.newArrayList();

    public ConversationHitInfo() {
        this((String) null);
    }

    public ConversationHitInfo(String id) {
        super(id);
    }

    public void setSortField(String sortField) { this.sortField = sortField; }
    public void setMessageHits(Iterable <ConversationMsgHitInfo> messageHits) {
        this.messageHits.clear();
        if (messageHits != null) {
            Iterables.addAll(this.messageHits,messageHits);
        }
    }

    public ConversationHitInfo addMessageHit(ConversationMsgHitInfo messageHit) {
        this.messageHits.add(messageHit);
        return this;
    }

    public String getSortField() { return sortField; }
    public List<ConversationMsgHitInfo> getMessageHits() {
        return Collections.unmodifiableList(messageHits);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("sortField", sortField)
            .add("messageHits", messageHits);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
