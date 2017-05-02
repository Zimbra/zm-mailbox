/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.client;

import com.google.common.base.Strings;
import com.zimbra.client.event.ZModifyEvent;
import com.zimbra.common.service.ServiceException;

public interface ZItem {
    public static final Flag[] CHAR2FLAG = new Flag[127];
    public enum Flag {
        UNREAD(-10, 'u'),
        FLAGGED(-6, 'f'),
        HIGH_PRIORITY(-11, '!'),
        LOW_PRIORITY(-12, '?'),
        ATTACHED(-2, 'a'),
        REPLIED(-3, 'r'),
        FROM_ME(-1, 's'),
        FORWARDED(-4, 'w'),
        DRAFT(-7, 'd'),
        DELETED(-8, 'x'),
        NOTIFIED(-9, 'n'),
        NOTE(-16, 't');

        final int id;
        final char ch;
        final int bitmask;

        public char getFlagChar() { return ch; }

        private Flag(int id, char ch) {
            this.id = id;
            this.ch = ch;
            this.bitmask = 1 << (-id - 1);
            CHAR2FLAG[ch] = this;
        }

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

        @Override
        public String toString() {
            return Character.toString(ch);
        }

        /**
         * Returns the "external" flag bitmask for the given flag string, which includes {@link Flag#BITMASK_UNREAD}.
         */
        public static int toBitmask(String flags) {
            if (Strings.isNullOrEmpty(flags)) {
                return 0;
            }

            int bitmask = 0;
            for (int i = 0, len = flags.length(); i < len; i++) {
                char c = flags.charAt(i);
                Flag flag = c > 0 && c < 127 ? CHAR2FLAG[c] : null;
                if (flag != null) {
                    bitmask |= flag.bitmask;
                }
            }
            return bitmask;
        }
    }

    public String getId();
    public String getUuid();

    //public ZMailbox getMailbox();

    public void modifyNotification(ZModifyEvent event) throws ServiceException;

}

