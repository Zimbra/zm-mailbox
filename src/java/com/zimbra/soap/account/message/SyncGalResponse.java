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

package com.zimbra.soap.account.message;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.account.type.ContactInfo;
import com.zimbra.soap.type.Id;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_SYNC_GAL_RESPONSE)
@XmlType(propOrder = {})
public class SyncGalResponse {

    @XmlAttribute(name=MailConstants.A_SORTBY /* sortBy */, required=false)
    private String sortBy;

    @XmlAttribute(name=MailConstants.A_QUERY_OFFSET /* offset */, required=false)
    private Integer offset;

    @XmlAttribute(name=MailConstants.A_QUERY_MORE /* more */, required=false)
    private ZmBoolean more;

    @XmlAttribute(name=MailConstants.A_TOKEN /* token */, required=false)
    private String token;

    @XmlElements({
        @XmlElement(name=MailConstants.E_CONTACT /* cn */,
            type=ContactInfo.class),
        @XmlElement(name=MailConstants.E_DELETED /* deleted */,
            type=Id.class)
    })
    private List<Object> hits = Lists.newArrayList();

    public SyncGalResponse() {
    }

    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public void setOffset(Integer offset) { this.offset = offset; }
    public void setMore(Boolean more) { this.more = ZmBoolean.fromBool(more); }
    public void setToken(String token) { this.token = token; }
    public void setHits(Iterable <Object> hits) {
        this.hits.clear();
        if (hits != null) {
            Iterables.addAll(this.hits,hits);
        }
    }

    public void addHit(Object hit) {
        this.hits.add(hit);
    }

    public String getSortBy() { return sortBy; }
    public Integer getOffset() { return offset; }
    public Boolean getMore() { return ZmBoolean.toBool(more); }
    public String getToken() { return token; }
    public List<Object> getHits() {
        return Collections.unmodifiableList(hits);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("sortBy", sortBy)
            .add("offset", offset)
            .add("more", more)
            .add("token", token)
            .add("hits", hits);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
