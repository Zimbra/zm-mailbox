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

import com.zimbra.common.localconfig.LC;

public class CreateCommand extends ImapCommand {

    private ImapPath path;
    private int repeats = 0;

    public CreateCommand(ImapPath path) {
        super();
        this.path = path;
    }

    @Override
    protected boolean throttle(ImapCommand previousCommand) {
        // count number of CREATE commands irrespective of parameters
        // this prevents client from continuously creating new folders
        if (previousCommand instanceof CreateCommand) {
            repeats = ((CreateCommand) previousCommand).repeats + 1;
            return repeats > LC.imap_throttle_command_limit.intValue();
        } else {
            repeats++;
            return false;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((path == null) ? 0 : path.hashCode());
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
        CreateCommand other = (CreateCommand) obj;
        if (path == null) {
            if (other.path != null) {
                return false;
            }
        } else if (!path.equals(other.path)) {
            return false;
        }
        return true;
    }
}
