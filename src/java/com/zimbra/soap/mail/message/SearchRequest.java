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

package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.MailSearchParams;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-description Search
 * <br />
 * For a response, the order of the returned results represents the sorted order.  There is not a separate index
 * attribute or element.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_SEARCH_REQUEST)
public final class SearchRequest extends MailSearchParams {

    /**
     * @zm-api-field-description Warmup: When this option is specified, all other options are simply ignored, so you
     * can't include this option in regular search requests. This option gives a hint to the index system to open the
     * index data and primes it for search. The client should send this warm-up request as soon as the user puts the
     * cursor on the search bar. This will not only prime the index but also opens a persistent HTTP connection
     * (HTTP 1.1 Keep-Alive) to the server, hence smaller latencies in subseqent search requests. Sending this warm-up
     * request too early (e.g. login time) will be in vain in most cases because the index data is evicted from the
     * cache due to inactivity timeout by the time you actually send a search request.
     */
    @XmlAttribute(name=MailConstants.A_WARMUP /* warmup */, required=false)
    private ZmBoolean warmup;

    public void setWarmup(Boolean warmup) { this.warmup = ZmBoolean.fromBool(warmup); }
    public Boolean getWarmup() { return ZmBoolean.toBool(warmup); }
}
