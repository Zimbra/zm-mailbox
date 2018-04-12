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

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.OpValue;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Modify the anti-spam WhiteList and BlackList addresses
 * <br />
 * Note: If no <b>&lt;addr></b> is present in a list, it means to remove all addresses in the list.
 * <br />
 * e.g. remove all addresses in the white list
 * <pre>
 *        &lt;ModifyWhiteBlackListRequest>
 *            &lt;whiteList/>
 *        &lt;/ModifyWhiteBlackListRequest>
 * </pre>
 * <p>
 * <b>{op}</b> = + | -
 * <ul>
 * <li> <b>+</b> : add, ignored if the value already exists
 * <li> <b>-</b> : remove, ignored if the value does not exist
 * </ul>
 * if not present, replace the entire list with provided values.
 * <br />
 * Note, can't mix +/- with non-present op)
 * <br />
 * <br />
 * e.g.
 * <ol>
 * <li> replace the entire white list with "foo", "bar".
 * <pre>
 *             &lt;whiteList>
 *                 &lt;addr>foo&lt;/addr>
 *                 &lt;addr>bar&lt;/addr>
 *             &lt;/whiteList>
 * </pre>
 * <li> add values "foo" and "bar" to white list
 * <pre>
 *             &lt;whiteList>
 *                 &lt;addr op="+">foo&lt;/addr>
 *                 &lt;addr op="+">bar&lt;/addr>
 *             &lt;/whiteList>
 * </pre>
 * <li> remove values "foo" and "bar" from white list
 * <pre>
 *             &lt;whiteList>
 *                 &lt;addr op="-">foo&lt;/addr>
 *                 &lt;addr op="-">bar&lt;/addr>
 *             &lt;/whiteList>
 * </pre>
 * <li> add "foo" and remove 'bar"
 * <pre>
 *             &lt;whiteList>
 *                 &lt;addr op="+">foo&lt;/addr>
 *                 &lt;addr op="-">bar&lt;/addr>
 *             &lt;/whiteList>
 * </pre>
 * <li> mix +/- and non-present op - not allowed, INVALID_REQUEST will be thrown
 * <pre>
 *             &lt;whiteList>
 *                 &lt;addr op="+">foo&lt;/addr>
 *                 &lt;addr>bar&lt;/addr>
 *             &lt;/whiteList>
 * </pre>
 * </ol>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_MODIFY_WHITE_BLACK_LIST_REQUEST)
public class ModifyWhiteBlackListRequest {

    /**
     * @zm-api-field-description Modifications for WhiteList
     */
    @XmlElementWrapper(name=AccountConstants.E_WHITE_LIST, required=false)
    @XmlElement(name=AccountConstants.E_ADDR, required=false)
    private List<OpValue> whiteListEntries = Lists.newArrayList();

    /**
     * @zm-api-field-description Modifications for BlackList
     */
    @XmlElementWrapper(name=AccountConstants.E_BLACK_LIST, required=false)
    @XmlElement(name=AccountConstants.E_ADDR, required=false)
    private List<OpValue> blackListEntries = Lists.newArrayList();

    public ModifyWhiteBlackListRequest() {
    }

    public void setWhiteListEntries(Iterable <OpValue> whiteListEntries) {
        this.whiteListEntries.clear();
        if (whiteListEntries != null) {
            Iterables.addAll(this.whiteListEntries,whiteListEntries);
        }
    }

    public ModifyWhiteBlackListRequest addWhiteListEntry(
                            OpValue whiteListEntry) {
        this.whiteListEntries.add(whiteListEntry);
        return this;
    }

    public void setBlackListEntries(Iterable <OpValue> blackListEntries) {
        this.blackListEntries.clear();
        if (blackListEntries != null) {
            Iterables.addAll(this.blackListEntries,blackListEntries);
        }
    }

    public ModifyWhiteBlackListRequest addBlackListEntry(
                            OpValue blackListEntry) {
        this.blackListEntries.add(blackListEntry);
        return this;
    }

    public List<OpValue> getWhiteListEntries() {
        return Collections.unmodifiableList(whiteListEntries);
    }
    public List<OpValue> getBlackListEntries() {
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
