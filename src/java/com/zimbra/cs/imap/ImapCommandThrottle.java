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

import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;

public class ImapCommandThrottle {
    private ImapCommand lastCommand = null;
    private int repeats = 0;
    private int repeatLimit = 0;
    private static final long REPEAT_TIME_THRESHOLD = 5 * Constants.MILLIS_PER_MINUTE;

    public ImapCommandThrottle(int repeatLimit) {
        super();
        this.repeatLimit = repeatLimit;
    }

    public int getRepeatLimit() {
        return repeatLimit;
    }

    private boolean isCommandRepeated(ImapCommand command) {
        return command != null && lastCommand != null
                && (command.getCreateTime() - lastCommand.getCreateTime() < REPEAT_TIME_THRESHOLD)
                && command.isDuplicate(lastCommand);
    }

    public boolean isCommandThrottled(ImapCommand command) {
        if (repeatLimit <= 0) {
            return false;
        } else if (command.throttle(lastCommand)) {
            // commands can implement their own throttle mechanism
            ZimbraLog.imap.debug("throttled by command");
            return true;
        } else if (isCommandRepeated(command)) {
            repeats++;
            lastCommand = command;
            if (repeats > repeatLimit) {
                ZimbraLog.imap.debug("throttled by repeat");
                return true;
            } else {
                return false;
            }
        } else {
            repeats = 1;
            lastCommand = command;
            return false;
        }
    }

    public void reset() {
        repeats = 0;
        lastCommand = null;
    }
}
