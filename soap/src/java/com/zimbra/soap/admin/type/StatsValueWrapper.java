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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.NamedElement;

// XmlRootElement is needed for classes referenced via @XmlElementRef
@XmlRootElement(name=AdminConstants.E_VALUES)
@XmlAccessorType(XmlAccessType.NONE)
public class StatsValueWrapper {

    /**
     * @zm-api-field-description Stats specification
     */
    @XmlElement(name=AdminConstants.E_STAT /* stat */, required=false)
    private List<NamedElement> stats = Lists.newArrayList();

    public StatsValueWrapper() {
    }

    public void setStats(Iterable <NamedElement> stats) {
        this.stats.clear();
        if (stats != null) {
            Iterables.addAll(this.stats, stats);
        }
    }

    public StatsValueWrapper addStat(NamedElement stat) {
        this.stats.add(stat);
        return this;
    }

    public List<NamedElement> getStats() {
        return Collections.unmodifiableList(stats);
    }
}
