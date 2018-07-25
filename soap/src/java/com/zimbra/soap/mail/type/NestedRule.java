/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonArrayForWrapper;

// JsonPropertyOrder added to make sure JaxbToJsonTest.bug65572_BooleanAndXmlElements passes
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"filterVariables", "tests", "actions","child"})
@JsonPropertyOrder({"filterVariables", "tests", "actions","child"})
public final class NestedRule {

    /**
     * @zm-api-field-tag variables
     * @zm-api-field-description Filter Variables
     */
    @ZimbraJsonArrayForWrapper
    @XmlElement(name=MailConstants.E_FILTER_VARIABLES /* filterVariables */, required=false)
    private FilterVariables filterVariables;

    /**
     * @zm-api-field-description Filter tests
     */
    @XmlElement(name=MailConstants.E_FILTER_TESTS /* filterTests */, required=true)
    private FilterTests tests;

    /**
     * @zm-api-field-description Filter actions
     */
    @ZimbraJsonArrayForWrapper
    @XmlElementWrapper(name=MailConstants.E_FILTER_ACTIONS /* filterActions */, required=false)
    @XmlElements({
        @XmlElement(name=MailConstants.E_FILTER_VARIABLES /* filterVariables */, type=FilterVariables.class),
        @XmlElement(name=MailConstants.E_ACTION_KEEP /* actionKeep */, type=FilterAction.KeepAction.class),
        @XmlElement(name=MailConstants.E_ACTION_DISCARD /* actionDiscard */, type=FilterAction.DiscardAction.class),
        @XmlElement(name=MailConstants.E_ACTION_FILE_INTO /* actionFileInto */, type=FilterAction.FileIntoAction.class),
        @XmlElement(name=MailConstants.E_ACTION_FLAG /* actionFlag */, type=FilterAction.FlagAction.class),
        @XmlElement(name=MailConstants.E_ACTION_TAG /* actionTag */, type=FilterAction.TagAction.class),
        @XmlElement(name=MailConstants.E_ACTION_REDIRECT /* actionRedirect */, type=FilterAction.RedirectAction.class),
        @XmlElement(name=MailConstants.E_ACTION_REPLY /* actionReply */, type=FilterAction.ReplyAction.class),
        @XmlElement(name=MailConstants.E_ACTION_NOTIFY /* actionNotify */, type=FilterAction.NotifyAction.class),
        @XmlElement(name=MailConstants.E_ACTION_RFCCOMPLIANTNOTIFY /* action (RFC compliant) */, type=FilterAction.RFCCompliantNotifyAction.class),
        @XmlElement(name=MailConstants.E_ACTION_STOP /* actionStop */, type=FilterAction.StopAction.class),
        @XmlElement(name=MailConstants.E_ACTION_REJECT /* actionReject */, type=FilterAction.RejectAction.class),
        @XmlElement(name=MailConstants.E_ACTION_EREJECT /* actionEreject */, type=FilterAction.ErejectAction.class),
        @XmlElement(name=MailConstants.E_ACTION_LOG /* actionLog */, type=FilterAction.LogAction.class),
        @XmlElement(name=MailConstants.E_ACTION_ADDHEADER /* actionAddheader */, type=FilterAction.AddheaderAction.class),
        @XmlElement(name=MailConstants.E_ACTION_DELETEHEADER /* actionDeleteheader */, type=FilterAction.DeleteheaderAction.class),
        @XmlElement(name=MailConstants.E_ACTION_REPLACEHEADER /* actionReplaceddheader */, type=FilterAction.ReplaceheaderAction.class)
    })
    // in nested rule case, actions could be null.
    private List<FilterAction> actions;

    // For Nested Rule
    /**
     * @zm-api-field-description NestedRule child
     */
    @XmlElement(name=MailConstants.E_NESTED_RULE /* nestedRule */)
    private NestedRule child;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private NestedRule() {
        this(null);
    }
    
    public NestedRule(FilterTests tests) {
        this.tests = tests;
        this.actions = null;
        this.filterVariables = null;
    }
    public void setFilterTests(FilterTests value) {
        tests = value;
    }

    public void setFilterActions(Collection<FilterAction> list) {
        if (list != null) {
            if(actions == null){
                actions=Lists.newArrayList();
            }else{
                actions.clear();
            }
            actions.addAll(list);
        } else { // re-initialise actions
            actions = null;
        }
    }

    public NestedRule addFilterAction(FilterAction action) {
        if(actions == null){
            actions=Lists.newArrayList();
        }
        actions.add(action);
        return this;
    }

    public FilterTests getFilterTests() {
        return tests;
    }

    public List<FilterAction> getFilterActions() {
        // there must be no actions.size()==0 case. This is for just in case.
        if(actions == null || actions.size() == 0) {
            return null;
        }
        return Collections.unmodifiableList(actions);
    }

    /**
     * @param variables
     */
    public void setFilterVariables(FilterVariables filterVariables) {
        this.filterVariables = filterVariables;
    }

    /**
     * @return variables
     */
    public FilterVariables getFilterVariables() {
        return this.filterVariables;
    }

    public int getActionCount() {
        if(actions == null){
            return 0;
        }
        return actions.size();
    }

    // For Nested Rule
    public NestedRule getChild() {
        return child;
    }
    
    // For Nested Rule
    public void setChild(NestedRule nestedRule) {
        child = nestedRule;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("filterVariables", filterVariables)
            .add("tests", tests)
            .add("actions", actions)
            .add("child", child)
            .toString();
    }
}
