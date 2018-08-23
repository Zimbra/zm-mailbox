/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.filter.jsieve;

import java.util.Map;

import org.apache.jsieve.mail.Action;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

/**
 * @since Nov 8, 2004
 */
public final class ActionFlag implements Action {

    private static final Map<String, ActionFlag> FLAGS = ImmutableMap.<String, ActionFlag>builder()
            .put("read", new ActionFlag(com.zimbra.cs.mailbox.Flag.FlagInfo.UNREAD, false, "read"))
            .put("unread", new ActionFlag(com.zimbra.cs.mailbox.Flag.FlagInfo.UNREAD, true, "unread"))
            .put("flagged", new ActionFlag(com.zimbra.cs.mailbox.Flag.FlagInfo.FLAGGED, true, "flagged"))
            .put("unflagged", new ActionFlag(com.zimbra.cs.mailbox.Flag.FlagInfo.FLAGGED, false, "unflagged"))
            .put("priority", new ActionFlag(com.zimbra.cs.mailbox.Flag.FlagInfo.PRIORITY, true, "priority"))
            .put("unpriority", new ActionFlag(com.zimbra.cs.mailbox.Flag.FlagInfo.PRIORITY, false, "priority"))
            .build();

    private final com.zimbra.cs.mailbox.Flag.FlagInfo flag;
    private final boolean set;
    private final String name;

    public static ActionFlag of(String name) {
        return FLAGS.get(name);
    }

    private ActionFlag(com.zimbra.cs.mailbox.Flag.FlagInfo flag, boolean set, String name) {
        this.flag = flag;
        this.set = set;
        this.name = name;
    }

    public com.zimbra.cs.mailbox.Flag.FlagInfo getFlag() {
        return flag;
    }

    public boolean isSet() {
        return set;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("name", name).add("set", set).toString();
    }
}
