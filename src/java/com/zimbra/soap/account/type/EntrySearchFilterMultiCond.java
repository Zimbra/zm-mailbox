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

package com.zimbra.soap.account.type;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.SearchFilterCondition;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class EntrySearchFilterMultiCond
implements SearchFilterCondition {

    /**
     * @zm-api-field-tag not
     * @zm-api-field-description Negation flag
     * <br />
     * If set to <b>1 (true)</b> then negate the compound condition
     */
    @XmlAttribute(name=AccountConstants.A_ENTRY_SEARCH_FILTER_NEGATION /* not */, required=false)
    private ZmBoolean not;

    /**
     * @zm-api-field-tag or
     * @zm-api-field-description OR flag
     * <table>
     * <tr> <td> <b>1 (true)</b> </td> <td> child conditions are OR'ed together </td> </tr>
     * <tr> <td> <b>0 (false) [default]</b> </td> <td> child conditions are AND'ed together </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AccountConstants.A_ENTRY_SEARCH_FILTER_OR /* or */, required=false)
    private ZmBoolean or;

    /**
     * @zm-api-field-description Compound condition or simple condition
     */
    @XmlElements({
        @XmlElement(name=AccountConstants.E_ENTRY_SEARCH_FILTER_MULTICOND /* conds */,
            type=EntrySearchFilterMultiCond.class),
        @XmlElement(name=AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND /* cond */,
            type=EntrySearchFilterSingleCond.class)
    })
    private List<SearchFilterCondition> conditions = Lists.newArrayList();

    public EntrySearchFilterMultiCond() {
    }

    @Override
    public void setNot(Boolean not) { this.not = ZmBoolean.fromBool(not); }
    public void setOr(Boolean or) { this.or = ZmBoolean.fromBool(or); }
    public void setConditions(Iterable <SearchFilterCondition> conditions) {
        this.conditions.clear();
        if (conditions != null) {
            Iterables.addAll(this.conditions,conditions);
        }
    }

    public void addCondition(SearchFilterCondition condition) {
        this.conditions.add(condition);
    }

    @Override
    public Boolean isNot() { return ZmBoolean.toBool(not); }
    public Boolean isOr() { return ZmBoolean.toBool(or); }
    public List<SearchFilterCondition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("not", not)
            .add("or", or)
            .add("conditions", conditions);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
