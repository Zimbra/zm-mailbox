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
package com.zimbra.doc.soap.apidesc;

import java.util.List;

import com.google.common.collect.Lists;
import com.zimbra.doc.soap.ChoiceNode;
import com.zimbra.doc.soap.DescriptionNode;
import com.zimbra.doc.soap.XmlElementDescription;

public class SoapApiElementChoiceInfo
implements SoapApiElementArtifact {
    private final boolean singleChild;
    private final List<SoapApiElementArtifact> subElements = Lists.newArrayList();

    /* no-argument constructor needed for deserialization */
    @SuppressWarnings("unused")
    private SoapApiElementChoiceInfo() {
        singleChild = true;
    }

    public SoapApiElementChoiceInfo(ChoiceNode descNode) {
        singleChild = descNode.isSingleChild();
        for (DescriptionNode child : descNode.getChildren()) {
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

    public boolean isSingleChild() { return singleChild; }
    public List<SoapApiElementArtifact> getSubElements() { return subElements; }
}
