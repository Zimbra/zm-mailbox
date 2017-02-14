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
package com.zimbra.doc.soap.apidesc;

import java.util.List;

import com.google.common.collect.Lists;
import com.zimbra.doc.soap.ChoiceNode;
import com.zimbra.doc.soap.DescriptionNode;
import com.zimbra.doc.soap.XmlElementDescription;

public class SoapApiWrapperElement
implements SoapApiNamedElement {
    private final String name;
    private final String namespace;
    private final boolean required;
    private final List<SoapApiElementArtifact> subElements = Lists.newArrayList();

    /* no-argument constructor needed for deserialization */
    @SuppressWarnings("unused")
    private SoapApiWrapperElement() {
        name = null;
        namespace = null;
        required = false;
    }

    public SoapApiWrapperElement(XmlElementDescription wrapperDesc) {
        name = wrapperDesc.getName();
        namespace = wrapperDesc.getTargetNamespace();
        required = !wrapperDesc.isOptional();
        for (DescriptionNode child : wrapperDesc.getChildren()) {
            if (child instanceof XmlElementDescription) {
                XmlElementDescription elemChild = (XmlElementDescription) child;
                if (elemChild.isWrapper()) {
                    subElements.add(new SoapApiWrapperElement(elemChild));
                } else if (elemChild.isJaxbType()) {
                    subElements.add(new SoapApiElement(elemChild));
                } else {
                    subElements.add(new SoapApiSimpleElement(elemChild));
                }
            } else if (child instanceof ChoiceNode) {
                subElements.add(new SoapApiElementChoiceInfo((ChoiceNode)child));
            }
        }
    }

    @Override
    public String getName() { return name; }
    @Override
    public String getNamespace() { return namespace; }
    public boolean isRequired() { return required; }
    public List<SoapApiElementArtifact> getSubElements() { return subElements; }
}
