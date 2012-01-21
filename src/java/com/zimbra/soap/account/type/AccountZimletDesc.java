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

package com.zimbra.soap.account.type;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.ZimletConstants;
import com.zimbra.soap.base.ZimletDesc;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class AccountZimletDesc implements ZimletDesc {

    // Turning schema validation on for WSDL clients using WSDL derived
    // from this class appears to hit problems.  Believe that @XmlAnyElement
    // and @XmlMixed are fundamentally incompatible with schema validation

    /**
     * @zm-api-field-tag zimlet-name
     * @zm-api-field-description Zimlet Name
     */
    @XmlAttribute(name=ZimletConstants.ZIMLET_ATTR_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-tag zimlet-version
     * @zm-api-field-description Zimlet Version
     */
    @XmlAttribute(name=ZimletConstants.ZIMLET_ATTR_VERSION /* version */, required=false)
    private String version;

    /**
     * @zm-api-field-tag zimlet-description
     * @zm-api-field-description Zimlet description
     */
    @XmlAttribute(name=ZimletConstants.ZIMLET_ATTR_DESCRIPTION /* description */, required=false)
    private String description;

    /**
     * @zm-api-field-tag zimlet-extension
     * @zm-api-field-description Zimlet extension
     */
    @XmlAttribute(name=ZimletConstants.ZIMLET_ATTR_EXTENSION /* extension */, required=false)
    private String extension;

    /**
     * @zm-api-field-tag zimlet-target
     * @zm-api-field-description Zimlet target
     */
    @XmlAttribute(name=ZimletConstants.ZIMLET_TAG_TARGET /* target */, required=false)
    private String target;

    /**
     * @zm-api-field-tag zimlet-label
     * @zm-api-field-description Zimlet label
     */
    @XmlAttribute(name=ZimletConstants.ZIMLET_TAG_LABEL /* label */, required=false)
    private String label;

    // See wiki.zimbra.com/wiki/Basic_Zimlet_Definition_Tags

    // ZimletConstants.ZIMLET_TAG_CONTENT_OBJECT will map to org.w3c.dom.Element
    // ZimletConstants.ZIMLET_TAG_PANEL_ITEM will map to org.w3c.dom.Element
    // ZimletConstants.ZIMLET_DISABLE_UI_UNDEPLOY will map to org.w3c.dom.Element

    /**
     * @zm-api-field-description Elements
     */
    @XmlElementRefs({
        @XmlElementRef(name=ZimletConstants.ZIMLET_TAG_SERVER_EXTENSION /* serverExtension */,
                type=ZimletServerExtension.class),
        @XmlElementRef(name=ZimletConstants.ZIMLET_TAG_SCRIPT /* include */, type=AccountZimletInclude.class),
        @XmlElementRef(name=ZimletConstants.ZIMLET_TAG_CSS /* includeCSS */, type=AccountZimletIncludeCSS.class),
        @XmlElementRef(name=ZimletConstants.ZIMLET_TAG_TARGET /* target */, type=AccountZimletTarget.class)
    })
    @XmlAnyElement
    private List<Object> elements = Lists.newArrayList();

    public AccountZimletDesc() {
    }

    @Override
    public void setName(String name) { this.name = name; }
    @Override
    public void setVersion(String version) { this.version = version; }
    @Override
    public void setDescription(String description) { this.description = description; }
    @Override
    public void setExtension(String extension) { this.extension = extension; }
    @Override
    public void setTarget(String target) { this.target = target; }
    @Override
    public void setLabel(String label) { this.label = label; }
    @Override
    public void setElements(Iterable <Object> elements) {
        if (this.elements == null) {
            this.elements = Lists.newArrayList();
        }
        this.elements.clear();
        if (elements != null) {
            Iterables.addAll(this.elements,elements);
        }
    }

    @Override
    public void addElement(Object element) {
        if (this.elements == null) {
            this.elements = Lists.newArrayList();
        }
        this.elements.add(element);
    }

    @Override
    public String getName() { return name; }
    @Override
    public String getVersion() { return version; }
    @Override
    public String getDescription() { return description; }
    @Override
    public String getExtension() { return extension; }
    @Override
    public String getTarget() { return target; }
    @Override
    public String getLabel() { return label; }
    @Override
    public List<Object> getElements() {
        return Collections.unmodifiableList(elements);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("version", version)
            .add("description", description)
            .add("extension", extension)
            .add("target", target)
            .add("label", label)
            .add("elements", elements);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
