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

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.SearchFilterCondition;

@XmlAccessorType(XmlAccessType.FIELD)
public class EntrySearchFilterMultiCond implements SearchFilterCondition {

    @XmlAttribute(name=AccountConstants.A_ENTRY_SEARCH_FILTER_NEGATION, required=false)
    private Boolean not;
    @XmlAttribute(name=AccountConstants.A_ENTRY_SEARCH_FILTER_OR, required=false)
    private Boolean or;

    @XmlElements({
        @XmlElement(name=AccountConstants.E_ENTRY_SEARCH_FILTER_MULTICOND,
            type=EntrySearchFilterMultiCond.class),
        @XmlElement(name=AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND,
            type=EntrySearchFilterSingleCond.class)
    })
    private List <SearchFilterCondition> conditions = Lists.newArrayList();

    public EntrySearchFilterMultiCond() {
    }

    public EntrySearchFilterMultiCond setConditions(Collection<SearchFilterCondition> conditions) {
        this.conditions.clear();
        if (conditions != null) {
            this.conditions.addAll(conditions);
        }
        return this;
    }

    public EntrySearchFilterMultiCond addCondition(SearchFilterCondition condition) {
        conditions.add(condition);
        return this;
    }


    public void setNot(Boolean not) { this.not = not; }
    public void setOr(Boolean or) { this.or = or; }

    public Boolean isNot() { return not; }
    public Boolean isOr() { return or; }

    public List<SearchFilterCondition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }
}
