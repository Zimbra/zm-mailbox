/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.imap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.zimbra.common.util.Constants;
import com.zimbra.cs.imap.ImapFlagCache.ImapFlag;
import com.zimbra.cs.imap.ImapMessage.ImapMessageSet;

abstract class ImapSearch {
    abstract boolean canBeRunLocally();
    abstract String toZimbraSearch(ImapFolder i4folder) throws ImapParseException;
    abstract ImapMessageSet evaluate(ImapFolder i4folder) throws ImapParseException;
    boolean requiresMODSEQ()  { return false; }

    static boolean isAllMessages(ImapFolder i4folder, Set<ImapMessage> i4set) {
        int size = i4set.size() - (i4set.contains(null) ? 1 : 0);
        return size == i4folder.getSize();
    }

    static String sequenceAsSearchTerm(ImapFolder i4folder, TreeSet<ImapMessage> i4set, boolean abbreviateAll) {
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
        return '"' + content.replace('"', ' ') + '"';
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

        @Override boolean requiresMODSEQ() {
            for (ImapSearch i4search : mChildren)
                if (i4search.requiresMODSEQ())
                    return true;
            return false;
        }
    }

    static class AndOperation extends LogicalOperation {
        AndOperation(ImapSearch... children)  { super();  for (ImapSearch i4search : children) addChild(i4search); }

        String toZimbraSearch(ImapFolder i4folder) throws ImapParseException {
            StringBuilder search = new StringBuilder("(");
            for (ImapSearch i4search : mChildren)
                search.append(search.length() == 1 ? "" : " ").append(i4search.toZimbraSearch(i4folder));
            return search.append(')').toString();
        }

        ImapMessageSet evaluate(ImapFolder i4folder) throws ImapParseException {
            ImapMessageSet matched = null;
            for (ImapSearch i4search : mChildren) {
                if (matched == null)
                    matched = i4search.evaluate(i4folder);
                else
                    matched.retainAll(i4search.evaluate(i4folder));
                if (matched.isEmpty())
                    break;
            }
            return matched;
        }
    }

    static class OrOperation extends LogicalOperation {
        OrOperation(ImapSearch... children)  { super();  for (ImapSearch i4search : children) addChild(i4search); }

        String toZimbraSearch(ImapFolder i4folder) throws ImapParseException {
            StringBuilder search = new StringBuilder("(");
            for (ImapSearch i4search : mChildren)
                search.append(search.length() == 1 ? "(" : " or (").append(i4search.toZimbraSearch(i4folder)).append(')');
            return search.append(')').toString();
        }

        ImapMessageSet evaluate(ImapFolder i4folder) throws ImapParseException {
            ImapMessageSet matched = null;
            for (ImapSearch i4search : mChildren) {
                if (matched == null)
                    matched = i4search.evaluate(i4folder);
                else
                    matched.addAll(i4search.evaluate(i4folder));
                if (isAllMessages(i4folder, matched))
                    break;
            }
            return matched;
        }
    }

    static class NotOperation extends LogicalOperation {
        NotOperation()                  { super(); }
        NotOperation(ImapSearch child)  { super();  addChild(child); }

        String toZimbraSearch(ImapFolder i4folder) throws ImapParseException {
            return '-' + mChildren.get(0).toZimbraSearch(i4folder);
        }

        ImapMessageSet evaluate(ImapFolder i4folder) throws ImapParseException {
            ImapMessageSet matches = i4folder.getAllMessages();
            matches.removeAll(mChildren.get(0).evaluate(i4folder));
            return matches;
        }
    }

    static class AllSearch extends ImapSearch {
        boolean canBeRunLocally()                     { return true; }
        String toZimbraSearch(ImapFolder i4folder)    { return "item:all"; }
        ImapMessageSet evaluate(ImapFolder i4folder)  { return i4folder.getAllMessages(); }
    }

    static class NoneSearch extends ImapSearch {
        boolean canBeRunLocally()                     { return true; }
        String toZimbraSearch(ImapFolder i4folder)    { return "item:none"; }
        ImapMessageSet evaluate(ImapFolder i4folder)  { return new ImapMessageSet(); }
    }

    static class SequenceSearch extends ImapSearch {
        private String mTag;
        private String mSubSequence;
        private boolean mIsUidSearch;
        SequenceSearch(String tag, String subSequence, boolean byUID)  { mTag = tag;  mSubSequence = subSequence;  mIsUidSearch = byUID; }

        boolean canBeRunLocally()  { return true; }

        String toZimbraSearch(ImapFolder i4folder) throws ImapParseException {
            return sequenceAsSearchTerm(i4folder, evaluate(i4folder), true);
        }

        ImapMessageSet evaluate(ImapFolder i4folder) throws ImapParseException {
            return i4folder.getSubsequence(mTag, mSubSequence, mIsUidSearch);
        }
    }

    static class FlagSearch extends ImapSearch {
        private String mFlagName;
        FlagSearch(String flagName)  { mFlagName = flagName; }

        boolean canBeRunLocally()  { return true; }

        String toZimbraSearch(ImapFolder i4folder) {
            ImapFlag i4flag = i4folder.getFlagByName(mFlagName);
            if (i4flag == null)
                return "item:none";
            String prefix = i4flag.mPositive ? "" : "(-", suffix = i4flag.mPositive ? "" : ")";
            if (i4flag.mPermanent)
                return prefix + "tag:" + i4flag.mName + suffix;
            return prefix + sequenceAsSearchTerm(i4folder, i4folder.getFlaggedMessages(i4flag), true) + suffix;
        }

