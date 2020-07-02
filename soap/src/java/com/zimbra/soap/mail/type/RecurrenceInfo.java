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

package com.zimbra.soap.mail.type;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.RecurRuleBaseInterface;
import com.zimbra.soap.base.RecurrenceInfoInterface;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_RECURRENCE_INFORMATION, description="Recurrence Information")
public class RecurrenceInfo
implements RecurRuleBase, RecurrenceInfoInterface {

    /**
     * @zm-api-field-description Recurrence rules
     */
    @XmlElements({
        @XmlElement(name=MailConstants.E_CAL_ADD /* add */, type=AddRecurrenceInfo.class),
        @XmlElement(name=MailConstants.E_CAL_EXCLUDE /* exclude */, type=ExcludeRecurrenceInfo.class),
        @XmlElement(name=MailConstants.E_CAL_EXCEPT /* except */, type=ExceptionRuleInfo.class),
        @XmlElement(name=MailConstants.E_CAL_CANCEL /* cancel */, type=CancelRuleInfo.class),
        @XmlElement(name=MailConstants.E_CAL_DATES /* dates */, type=SingleDates.class),
        @XmlElement(name=MailConstants.E_CAL_RULE /* rule */, type=SimpleRepeatingRule.class)
    })
    @GraphQLIgnore
    private final List<RecurRuleBase> rules = Lists.newArrayList();

    public RecurrenceInfo() {
    }

    @GraphQLIgnore
    public static RecurrenceInfo create(RecurRuleBase rule) {
        RecurrenceInfo ri = new RecurrenceInfo();
        ri.addRule(rule);
        return ri;
    }

    @GraphQLIgnore
    public void setRules(Iterable <RecurRuleBase> rules) {
        this.rules.clear();
        if (rules != null) {
            Iterables.addAll(this.rules,rules);
        }
    }

    @GraphQLIgnore
    public RecurrenceInfo addRule(RecurRuleBase rule) {
        this.rules.add(rule);
        return this;
    }

    @GraphQLIgnore
    public List<RecurRuleBase> getRules() {
        return Collections.unmodifiableList(rules);
    }

    @Override
    @GraphQLIgnore
    public void setRuleInterfaces(Iterable<RecurRuleBaseInterface> rules) {
        setRules(RecurrenceInfo.fromInterfaces(rules));
    }

    @Override
    @GraphQLIgnore
    public void addRuleInterface(RecurRuleBaseInterface rule) {
        addRule((RecurRuleBase) rule);
    }

    @Override
    @GraphQLIgnore
    public List<RecurRuleBaseInterface> getRuleInterfaces() {
        return RecurrenceInfo.toInterfaces(rules);
    }

    public static Iterable <RecurRuleBase> fromInterfaces(
                    Iterable <RecurRuleBaseInterface> params) {
        if (params == null)
            return null;
        List <RecurRuleBase> newList = Lists.newArrayList();
        for (RecurRuleBaseInterface param : params) {
            newList.add((RecurRuleBase) param);
        }
        return newList;
    }

    public static List <RecurRuleBaseInterface> toInterfaces(
            Iterable <RecurRuleBase> params) {
        if (params == null)
            return null;
        List <RecurRuleBaseInterface> newList = Lists.newArrayList();
        for (RecurRuleBase param : params) {
            newList.add((RecurRuleBaseInterface) param);
        }
        return newList;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("rules", rules);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
    
    // methods below are added for zm-gql to bypass interface issues
    // remove these and properly annotate the rules classes
    // once some graphql dependencies have been updated
    
    @GraphQLInputField(name=GqlConstants.RULES_ADD, description="Recurrence rules for adding")
    public void setRulesAdd(List<AddRecurrenceInfo> addRecurrenceInfos) {
        addRules(addRecurrenceInfos);
    }
    @GraphQLQuery(name=GqlConstants.RULES_ADD, description="Recurrence rules for adding")
    public List<AddRecurrenceInfo> getRulesAdd() {
        return rules.stream()
            .filter(r -> (r instanceof AddRecurrenceInfo))
            .map(r -> (AddRecurrenceInfo) r)
            .collect(Collectors.toList());
    }
    @GraphQLInputField(name=GqlConstants.RULES_EXCLUDE, description="Recurrence rules for excluding")
    public void setRulesExclude(List<ExcludeRecurrenceInfo> excludeRecurrenceInfos) {
        addRules(excludeRecurrenceInfos);
    }
    @GraphQLQuery(name=GqlConstants.RULES_EXCLUDE, description="Recurrence rules for excluding")
    public List<ExcludeRecurrenceInfo> getRulesExclude() {
        return rules.stream()
            .filter(r -> (r instanceof ExcludeRecurrenceInfo))
            .map(r -> (ExcludeRecurrenceInfo) r)
            .collect(Collectors.toList());
    }
    @GraphQLInputField(name=GqlConstants.RULES_EXCEPT, description="Recurrence rules for excepting")
    public void setRulesExcept(List<ExceptionRuleInfo> exceptionRuleInfos) {
        addRules(exceptionRuleInfos);
    }
    @GraphQLQuery(name=GqlConstants.RULES_EXCEPT, description="Recurrence rules for excepting")
    public List<ExceptionRuleInfo> getRulesExcept() {
        return rules.stream()
            .filter(r -> (r instanceof ExceptionRuleInfo))
            .map(r -> (ExceptionRuleInfo) r)
            .collect(Collectors.toList());
    }
    @GraphQLInputField(name=GqlConstants.RULES_CANCEL, description="Recurrence rules for canceling")
    public void setRulesCancel(List<CancelRuleInfo> cancelRuleInfos) {
        addRules(cancelRuleInfos);
    }
    @GraphQLQuery(name=GqlConstants.RULES_CANCEL, description="Recurrence rules for canceling")
    public List<CancelRuleInfo> getRulesCancel() {
        return rules.stream()
            .filter(r -> (r instanceof CancelRuleInfo))
            .map(r -> (CancelRuleInfo) r)
            .collect(Collectors.toList());
    }
    @GraphQLInputField(name=GqlConstants.RULES_DATES, description="Recurrence rules for dates")
    public void setRulesDates(List<SingleDates> singleDates) {
        addRules(singleDates);
    }
    @GraphQLQuery(name=GqlConstants.RULES_DATES, description="Recurrence rules for dates")
    public List<SingleDates> getRulesDates() {
        return rules.stream()
            .filter(r -> (r instanceof SingleDates))
            .map(r -> (SingleDates) r)
            .collect(Collectors.toList());
    }
    @GraphQLInputField(name=GqlConstants.RULES_SIMPLE, description="Simple recurrence rules")
    public void setRulesSimple(List<SimpleRepeatingRule> simpleRepeatingRules) {
        addRules(simpleRepeatingRules);
    }
    @GraphQLQuery(name=GqlConstants.RULES_SIMPLE, description="Simple recurrence rules")
    public List<SimpleRepeatingRule> getRulesSimple() {
        return rules.stream()
            .filter(r -> (r instanceof SimpleRepeatingRule))
            .map(r -> (SimpleRepeatingRule) r)
            .collect(Collectors.toList());
    }
    private void addRules(Iterable<? extends RecurRuleBase> recurrenceRule) {
        if (recurrenceRule != null) {
            recurrenceRule.iterator().forEachRemaining(i -> addRule(i));
        }
    }
}
