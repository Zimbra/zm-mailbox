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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("sortField", sortField)
            .add("messageHits", messageHits);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
