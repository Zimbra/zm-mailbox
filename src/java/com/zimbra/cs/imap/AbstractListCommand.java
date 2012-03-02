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

import java.util.Set;

public abstract class AbstractListCommand extends ImapCommand {

    protected String referenceName;
    protected Set<String> mailboxNames;

    public AbstractListCommand(String referenceName, Set<String> mailboxNames) {
        super();
        this.referenceName = referenceName;
        this.mailboxNames = mailboxNames;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public Set<String> getMailboxNames() {
        return mailboxNames;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mailboxNames == null) ? 0 : mailboxNames.hashCode());
        result = prime * result + ((referenceName == null) ? 0 : referenceName.hashCode());
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
        AbstractListCommand other = (AbstractListCommand) obj;
        if (mailboxNames == null) {
            if (other.mailboxNames != null) {
                return false;
            }
        } else if (!mailboxNames.equals(other.mailboxNames)) {
            return false;
        }
        if (referenceName == null) {
            if (other.referenceName != null) {
                return false;
            }
        } else if (!referenceName.equals(other.referenceName)) {
            return false;
        }
        return true;
    }

}