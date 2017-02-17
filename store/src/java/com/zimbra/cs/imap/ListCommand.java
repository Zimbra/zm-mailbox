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

public class ListCommand extends AbstractListCommand {

    private byte selectOptions;
    private byte returnOptions;
    private byte status;

    public ListCommand(String referenceName, Set<String> mailboxNames, byte selectOptions, byte returnOptions,
            byte status) {
        super(referenceName, mailboxNames);
        this.selectOptions = selectOptions;
        this.returnOptions = returnOptions;
        this.status = status;
    }

    public byte getSelectOptions() {
        return selectOptions;
    }

    public byte getReturnOptions() {
        return returnOptions;
    }

    public byte getStatus() {
        return status;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + returnOptions;
        result = prime * result + selectOptions;
        result = prime * result + status;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ListCommand other = (ListCommand) obj;
        if (returnOptions != other.returnOptions) {
            return false;
        }
        if (selectOptions != other.selectOptions) {
            return false;
        }
        if (status != other.status) {
            return false;
        }
        return true;
    }
}
