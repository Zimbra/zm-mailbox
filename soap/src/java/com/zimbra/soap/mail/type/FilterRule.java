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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.codehaus.jackson.annotate.JsonPropertyOrder;

import com.google.common.base.Objects;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonArrayForWrapper;
import com.zimbra.soap.type.ZmBoolean;

// JsonPropertyOrder added to make sure JaxbToJsonTest.bug65572_BooleanAndXmlElements passes
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"filterVariables"})
@JsonPropertyOrder({ "name", "active", "filterVariables"})
public final class FilterRule extends NestedRule  {

    /**
     * @zm-api-field-tag rule-name
     * @zm-api-field-description Rule name
     */
    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=true)
    private String name;

    /**
     * @zm-api-field-tag active-flag
     * @zm-api-field-description Active flag.  Set by default.
     */
    @XmlAttribute(name=MailConstants.A_ACTIVE /* active */, required=true)
    private ZmBoolean active;

    /**
     * @zm-api-field-tag variables
     * @zm-api-field-description Filter Variables
     */
    @ZimbraJsonArrayForWrapper
    @XmlElement(name=MailConstants.E_FILTER_VARIABLES /* filterVariables */, required=false)
    private FilterVariables filterVariables;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private FilterRule() {
        this(null, false);
    }

    public FilterRule(String name, boolean active) {
        this.name = name;
        this.active = ZmBoolean.fromBool(active);
        this.actions = null;
        this.filterVariables = null;
    }

    public FilterRule(String name, FilterTests tests, boolean active) {
        this.name = name;
        this.tests = tests;
        this.active = ZmBoolean.fromBool(active);
        this.actions = null;
        this.filterVariables = null;
    }

    public FilterRule(String name, FilterTests tests, boolean active, FilterVariables filterVariables) {
        this.name = name;
        this.tests = tests;
        this.active = ZmBoolean.fromBool(active);
        this.actions = null;
        this.filterVariables = filterVariables;
    }

    public static FilterRule createForNameFilterTestsAndActiveSetting(String name, FilterTests tests, boolean active) {
        return new FilterRule(name, tests, active);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return ZmBoolean.toBool(active);
    }

    public void setActive(ZmBoolean active) {
        this.active = active;
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
    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
                .add("name", name)
                .add("active", active)
                .add("filterVariables", filterVariables);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }

}
