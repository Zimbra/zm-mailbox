/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.type;

import com.google.common.base.Objects;
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

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("condition", condition);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
