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

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class ChoiceNode
implements DescriptionNode {
    static final String name = "{CHOICE NODE}";
    private DescriptionNode parent;
    private List<DescriptionNode> children = Lists.newArrayList();
    private boolean singleChild;
    public ChoiceNode(boolean canHaveMultipleChildren) {
        this.singleChild = !canHaveMultipleChildren;
    }

    public boolean isSingleChild() {
        return singleChild;
    }

    public String getHtmlDescription() {
        if (Strings.isNullOrEmpty(name)) {
            return "";
        }
        StringBuilder desc = new StringBuilder();
        writeDescription(desc, 1);
        return desc.toString();
    }
    
    public void addChild(DescriptionNode child) {
        children.add(child);
    }

    @Override
    public void writeDescription(StringBuilder desc, int depth) {
        XmlElementDescription.writeRequiredIndentation(desc, true, depth);
        if (singleChild) {
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
        return (parent == null) ? "" : parent.getXPath();
    }

    @Override
    public String xmlLinkTargetName() {
        return (parent == null) ? "" : parent.xmlLinkTargetName();
    }

    @Override
    public String tableLinkTargetName() {
        return (parent == null) ? "" : parent.tableLinkTargetName();
    }
}
