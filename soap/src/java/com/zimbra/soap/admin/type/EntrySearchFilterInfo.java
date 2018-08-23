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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.base.EntrySearchFilterInterface;
import com.zimbra.soap.type.SearchFilterCondition;

@XmlAccessorType(XmlAccessType.NONE)
public class EntrySearchFilterInfo
implements EntrySearchFilterInterface {

    /**
     * @zm-api-field-description Search filter conditions
     */
    @XmlElements({
        @XmlElement(name=AccountConstants.E_ENTRY_SEARCH_FILTER_MULTICOND /* conds */,
            type=EntrySearchFilterMultiCond.class),
        @XmlElement(name=AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND /* cond */,
            type=EntrySearchFilterSingleCond.class)
    })
    private SearchFilterCondition condition;

    public EntrySearchFilterInfo() {
    }

    public EntrySearchFilterInfo(SearchFilterCondition condition) {
        this.setCondition(condition);
    }

    @Override
    public void setCondition(SearchFilterCondition condition) { this.condition = condition; }
    @Override
    public SearchFilterCondition getCondition() { return condition; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("condition", condition);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
