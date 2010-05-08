/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
            String escapedName = "\"" + FilterUtil.escape(newPath) + "\"";
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
