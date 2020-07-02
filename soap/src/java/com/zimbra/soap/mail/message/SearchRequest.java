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

package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.MailSearchParams;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
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

    /**
     * @zm-api-field-description Whether this search should be logged in search history. Defaults to true,
     * but should be set to false for searches not initiated by the user
     */
    @XmlAttribute(name=MailConstants.A_LOG_SEARCH /* logSearch */, required=false)
    private ZmBoolean logSearch;

    public void setWarmup(Boolean warmup) { this.warmup = ZmBoolean.fromBool(warmup); }
    public Boolean getWarmup() { return ZmBoolean.toBool(warmup); }

    public void setLogSearch(Boolean logSearch) { this.logSearch = ZmBoolean.fromBool(logSearch); }
    public Boolean getLogSearch() { return ZmBoolean.toBool(logSearch, true); };
}
