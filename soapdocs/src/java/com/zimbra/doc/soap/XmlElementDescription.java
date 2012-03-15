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

import java.util.List;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.zimbra.soap.util.JaxbAttributeInfo;
import com.zimbra.soap.util.JaxbElementInfo;
import com.zimbra.soap.util.JaxbInfo;
import com.zimbra.soap.util.JaxbNodeInfo;
import com.zimbra.soap.util.JaxbPseudoNodeChoiceInfo;
import com.zimbra.soap.util.JaxbValueInfo;
import com.zimbra.soap.util.WrappedElementInfo;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/**
 * Represents a description of an XML element and all the attributes and elements it contains
 */
public class XmlElementDescription
implements DescriptionNode, XmlUnit {
    private final static int INDENT_CHAR_NUM = 4;
    private final static int WRAP_COLUMN_GUIDE = 320;
    DescriptionNode parent;
    private List<XmlAttributeDescription> attribs = Lists.newArrayList();
    private List<DescriptionNode> children = Lists.newArrayList();
    private boolean isOptional;
    private boolean isSingleton;
    private boolean isInnerRecursionElement;
    private int minOccurs;
    private String name;
    private String targetNamespace;
    private Class<?> jaxbClass;
    private String typeName;
    private String typeIdString;
    private String fieldName;
    private String valueFieldName;    // Field that has @XmlValue annotation
    private String valueFieldDescription;
    private String elementText;
    private String description;
    private String fieldTag;

    private static final Logger LOG = Logger.getLogger(DescriptionNode.class);
    static {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        LOG.setLevel(Level.INFO);
    }

    public XmlElementDescription() {
    }

    public static XmlElementDescription createTopLevel(JaxbInfo jaxbInfo, String namespace, String name) {
        XmlElementDescription desc = new XmlElementDescription();
        desc.parent = null;
        desc.isOptional = false;
        desc.isSingleton = true;
        desc.isInnerRecursionElement = false;
        desc.minOccurs = 1;
        desc.targetNamespace = namespace;
        desc.name = name;
        desc.jaxbClass = jaxbInfo.getJaxbClass();
        desc.fieldName = null;
        processTypeInfo(desc, desc.jaxbClass);
        addXmlInfo(desc, jaxbInfo);
        return desc;
    }

    private static XmlElementDescription createForElement(DescriptionNode parent, JaxbElementInfo nodeInfo) {
        XmlElementDescription desc = new XmlElementDescription();
        desc.parent = parent;
        desc.minOccurs = nodeInfo.isRequired() ? 1 : 0;
        desc.isSingleton = ! nodeInfo.isMultiElement();
        desc.targetNamespace = nodeInfo.getNamespace();
        desc.name = nodeInfo.getName();
        desc.jaxbClass = nodeInfo.getAtomClass();
        desc.elementText = "";
        desc.fieldName = nodeInfo.getFieldName();
        processTypeInfo(desc, nodeInfo.getAtomClass());
        desc.isInnerRecursionElement = isInnerRecursion(parent, desc.typeName);
        if (!desc.isInnerRecursionElement) {
            JaxbInfo jaxbInfo = getJaxbInfoForClass(nodeInfo.getAtomClass());
            if (jaxbInfo != null) {
                addXmlInfo(desc, jaxbInfo);
            }
        }
        return desc;
    }

    private static XmlElementDescription createForWrappedElement(DescriptionNode parent, WrappedElementInfo nodeInfo) {
        XmlElementDescription desc = new XmlElementDescription();
        desc.parent = parent;
        desc.minOccurs = 1;
        desc.isSingleton = true;
        desc.targetNamespace = nodeInfo.getNamespace();
        desc.name = nodeInfo.getName();
        desc.isInnerRecursionElement = false;
        desc.jaxbClass = null;
        desc.typeName = "";
        desc.typeIdString = "";
        desc.elementText = "";
        desc.fieldName = null;
        for (JaxbNodeInfo child : nodeInfo.getElements()) {
            if (child instanceof JaxbElementInfo) {
                desc.children.add(XmlElementDescription.createForElement(desc, (JaxbElementInfo)child));
            } else if (child instanceof JaxbPseudoNodeChoiceInfo) {
                JaxbPseudoNodeChoiceInfo jaxbChoice = (JaxbPseudoNodeChoiceInfo) child;
                ChoiceNode choiceNode = new ChoiceNode(jaxbChoice.isMultiElement());
                desc.children.add(choiceNode);
                for (JaxbElementInfo choice : jaxbChoice.getElements()) {
                    choiceNode.addChild(XmlElementDescription.createForElement(desc, choice));
                }
            }
        }
        return desc;
    }

    /**
     * Populate {@link desc} with value, attribute and sub-element information
     */
    private static void addXmlInfo(XmlElementDescription desc, JaxbInfo jaxbInfo) {
        JaxbValueInfo valueInfo = jaxbInfo.getElementValue();
        if (valueInfo != null) {
            desc.valueFieldName = valueInfo.getFieldName();
            desc.elementText = ValueDescription.getRepresentation(desc.valueFieldName, valueInfo.getAtomClass());
        }
        for (JaxbAttributeInfo attrInfo : jaxbInfo.getAttributes()) {
            desc.attribs.add(XmlAttributeDescription.create(desc, attrInfo));
        }
        for (JaxbNodeInfo nodeInfo : jaxbInfo.getJaxbNodeInfos()) {
            if (nodeInfo instanceof WrappedElementInfo) {
                desc.children.add(XmlElementDescription.createForWrappedElement(desc, (WrappedElementInfo) nodeInfo));
            }
            else if (nodeInfo instanceof JaxbElementInfo) {
                desc.children.add(XmlElementDescription.createForElement(desc, (JaxbElementInfo) nodeInfo));
            } else if (nodeInfo instanceof JaxbPseudoNodeChoiceInfo) {
                JaxbPseudoNodeChoiceInfo jaxbChoice = (JaxbPseudoNodeChoiceInfo) nodeInfo;
                ChoiceNode choiceNode = new ChoiceNode(jaxbChoice.isMultiElement());
                desc.children.add(choiceNode);
                for (JaxbElementInfo choice : jaxbChoice.getElements()) {
                    choiceNode.addChild(XmlElementDescription.createForElement(desc, choice));
                }
            }
        }
    }

    private static JaxbInfo getJaxbInfoForClass(Class<?> klass) {
        JaxbInfo jaxbInfo = null;
        if (klass == null || klass.getName().startsWith("com.zimbra.soap")) {
            jaxbInfo = JaxbInfo.getFromCache(klass);
        }
        return jaxbInfo;
    }

    private static void processTypeInfo(XmlElementDescription desc, Class<?> klass) {
        desc.typeName = ValueDescription.getRepresentation(klass);
        if (klass.isPrimitive() || klass.getName().startsWith("java.lang.")) {
            desc.typeIdString = "";
            String ftag = desc.getFieldTag();
            if (Strings.isNullOrEmpty(ftag)) {
                desc.elementText = desc.typeName;
            } else {
                desc.elementText = String.format("{%s} (%s)", ftag, desc.typeName);
            }
        } else {
            desc.typeIdString = String.format(" ## %s", desc.typeName);
        }
    }

    public String getHtmlDescription() {
        if (Strings.isNullOrEmpty(name)) {
            return "";
        }
        StringBuilder desc = new StringBuilder();
        writeDescription(desc, 1);
        return desc.toString();
    }

    @Override
    public void writeDescription(StringBuilder desc, int depth) {
        writeRequiredIndentation(desc, true, depth);
        writeStartOptionalInfo(desc);
        writeStartMultiElementInfo(desc);
        desc.append("&lt;<span id=\"elementName\">").append(name).append("</span>");
        if (isInnerRecursionElement) {
            desc.append(" />");
            desc.append(typeIdString);
            desc.append(" # inside itself");
        } else {
            if (attribs.size() > 0) {
                int attrIndentSize = name.length() + 4;
                int fullIndentSize = indentForCurrentElement(depth) + attrIndentSize;
                int sofar = fullIndentSize;
                StringBuilder attrSb = new StringBuilder();
                for (XmlAttributeDescription attrib : attribs) {
                    attrSb.setLength(0);
                    attrSb.append(" ");
                    attrib.writeHtmlDescription(attrSb, depth);
                    if (sofar + attrSb.length() > WRAP_COLUMN_GUIDE) {
                        // wrap to next line
                        desc.append("<br />\n");
                        writeNbsp(desc, fullIndentSize);
                        sofar = fullIndentSize;
                    }
                    sofar += attrSb.length();
                    desc.append(attrSb);
                }
            }
            if (getChildren().size() == 0) {
                if (!Strings.isNullOrEmpty(elementText)) {
                    desc.append(">");  // end of opening element tag
                    desc.append("<span id=\"value\">").append(elementText).append("</span>");
                    writeClosingElementTag(desc);
                    desc.append(typeIdString);
                } else {
                    /* can use just the opening element tag */
                    desc.append(" />");
                    desc.append(typeIdString);
                }
            } else {
                desc.append(">").append(typeIdString);  // tagged against opening element
                desc.append("<br />\n");
                for (DescriptionNode child : getChildren()) {
                    child.writeDescription(desc, depth+1);
                }
                if (!Strings.isNullOrEmpty(elementText)) {
                    writeIndentForCurrentElement(desc, depth);
                    desc.append("<span id=\"value\">").append(elementText).append("</span>").append("<br />\n");
                    writeIndentedClosingElementTag(desc, depth);
                }
                writeIndentedClosingElementTag(desc, depth);
            }
        }
        writeEndMultiElementInfo(desc);
        writeEndOptionalInfo(desc);
        desc.append("<br />\n");
    }

    private void writeIndentedClosingElementTag(StringBuilder desc, int depth) {
        writeIndentForCurrentElement(desc, depth);
        writeClosingElementTag(desc);
    }

    private void writeClosingElementTag(StringBuilder desc) {
        desc.append("&lt;/<span id=\"elementName\">").append(name).append("</span>").append(">");
    }

    private void writeIndentForCurrentElement(StringBuilder desc, int depth) {
        writeNbsp(desc, indentForCurrentElement(depth));
    }

    private int indentForCurrentElement(int depth) {
        
        int cnt = depth * INDENT_CHAR_NUM;
        if (!isSingleton) {
            cnt++;
        }
        if (minOccurs != 1) {
            cnt++;
        }
        return cnt;
    }

    static void writeRequiredIndentation(StringBuilder desc, boolean needIndent, int cnt) {
        if (!needIndent) {
            return;
        }
        writeNbsp(desc, cnt * INDENT_CHAR_NUM);
    }

    private static void writeNbsp(StringBuilder desc, int cnt) {
        for (int c = 1; c <= cnt; c++) {
            desc.append("&nbsp;");
        }
    }

    private void writeStartOptionalInfo(StringBuilder desc) {
        if (isOptional) {
            desc.append("[");
        }
    }

    private void writeEndOptionalInfo(StringBuilder desc) {
        if (isOptional) {
            desc.append("]");
        }
    }
    private void writeStartMultiElementInfo(StringBuilder desc) {
        if (!isSingleton) {
            desc.append("(");
        }
    }

    private void writeEndMultiElementInfo(StringBuilder desc) {
        if (isSingleton) {
            return;
        }
        desc.append(")");
        if (minOccurs > 0) {
            desc.append("+");  // 1 or more
        } else {
            desc.append("*");  // 0 or more
        }
    }

    private String getTypeName() {
        return typeName;
    }

    private static boolean isInnerRecursion(DescriptionNode parent, String testTypeName) {
        if (Strings.isNullOrEmpty(testTypeName) || (parent == null)) {
            return false;
        }
        if (parent instanceof XmlElementDescription) {
            XmlElementDescription parentE = (XmlElementDescription) parent;
            if (testTypeName.equals(parentE.getTypeName())) {
                return true;
            }
        }
        return isInnerRecursion(parent.getParent(), testTypeName);
    }

    @Override
    public String getXPath() {
        String retval;
        if (this.parent == null) {
            retval = ""; /* leave out the top level name */
        }
        else {
            retval = String.format("%s/%s", this.parent.getXPath(), this.name);
        }
        return retval;
    }

    public List<XmlAttributeDescription> getAttribs() {
        return attribs;
    }

    @Override
    public List<DescriptionNode> getChildren() { return children; }

    public List<XmlUnit> getChildDocumentableXmlUnits() {
        List<XmlUnit> units = Lists.newArrayList();
        units.addAll(this.getAttribs());
        for (DescriptionNode child : this.getChildren()) {
            if (child instanceof XmlElementDescription) {
                XmlElementDescription xChild = (XmlElementDescription)child;
                units.addAll(xChild.getDocumentableXmlUnits());
            } else if (child instanceof ChoiceNode) {
                ChoiceNode xChild = (ChoiceNode)child;
                for (DescriptionNode dn : xChild.getChildren()) {
                    if (dn instanceof XmlElementDescription) {
                        XmlElementDescription xDn = (XmlElementDescription)dn;
                        units.addAll(xDn.getDocumentableXmlUnits());
                    }
                }
            }
        }
        return units;
    }

    public List<XmlUnit> getDocumentableXmlUnits() {
        List<XmlUnit> units = Lists.newArrayList();
        units.add(this);
        units.addAll(this.getAttribs());
        for (DescriptionNode child : this.getChildren()) {
            if (child instanceof XmlElementDescription) {
                XmlElementDescription xChild = (XmlElementDescription)child;
                units.addAll(xChild.getDocumentableXmlUnits());
            } else if (child instanceof ChoiceNode) {
                ChoiceNode xChild = (ChoiceNode)child;
                for (DescriptionNode dn : xChild.getChildren()) {
                    if (dn instanceof XmlElementDescription) {
                        XmlElementDescription xDn = (XmlElementDescription)dn;
                        units.addAll(xDn.getDocumentableXmlUnits());
                    }
                }
            }
        }
        return units;
    }

    @Override
    public DescriptionNode getParent() { return parent; }

    @Override
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(targetNamespace).append(":").append(name).append(typeIdString);
        return sb.toString();
    }

    @Override
    public String getName() { return name; }
    public String getTargetNamespace() { return Strings.nullToEmpty(targetNamespace); }
    public Class<?> getJaxbClass() { return jaxbClass; }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setValueFieldDescription(String description) {
        this.valueFieldDescription = description;
    }

    @Override
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        if (!Strings.isNullOrEmpty(elementText)) {
            desc.append("<b>Type:").append(elementText).append("</b>");
            if (!Strings.isNullOrEmpty(description)) {
                desc.append("<br />\n").append(description);
            }
            if (!Strings.isNullOrEmpty(this.valueFieldDescription)) {
                desc.append("<br />\n");
                appendTableTextForValueFieldDescription(desc);
            }
        } else if (!Strings.isNullOrEmpty(description)) {
            desc.append(description);
            if (!Strings.isNullOrEmpty(this.valueFieldDescription)) {
                desc.append("<br />\n");
                appendTableTextForValueFieldDescription(desc);
            }
        } else if (!Strings.isNullOrEmpty(this.valueFieldDescription)) {
            appendTableTextForValueFieldDescription(desc);
        }
        return desc.toString();
    }

    public String getValueFieldDescription() {
        return Strings.nullToEmpty(this.valueFieldDescription);
    }

    public StringBuilder appendTableTextForValueFieldDescription(StringBuilder desc) {
        if (!Strings.isNullOrEmpty(this.valueFieldDescription)) {
            desc.append("<b>Description for element text content</b>:").append(this.valueFieldDescription);
        }
        return desc;
    }

    public void setFieldTag(String fieldTag) {
        this.fieldTag = fieldTag;
    }

    public String getFieldTag() {
        if (Strings.isNullOrEmpty(fieldTag)) {
            if (Strings.isNullOrEmpty(fieldName)) {
                return "";
            }
            return fieldName;
        } else {
            return fieldTag;
        }
    }

    public String getFieldName() {
        return Strings.nullToEmpty(fieldName);
    }

    public String getValueFieldName() {
        return Strings.nullToEmpty(valueFieldName);
    }

    @Override
    public OccurrenceSpec getOccurrence() {
        if (isSingleton) {
            if (minOccurs > 0) {
                return OccurrenceSpec.REQUIRED;
            } else {
                return OccurrenceSpec.OPTIONAL;
            }
        } else {
            if (minOccurs > 0) {
                return OccurrenceSpec.REQUIRED_MORE;
            } else {
                return OccurrenceSpec.OPTIONAL_MORE;
            }
        }
    }
}
