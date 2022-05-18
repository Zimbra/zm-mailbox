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

import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.util.JaxbAttributeInfo;
import com.zimbra.soap.util.JaxbElementInfo;
import com.zimbra.soap.util.JaxbInfo;
import com.zimbra.soap.util.JaxbNodeInfo;
import com.zimbra.soap.util.JaxbPseudoNodeChoiceInfo;
import com.zimbra.soap.util.JaxbValueInfo;
import com.zimbra.soap.util.WrappedElementInfo;

import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
/**
 * Represents a description of an XML element and all the attributes and elements it contains
 */
public class XmlElementDescription
implements DescriptionNode, XmlUnit {
    private final static int INDENT_CHAR_NUM = 4;
    private final static int WRAP_COLUMN_GUIDE = 120;
    DescriptionNode parent;
    private final List<XmlAttributeDescription> attribs = Lists.newArrayList();
    private final List<DescriptionNode> children = Lists.newArrayList();
    private final boolean optional;
    private final boolean singleton;  /* maximum of 1 */
    private final boolean wrapper;
    private boolean isInnerRecursionElement;
    private boolean isMasterDescriptionForJaxbClass = false;
    private int minOccurs;
    private String name;
    private String targetNamespace;
    private Class<?> jaxbClass;
    private String typeName;
    private String typeIdString;
    private String fieldName;
    private String valueFieldName;    // Field that has @XmlValue annotation
    private String valueFieldDescription; // Content of JavaDoc annotation associated with @XmlValue field
    private ValueDescription valueType = null;  // Description for type associated with @XmlValue field
    private String elementText; // Text to use between the start and end tags in the XML representation
    private String description;
    private String fieldTag;
    // If there is another description we have already seen for the XML being described for the same class,
    // this will point at that (otherwise, remains null)
    private XmlElementDescription masterDescriptionForThisJaxbClass = null;

    private static final Logger LOG = LogManager.getLogger(DescriptionNode.class);
    static {
        Configurator.reconfigure();
        Configurator.setRootLevel(Level.INFO);
        Configurator.setLevel(LOG.getName(), Level.INFO);
    }

    private XmlElementDescription(boolean isSingleton, boolean isWrapper, boolean isOptional) {
        this.singleton = isSingleton;
        this.wrapper = isWrapper;
        this.optional = isOptional;
    }

    public static XmlElementDescription createTopLevel(JaxbInfo jaxbInfo, String namespace, String name) {
        XmlElementDescription desc = new XmlElementDescription(true, false, false);
        desc.parent = null;
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
        XmlElementDescription desc = new XmlElementDescription(! nodeInfo.isMultiElement(), false, false);
        desc.parent = parent;
        desc.minOccurs = nodeInfo.isRequired() ? 1 : 0;
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
        XmlElementDescription desc = new XmlElementDescription(true, true, false);
        desc.parent = parent;
        desc.minOccurs = 1;
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
            desc.valueType = ValueDescription.create(desc.valueFieldName, valueInfo.getAtomClass());
            desc.elementText = desc.valueType.getRepresentation();
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
        if (JaxbUtil.isJaxbType(klass)) {
            jaxbInfo = JaxbInfo.getFromCache(klass);
        }
        return jaxbInfo;
    }

    private static void processTypeInfo(XmlElementDescription desc, Class<?> klass) {
        desc.typeName = ValueDescription.create(klass).getRepresentation();
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

    /**
     * Intended to be called when processing a Freemarker template
     */
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
        if (isMasterDescriptionForJaxbClass) {
            // Target anchor for master
            desc.append(String.format("<a name=\"%s\"></a>", xmlLinkTargetName()));
        }
        String startElemString = 
                (parent == null) ? name : String.format("<a href=\"#%s\">%s</a>", tableLinkTargetName(), name);
        desc.append("&lt;<span id=\"elementName\">").append(startElemString).append("</span>");
        if (isInnerRecursionElement) {
            if (attribs.size() > 0) {
                desc.append(" ... ");
            }
            desc.append("> ... ");
            writeClosingElementTag(desc);
            writeTypeIdentifierInfo(desc);
            desc.append(" # [inside itself]");
        } else {
            if (attribs.size() > 0) {
                if ((this.masterDescriptionForThisJaxbClass != null) && (attribs.size() > 2)) {
                    desc.append(" ... ");
                } else {
                    int attrIndentSize = name.length() + 4;
                    int fullIndentSize = indentForCurrentElement(depth) + attrIndentSize;
                    int sofar = fullIndentSize;
                    StringBuilder attrSb = new StringBuilder();
                    for (XmlAttributeDescription attrib : attribs) {
                        attrSb.setLength(0);
                        attrSb.append(" ");
                        sofar++;
                        int visibleLen = attrib.writeHtmlDescription(attrSb, depth);
                        if (sofar + visibleLen > WRAP_COLUMN_GUIDE) {
                            // wrap to next line
                            desc.append("<br />\n");
                            writeNbsp(desc, fullIndentSize);
                            sofar = fullIndentSize;
                        }
                        sofar += visibleLen;
                        desc.append(attrSb);
                    }
                }
            }
            if (getChildren().size() == 0) {
                if (!Strings.isNullOrEmpty(elementText)) {
                    desc.append(">");  // end of opening element tag
                    desc.append("<span id=\"value\">").append(elementText).append("</span>");
                    writeClosingElementTag(desc);
                    writeTypeIdentifierInfo(desc);
                } else {
                    /* can use just the opening element tag */
                    desc.append(" />");
                    writeTypeIdentifierInfo(desc);
                }
            } else {
                // Have children
                if (this.masterDescriptionForThisJaxbClass != null) {
                    desc.append("> ... ");
                    writeClosingElementTag(desc);
                    writeTypeIdentifierInfo(desc);
                } else {
                    desc.append(">");
                    writeTypeIdentifierInfo(desc); // tagged against opening element
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
        }
        writeEndMultiElementInfo(desc);
        writeEndOptionalInfo(desc);
        desc.append("<br />\n");
    }

    private void writeTypeIdentifierInfo(StringBuilder desc) {
        if (masterDescriptionForThisJaxbClass != null) {
            desc.append(String.format(" ## See <a href=\"#%s\">%s [%s]</a>",
                    masterDescriptionForThisJaxbClass.xmlLinkTargetName(),
                    masterDescriptionForThisJaxbClass.getXPath(), typeIdString));
        } else {
            desc.append(typeIdString);
        }
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
        if (!singleton) {
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
        if (optional) {
            desc.append("[");
        }
    }

    private void writeEndOptionalInfo(StringBuilder desc) {
        if (optional) {
            desc.append("]");
        }
    }
    private void writeStartMultiElementInfo(StringBuilder desc) {
        if (!singleton) {
            desc.append("(");
        }
    }

    private void writeEndMultiElementInfo(StringBuilder desc) {
        if (singleton) {
            return;
        }
        desc.append(")");
        if (minOccurs > 0) {
            desc.append("+");  // 1 or more
        } else {
            desc.append("*");  // 0 or more
        }
    }

    public String getTypeName() {
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

    /**
     * 
     * @param primaryDescriptions Map of primary Description elements for nodes that have already been visited.
     */
    public void markupDuplicateElements(Map<Class<?>,XmlElementDescription> primaryDescriptions) {
        if ((jaxbClass != null) && jaxbClass.getName().startsWith("com.zimbra.soap")) {
            masterDescriptionForThisJaxbClass = primaryDescriptions.get(jaxbClass);
            if (masterDescriptionForThisJaxbClass != null) {
                masterDescriptionForThisJaxbClass.isMasterDescriptionForJaxbClass = true;
            } else {
                primaryDescriptions.put(jaxbClass, this);
            }
        }
        for (DescriptionNode child : getChildren()) {
            if (child instanceof XmlElementDescription) {
                XmlElementDescription xChild = (XmlElementDescription)child;
                xChild.markupDuplicateElements(primaryDescriptions);
            } else if (child instanceof ChoiceNode) {
                ChoiceNode xChild = (ChoiceNode)child;
                for (DescriptionNode dn : xChild.getChildren()) {
                    if (dn instanceof XmlElementDescription) {
                        XmlElementDescription xDn = (XmlElementDescription)dn;
                        xDn.markupDuplicateElements(primaryDescriptions);
                    }
                }
            }
        }
    }

    @Override
    public String getTableKeyColumnContents() {
        String retval = String.format("<a name=\"%s\">%s</a>", tableLinkTargetName(), getXPath());
        return retval;
    }

    @Override
    public String getXPath() {
        String retval;
        if (parent == null) {
            retval = ""; /* leave out the top level name */
        }
        else {
            retval = String.format("%s/%s", parent.getXPath(), name);
        }
        return retval;
    }

    @Override
    public String xmlLinkTargetName() {
        String retval;
        if (parent == null) {
            retval = String.format("xml-%s", name);
        } else {
            retval = String.format("%s-%s", parent.xmlLinkTargetName(), name);
        }
        return retval;
    }

    @Override
    public String tableLinkTargetName() {
        String retval;
        if (parent == null) {
            retval = String.format("tbl-%s", name);
        } else {
            retval = String.format("%s-%s", parent.tableLinkTargetName(), name);
        }
        return retval;
    }

    public List<XmlAttributeDescription> getAttribs() {
        return attribs;
    }

    @Override
    public List<DescriptionNode> getChildren() { return children; }

    /**
     * Used in Freemarker templates to produce the tables describing elements of XML
     */
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

    private List<XmlUnit> getDocumentableXmlUnits() {
        List<XmlUnit> units = Lists.newArrayList();
        units.add(this);
        if (masterDescriptionForThisJaxbClass != null) {
            return units;
        }
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
    public String getDescriptionForTable() {
        if (masterDescriptionForThisJaxbClass != null) {
            String desc = getDescription();
            if (desc.isEmpty()) {
                return String.format("See <a href=\"#%s\">%s</a> for more details.",
                        masterDescriptionForThisJaxbClass.tableLinkTargetName(),
                        masterDescriptionForThisJaxbClass.getXPath());
            } else {
                return String.format("%s\n<br />\nSee <a href=\"#%s\">%s</a> for more details.",
                        getDescription(),
                        masterDescriptionForThisJaxbClass.tableLinkTargetName(),
                        masterDescriptionForThisJaxbClass.getXPath());
            }
        }
        return getDescription();
    }

    public String getRawDescription() {
        return description;
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
        if (singleton) {
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

    public boolean isJaxbType() {
        return JaxbUtil.isJaxbType(jaxbClass);
    }

    public boolean isWrapper() { return wrapper; }
    public boolean isOptional() { return optional; }
    public boolean isSingleton() { return singleton; }
    public ValueDescription getValueType() { return valueType; }
}
