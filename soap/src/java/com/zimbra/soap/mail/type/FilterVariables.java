/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.mail.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonArrayForWrapper;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"variables"})
@JsonPropertyOrder({ "variables" })
public final class FilterVariables extends FilterAction {

    @XmlElement(name=MailConstants.E_FILTER_VARIABLE /* filterVariable */, required=true)
    private List<FilterVariable> variables;

    /**
     * @return the variables
     */
    @ZimbraJsonArrayForWrapper
    public List<FilterVariable> getVariables() {
        return variables;
    }

    /**
     * @param variables the variables to set
     */
    public void setVariables(Collection<FilterVariable> variables) {
        if(variables != null) {
            if(this.variables == null) {
                this.variables = Lists.newArrayList();
            }
            for (FilterVariable filterVariable : variables) {
                if(filterVariable != null) {
                    this.variables.add(filterVariable);
                }
            }
        }
    }

    /**
     * no-argument constructor wanted by JAXB
     */
    public FilterVariables() {
        this(Lists.newArrayList());
    }

    public FilterVariables(Collection<FilterVariable> variables) {
        this.variables = (ArrayList<FilterVariable>) variables;
    }

    public void addFilterVariable(FilterVariable filterVariable) {
        if(filterVariable != null) {
            if(variables == null) {
                variables = Lists.newArrayList();
            }
            variables.add(filterVariable);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("variables", variables)
            .toString();
    }
}
