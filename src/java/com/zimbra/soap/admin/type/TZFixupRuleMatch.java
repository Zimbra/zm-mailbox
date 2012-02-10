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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.Id;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class TZFixupRuleMatch {

    /**
     * @zm-api-field-description Match rules:
     * <table>
     * <tr> <td> <b> &lt;any&gt; </b> </td> <td> match any timezone </td> </tr>
     * <tr> <td> <b> &lt;tzid&gt; </b> </td> <td> match the timezone's TZID string </td> </tr>
     * <tr> <td> <b> &lt;nonDst&gt; </b> </td>
     *      <td> match the GMT offset of a timezone that doesn't use daylight saving time </td> </tr>
     * <tr> <td> <b> &lt;rules&gt; </b> </td>
     *      <td> match DST timezone based on transition rules specified as month/week number in month/week day </td> </tr>
     * <tr> <td> <b> &lt;dates&gt; </b> </td>
     *      <td> match DST timezone based on transition rules specified as month and day of month </td> </tr>
     * </table>
     */
    @XmlElements({
        @XmlElement(name=AdminConstants.E_ANY /* any */, type=SimpleElement.class),
        @XmlElement(name=AdminConstants.E_TZID /* tzid */, type=Id.class),
        @XmlElement(name=AdminConstants.E_NON_DST /* nonDst */, type=Offset.class),
        @XmlElement(name=AdminConstants.E_RULES /* rules */, type=TZFixupRuleMatchRules.class),
        @XmlElement(name=AdminConstants.E_DATES /* dates */, type=TZFixupRuleMatchDates.class)
    })
    private List<Object> elements = Lists.newArrayList();

    public TZFixupRuleMatch() {
    }

    public void setElements(Iterable <Object> elements) {
        this.elements.clear();
        if (elements != null) {
            Iterables.addAll(this.elements,elements);
        }
    }

    public void addElement(Object element) {
        this.elements.add(element);
    }

    public List<Object> getElements() {
        return Collections.unmodifiableList(elements);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("elements", elements);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
