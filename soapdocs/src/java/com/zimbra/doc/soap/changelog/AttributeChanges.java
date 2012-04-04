/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
package com.zimbra.doc.soap.changelog;

import java.util.Map;

import com.zimbra.doc.soap.ValueDescription;
import com.zimbra.doc.soap.apidesc.SoapApiAttribute;
import com.zimbra.doc.soap.apidesc.SoapApiType;

/**
 * Encodes information about changes to an attribute between a baseline revision and the current revision
 * @author gren
 */
public class AttributeChanges {

    private final String xpath;
    private final SoapApiAttribute baseAttr;
    private final SoapApiAttribute currAttr;
    private final Map<String,SoapApiType> baselineTypes;
    private final Map<String,SoapApiType> currentTypes;
    
    private String baselineRepresentation;
    private String currentRepresentation;

    public AttributeChanges(String parentXpath, SoapApiAttribute baseAttr, SoapApiAttribute currAttr,
            Map<String,SoapApiType> baselineTypes, Map<String,SoapApiType> currentTypes) {
        this.xpath = parentXpath + "@" + baseAttr.getName();
        this.baseAttr = baseAttr;
        this.currAttr = currAttr;
        this.baselineTypes = baselineTypes;
        this.currentTypes = currentTypes;
        findChanges();
    }

    private void findChanges() {
        String baseJaxbName = baseAttr.getJaxb();
        String currJaxbName = currAttr.getJaxb();
        if (baseJaxbName != null) {
            baselineRepresentation = getRepresentation(baseJaxbName, baselineTypes, "baseline");
        } else {
            baselineRepresentation = getRepresentation(baseAttr.getValueType(), "baseline");
        }
        if (currJaxbName != null) {
            currentRepresentation = getRepresentation(currJaxbName, currentTypes, "comparison");
        } else {
            currentRepresentation = getRepresentation(currAttr.getValueType(), "comparison");
        }
    }
    
    private String getRepresentation(String jaxbName, Map<String,SoapApiType> apiTypes, String marker) {
        String representation;
        SoapApiType apiType = apiTypes.get(jaxbName);
        if (apiType == null) {
            representation = String.format("Problem for %s (%s)", xpath, marker);
        } else {
            representation = apiType.getType().getRepresentation();
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
