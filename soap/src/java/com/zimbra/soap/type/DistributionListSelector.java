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

package com.zimbra.soap.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.DistributionListBy;

@XmlAccessorType(XmlAccessType.NONE)
public class DistributionListSelector {

    /**
     * @zm-api-field-tag dl-selector-by
     * @zm-api-field-description Select the meaning of <b>{dl-selector-key}</b>
     */
    @XmlAttribute(name=AdminConstants.A_BY, required=true)
    private final DistributionListBy dlBy;

    /**
     * @zm-api-field-tag dl-selector-key
     * @zm-api-field-description The key used to identify the account. Meaning determined by <b>{dl-selector-by}</b>
     */
    @XmlValue
    private final String key;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DistributionListSelector() {
        this.dlBy = null;
        this.key = null;
    }

    public DistributionListSelector(DistributionListBy by, String key) {
        this.dlBy = by;
        this.key = key;
    }

    public String getKey() { return key; }

    public DistributionListBy getBy() { return dlBy; }

    public static DistributionListSelector fromId(String id) {
        return new DistributionListSelector(DistributionListBy.id, id);
    }

    public static DistributionListSelector fromName(String name) {
        return new DistributionListSelector(DistributionListBy.name, name);
    }
}
