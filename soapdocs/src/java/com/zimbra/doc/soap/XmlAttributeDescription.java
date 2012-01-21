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

package com.zimbra.doc.soap;

import com.google.common.base.Strings;
import com.zimbra.soap.util.JaxbAttributeInfo;

public class XmlAttributeDescription
implements XmlUnit {
    private XmlElementDescription parent;
    private String name;
    private boolean required;
    private String valueRepresentation;
    private Class<?> jaxbClass;
    private String fieldName;
    private String description;
    private String fieldTag;

    private XmlAttributeDescription() {
    }

    public static XmlAttributeDescription create(XmlElementDescription parent, JaxbAttributeInfo info) {
        XmlAttributeDescription desc = new XmlAttributeDescription();
        desc.parent = parent;
        desc.name = info.getName();
        desc.required = info.isRequired();
        desc.jaxbClass = info.getAtomClass();
        desc.fieldName = info.getFieldName();
        desc.valueRepresentation = ValueDescription.getRepresentation(desc.jaxbClass);
        return desc;
    }

    @Override
    public String getName() { return name; }
    public boolean isRequired() { return required; }
    public String getValueRepresentation() { return Strings.nullToEmpty(valueRepresentation); }

    public void setFieldTag(String fieldTag) { this.fieldTag = fieldTag; }
    public String getFieldTag() { return Strings.nullToEmpty(fieldTag); }

    public void setDescription(String description) { this.description = description; }
    @Override
    public String getDescription() {
    	StringBuilder desc = new StringBuilder();
        if (!Strings.isNullOrEmpty(valueRepresentation)) {
        	desc.append("<b>Type:").append(valueRepresentation).append("</b>");
	    	if (!Strings.isNullOrEmpty(description)) {
	        	desc.append("<br />\n").append(description);
	    	}
        } else if (!Strings.isNullOrEmpty(description)) {
        	desc.append(description);
    	}
        return desc.toString();
    }


    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getFieldName() { return Strings.nullToEmpty(fieldName); }

    public void writeHtmlDescription(StringBuilder desc, int depth) {
        if (!required) {
            desc.append("[");
        }
        desc.append("<span id=\"attrName\">").append(name).append("</span>");
        String valRep = getValueRepresentation();
        String ftag = getFieldTag();
        if ("String".equals(valRep)) {
            if (Strings.isNullOrEmpty(ftag)) {
                valRep = "\"...\"";
            } else {
                valRep = String.format("\"{%s}\"", ftag);
            }
        } else {
            if (Strings.isNullOrEmpty(ftag)) {
                valRep = String.format("\"(%s)\"", valRep);
            } else {
                valRep = String.format("\"{%s} (%s)\"", ftag, valRep);
            }
        }
        desc.append("=<span id=\"value\">").append(valRep).append("</span>");
        if (!required) {
            desc.append("]");
        }
    }

    @Override
    public String getXPath() {
        if (parent == null) {
            return String.format("@%s", name);
        } else {
            return String.format("%s@%s", parent.getXPath(), name);
        }
    }

    @Override
    public OccurrenceSpec getOccurrence() {
        return required ? OccurrenceSpec.REQUIRED : OccurrenceSpec.OPTIONAL;
    }
}
