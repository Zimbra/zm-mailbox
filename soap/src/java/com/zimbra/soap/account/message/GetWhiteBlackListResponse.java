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

package com.zimbra.soap.account.message;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonArrayForWrapper;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_GET_WHITE_BLACK_LIST_RESPONSE)
@XmlType(propOrder = {"whiteListEntries", "blackListEntries"})
public class GetWhiteBlackListResponse {

    /**
     * @zm-api-field-description White list
     */
    @ZimbraJsonArrayForWrapper
    @XmlElementWrapper(name=AccountConstants.E_WHITE_LIST, required=true)
    @XmlElement(name=AccountConstants.E_ADDR, required=false)
    private List<String> whiteListEntries = Lists.newArrayList();

    /**
     * @zm-api-field-description Black list
     */
    @ZimbraJsonArrayForWrapper
    @XmlElementWrapper(name=AccountConstants.E_BLACK_LIST, required=true)
    @XmlElement(name=AccountConstants.E_ADDR, required=false)
    private List<String> blackListEntries = Lists.newArrayList();

    public GetWhiteBlackListResponse() {
    }

    public void setWhiteListEntries(Iterable <String> whiteListEntries) {
        this.whiteListEntries.clear();
        if (whiteListEntries != null) {
            Iterables.addAll(this.whiteListEntries,whiteListEntries);
        }
    }

    public GetWhiteBlackListResponse addWhiteListEntry(String whiteListEntry) {
        this.whiteListEntries.add(whiteListEntry);
        return this;
    }

    public void setBlackListEntries(Iterable <String> blackListEntries) {
        this.blackListEntries.clear();
        if (blackListEntries != null) {
            Iterables.addAll(this.blackListEntries,blackListEntries);
        }
    }

    public GetWhiteBlackListResponse addBlackListEntry(String blackListEntry) {
        this.blackListEntries.add(blackListEntry);
        return this;
    }

    public List<String> getWhiteListEntries() {
        return Collections.unmodifiableList(whiteListEntries);
    }
    public List<String> getBlackListEntries() {
        return Collections.unmodifiableList(blackListEntries);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("whiteListEntries", whiteListEntries)
            .add("blackListEntries", blackListEntries)
            .toString();
    }
}
