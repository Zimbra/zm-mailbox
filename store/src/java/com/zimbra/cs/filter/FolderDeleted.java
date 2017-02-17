/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.filter;

import org.apache.jsieve.parser.SieveNode;
import org.apache.jsieve.parser.generated.Node;

/**
 * Disables filter rules that reference a folder that was deleted. 
 */
public class FolderDeleted
extends SieveVisitor {

    private String mDeletedFolderPath;
    private Node mIfNode;
    private boolean mModified = false;
    
    public FolderDeleted(String deletedFolderPath) {
        mDeletedFolderPath = FolderRenamer.prefixWithSlash(deletedFolderPath);
    }
    
    public boolean modified() {
        return mModified;
    }

    @Override
    protected void visitNode(Node node, VisitPhase phase, RuleProperties props) {
        if (phase != VisitPhase.begin) {
            return;
        }
        String name = getNodeName(node);
        if ("if".equals(name) || "disabled_if".equals(name)) {
            // Remember the top-level node so we can modify it later.
            mIfNode = node;
        }
    }

    @Override
    protected void visitFileIntoAction(Node node, VisitPhase phase, RuleProperties props,
                                       String folderPath) {
        if (phase != VisitPhase.begin) {
            return;
        }
        folderPath = FolderRenamer.prefixWithSlash(folderPath);
        String ifNodeName = getNodeName(mIfNode);
        if (folderPath.startsWith(mDeletedFolderPath) && "if".equals(ifNodeName)) {
            ((SieveNode) mIfNode).setName("disabled_if");
            mModified = true;
        }
    }
    
    
}
