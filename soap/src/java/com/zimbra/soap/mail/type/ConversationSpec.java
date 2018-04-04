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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.AttributeName;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class ConversationSpec {

    /**
     * @zm-api-field-tag conv-id
     * @zm-api-field-description Conversation ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    // Values related to SearchParams.ExpandResults but case insensitive
    /**
     * @zm-api-field-tag fetch-1|all|{item-id}
     * @zm-api-field-description if value is "1" or "all" the full expanded message structure is inlined for the
     * first (or for all) messages in the conversation.
     * <br />
     * If fetch="{item-id}", only the message with the given {item-id} is expanded inline
     */
    @XmlAttribute(name=MailConstants.A_FETCH /* fetch */, required=false)
    private String inlineRule;

    /**
     * @zm-api-field-tag want-html
     * @zm-api-field-description Set to return defanged HTML content by default.  (default is unset)
     */
    @XmlAttribute(name=MailConstants.A_WANT_HTML /* html */, required=false)
    private ZmBoolean wantHtml;

    /**
     * @zm-api-field-tag max-inlined-length
     * @zm-api-field-description Maximum inlined length
     */
    @XmlAttribute(name=MailConstants.A_MAX_INLINED_LENGTH /* max */, required=false)
    private Integer maxInlinedLength;

    /**
     * @zm-api-field-description Requested headers.  if <b>&lt;header></b>s are requested, any matching headers are
     * inlined into the response (not available when <b>raw</b> is set)
     */
    @XmlElement(name=MailConstants.A_HEADER /* header */, required=false)
    private List<AttributeName> headers = Lists.newArrayList();

    /**
     * @zm-api-field-tag need-can-expand
     * @zm-api-field-description Set to return group info (isGroup and exp flags) on <b>&lt;e></b> elements in the
     * response (default is unset.)
     */
    @XmlAttribute(name=MailConstants.A_NEED_EXP /* needExp */, required=false)
    private ZmBoolean needCanExpand;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ConversationSpec() {
        this((String) null);
    }

    public ConversationSpec(String id) {
        this.id = id;
    }

    public void setInlineRule(String inlineRule) { this.inlineRule = inlineRule; }
    public void setWantHtml(Boolean wantHtml) { this.wantHtml = ZmBoolean.fromBool(wantHtml); }
    public void setMaxInlinedLength(Integer maxInlinedLength) { this.maxInlinedLength = maxInlinedLength; }
    public void setNeedCanExpand(Boolean needCanExpand) { this.needCanExpand = ZmBoolean.fromBool(needCanExpand); }
    public void setHeaders(Iterable <AttributeName> headers) {
        this.headers.clear();
        if (headers != null) {
            Iterables.addAll(this.headers,headers);
        }
    }

    public void addHeader(AttributeName header) {
        this.headers.add(header);
    }

    public String getId() { return id; }
    public String getInlineRule() { return inlineRule; }
    public Boolean getWantHtml() { return ZmBoolean.toBool(wantHtml); }
    public Integer getMaxInlinedLength() { return maxInlinedLength; }
    public Boolean getNeedCanExpand() { return ZmBoolean.toBool(needCanExpand); }
    public List<AttributeName> getHeaders() {
        return headers;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("inlineRule", inlineRule)
            .add("wantHtml", wantHtml)
            .add("maxInlinedLength", maxInlinedLength)
            .add("headers", headers);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
