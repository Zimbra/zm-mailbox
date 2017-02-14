/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.doc.soap.changelog;

import java.util.Map;

import com.zimbra.doc.soap.ValueDescription;
import com.zimbra.doc.soap.apidesc.SoapApiNamedElement;
import com.zimbra.doc.soap.apidesc.SoapApiSimpleElement;
import com.zimbra.doc.soap.apidesc.SoapApiType;

/**
 * Encodes information about changes to the value of an element between a baseline revision and the current revision
 * @author gren
 */
public class ElementChanges {

    private final static String NO_VALUE = "[NONE]";
    
    private final String xpath;
    private final SoapApiNamedElement baseElem;
    private final SoapApiNamedElement currElem;
    private final Map<String,SoapApiType> baselineTypes;
    private final Map<String,SoapApiType> currentTypes;
    
    private String baselineRepresentation;
    private String currentRepresentation;

    public ElementChanges(String xpath, SoapApiNamedElement baseElem, SoapApiNamedElement currElem,
            Map<String,SoapApiType> baselineTypes, Map<String,SoapApiType> currentTypes) {
        this.xpath = xpath;
        this.baseElem = baseElem;
        this.currElem = currElem;
        this.baselineTypes = baselineTypes;
        this.currentTypes = currentTypes;
        findChanges();
    }

    private void findChanges() {
        baselineRepresentation = valueDescription(baseElem, baselineTypes, "baseline");
        currentRepresentation = valueDescription(currElem, currentTypes, "comparison");
    }

    private String valueDescription(SoapApiNamedElement elem, Map<String,SoapApiType> apiTypes, String marker) {
        if (elem instanceof SoapApiSimpleElement) {
            SoapApiSimpleElement simple = (SoapApiSimpleElement) elem;
            String jaxbName = simple.getJaxb();
            if (jaxbName != null) {
                return getRepresentation(jaxbName, apiTypes, marker);
            } else {
                return simple.getType() == null ? NO_VALUE : simple.getType();
            }
        } else {
            return NO_VALUE;
        }
    }

    private String getRepresentation(String jaxbName, Map<String,SoapApiType> apiTypes, String marker) {
        String representation;
        SoapApiType apiType = apiTypes.get(jaxbName);
        if (apiType == null) {
            representation = String.format("Problem for %s (%s)", xpath, marker);
        } else {
            ValueDescription valueDesc = apiType.getValueType();
            if (valueDesc == null) {
                return NO_VALUE;
            } else {
                return getRepresentation(valueDesc, marker);
            }
        }
        return representation;
    }

    private String getRepresentation(ValueDescription desc, String marker) {
        String representation;
        if (desc == null) {
            representation = String.format("Problem for %s (%s)", xpath, marker);
        } else {
            representation = desc.getRepresentation();
        }
        return representation;
    }

    public String getXpath() { return xpath; }
    public boolean isSame() {
        return baselineRepresentation.equals(currentRepresentation);
    }
    public String getBaselineRepresentation() { return baselineRepresentation; }
    public String getCurrentRepresentation() { return currentRepresentation; }
}
