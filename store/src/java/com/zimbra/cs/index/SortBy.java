/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.zimbra.common.mailbox.ZimbraSortBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;

/**
 * Sort order.
 *
 * @author dkarp
 * @author ysasaki
 */
public enum SortBy {

    NONE("none", Key.NONE, Direction.ASC, ZimbraSortBy.none),
    DATE_ASC("dateAsc", Key.DATE, Direction.ASC, ZimbraSortBy.dateAsc),
    DATE_DESC("dateDesc", Key.DATE, Direction.DESC, ZimbraSortBy.dateDesc),
    SUBJ_ASC("subjAsc", Key.SUBJECT, Direction.ASC, ZimbraSortBy.subjAsc),
    SUBJ_DESC("subjDesc", Key.SUBJECT, Direction.DESC, ZimbraSortBy.subjDesc),
    NAME_ASC("nameAsc", Key.SENDER, Direction.ASC, ZimbraSortBy.nameAsc),
    NAME_DESC("nameDesc", Key.SENDER, Direction.DESC, ZimbraSortBy.nameDesc),
    RCPT_ASC("rcptAsc", Key.RCPT, Direction.ASC, ZimbraSortBy.rcptAsc),
    RCPT_DESC("rcptDesc", Key.RCPT, Direction.DESC, ZimbraSortBy.rcptDesc),
    SIZE_ASC("sizeAsc", Key.SIZE, Direction.ASC, ZimbraSortBy.sizeAsc),
    SIZE_DESC("sizeDesc", Key.SIZE, Direction.DESC, ZimbraSortBy.sizeDesc),
    ATTACHMENT_ASC("attachAsc", Key.ATTACHMENT, Direction.ASC, ZimbraSortBy.attachAsc),
    ATTACHMENT_DESC("attachDesc", Key.ATTACHMENT, Direction.DESC, ZimbraSortBy.attachDesc),
    FLAG_ASC("flagAsc", Key.FLAG, Direction.ASC, ZimbraSortBy.flagAsc),
    FLAG_DESC("flagDesc", Key.FLAG, Direction.DESC, ZimbraSortBy.flagDesc),
    PRIORITY_ASC("priorityAsc", Key.PRIORITY, Direction.ASC, ZimbraSortBy.priorityAsc),
    PRIORITY_DESC("priorityDesc", Key.PRIORITY, Direction.DESC, ZimbraSortBy.priorityDesc),
    ID_ASC("idAsc", Key.ID, Direction.ASC, ZimbraSortBy.idAsc),
    ID_DESC("idDesc", Key.ID, Direction.DESC, ZimbraSortBy.idDesc),
    READ_DESC("readDesc", Key.UNREAD, Direction.DESC, ZimbraSortBy.readDesc),
    READ_ASC("readAsc", Key.UNREAD, Direction.ASC, ZimbraSortBy.readAsc),
    RECENTLY_VIEWED("recentlyViewed", Key.RECENTLYVIEWED, Direction.DESC, ZimbraSortBy.recentlyViewed),

    // wiki "natural order" sorts are not exposed via SOAP
    NAME_NATURAL_ORDER_ASC(null, Key.NAME_NATURAL_ORDER, Direction.ASC, null),
    NAME_NATURAL_ORDER_DESC(null, Key.NAME_NATURAL_ORDER, Direction.DESC, null),

    NAME_LOCALIZED_ASC(null, Key.NAME, Direction.ASC, null),
    NAME_LOCALIZED_DESC(null, Key.NAME, Direction.DESC, null),

    // special TASK-only sorts
    TASK_DUE_ASC("taskDueAsc", Key.DATE, Direction.ASC, ZimbraSortBy.taskDueAsc),
    TASK_DUE_DESC("taskDueDesc", Key.DATE, Direction.DESC, ZimbraSortBy.taskDueDesc),
    TASK_STATUS_ASC("taskStatusAsc", Key.DATE, Direction.ASC, ZimbraSortBy.taskStatusAsc),
    TASK_STATUS_DESC("taskStatusDesc", Key.DATE, Direction.DESC, ZimbraSortBy.taskStatusDesc),
    TASK_PERCENT_COMPLETE_ASC("taskPercCompletedAsc",  Key.DATE, Direction.ASC, ZimbraSortBy.taskPercCompletedAsc),
    TASK_PERCENT_COMPLETE_DESC("taskPercCompletedDesc", Key.DATE, Direction.DESC, ZimbraSortBy.taskPercCompletedDesc);

