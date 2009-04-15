/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.util.HashMap;

public enum SortBy {

    DATE_ASCENDING  ("dateAsc",  SortCriterion.DATE,    SortDirection.ASCENDING), 
    DATE_DESCENDING ("dateDesc", SortCriterion.DATE,    SortDirection.DESCENDING),
    SUBJ_ASCENDING  ("subjAsc",  SortCriterion.SUBJECT, SortDirection.ASCENDING),
    SUBJ_DESCENDING ("subjDesc", SortCriterion.SUBJECT, SortDirection.DESCENDING),
    NAME_ASCENDING  ("nameAsc",  SortCriterion.SENDER,  SortDirection.ASCENDING),
    NAME_DESCENDING ("nameDesc", SortCriterion.SENDER,  SortDirection.DESCENDING),
    SIZE_ASCENDING  ("sizeAsc",  SortCriterion.SIZE,    SortDirection.ASCENDING), 
    SIZE_DESCENDING ("sizeDesc", SortCriterion.SIZE,    SortDirection.DESCENDING),
    SCORE_DESCENDING("score",    SortCriterion.DATE,    SortDirection.DESCENDING),

    // wiki "natural order" sorts not exposed via SOAP
    NAME_NATURAL_ORDER_ASCENDING (null, SortCriterion.NAME_NATURAL_ORDER, SortDirection.ASCENDING),
    NAME_NATURAL_ORDER_DESCENDING(null, SortCriterion.NAME_NATURAL_ORDER, SortDirection.DESCENDING),

    // special TASK-only sorts
    TASK_DUE_ASCENDING    ("taskDueAsc",     SortCriterion.DATE, SortDirection.ASCENDING),
    TASK_DUE_DESCENDING   ("taskDueDesc",    SortCriterion.DATE, SortDirection.ASCENDING),
    TASK_STATUS_ASCENDING ("taskStatusAsc",  SortCriterion.DATE, SortDirection.ASCENDING),
    TASK_STATUS_DESCENDING("taskStatusDesc", SortCriterion.DATE, SortDirection.ASCENDING),
    TASK_PERCENT_COMPLETE_ASCENDING ("taskPercCompletedAsc",  SortCriterion.DATE, SortDirection.ASCENDING),
    TASK_PERCENT_COMPLETE_DESCENDING("taskPercCompletedDesc", SortCriterion.DATE, SortDirection.ASCENDING),

    NONE("none", SortCriterion.NONE, SortDirection.ASCENDING),
    ;

    public static enum SortCriterion {
        DATE, SENDER, SUBJECT, ID, NONE, NAME, NAME_NATURAL_ORDER, SIZE
    }

    public static enum SortDirection {
        DESCENDING, ASCENDING
    }

    static HashMap<String, SortBy> sNameMap = new HashMap<String, SortBy>();

    static {
        for (SortBy s : SortBy.values()) {
            if (s.mName != null)
                sNameMap.put(s.mName.toLowerCase(), s);
        }
    }

    private String        mName;
    private SortCriterion mCriterion;
    private SortDirection mDirection;

    SortBy(String str, SortCriterion criterion, SortDirection direction) {
        mName = str;
        mCriterion = criterion;
        mDirection = direction;
    }

    @Override public String toString()  { return mName == null ? super.toString() : mName; }

    public SortCriterion getCriterion()  { return mCriterion; }
    public SortDirection getDirection()  { return mDirection; }

    public boolean isDescending() {
        return mDirection == SortDirection.DESCENDING;
    }

    public static SortBy lookup(String str) {
        if (str != null)
            return sNameMap.get(str.toLowerCase());
        else
            return null;
    }
}