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

import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.imap.ImapCommand;

public class MockImapCommand extends ImapCommand {

    private String param1;
    private String param2;
    private int param3;

    public MockImapCommand(String param1, String param2, int param3) {
        super();
        this.param1 = param1;
        this.param2 = param2;
        this.param3 = param3;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MockImapCommand)) {
            return false;
        }
        MockImapCommand mock = (MockImapCommand) obj;
        return StringUtil.equal(param1, mock.param1) && StringUtil.equal(param2, mock.param2) && param3 == mock.param3;
    }

}
