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