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
import com.sun.xml.internal.xsom.XSModelGroup;

public class ChoiceNode
implements DescriptionNode {
    static final String name = "{CHOICE NODE}";
    private DescriptionNode parent;
    private int minOccurs;
    private int maxOccurs;
    private List<DescriptionNode> children = Lists.newArrayList();
    public ChoiceNode() {
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
        XmlElementDescription.writeRequiredIndentation(desc, true, depth);
        if (maxOccurs == 1) {
            desc.append("Choose one of");
        } else {
            desc.append("List of any of");
        }
        desc.append(": {<br />\n");
        for (DescriptionNode child : getChildren()) {
            child.writeDescription(desc, depth+1);
        }
        XmlElementDescription.writeRequiredIndentation(desc, true, depth);
        desc.append("}<br />\n");
    }

    @Override
    public List<DescriptionNode> getChildren() {
        return children;
    }

    @Override
    public DescriptionNode getParent() {
        return parent;
    }
    @Override
    public String getSummary() {
        return name;
    }
    @Override
    public String getDescription() {
        return "";
    }

    /**
     * A Choice node is a pseudo node that doesn't contribute anything to XPath
     */
    @Override
    public String getXPath() {
        return (this.parent == null) ? "" : this.parent.getXPath();
    }
}
