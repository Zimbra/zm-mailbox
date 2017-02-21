/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.service.ServiceException;

class FolderRenamer extends SieveVisitor {
    
    String mOldPath;
    String mNewPath;
    boolean mRenamed = false;
    
    FolderRenamer(String oldPath, String newPath) {
        // Make sure paths are always prefixed with a slash.
        mOldPath = prefixWithSlash(oldPath);
        mNewPath = prefixWithSlash(newPath);
    }
    
    boolean renamed() {
        return mRenamed;
    }
    
    @Override
    protected void visitFileIntoAction(Node node, VisitPhase phase, RuleProperties props, String folderPath)
    throws ServiceException {
        if (phase != SieveVisitor.VisitPhase.begin || folderPath == null) {
            return;
        }
        folderPath = prefixWithSlash(folderPath);
        if (folderPath.startsWith(mOldPath)) {
            String newPath = folderPath.replace(mOldPath, mNewPath);
            SieveNode folderNameNode = (SieveNode) getNode(node, 0, 0, 0, 0);
            String escapedName = FilterUtil.escape(newPath);
            folderNameNode.setValue(escapedName);
            mRenamed = true;
        }
    }

    static String prefixWithSlash(String path) {
        if (path == null || path.length() == 0) {
            return path;
        }
        if (!(path.charAt(0) == '/')) {
            return "/" + path;
        }
        return path;
    }

}
