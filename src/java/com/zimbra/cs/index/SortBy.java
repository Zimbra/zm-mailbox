/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

import java.util.HashMap;

public class SortBy {
    
    public enum Type {
        DATE_ASCENDING,
        DATE_DESCENDING,
        SUBJ_ASCENDING,
        SUBJ_DESCENDING,
        NAME_ASCENDING,
        NAME_DESCENDING,
        SIZE_ASCENDING,
        SIZE_DESCENDING,
        SCORE_DESCENDING,
        NAME_NATURAL_ORDER_ASCENDING,
        NAME_NATURAL_ORDER_DESCENDING,
        TASK_DUE_ASCENDING,
        TASK_DUE_DESCENDING,
        TASK_STATUS_ASCENDING,
        TASK_STATUS_DESCENDING,
        TASK_PERCENT_COMPLETE_ASCENDING,
        TASK_PERCENT_COMPLETE_DESCENDING,
        NAME_LOCALIZED_ASCENDING,
        NAME_LOCALIZED_DESCENDING,
        NONE,
    }
    
    static HashMap<String, SortBy> sNameMap = new HashMap<String, SortBy>();

    public static final SortBy DATE_ASCENDING  = new SortBy(Type.DATE_ASCENDING, "dateAsc",  SortCriterion.DATE,    SortDirection.ASCENDING); 
    public static final SortBy DATE_DESCENDING = new SortBy(Type.DATE_DESCENDING, "dateDesc", SortCriterion.DATE,    SortDirection.DESCENDING);
    public static final SortBy SUBJ_ASCENDING  = new SortBy(Type.SUBJ_ASCENDING, "subjAsc",  SortCriterion.SUBJECT, SortDirection.ASCENDING);
    public static final SortBy SUBJ_DESCENDING = new SortBy(Type.SUBJ_DESCENDING, "subjDesc", SortCriterion.SUBJECT, SortDirection.DESCENDING);
    public static final SortBy NAME_ASCENDING  = new SortBy(Type.NAME_ASCENDING, "nameAsc",  SortCriterion.SENDER,  SortDirection.ASCENDING);
    public static final SortBy NAME_DESCENDING = new SortBy(Type.NAME_DESCENDING, "nameDesc", SortCriterion.SENDER,  SortDirection.DESCENDING);
    public static final SortBy SIZE_ASCENDING  = new SortBy(Type.SIZE_ASCENDING, "sizeAsc",  SortCriterion.SIZE,    SortDirection.ASCENDING);
    public static final SortBy SIZE_DESCENDING = new SortBy(Type.SIZE_DESCENDING, "sizeDesc", SortCriterion.SIZE,    SortDirection.DESCENDING);
    public static final SortBy SCORE_DESCENDING= new SortBy(Type.SCORE_DESCENDING, "score",    SortCriterion.DATE,    SortDirection.DESCENDING);

    // wiki "natural order" sorts not exposed via SOAP
    public static final SortBy NAME_NATURAL_ORDER_ASCENDING = new SortBy(Type.NAME_NATURAL_ORDER_ASCENDING, null, SortCriterion.NAME_NATURAL_ORDER, SortDirection.ASCENDING);
    public static final SortBy NAME_NATURAL_ORDER_DESCENDING= new SortBy(Type.NAME_NATURAL_ORDER_DESCENDING, null, SortCriterion.NAME_NATURAL_ORDER, SortDirection.DESCENDING);

    // special TASK-only sorts
    public static final SortBy TASK_DUE_ASCENDING    = new SortBy(Type.TASK_DUE_ASCENDING, "taskDueAsc",     SortCriterion.DATE, SortDirection.ASCENDING);
    public static final SortBy TASK_DUE_DESCENDING   = new SortBy(Type.TASK_DUE_DESCENDING, "taskDueDesc",    SortCriterion.DATE, SortDirection.ASCENDING);
    public static final SortBy TASK_STATUS_ASCENDING = new SortBy(Type.TASK_STATUS_ASCENDING, "taskStatusAsc",  SortCriterion.DATE, SortDirection.ASCENDING);
    public static final SortBy TASK_STATUS_DESCENDING= new SortBy(Type.TASK_STATUS_DESCENDING, "taskStatusDesc", SortCriterion.DATE, SortDirection.ASCENDING);
    public static final SortBy TASK_PERCENT_COMPLETE_ASCENDING = new SortBy(Type.TASK_PERCENT_COMPLETE_ASCENDING, "taskPercCompletedAsc",  SortCriterion.DATE, SortDirection.ASCENDING);
    public static final SortBy TASK_PERCENT_COMPLETE_DESCENDING= new SortBy(Type.TASK_PERCENT_COMPLETE_DESCENDING, "taskPercCompletedDesc", SortCriterion.DATE, SortDirection.ASCENDING);

    //
    // the LOCALIZED sorts aren't in here, they have to be constructed at runtime with the specific Locale information in them
    //
    
    public static final SortBy NONE = new SortBy(Type.NONE, "none", SortCriterion.NONE, SortDirection.ASCENDING);

    
    
    /**
     * This is the sort that the DB/Lucene knows about 
     */
    public static enum SortCriterion {
        DATE, SENDER, SUBJECT, ID, NONE, NAME, NAME_NATURAL_ORDER, SIZE
    }

    public static enum SortDirection {
        DESCENDING, ASCENDING
    }

    private String        mName;
    private SortCriterion mCriterion;
    private SortDirection mDirection;
    private Type          mType;
    
    SortBy(Type t, String str, SortCriterion criterion, SortDirection direction) {
        mType = t;
        mName = str;
        if (mName != null)
            sNameMap.put(mName.toLowerCase(), this);
        mCriterion = criterion;
        mDirection = direction;
    }

    @Override public String toString()  { return mName == null ? super.toString() : mName; }

    public Type getType() { return mType; }

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
