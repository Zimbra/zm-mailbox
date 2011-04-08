/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011 Zimbra, Inc.
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

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

/**
 * Sort order.
 *
 * @author dkarp
 * @author ysasaki
 */
public enum SortBy {

    NONE("none", Key.NONE, Direction.ASC),
    DATE_ASC("dateAsc", Key.DATE, Direction.ASC),
    DATE_DESC("dateDesc", Key.DATE, Direction.DESC),
    SUBJ_ASC("subjAsc", Key.SUBJECT, Direction.ASC),
    SUBJ_DESC("subjDesc", Key.SUBJECT, Direction.DESC),
    NAME_ASC("nameAsc", Key.SENDER, Direction.ASC),
    NAME_DESC("nameDesc", Key.SENDER, Direction.DESC),
    RCPT_ASC("rcptAsc", Key.RCPT, Direction.ASC),
    RCPT_DESC("rcptDesc", Key.RCPT, Direction.DESC),
    SIZE_ASC("sizeAsc", Key.SIZE, Direction.ASC),
    SIZE_DESC("sizeDesc", Key.SIZE, Direction.DESC),
    ATTACHMENT_ASC("attachAsc", Key.ATTACHMENT, Direction.ASC),
    ATTACHMENT_DESC("attachDesc", Key.ATTACHMENT, Direction.DESC),
    FLAG_ASC("flagAsc", Key.FLAG, Direction.ASC),
    FLAG_DESC("flagDesc", Key.FLAG, Direction.DESC),
    PRIORITY_ASC("priorityAsc", Key.PRIORITY, Direction.ASC),
    PRIORITY_DESC("priorityDesc", Key.PRIORITY, Direction.DESC),

    // wiki "natural order" sorts are not exposed via SOAP
    NAME_NATURAL_ORDER_ASC(null, Key.NAME_NATURAL_ORDER, Direction.ASC),
    NAME_NATURAL_ORDER_DESC(null, Key.NAME_NATURAL_ORDER, Direction.DESC),

    NAME_LOCALIZED_ASC(null, Key.NAME, Direction.ASC),
    NAME_LOCALIZED_DESC(null, Key.NAME, Direction.DESC),

    // special TASK-only sorts
    TASK_DUE_ASC("taskDueAsc", Key.DATE, Direction.ASC),
    TASK_DUE_DESC("taskDueDesc", Key.DATE, Direction.DESC),
    TASK_STATUS_ASC("taskStatusAsc", Key.DATE, Direction.ASC),
    TASK_STATUS_DESC("taskStatusDesc", Key.DATE, Direction.DESC),
    TASK_PERCENT_COMPLETE_ASC("taskPercCompletedAsc",  Key.DATE, Direction.ASC),
    TASK_PERCENT_COMPLETE_DESC("taskPercCompletedDesc", Key.DATE, Direction.DESC);

    public enum Key {
        DATE, SENDER, RCPT, SUBJECT, ID, NONE, NAME, NAME_NATURAL_ORDER, SIZE, ATTACHMENT, FLAG, PRIORITY
    }

    public enum Direction {
        DESC, ASC
    }

    private static final Map<String, SortBy> NAME2SORT;
    static {
        ImmutableMap.Builder<String, SortBy> builder = ImmutableMap.builder();
        for (SortBy sort : SortBy.values()) {
            if (sort.name != null) {
                builder.put(sort.name.toLowerCase(), sort);
            }
        }
        NAME2SORT = builder.build();
    }

    private final String name;
    private final Key key;
    private final Direction direction;

    private SortBy(String name, Key key, Direction dir) {
        this.name = name;
        this.key = key;
        direction = dir;
    }

    public static SortBy of(String key) {
        return key != null ? NAME2SORT.get(key.toLowerCase()) : null;
    }

    public Key getKey() {
        return key;
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public String toString() {
        return name != null ? name : name();
    }

    public Comparator<ZimbraHit> getHitComparator(Locale locale) {
        return new NameComparator(this, Collator.getInstance(locale));
    }

    private static final class NameComparator implements Comparator<ZimbraHit> {
        private final SortBy sort;
        private final Collator collator;

        NameComparator(SortBy sort, Collator collator) {
            this.sort = sort;
            this.collator = collator;
        }

        @Override
        public int compare(ZimbraHit lhs, ZimbraHit rhs) {
            int result = 0;
            try {
                result = collator.compare(lhs.getName(), rhs.getName());
                if (result == 0) {
                    result = lhs.getItemId() - rhs.getItemId();
                }
                if (sort.getDirection() == SortBy.Direction.DESC) {
                    result = -1 * result;
                }
            } catch (ServiceException e) {
                ZimbraLog.index.debug("Failed to compare %s and %s", lhs, rhs, e);
                result = 0;
            }
            return result;
        }
    }

}
