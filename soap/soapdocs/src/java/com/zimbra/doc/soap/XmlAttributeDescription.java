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

package com.zimbra.doc.soap;

import com.google.common.base.Strings;
import com.zimbra.soap.util.JaxbAttributeInfo;

public class XmlAttributeDescription
implements XmlUnit {
    private XmlElementDescription parent;
    private final String name;
    private final boolean required;
    private final String fieldName;
    private ValueDescription valueDescription;
    private Class<?> jaxbClass;
    private String description;
    private String fieldTag;

    private XmlAttributeDescription(String name, boolean required, String fieldName) {
        this.name = name;
        this.required = required;
        this.fieldName = fieldName;
    }

    public static XmlAttributeDescription create(XmlElementDescription parent, JaxbAttributeInfo info) {
        XmlAttributeDescription desc = new XmlAttributeDescription(
                info.getName(), info.isRequired(), info.getFieldName());
        desc.parent = parent;
        desc.jaxbClass = info.getAtomClass();
        desc.valueDescription = ValueDescription.create(desc.jaxbClass);
        return desc;
    }

    @Override
    public String getName() { return name; }
    public boolean isRequired() { return required; }
    public String getValueRepresentation() {
        return (valueDescription == null) ? "" : Strings.nullToEmpty(valueDescription.getRepresentation());
    }

    public void setFieldTag(String fieldTag) { this.fieldTag = fieldTag; }
    public String getFieldTag() { return Strings.nullToEmpty(fieldTag); }

    public void setDescription(String description) { this.description = description; }

    @Override
    public String getDescriptionForTable() {
        return getDescription();
    }

    public String getRawDescription() {
        return description;
    }

    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        String valueRepresentation = getValueRepresentation();
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

    public String getFieldName() { return Strings.nullToEmpty(fieldName); }

    /**
     * @return Visible length added
     */
    public int writeHtmlDescription(StringBuilder desc, int depth) {
        int visibleLen = 0;
        if (!required) {
            desc.append("[");
            visibleLen++;
        }
        String attrName = String.format("<a href=\"#%s\">%s</a>", tableLinkTargetName(), name);
        visibleLen = visibleLen + name.length();
        desc.append("<span id=\"attrName\">").append(attrName).append("</span>");
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
        visibleLen = visibleLen + valRep.length();
        desc.append("=<span id=\"value\">").append(valRep).append("</span>");
        if (!required) {
            desc.append("]");
            visibleLen++;
        }
        return visibleLen;
    }

    @Override
    public String getTableKeyColumnContents() {
        String retval = String.format("<a name=\"%s\">%s</a>", tableLinkTargetName(), getXPath());
        return retval;
    }

    public String getXPath() {
        if (parent == null) {
            return String.format("@%s", name);
        } else {
            return String.format("%s@%s", parent.getXPath(), name);
        }
    }

    public String tableLinkTargetName() {
        String retval;
        if (parent == null) {
            retval = String.format("tbl-%s", name);
        } else {
            retval = String.format("%s-%s", parent.tableLinkTargetName(), name);
        }
        return retval;
    }


    @Override
    public OccurrenceSpec getOccurrence() {
        return required ? OccurrenceSpec.REQUIRED : OccurrenceSpec.OPTIONAL;
    }

    public Class<?> getJaxbClass() {
        return jaxbClass;
    }

    public ValueDescription getValueDescription() {
        return valueDescription;
    }
}
