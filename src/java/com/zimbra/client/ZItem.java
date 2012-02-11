/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.client;

import com.zimbra.client.event.ZModifyEvent;
import com.zimbra.common.service.ServiceException;

public interface ZItem {

    public enum Flag {
        unread('u'),
        flagged('f'),
        highPriority('!'),
        lowPriority('?'),
        attachment('a'),
        replied('r'),
        sentByMe('s'),
        forwarded('w'),
        draft('d'),
        deleted('x'),
        notificationSent('n'),
        note('t');

        private char mFlagChar;

        public char getFlagChar() { return mFlagChar; }

        public static String toNameList(String flags) {
            if (flags == null || flags.length() == 0) return "";
            StringBuilder sb = new StringBuilder();
            for (int i=0; i < flags.length(); i++) {
                String v = null;
                for (Flag f : Flag.values()) {
                    if (f.getFlagChar() == flags.charAt(i)) {
                        v = f.name();
                        break;
                    }
                }
                if (sb.length() > 0) sb.append(", ");
                sb.append(v == null ? flags.substring(i, i+1) : v);
            }
            return sb.toString();
        }

        Flag(char flagChar) {
            mFlagChar = flagChar;

        }

        @Override
        public String toString() {
            return Character.toString(mFlagChar);
        }
    }

    public String getId();
    public String getUuid();

    //public ZMailbox getMailbox();

    public void modifyNotification(ZModifyEvent event) throws ServiceException;

}

