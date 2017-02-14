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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.Stat;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AdminConstants.E_GET_SERVER_STATS_RESPONSE)
public class GetServerStatsResponse {

    /**
     * @zm-api-field-description Details of server monitoring statistics
     */
    @XmlElement(name = AdminConstants.E_STAT)
    private List<Stat> stats = Lists.newArrayList();

    public List<Stat> getStats() {
        return Collections.unmodifiableList(stats);
    }

    public void setStats(Iterable<Stat> stats) {
        this.stats.clear();
        if (stats != null) {
            Iterables.addAll(this.stats, stats);
        }
    }

    public String getValue(String statName) {
        Iterator<Stat> i = Stat.filterByName(stats, statName);
        if (i.hasNext()) {
            return i.next().getValue();
        }
        return null;
    }

    /**
     * @throws NullPointerException if the value does not exist
     * @throws NumberFormatException if the value cannot be parsed
     */
    public int getIntValue(String statName) {
        String val = getValue(statName);
        if (val == null) {
            throw new NullPointerException("No value found for stat " + statName);
        }
        return Integer.parseInt(val);
    }
}