        ImapMessageSet evaluate(ImapFolder i4folder) {
            ImapFlag i4flag = i4folder.getFlagByName(mFlagName);
            if (i4flag == null)
                return new ImapMessageSet();
            if (i4flag.mPositive)
                return i4folder.getFlaggedMessages(i4flag);
            ImapMessageSet matched = i4folder.getAllMessages();
            matched.removeAll(i4folder.getFlaggedMessages(i4flag));
            return matched;
        }
    }

    static class DateSearch extends ImapSearch {
        enum Relation {
            before("before:"), after("date:>="), date("date:");

            String query;
            Relation(String rep)                { query = rep; }
            @Override public String toString()  { return query; }
        };
        private Relation mRelation;
        private Date mDate;
        private long mTimestamp;
        DateSearch(Relation relation, Date date)  { mDate = date;  mTimestamp = date.getTime();  mRelation = relation; }

        boolean canBeRunLocally() {
            return mTimestamp < 0 || mTimestamp > System.currentTimeMillis() + 36 * Constants.MILLIS_PER_MONTH;
        }

        String toZimbraSearch(ImapFolder i4folder)  {
            if (mTimestamp < 0)
                return (mRelation == Relation.after ? "item:all" : "item:none");
            else if (mTimestamp > System.currentTimeMillis() + 36 * Constants.MILLIS_PER_MONTH)
                return (mRelation == Relation.before ? "item:all" : "item:none");
            else
                return mRelation + i4folder.getHandler().getZimbraFormat().format(mDate);
        }

        ImapMessageSet evaluate(ImapFolder i4folder) {
            if (mTimestamp < 0)
                return (mRelation == Relation.after ? i4folder.getAllMessages() : new ImapMessageSet());
            else if (mTimestamp > System.currentTimeMillis() + 36 * Constants.MILLIS_PER_MONTH)
                return (mRelation == Relation.before ? i4folder.getAllMessages() : new ImapMessageSet());
            else
                throw new UnsupportedOperationException("evaluate of " + toZimbraSearch(i4folder));
        }
    }

    static class RelativeDateSearch extends ImapSearch {
        private DateSearch.Relation mRelation;
        private int mOffset;
        RelativeDateSearch(DateSearch.Relation relation, int offset)  { mOffset = offset;  mRelation = relation; }

        boolean canBeRunLocally()                   { return false; }
        String toZimbraSearch(ImapFolder i4folder)  { return mRelation.toString() + (System.currentTimeMillis() - mOffset * Constants.MILLIS_PER_SECOND); }
        ImapMessageSet evaluate(ImapFolder i4folder) {
            throw new UnsupportedOperationException("evaluate of " + toZimbraSearch(i4folder));
        }
    }

    static class ModifiedSearch extends ImapSearch {
        private int mChangedSince;
        ModifiedSearch(int changeId)  { mChangedSince = changeId; }

        @Override boolean requiresMODSEQ()  { return true; }
        boolean canBeRunLocally()           { return false; }
        String toZimbraSearch(ImapFolder i4folder) {
            ImapFlagCache i4cache = i4folder.getTagset();
            StringBuilder query = new StringBuilder("(modseq:>").append(mChangedSince);
            if (i4cache.getMaximumModseq() > mChangedSince) {
                for (ImapFlag i4flag : i4cache)
                    if (i4flag.mModseq > mChangedSince)
                        query.append(" or tag:").append(i4flag.mName);
            }
            return query.append(')').toString();
        }
        ImapMessageSet evaluate(ImapFolder i4folder) {
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
        ImapMessageSet evaluate(ImapFolder i4folder) {
            throw new UnsupportedOperationException("evaluate of " + toZimbraSearch(i4folder));
        }
    }

    static class ContentSearch extends ImapSearch {
        private String mValue;
        ContentSearch(String value)  { mValue = value; }

        boolean canBeRunLocally()                   { return false; }
        String toZimbraSearch(ImapFolder i4folder)  { return stringAsSearchTerm(mValue); }
        ImapMessageSet evaluate(ImapFolder i4folder) {
            throw new UnsupportedOperationException("evaluate of " + toZimbraSearch(i4folder));
        }
    }

    static class HeaderSearch extends ImapSearch {
        static class Header {
            static final Header SUBJECT = new Header("subject", "subject");
            static final Header FROM = new Header("from", "from");
            static final Header TO = new Header("to", "to");
            static final Header CC = new Header("cc", "cc");
            static final Header MSGID = new Header("message-id", "msgid");

            private static final Header[] SPECIAL_HEADERS = new Header[] { SUBJECT, FROM, TO, CC, MSGID };

            final String mField, mKey;
            private Header(String fieldName, String searchKey) {
                mField = fieldName;  mKey = searchKey;
            }

            static Header parse(String fieldName) {
                fieldName = fieldName.toLowerCase();
                for (Header syshdr : SPECIAL_HEADERS) {
                    if (fieldName.equals(syshdr.mField))
                        return syshdr;
                }
                return new Header(fieldName, '#' + fieldName.replace(' ', '-').replace('\t', '-').replace('"', '-'));
            }

            @Override public String toString()  { return mKey; }
        };

        private Header mHeader;
        private String mValue;
        HeaderSearch(Header header, String value) {
            while (value.startsWith("<") || value.startsWith(">") || value.startsWith("="))
                value = value.substring(1);
            if (header == Header.MSGID && value.endsWith(">"))
                value = value.substring(0, value.length());
            value = value.replace('*', ' ');
            if (value.trim().equals(""))
                value = "*";
            mValue = value;  mHeader = header;
        }

        boolean canBeRunLocally()                   { return false; }
        String toZimbraSearch(ImapFolder i4folder)  { return mHeader + ":" + stringAsSearchTerm(mValue); }
        ImapMessageSet evaluate(ImapFolder i4folder) {
            throw new UnsupportedOperationException("evaluate of " + toZimbraSearch(i4folder));
        }
    }
}
