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

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.BackupConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class BackupQueryCounter {

    /**
     * @zm-api-field-tag counter-name
     * @zm-api-field-description Counter name
     */
    @XmlAttribute(name=BackupConstants.A_NAME /* name */, required=true)
    private final String name;

    /**
     * @zm-api-field-tag counter-unit
     * @zm-api-field-description Counter unit
     */
    @XmlAttribute(name=BackupConstants.A_COUNTER_UNIT /* unit */, required=true)
    private final String counterUnit;

    /**
     * @zm-api-field-tag counter-value
     * @zm-api-field-description Counter value
     */
    @XmlAttribute(name=BackupConstants.A_COUNTER_SUM /* sum */, required=true)
    private final Long counterSum;

    /**
     * @zm-api-field-tag num-samples-or-data-points
     * @zm-api-field-description Number of samples or data points
     */
    @XmlAttribute(name=BackupConstants.A_COUNTER_NUM_SAMPLES /* numSamples */, required=true)
    private final Long counterNumSamples;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private BackupQueryCounter() {
        this((String) null, (String) null, (Long) null, (Long) null);
    }

    public BackupQueryCounter(String name, String counterUnit,
                            Long counterSum, Long counterNumSamples) {
        this.name = name;
        this.counterUnit = counterUnit;
        this.counterSum = counterSum;
        this.counterNumSamples = counterNumSamples;
    }

    public String getName() { return name; }
    public String getCounterUnit() { return counterUnit; }
    public Long getCounterSum() { return counterSum; }
    public Long getCounterNumSamples() { return counterNumSamples; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("counterUnit", counterUnit)
            .add("counterSum", counterSum)
            .add("counterNumSamples", counterNumSamples);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
