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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.SearchHit;
import com.zimbra.soap.type.ZmBoolean;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_MESSAGE_HIT_INFO, description="Message search result information containing a list of messages")
public class MessageHitInfo
extends MessageInfo
implements SearchHit {

    /**
     * @zm-api-field-tag sort-field-value
     * @zm-api-field-description Sort field value
     */
    @XmlAttribute(name=MailConstants.A_SORT_FIELD /* sf */, required=false)
    private String sortField;

    /**
     * @zm-api-field-tag message-matched-query
     * @zm-api-field-description If the message matched the specified query string
     */
    @XmlAttribute(name=MailConstants.A_CONTENTMATCHED /* cm */, required=false)
    private ZmBoolean contentMatched;

    /**
     * @zm-api-field-description Hit Parts -- indicators that the named parts matched the search string
     */
    @XmlElement(name=MailConstants.E_HIT_MIMEPART /* hp */, required=false)
    private final List<Part> messagePartHits = Lists.newArrayList();

    public MessageHitInfo() {
        this(null);
    }
    public MessageHitInfo(String id) {
        super(id);
    }

    @Override
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

    @Override
    @GraphQLQuery(name=GqlConstants.SORT_FIELD, description="The sort field value")
    public String getSortField() { return sortField; }
    @GraphQLQuery(name=GqlConstants.CONTENT_MATCHED, description="If the message matched the specified query string")
    public Boolean getContentMatched() { return ZmBoolean.toBool(contentMatched); }
    @GraphQLQuery(name=GqlConstants.MESSAGE_PART_HITS, description="Hit Parts, indicators that the named parts matched the search string")
    public List<Part> getMessagePartHits() {
        return Collections.unmodifiableList(messagePartHits);
    }

    @Override
    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("sortField", sortField)
            .add("contentMatched", contentMatched)
            .add("messagePartHits", messagePartHits);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
