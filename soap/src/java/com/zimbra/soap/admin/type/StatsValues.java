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

package com.zimbra.soap.admin.type;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class StatsValues {

    /**
     * @zm-api-field-tag t
     * @zm-api-field-description t
     */
    @XmlAttribute(name=AdminConstants.A_T /* t */, required=true)
    private final String t;

    /**
     * @zm-api-field-description Stats
     */
    @XmlElement(name=AdminConstants.E_STAT /* stat */, required=false)
    private List<NameAndValue> stats = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private StatsValues() {
        this((String) null);
    }

    public StatsValues(String t) {
        this.t = t;
    }

    public void setStats(Iterable <NameAndValue> stats) {
        this.stats.clear();
        if (stats != null) {
            Iterables.addAll(this.stats,stats);
        }
    }

    public StatsValues addStat(NameAndValue stat) {
        this.stats.add(stat);
        return this;
    }

    public String getT() { return t; }
    public List<NameAndValue> getStats() {
        return Collections.unmodifiableList(stats);
    }
}
