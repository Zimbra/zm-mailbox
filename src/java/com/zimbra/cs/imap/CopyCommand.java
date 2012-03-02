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

package com.zimbra.cs.imap;

public class CopyCommand extends ImapCommand {

    private ImapPath destPath;
    private String sequenceSet;

    public CopyCommand(String sequenceSet, ImapPath destPath) {
        super();
        this.destPath = destPath;
        this.sequenceSet = sequenceSet;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sequenceSet == null) ? 0 : sequenceSet.hashCode());
        result = prime * result + ((destPath == null) ? 0 : destPath.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CopyCommand other = (CopyCommand) obj;
        if (sequenceSet == null) {
            if (other.sequenceSet != null) {
                return false;
            }
        } else if (!sequenceSet.equals(other.sequenceSet)) {
            return false;
        }
        if (destPath == null) {
            if (other.destPath != null) {
                return false;
            }
        } else if (!destPath.equals(other.destPath)) {
            return false;
        }
        return true;
    }
}
