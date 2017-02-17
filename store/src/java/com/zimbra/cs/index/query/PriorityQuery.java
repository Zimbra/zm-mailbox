/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.index.query;

import com.google.common.base.Preconditions;

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

    public PriorityQuery(Priority priority) {
        super(Preconditions.checkNotNull(priority).toFlag(), true);
        this.priority = priority;
    }

    @Override
    public void dump(StringBuilder out) {
        out.append("PRIORITY:");
        out.append(priority.name());
    }

}
