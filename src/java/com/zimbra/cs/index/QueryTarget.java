/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
