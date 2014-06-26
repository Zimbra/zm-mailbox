/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
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

