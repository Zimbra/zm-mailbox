/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.soap.admin.type;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public final class CacheSelector {

    // Note: Valid types from Provisioning.CacheEntryType PLUS there is an extension mechanism
    /**
     * @zm-api-field-tag comma-sep-cache-types
     * @zm-api-field-description Comma separated list of cache types.
     * e.g. from <b>acl|locale|skin|uistrings|license|all|account|config|globalgrant|cos|domain|galgroup|group|mime|
     *              server|alwaysOnCluster|zimlet|&lt;extension-cache-type&gt;</b>
     */
    @XmlAttribute(name=AdminConstants.A_TYPE /* type */, required=true)
    private String types;

    /**
     * @zm-api-field-tag all-servers
     * @zm-api-field-description
     * <table>
     * <tr> <td> <b>0 (false) [default]</b> </td> <td> flush cache only on the local server </td> </tr>
     * <tr> <td> <b>1 (true)</b> </td>
     *      <td> flush cache only on all servers (can take a long time on systems with lots of servers) </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AdminConstants.A_ALLSERVERS /* allServers */, required=false)
    private ZmBoolean allServers;


    /**
     * @zm-api-field-tag imap-servers
     * @zm-api-field-description
     * <table>
     * <tr> <td> <b>0 (false)</b> </td> <td> don't issue X-ZIMBRA-FLUSHCACHE IMAP command to upstream IMAP servers </td> </tr>
     * <tr> <td> <b>1 (true) [default]</b> </td>
     *      <td> flush cache on servers listed in zimbraReverseProxyUpstreamImapServers for the current server via X-ZIMBRA-FLUSHCACHE</td> </tr>
     * </table>
     */
    @XmlAttribute(name=AdminConstants.A_IMAPSERVERS /* imapServers */, required=false)
    private ZmBoolean imapServers;

    /**
     * @zm-api-field-description Cache entry selectors
     */
    @XmlElement(name=AdminConstants.E_ENTRY /* entry */, required=false)
    private final List <CacheEntrySelector> entries = Lists.newArrayList();

    public CacheSelector() {
        this(false, "");
    }

    public CacheSelector(Boolean allServers, String types) {
        this.allServers = ZmBoolean.fromBool(allServers);
        this.setTypes(types);
    }

    public void setTypes(String types) {
        this.types = (types == null) ? "" : types;
    }

    public void setAllServers(Boolean allServers) {
        this.allServers = ZmBoolean.fromBool(allServers);
    }

    public void setEntries(Iterable<CacheEntrySelector> entryList) {
        this.entries.clear();
        if (entryList != null) {
            Iterables.addAll(this.entries, entryList);
        }
    }

    public void addEntry(CacheEntrySelector entry) {
        this.entries.add(entry);
    }

    public boolean isAllServers() { return ZmBoolean.toBool(allServers, false); }

    public String getTypes() { return types; }
    public List<CacheEntrySelector> getEntries() {
        return Collections.unmodifiableList(entries);
    }
    public void setIncludeImapServers(boolean bool) { imapServers = ZmBoolean.fromBool(bool); }
    public boolean isIncludeImapServers() { return ZmBoolean.toBool(imapServers, true); }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
                .add("allServers", allServers)
                .add("imapServers", imapServers)
                .add("types", types)
                .add("entries", entries);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
