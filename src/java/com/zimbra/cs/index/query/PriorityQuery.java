/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.cs.index.query;

import com.google.common.base.Preconditions;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Query messages by priority.
 * <p>
 * <ul>
 *  <li>High priority messages are tagged with {@code \Urgent}.
 *  <li>Low priority messages are tagged with {@code \Bulk}.
 * </ul>
 *
 * @author ysasaki
 */
public final class PriorityQuery extends TagQuery {

    public enum Priority {
        HIGH("\\Urgent"), LOW("\\Bulk");

        private final String flag;

        private Priority(String flag) {
            this.flag = flag;
        }

        private String toFlag() {
            return flag;
        }
    }

    private final Priority priority;

    public PriorityQuery(Mailbox mailbox, Priority priority) throws ServiceException {
        super(mailbox, Preconditions.checkNotNull(priority).toFlag(), true);
        this.priority = priority;
    }

    @Override
    public void dump(StringBuilder out) {
        out.append("Priority,");
        out.append(priority.name());
    }

}
