/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.zimbra.common.util.SetUtil;
import com.zimbra.cs.imap.ImapFlagCache.ImapFlag;

abstract class ImapSearch {
    abstract boolean canBeRunLocally();
    abstract String toZimbraSearch(ImapFolder i4folder);
    abstract Set<ImapMessage> evaluate(ImapFolder i4folder);

    static boolean isAllMessages(ImapFolder i4folder, Set<ImapMessage> i4set) {
        int size = i4set.size() - (i4set.contains(null) ? 1 : 0);
        return size == i4folder.getSize();
    }

    static String sequenceAsSearchTerm(ImapFolder i4folder, Set<ImapMessage> i4set, boolean abbreviateAll) {
        i4set.remove(null);
        if (i4set.isEmpty())
            return "item:none";
        else if (abbreviateAll && isAllMessages(i4folder, i4set))
            return "item:all";
        StringBuilder sb = new StringBuilder("item:{");
        for (ImapMessage i4msg : i4set)
            sb.append(sb.length() == 6 ? "" : ",").append(i4msg.msgId);
        return sb.append('}').toString();
    }

    static String stringAsSearchTerm(String content) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\\')      sb.append("\\\"");
            else if (c == '"')  sb.append("\\\\");
            else                sb.append(c);
        }
        return sb.append('"').toString();
    }


    static abstract class LogicalOperation extends ImapSearch {
        List<ImapSearch> mChildren = new ArrayList<ImapSearch>();

        boolean canBeRunLocally() {
            for (ImapSearch i4search : mChildren)
                if (!i4search.canBeRunLocally())
                    return false;
            return true;
        }

        LogicalOperation addChild(ImapSearch i4search) {
            mChildren.add(i4search);  return this;
        }
    }

    static class AndOperation extends LogicalOperation {
        AndOperation(ImapSearch... children)  { super();  for (ImapSearch i4search : children) addChild(i4search); }

        String toZimbraSearch(ImapFolder i4folder) {
            StringBuilder search = new StringBuilder("(");
            for (ImapSearch i4search : mChildren)
                search.append(search.length() == 1 ? "" : " ").append(i4search.toZimbraSearch(i4folder));
            return search.append(')').toString();
        }

        Set<ImapMessage> evaluate(ImapFolder i4folder) {
            Set<ImapMessage> matched = null;
            for (ImapSearch i4search : mChildren) {
                matched = (matched == null ? i4search.evaluate(i4folder) : SetUtil.intersect(matched, i4search.evaluate(i4folder)));
                if (matched.isEmpty())
                    break;
            }
            return matched;
        }
    }

    static class OrOperation extends LogicalOperation {
        OrOperation(ImapSearch... children)  { super();  for (ImapSearch i4search : children) addChild(i4search); }

        String toZimbraSearch(ImapFolder i4folder) {
            StringBuilder search = new StringBuilder("(");
            for (ImapSearch i4search : mChildren)
                search.append(search.length() == 1 ? "(" : " or (").append(i4search.toZimbraSearch(i4folder)).append(')');
            return search.append(')').toString();
        }

        Set<ImapMessage> evaluate(ImapFolder i4folder) {
            Set<ImapMessage> matched = null;
            for (ImapSearch i4search : mChildren) {
                matched = (matched == null ? i4search.evaluate(i4folder) : SetUtil.union(matched, i4search.evaluate(i4folder)));
                if (isAllMessages(i4folder, matched))
                    break;
            }
            return matched;
        }
    }

    static class NotOperation extends LogicalOperation {
        NotOperation()                  { super(); }
        NotOperation(ImapSearch child)  { super();  addChild(child); }

        String toZimbraSearch(ImapFolder i4folder) {
            return '-' + mChildren.get(0).toZimbraSearch(i4folder);
        }

        Set<ImapMessage> evaluate(ImapFolder i4folder) {
            Set<ImapMessage> matches = i4folder.getAllMessages();
            matches.removeAll(mChildren.get(0).evaluate(i4folder));
            return matches;
        }
    }

    static class AllSearch extends ImapSearch {
        boolean canBeRunLocally()                       { return true; }
        String toZimbraSearch(ImapFolder i4folder)      { return "item:all"; }
        Set<ImapMessage> evaluate(ImapFolder i4folder)  { return i4folder.getAllMessages(); }
    }

    static class NoneSearch extends ImapSearch {
        boolean canBeRunLocally()                       { return true; }
        String toZimbraSearch(ImapFolder i4folder)      { return "item:none"; }
        Set<ImapMessage> evaluate(ImapFolder i4folder)  { return Collections.emptySet(); }
    }

    static class SequenceSearch extends ImapSearch {
        private String mSubSequence;
        private boolean mIsUidSearch;
        SequenceSearch(String subSequence, boolean byUID)  { mSubSequence = subSequence;  mIsUidSearch = byUID; }

        boolean canBeRunLocally()                       { return true; }
        String toZimbraSearch(ImapFolder i4folder)      { return sequenceAsSearchTerm(i4folder, evaluate(i4folder), true); }
        Set<ImapMessage> evaluate(ImapFolder i4folder)  { return i4folder.getSubsequence(mSubSequence, mIsUidSearch); }
    }

    static class FlagSearch extends ImapSearch {
        private String mFlagName;
        FlagSearch(String flagName)  { mFlagName = flagName; }

        boolean canBeRunLocally()  { return true; }

        String toZimbraSearch(ImapFolder i4folder) {
            ImapFlag i4flag = i4folder.getSession().getFlagByName(mFlagName);
            if (i4flag == null)
                return "item:none";
            String prefix = i4flag.mPositive ? "" : "(-", suffix = i4flag.mPositive ? "" : ")";
            if (i4flag.mPermanent)
                return prefix + "tag:" + i4flag.mName + suffix;
            return prefix + sequenceAsSearchTerm(i4folder, i4folder.getFlaggedMessages(i4flag), true) + suffix;
        }

        Set<ImapMessage> evaluate(ImapFolder i4folder) {
            ImapFlag i4flag = i4folder.getSession().getFlagByName(mFlagName);
            if (i4flag == null)
                return Collections.emptySet();
            if (i4flag.mPositive)
                return i4folder.getFlaggedMessages(i4flag);
            Set<ImapMessage> matched = i4folder.getAllMessages();
            matched.removeAll(i4folder.getFlaggedMessages(i4flag));
            return matched;
        }
    }

    static class DateSearch extends ImapSearch {
        enum Relation { before, after, date };
        private Relation mRelation;
        private Date mDate;
        DateSearch(Relation relation, Date date)  { mDate = date;  mRelation = relation; }

        boolean canBeRunLocally()                   { return false; }
        String toZimbraSearch(ImapFolder i4folder)  { return mRelation + ":" + i4folder.getSession().getZimbraDateFormat().format(mDate); }
        Set<ImapMessage> evaluate(ImapFolder i4folder) {
            throw new UnsupportedOperationException("evaluate of " + toZimbraSearch(i4folder));
        }
    }

    static class RelativeDateSearch extends ImapSearch {
        private DateSearch.Relation mRelation;
        private int mOffset;
        RelativeDateSearch(DateSearch.Relation relation, int offset)  { mOffset = offset;  mRelation = relation; }

        boolean canBeRunLocally()                   { return false; }
        String toZimbraSearch(ImapFolder i4folder)  { return mRelation + ":-" + mOffset + 'h'; }
        Set<ImapMessage> evaluate(ImapFolder i4folder) {
            throw new UnsupportedOperationException("evaluate of " + toZimbraSearch(i4folder));
        }
    }

    static class SizeSearch extends ImapSearch {
        enum Relation { larger, smaller };
        private Relation mRelation;
        private long mSize;
        SizeSearch(Relation relation, long size)  { mSize = size;  mRelation = relation; }

        boolean canBeRunLocally()                   { return false; }
        String toZimbraSearch(ImapFolder i4folder)  { return mRelation + ":" + mSize; }
        Set<ImapMessage> evaluate(ImapFolder i4folder) {
            throw new UnsupportedOperationException("evaluate of " + toZimbraSearch(i4folder));
        }
    }

    static class ContentSearch extends ImapSearch {
        enum Relation {
            cc, from, subject, to, body;

            static Relation parse(String tag, String header) throws ImapParseException {
                try {
                    return Relation.valueOf(header.toLowerCase());
                } catch (IllegalArgumentException iae) {
                    throw new ImapParseException(tag, "unindexed header: " + header, true);
                }
            }
        };
        private Relation mRelation;
        private String mValue;
        ContentSearch(Relation relation, String value)  { mValue = value;  mRelation = relation; }

        boolean canBeRunLocally()                   { return false; }
        String toZimbraSearch(ImapFolder i4folder)  { return (mRelation == Relation.body ? "" : mRelation + ":") + stringAsSearchTerm(mValue); }
        Set<ImapMessage> evaluate(ImapFolder i4folder) {
            throw new UnsupportedOperationException("evaluate of " + toZimbraSearch(i4folder));
        }
    }
}
