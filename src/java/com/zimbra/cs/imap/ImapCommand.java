/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.imap;

public abstract class ImapCommand {
    private long createTime;

    public ImapCommand() {
        this.createTime = System.currentTimeMillis();
    }

    public long getCreateTime() {
        return createTime;
    }

    protected boolean hasSameParams(ImapCommand command) {
        return this.equals(command);
    }

    protected boolean isDuplicate(ImapCommand command) {
        return this.getClass().equals(command.getClass()) && this.hasSameParams(command);
    }

    protected boolean throttle(ImapCommand previousCommand) {
        return false;
    }
}
