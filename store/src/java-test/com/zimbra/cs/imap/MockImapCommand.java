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