    private final String name;
    private final Key key;
    private final Direction direction;
    private final ZimbraSortBy zsb;

    public enum Key {
        DATE, SENDER, RCPT, SUBJECT, ID, NONE, NAME, NAME_NATURAL_ORDER, SIZE, ATTACHMENT, FLAG, PRIORITY, UNREAD, RECENTLYVIEWED
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

    private SortBy(String name, Key key, Direction dir, ZimbraSortBy zimbraSortBy) {
        this.name = name;
        this.key = key;
        direction = dir;
        zsb = zimbraSortBy;
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

    public static SortBy fromZimbraSortBy(ZimbraSortBy commGT) {
        if (commGT == null) {
            return null;
        }
        for (SortBy val :SortBy.values()) {
            if (val.zsb == commGT) {
                return val;
            }
        }
        throw new IllegalArgumentException("Unrecognised ZimbraSortBy:" + commGT);
    }

    public ZimbraSortBy toZimbraSortBy() {
        return zsb;
    }

    public Comparator<ZimbraHit> getHitComparator(Locale locale) {
        locale = (locale != null) ? locale : Locale.getDefault();
        return new NameComparator(this, locale);
    }

    private static final class NameComparator implements Comparator<ZimbraHit> {
        private final SortBy sort;
        private final Collator collator;

        NameComparator(SortBy sort, Locale locale) {
            this.sort = sort;
            if (locale.equals(Locale.JAPANESE)) {
                this.collator = getJapaneseNameRuleBaseCollator();
            } else {
                this.collator = Collator.getInstance(locale);
            }

            try {
                int localDecomposition = Provisioning.getInstance().getLocalServer().getContactSearchDecomposition();
                collator.setDecomposition(localDecomposition);
            } catch (IllegalArgumentException e) {
                collator.setDecomposition(Collator.FULL_DECOMPOSITION);
                ZimbraLog.index.info("The given value is not a valid decomposition mode.  Set default value (%d)", Collator.FULL_DECOMPOSITION);
            } catch (ServiceException e) {
                collator.setDecomposition(Collator.FULL_DECOMPOSITION);
                ZimbraLog.index.info("Failed to get a valid decomposition mode.  Set default value (%d)", Collator.FULL_DECOMPOSITION);
            }
        }

        /** Create a Collator instance with Japanese specific sort order.  The instantiated collator has
         * an extended comparing rule so that the a set of ascii symbols can be treated as same manner as
         * that of 3-byte symbols defined in the higher code point.
         * @return Collator
         */
        private Collator getJapaneseNameRuleBaseCollator() {
            Collator collator = Collator.getInstance(Locale.JAPANESE);
            RuleBasedCollator jaCollator;
            if (collator instanceof RuleBasedCollator) {
                jaCollator = (RuleBasedCollator)collator;
            } else {
                ZimbraLog.index.debug("Unexpected Collator for Japanese locale. Use the rule of [%s]", collator.getClass().getName());
                return collator;
            }
            String jaRules = jaCollator.getRules();
            String supplementaryString = "& \u3001 < '!' < '\"' < '#' < '$' < '%' < '&' < '\'' < '(' < ')' < '*' < '+' < ',' < '-' < '.' < '/' < ':' < ';' < '<' < '=' < '>' < '?' < '@' < '[' < '\u00a2' = \uffe0 < '\u00a3' = '\uffe1' < '\\' < '\u00a5' = \uffe5 < ']' < '\u00a6' = '\uffe4' < '^' < '_' < '`' < '{' < '|' < '}' < '~' < \u309d < \u309e < \u30fd <  \u30fe <  \u20a1 < \u20a2 < \u20ab < \u20ac < \u20a3 < \u20a4 < \u20a5 < \u20a6 < \u20a7 < \uffe1 < \u20aa < \u20a9 < \uffe6";
            try {
                return new RuleBasedCollator (jaRules + supplementaryString);
            } catch (ParseException e) {
                // Fall back to the default collator
                ZimbraLog.index.debug("Rule parse error.  Use default rule");
                return collator;
            }
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
