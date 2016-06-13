/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.index;

import java.util.Set;

/**
 * A {@link QueryTarget} is something we run a query against, i.e. a mailbox.
 */
final class QueryTarget {

    public static final QueryTarget UNSPECIFIED = new QueryTarget();
    public static final QueryTarget LOCAL = new QueryTarget();
    public static final QueryTarget REMOTE = new QueryTarget();

    private final String target;

    private QueryTarget() {
        target = null;
    }

    QueryTarget(String target) {
        this.target = target;
    }


    boolean isCompatibleLocal() {
        return this == UNSPECIFIED || this == LOCAL;
    }

    boolean isCompatible(String targetAcctId) {
        if (isCompatibleLocal()) {
            return false;
        }
        if (this == REMOTE) {
            return false;
        }
        return target.equals(targetAcctId);
    }

    @Override
    public String toString() {
        if (this == UNSPECIFIED) {
            return "UNSPECIFIED";
        } else if (this == LOCAL) {
            return "LOCAL";
        } else {
            return target;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other != null && other.getClass() == this.getClass()) {
            QueryTarget o = (QueryTarget) other;
            // one of us is a "special" instance, so just normal equals
            if (target == null || o.target == null) {
                return this == other;
            }
            // compare folders
            return target.equals(o.target);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return target != null ? target.hashCode() : 0;
    }

    /**
     * Used in the optimize() pathway, count the number of explicit* QueryTarget's (i.e. don't count "unspecified")
     */
    static int getExplicitTargetCount(Set<QueryTarget> set) {
        int count = 0;
        for (QueryTarget entry : set) {
            if (entry != QueryTarget.UNSPECIFIED) {
                count++;
            }
        }
        return count;
    }

    static boolean hasExternalTarget(Set<QueryTarget> set) {
        for (QueryTarget entry : set) {
            if (entry != QueryTarget.UNSPECIFIED && entry != QueryTarget.LOCAL) {
                return true;
            }
        }
        return false;
    }
}
