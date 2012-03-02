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
