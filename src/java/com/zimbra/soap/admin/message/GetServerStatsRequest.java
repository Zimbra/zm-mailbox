/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.admin.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.collect.Iterables;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.Stat;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Returns server monitoring stats.  These are the same stats that are logged to
 * mailboxd.csv.  If no <b>&lt;stat></b> element is specified, all server stats are returned.
 * If the stat name is invalid, returns a SOAP fault.
 */
@XmlRootElement(name = AdminConstants.E_GET_SERVER_STATS_REQUEST)
@XmlType(propOrder = {})
public class GetServerStatsRequest {

    /**
     * @zm-api-field-description Stats
     */
    @XmlElement(name=AdminConstants.E_STAT)
    private List<Stat> stats = new ArrayList<Stat>();

    public GetServerStatsRequest() {
    }

    public GetServerStatsRequest(String ... statNames) {
        if (statNames != null) {
            for (String name : statNames) {
                addStat(name);
            }
        }
    }
    public List<Stat> getStats() {
        return Collections.unmodifiableList(stats);
    }

    public void setStats(Iterable<Stat> stats) {
        this.stats.clear();
        if (stats != null) {
            Iterables.addAll(this.stats, stats);
        }
    }

    public void setStatNames(Iterable<String> names) {
        this.stats.clear();
        if (names != null) {
            for (String name : names) {
                addStat(name);
            }
        }
    }

    public void addStat(String statName) {
        Stat stat = new Stat();
        stat.setName(statName);
        this.stats.add(stat);
    }
}
