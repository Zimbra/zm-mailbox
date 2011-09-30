/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.filter.jsieve;

import java.util.Map;

import org.apache.jsieve.mail.Action;

import com.google.common.base.Objects;
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
        return Objects.toStringHelper(this).add("name", name).add("set", set).toString();
    }
}
