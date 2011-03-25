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
package com.zimbra.cs.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.common.util.ListUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

/**
 * A class which encapsulates all of the constraints we can do on a mailbox search.
 * <ul>
 *  <li>"required" entries must be set, or you won't get what you want
 *  <li>"optional" entries are ignored if the default value is passed
 * </ul>
 *
 * @author tim
 */
public class DbSearchConstraints implements DbSearchConstraintsNode, Cloneable {

    @Override
    public DbSearchConstraintsNode.NodeType getNodeType() {
        return DbSearchConstraintsNode.NodeType.LEAF;
    }

    @Override
    public Iterable<DbSearchConstraintsNode> getSubNodes() {
        return null;
    }

    @Override
    public DbSearchConstraints getSearchConstraints() {
        return this;
    }

    public static class StringRange {
        public boolean negated = false;
        public String lowest = null;  public boolean lowestEqual = false;
        public String highest = null;  public boolean highestEqual = false;

        @Override
        public boolean equals(Object o) {
            DbSearchConstraints.StringRange other = (DbSearchConstraints.StringRange) o;
            return (
                        StringUtil.equal(other.lowest, lowest) && other.lowestEqual == lowestEqual &&
                        StringUtil.equal(other.highest, highest) && other.highestEqual == highestEqual &&
                        other.negated == negated
            );
        }

        @Override
        public String toString() {
            StringBuilder retVal = new StringBuilder();

            if (negated) {
                retVal.append("NOT (");
            }

            if (lowest != null) {
                retVal.append("\">");
                if (lowestEqual)
                    retVal.append('=');
                retVal.append(lowest);
                retVal.append("\" ");
            }

            if (highest != null) {
                retVal.append("\"<");
                if (highestEqual)
                    retVal.append('=');
                retVal.append(highest);
                retVal.append("\" ");
            }
            if (negated) {
                retVal.append(")");
            }

            return retVal.toString();
        }

        boolean isValid()  { return true; }
    }

    public static class NumericRange {
        public boolean negated = false;
        public long lowest = -1;
        public boolean lowestEqual = false;
        public long highest = -1;
        public boolean highestEqual = false;

        @Override
        public boolean equals(Object o) {
            DbSearchConstraints.NumericRange other = (DbSearchConstraints.NumericRange) o;
            return (
                        (other.negated == negated) && (other.lowest == lowest) && (other.highest == highest) &&
                        (other.lowestEqual == lowestEqual) && (other.highestEqual == highestEqual)
            );
        }

        boolean isValid() {
            return lowest > 0 || highest > 0;
        }

        @Override
        public String toString() {
            StringBuilder retVal = new StringBuilder();

            if (negated) {
                retVal.append("NOT (");
            }

            // special-case ">=N AND <=N" to be "=N"
            if (lowest>-1 && lowestEqual && highestEqual && lowest == highest) {
                retVal.append(lowest);
            } else {
                boolean hasLowest = false;

                if (lowest > -1) {
                    retVal.append(">");
                    if (lowestEqual)
                        retVal.append('=');
                    retVal.append(lowest);
                    hasLowest = true;
                }

                if (highest > -1) {
                    if (hasLowest)
                        retVal.append(' ');
                    retVal.append("<");
                    if (highestEqual)
                        retVal.append('=');
                    retVal.append(highest);
                }
            }

            if (negated) {
                retVal.append(")");
            }

            return retVal.toString();
        }
    }

    /**
     * When we COMBINE the operations during query optimization, we'll need to track some state values for example
     * "no results".
     */
    public boolean noResults = false;

    // These are the main constraints

    /** optional - SPECIAL CASE -- ALL listed tags must be present. NULL IS DIFFERENT THAN EMPTY SET!!! */
    public Set<Tag> tags = null;

    /** optional - ALL listed tags must be NOT present. */
    public Set<Tag> excludeTags = new HashSet<Tag>();

    /** optional. */
    public Boolean hasTags = null;

    /** optional - ANY of these folders are OK. */
    public Set<Folder> folders = new HashSet<Folder>();

    /** optional - ALL listed folders not allowed. */
    public Set<Folder> excludeFolders = new HashSet<Folder>();

    /** optional - ANY of these folders are OK. */
    public Set<RemoteFolderDescriptor> remoteFolders = new HashSet<RemoteFolderDescriptor>();

    /** optional - ALL listed folders are not OK. */
    public Set<RemoteFolderDescriptor> excludeRemoteFolders = new HashSet<RemoteFolderDescriptor>();

    /** optional - must match this convId. */
    public int convId = 0;

    /** optional - ALL listed convs not allowed. */
    public Set<Integer> prohibitedConvIds = new HashSet<Integer>();

    /** optional. */
    public ItemId remoteConvId = null;

    /** optional - ALL listed convs not allowed. */
    public Set<ItemId> prohibitedRemoteConvIds = new HashSet<ItemId>();

    /** optional - ANY of these itemIDs are OK. */
    public Set<Integer> itemIds = new HashSet<Integer>();

    /** optional - ALL of these itemIDs are excluded. */
    public Set<Integer> prohibitedItemIds = new HashSet<Integer>();

    /** optional - ANY of these itemIDs are OK. */
    public Set<ItemId> remoteItemIds = new HashSet<ItemId>();

    /** optional - ALL of these itemIDs are excluded. */
    public Set<ItemId> prohibitedRemoteItemIds = new HashSet<ItemId>();

    /** optional - ANY of these indexIDs are OK. */
    public Set<Integer> indexIds = new HashSet<Integer>();

    /** optional - index_id must be present. */
    public Boolean hasIndexId = null;

    /** optional - ANY of these types are OK. */
    public Set<MailItem.Type> types = EnumSet.noneOf(MailItem.Type.class);

    /** optional - ALL of these types are excluded. */
    public Set<MailItem.Type> excludeTypes = EnumSet.noneOf(MailItem.Type.class);

    /** optional - join with mail_address on sender_id = id, and mail_address.contact_count > 0. */
    public Boolean fromContact;

    //
    // A possible result must match *ALL* the range constraints below.  It might seem strange that we don't
    // just combine the ranges -- yes this would be easy for positive ranges (foo>5 AND foo >7 and foo<10)
    // but it quickly gets very complicated with negative ranges such as (3<foo<100 AND NOT 7<foo<20)
    //
    public Collection<NumericRange> dates = new ArrayList<NumericRange>();    /* optional */
    public Collection<NumericRange> calStartDates = new ArrayList<NumericRange>();    /* optional */
    public Collection<NumericRange> calEndDates = new ArrayList<NumericRange>();    /* optional */
    public Collection<NumericRange> modified = new ArrayList<NumericRange>(); /* optional */
    public Collection<NumericRange> modifiedContent = new ArrayList<NumericRange>(); /* optional */
    public Collection<NumericRange> sizes = new ArrayList<NumericRange>();    /* optional */
    public Collection<NumericRange> convCounts = new ArrayList<NumericRange>(); /* optional */
    public Collection<StringRange> subjectRanges = new ArrayList<StringRange>(); /* optional */
    public Collection<StringRange> senderRanges = new ArrayList<StringRange>(); /* optional */

    private Set<MailItem.Type> calcTypes() {
        if (excludeTypes.isEmpty()) {
            return types;
        }
        if (types.isEmpty()) {
            types.addAll(EnumSet.allOf(MailItem.Type.class));
            types.remove(MailItem.Type.UNKNOWN);
        }
        for (MailItem.Type type : excludeTypes) {
            types.remove(type);
        }
        excludeTypes.clear();
        return types;
    }

    /**
     * @return TRUE if these constraints have a non-appointment type in them
     */
    boolean hasNonAppointmentTypes() {
        Set<MailItem.Type> types = EnumSet.copyOf(calcTypes());
        types.remove(MailItem.Type.APPOINTMENT);
        types.remove(MailItem.Type.TASK);
        return !types.isEmpty();
    }

    /**
     * @return TRUE if these constraints have a constraint that requires a join with the Appointment table
     */
    boolean hasAppointmentTableConstraints() {
        Set<MailItem.Type> fullTypes = calcTypes();
        return ((calStartDates.size() > 0 || calEndDates.size() > 0) &&
                (fullTypes.contains(MailItem.Type.APPOINTMENT) || fullTypes.contains(MailItem.Type.TASK)));
    }

    /**
     * Returns the only folder if query is for a single folder, item type list includes {@link MailItem.Type#MESSAGE},
     * and no other conditions are specified. Otherwise returns null.
     */
    public Folder getOnlyFolder() {
        if (folders.size() == 1 && excludeFolders.isEmpty() &&
                types.contains(MailItem.Type.MESSAGE) && !excludeTypes.contains(MailItem.Type.MESSAGE) &&
                hasTags == null && (excludeTags == null || excludeTags.isEmpty()) &&
                (tags == null || tags.isEmpty()) && convId == 0 && prohibitedConvIds.isEmpty() &&
                itemIds.isEmpty() && prohibitedItemIds.isEmpty() && indexIds.isEmpty() && dates.isEmpty() &&
                calStartDates.isEmpty() && calEndDates.isEmpty() && modified.isEmpty() && modifiedContent.isEmpty() &&
                sizes.isEmpty() && convCounts.isEmpty() && subjectRanges.isEmpty() && senderRanges.isEmpty()) {
            return Iterables.getOnlyElement(folders);
        } else {
            return null;
        }
    }

    /**
     * @return TRUE/FALSE if the conv-count constraint is set, and if the constraint
     * is exactly 1 (ie >=1 && <=1)....this is a useful special case b/c if
     * this is true then we can generate much simpler SQL than for the more complicated
     * conv-count cases...return NULL otherwise.
     *
     *    TRUE   "is:solo"
     *    FLASE  "-is:solo"
     *    null   everything else
     */
    public Boolean getIsSoloPart() {
        if (convCounts.size() != 1)
            return null;
        NumericRange r = convCounts.iterator().next();

        if (r.highest == 1 && r.highestEqual == true &&
                    r.lowest == 1 && r.lowestEqual == true) {
            if (r.negated)
                return Boolean.FALSE;
            else
                return Boolean.TRUE;
        } else {
            return null;
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        DbSearchConstraints result = (DbSearchConstraints) super.clone();

        result.tags = SetHelper.cloneHashSet(tags);
        result.excludeTags = SetHelper.cloneHashSet(excludeTags);

        result.folders = SetHelper.cloneHashSet(folders);
        result.excludeFolders = SetHelper.cloneHashSet(excludeFolders);

        result.remoteFolders = SetHelper.cloneHashSet(remoteFolders);
        result.excludeRemoteFolders = SetHelper.cloneHashSet(excludeRemoteFolders);

        result.convId = convId;
        result.prohibitedConvIds = SetHelper.cloneHashSet(prohibitedConvIds);

        result.remoteConvId = remoteConvId;
        result.prohibitedRemoteConvIds = SetHelper.cloneHashSet(prohibitedRemoteConvIds);

        result.itemIds = SetHelper.cloneHashSet(itemIds);
        result.prohibitedItemIds = SetHelper.cloneHashSet(prohibitedItemIds);

        result.remoteItemIds = SetHelper.cloneHashSet(remoteItemIds);
        result.prohibitedRemoteItemIds = SetHelper.cloneHashSet(prohibitedRemoteItemIds);

        result.indexIds = SetHelper.cloneHashSet(indexIds);

        result.types = SetHelper.cloneHashSet(types);
        result.excludeTypes = SetHelper.cloneHashSet(excludeTypes);

        result.dates = SetHelper.cloneHashSet(dates);
        result.calStartDates = SetHelper.cloneHashSet(calStartDates);
        result.calEndDates = SetHelper.cloneHashSet(calEndDates);

        result.modified = SetHelper.cloneHashSet(modified);
        result.modifiedContent = SetHelper.cloneHashSet(modifiedContent);
        result.sizes = SetHelper.cloneHashSet(sizes);
        result.convCounts = SetHelper.cloneHashSet(convCounts);
        result.subjectRanges = SetHelper.cloneHashSet(subjectRanges);
        result.senderRanges = SetHelper.cloneHashSet(senderRanges);
        result.fromContact = fromContact;

        return result;
    }

    /**
     * Hackhackhack -- the TagConstraints are generated during the query generating process during
     * DbMailItem.encodeConstraint, and then they are cached in this object.  This is pretty ugly
     * but works for now.  TODO cleanup.
     */
    DbSearch.TagConstraints tagConstraints;

    private static abstract class Printer<T> {
        void run(StringBuilder str, Collection<T> collect, String intro) {
            if (!ListUtil.isEmpty(collect)) {
                str.append(intro).append(":(");
                boolean atFirst = true;
                for (T elt: collect) {
                    if (!atFirst)
                        str.append(", ");
                    printOne(str, elt);
                    atFirst = false;
                }
                str.append(')');
            }
        }

        abstract void printOne(StringBuilder s, T t);
    }

    private static class FolderPrinter {
        void run(StringBuilder out, boolean bool, Collection<Folder> collect) {
            if (!ListUtil.isEmpty(collect)) {
                out.append('(');
                boolean atFirst = true;
                for (Folder f: collect) {
                    if (!atFirst) {
                        out.append(' ');
                    }
                    if (!bool) {
                        out.append('-');
                    }
                    if (f instanceof Mountpoint) {
                        out.append("INID:");
                        out.append(((Mountpoint) f).getRemoteId());
                    } else {
                        out.append("IN:").append(f.getPath());
                    }
                    atFirst = false;
                }
                out.append(')');
            }
        }
    }

    private static class ItemIdPrinter {
        void run(StringBuilder out, boolean bool, Collection<ItemId> collect, String intro) {
            if (!ListUtil.isEmpty(collect)) {
                out.append('(');
                boolean atFirst = true;
                for (ItemId iid: collect) {
                    if (!atFirst) {
                        out.append(' ');
                    }
                    if (!bool) {
                        out.append('-');
                    }
                    out.append(intro).append(":\"").append(iid.toString()).append('"');
                    atFirst = false;
                }
                out.append(')');
            }
        }
    }

    private static class RemoteFolderPrinter {
        void run(StringBuilder out, boolean bool, Collection<RemoteFolderDescriptor> collect) {
            if (!ListUtil.isEmpty(collect)) {
                out.append('(');
                boolean atFirst = true;
                for (RemoteFolderDescriptor rf: collect) {
                    if (!atFirst) {
                        out.append(' ');
                    }
                    if (!bool) {
                        out.append('-');
                    }
                    String intro = rf.getIncludeSubfolders() ? "UNDERID" : "INID";
                    out.append(intro).append(":\"").append(rf.getFolderId().toString());
                    if (rf.getSubfolderPath() != null && rf.getSubfolderPath().length() > 0) {
                        out.append('/').append(rf.getSubfolderPath());
                    }
                    out.append('"');
                    atFirst = false;
                }
                out.append(')');
            }
        }
    }

    private static class ObjectPrinter<T extends Object> extends Printer<T> {
        @Override
        void printOne(StringBuilder s, T t) {
            s.append(t.toString());
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        if (noResults) {
            result.append("-is:anywhere ");
        }

        Printer<Tag> tp = new Printer<Tag>() {
            @Override
            void printOne(StringBuilder out, Tag tag) {
                out.append(tag.getName());
            }
        };
        Printer<Integer> ip = new Printer<Integer>() {
            @Override
            void printOne(StringBuilder out, Integer i) {
                out.append(i);
            }
        };
        Printer<MailItem.Type> bp = new Printer<MailItem.Type>() {
            @Override
            void printOne(StringBuilder out, MailItem.Type type) {
                out.append(type);
            }
        };

        FolderPrinter fp = new FolderPrinter();
        RemoteFolderPrinter rfp = new RemoteFolderPrinter();
        ItemIdPrinter iip = new ItemIdPrinter();

        // tags
        tp.run(result, tags, "TAG");
        tp.run(result, excludeTags, "-TAG");

        // hasTags?
        if (hasTags != null) {
            if (!hasTags) {
                result.append('-');
            }
            result.append("(HAS_TAG)");
        }

        // folders
        fp.run(result, true, folders);
        fp.run(result, false, excludeFolders);

        // remote folders
        rfp.run(result, true, remoteFolders);
        rfp.run(result, false, excludeRemoteFolders);

        // convId
        if (convId != 0) {
            result.append("CONV:(").append(convId).append(") ");
        }
        ip.run(result, prohibitedConvIds, "-CONVID");

        // remoteConvId
        if (remoteConvId != null) {
            result.append("CONV:\"").append(remoteConvId).append("\" ");
        }
        iip.run(result, false, prohibitedRemoteConvIds, "CONV");

        // itemId
        ip.run(result, itemIds, "ITEM");
        ip.run(result, prohibitedItemIds, "-ITEM");

        // remoteItemId
        iip.run(result, true, remoteItemIds, "ITEM");
        iip.run(result, false, prohibitedRemoteItemIds, "ITEM");

        // indexId
        ip.run(result, indexIds, "INDEXID");

        if (hasIndexId != null) {
            if (!hasIndexId) {
                result.append('-');
            }
            result.append("(HAS_INDEXID)");
        }

        // type
        bp.run(result, types, "TYPE");
        bp.run(result, excludeTypes, "-TYPE");

        if (!dates.isEmpty()) {
            new ObjectPrinter<NumericRange>().run(result, dates, "DATE");
        }
        if (!calStartDates.isEmpty()) {
            new ObjectPrinter<NumericRange>().run(result, calStartDates, "APPT-START");
        }
        if (!calEndDates.isEmpty()) {
            new ObjectPrinter<NumericRange>().run(result, calEndDates, "APPT-END");
        }
        if (!modified.isEmpty()) {
            new ObjectPrinter<NumericRange>().run(result, modified, "MOD") ;
        }
        if (!modifiedContent.isEmpty()) {
            new ObjectPrinter<NumericRange>().run(result, modified, "MOD-CONTENT") ;
        }
        if (!sizes.isEmpty()) {
            new ObjectPrinter<NumericRange>().run(result, sizes, "SIZE");
        }
        if (!convCounts.isEmpty()) {
            new ObjectPrinter<NumericRange>().run(result, convCounts, "CONV-COUNT");
        }
        if (!subjectRanges.isEmpty()) {
            new ObjectPrinter<StringRange>().run(result, subjectRanges, "SUBJECT");
        }
        if (!senderRanges.isEmpty()) {
            new ObjectPrinter<StringRange>().run(result, senderRanges, "FROM");
        }
        if (fromContact != null) {
            if (!fromContact) {
                result.append('-');
            }
            result.append("(FROM-CONTACT)");
        }
        return result.toString();
    }

    /**
     * NOT general-purpose, makes assumptions about when-to-copy and
     * that NULL != empty-set that are not something one would generally want
     */
    private static final class SetHelper {
        static <T> HashSet<T> cloneHashSet(Collection<T> current) {
            if (current == null)
                return null;
            HashSet<T> toRet = new HashSet<T>();
            toRet.addAll(current);
            return toRet;
        }

        static void addIdsToSet(Set<Integer> s, Collection<?> items) {
            if (items != null)
                for (Object obj : items)
                    s.add(((MailItem)obj).getId());
        }


        static <T> Set<T> clone(Set<T> s) {
            Set<T> toRet = null;
            if (s != null) {
                toRet = new HashSet<T>();
                toRet.addAll(s);
            }
            return toRet;
        }

        static <T> Set<T> intersectIfNonempty(Set<T> lhs, Set<T> rhs) {
            assert(lhs != null && rhs != null);

            if (lhs.isEmpty()) {
                return clone(rhs);
            } else if (rhs.isEmpty()) {
                return lhs;
            } else {
                Set<T> result = new HashSet<T>();
                for (T t : rhs) {
                    if (lhs.contains(t)) {
                        result.add(t);
                    }
                }
                return result;
            }
        }

        static <T> Set<T> union(Set<T> lhs, Set<T> rhs) {
            if (rhs == null) {
                return lhs;
            } else if (lhs == null) {
                return clone(rhs);
            } else {
                lhs.addAll(rhs);
                return lhs;
            }
        }
    }

    /**
     * this = this AND other
     */
    public void andConstraints(DbSearchConstraints other) {
        if (noResults || other.noResults) {
            noResults = true;
            return;
        }

        // tags
        tags = SetHelper.union(tags, other.tags);
        excludeTags = SetHelper.union(excludeTags, other.excludeTags);

        // bug 2426
        if (tags != null && excludeTags != null)
            for (Tag t : tags) {
                if (excludeTags.contains(t)) {
                    noResults = true;
                    return;
                }
            }

        // has tags
        if (hasTags == null) {
            hasTags = other.hasTags;
        } else if (other.hasTags != null) {
            if (!hasTags.equals(other.hasTags)) {
                noResults = true;
                ZimbraLog.index.debug("Adding a HAS_NO_TAGS constraint to a HAS_TAGS one, this is a NO_RESULTS result");
                return;
            }
        }

        // folders
        // these we have to intersect:
        //  Folder{A or B or C} AND Folder{B or C or D} --> Folder{IN-BOTH}
        //  if both sets are empty going in, then an empty set means "no constraint"....on the other hand if either set
        //  is nonempty going in, then an empty set coming out means "no results".
        // TODO ugly. Should modify this so folders=null means "no constraint" and folders=[] means "no results".
        if (folders.size() >  0 ||  other.folders.size() > 0) {
            folders = SetHelper.intersectIfNonempty(folders, other.folders);
            if (folders.size() == 0)
                noResults = true;
        }

        excludeFolders = SetHelper.union(excludeFolders, other.excludeFolders);

        // remoteFolders
        // include-remote-folders searches cannot be properly intersected,
        // so don't try to intersect the sets right now -- just union (bug
        remoteFolders = SetHelper.union(remoteFolders, other.remoteFolders);

        excludeRemoteFolders = SetHelper.union(excludeRemoteFolders, other.excludeRemoteFolders);

        // convId
        if (other.convId != 0) {
            if (convId != 0) {
                if (convId != other.convId) {
                    ZimbraLog.index.debug("ANDING a constraint with incompatible convIds, this is a NO_RESULTS constraint now");
                    noResults = true;
                }
            } else {
                convId = other.convId;
            }
        }
        prohibitedConvIds = SetHelper.union(prohibitedConvIds, other.prohibitedConvIds);

        // remoteConvID
        if (other.remoteConvId != null) {
            if (remoteConvId != null) {
                if (!remoteConvId.equals(other.remoteConvId)) {
                    ZimbraLog.index.debug("ANDING a constraint with incompatible remoteConvIds, this is a NO_RESULTS constraint now");
                    noResults = true;
                }
            } else {
                remoteConvId = other.remoteConvId;
            }
        }
        prohibitedRemoteConvIds = SetHelper.union(prohibitedRemoteConvIds, other.prohibitedRemoteConvIds);

        // itemId
        // these we have to intersect:
        //  Item{A or B or C} AND Item{B or C or D} --> Item{IN-BOTH}
        boolean prevNonempty = false;
        if (itemIds.size() > 0 || other.itemIds.size() > 0) {
            prevNonempty = true;
        }
        itemIds = SetHelper.intersectIfNonempty(itemIds, other.itemIds);
        if (itemIds.size() == 0 && prevNonempty) {
            noResults = true;
        }

        // these we can just union, since:
        // -Item{A or B} AND -Item{C or D} -->
        //   (-Item(A) AND -Item(B)) AND (-C AND -D) -->
        //     (A AND B AND C AND D)
        prohibitedItemIds = SetHelper.union(prohibitedItemIds, other.prohibitedItemIds);


        // remoteItemId
        // these we have to intersect:
        //   Item{A or B or C} AND Item{B or C or D} --> Item{IN-BOTH}
        prevNonempty = false;
        if (remoteItemIds.size() > 0 || other.remoteItemIds.size() > 0) {
            prevNonempty = true;
        }
        remoteItemIds = SetHelper.intersectIfNonempty(remoteItemIds, other.remoteItemIds);
        if (itemIds.size() == 0 && prevNonempty) {
            noResults = true;
        }

        // remote prohibitedItemId
        // these we can just union, since:
        // -Item{A or B} AND -Item{C or D} -->
        //   (-Item(A) AND -Item(B)) AND (-C AND -D) -->
        //     (A AND B AND C AND D)
        prohibitedRemoteItemIds = SetHelper.union(prohibitedRemoteItemIds, other.prohibitedRemoteItemIds);

        // indexId
        //   IndexId{A or B or C} AND IndexId{B or C or D} --> IndexId{IN-BOTH}
        // see comment above at folders
        if (indexIds.size() > 0 || other.indexIds.size() > 0) {
            indexIds = SetHelper.intersectIfNonempty(indexIds, other.indexIds);
            if (indexIds.isEmpty()) {
                noResults = true;
            }
        }

        // has indexId
        if (hasIndexId == null) {
            hasIndexId = other.hasIndexId;
        } else if (other.hasIndexId != null) {
            if (!hasIndexId.equals(other.hasIndexId)) {
                noResults = true;
                ZimbraLog.index.debug("Adding a HAS_NO_INDEXIDS constraint to a HAS_INDEXIDS one, this is a NO_RESULTS result");
                return;
            }
        }

        // types
        // see comments above
        if (!types.isEmpty() || !other.types.isEmpty()) {
            types = SetHelper.intersectIfNonempty(types, other.types);
            if (types.size() ==  0) {
                noResults = true;
            }
        }

        // see comments above
        excludeTypes = SetHelper.union(excludeTypes, other.excludeTypes);

        // dates
        if (other.dates != null) {
            if (dates == null) {
                dates = new ArrayList<NumericRange>();
            }
            dates.addAll(other.dates);
        }

        // calStartDates
        if (other.calStartDates != null) {
            if (calStartDates == null) {
                calStartDates = new ArrayList<NumericRange>();
            }
            calStartDates.addAll(other.calStartDates);
        }

        // calEndDates
        if (other.calEndDates != null) {
            if (calEndDates == null) {
                calEndDates = new ArrayList<NumericRange>();
            }
            calEndDates.addAll(other.calEndDates);
        }

        // modified
        if (other.modified != null) {
            if (modified == null) {
                modified = new ArrayList<NumericRange>();
            }
            modified.addAll(other.modified);
        }

        // modifiedContent
        if (other.modifiedContent != null) {
            if (modifiedContent == null) {
                modifiedContent = new ArrayList<NumericRange>();
            }
            modifiedContent.addAll(other.modifiedContent);
        }

        // sizes
        if (other.sizes != null) {
            if (sizes == null) {
                sizes = new ArrayList<NumericRange>();
            }
            sizes.addAll(other.sizes);
        }

        // conv-counts
        if (other.convCounts != null) {
            if (convCounts == null) {
                convCounts = new ArrayList<NumericRange>();
            }
            convCounts.addAll(other.convCounts);
        }

        // subjectRanges
        if (other.subjectRanges!= null) {
            if (subjectRanges== null) {
                subjectRanges = new ArrayList<StringRange>();
            }
            subjectRanges.addAll(other.subjectRanges);
        }

        // senderRanges
        if (other.senderRanges!= null) {
            if (senderRanges== null) {
                senderRanges = new ArrayList<StringRange>();
            }
            senderRanges.addAll(other.senderRanges);
        }

        if (other.fromContact != null) {
            if (fromContact != null) {
                if (fromContact != other.fromContact) {
                    noResults = true;
                }
            } else {
                fromContact = other.fromContact;
            }
        }
    }

    public static final class RemoteFolderDescriptor {
        public RemoteFolderDescriptor(ItemId iidFolder, String subpath, boolean includeSubfolders) {
            this.includeSubfolders = includeSubfolders;
            this.folderId = iidFolder;
            this.subfolderPath = subpath;
            if (this.subfolderPath == null)
                this.subfolderPath = "";
        }

        /** @return the folderId */
        public ItemId getFolderId() {
            return folderId;
        }
        /** @return the subfolderPath */
        public String getSubfolderPath() {
            return subfolderPath;
        }

        public boolean getIncludeSubfolders() {
            return includeSubfolders;
        }

        @Override
        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + ((folderId == null) ? 0 : folderId.hashCode());
            result = PRIME * result + ((subfolderPath == null) ? 0 : subfolderPath.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final RemoteFolderDescriptor other = (RemoteFolderDescriptor) obj;
            if (other.includeSubfolders != includeSubfolders)
                return false;
            if (folderId == null) {
                if (other.folderId != null)
                    return false;
            } else if (!folderId.equals(other.folderId))
                return false;
            if (subfolderPath == null) {
                if (other.subfolderPath != null)
                    return false;
            } else if (!subfolderPath.equals(other.subfolderPath))
                return false;
            return true;
        }

        private ItemId folderId;
        private String subfolderPath;
        private boolean includeSubfolders;
    }


    boolean automaticEmptySet() {
        // Check for tags and folders that are both included and excluded.
        Set<Integer> s = new HashSet<Integer>();
        SetHelper.addIdsToSet(s, tags);
        SetHelper.addIdsToSet(s, folders);

        // FIXME: Tim removed this assert temporarily while I'm reorganizing the DbQueryObject code...I know
        // about this and will fix it soon.
//      assert(!(setContainsAnyId(s, excludeTags) || setContainsAnyId(s, excludeFolders)));

        if (Boolean.FALSE.equals(hasTags) && tags != null && tags.size() != 0)
            return true;

        // lots more optimizing we could do here...
        if (!ListUtil.isEmpty(dates))
            for (NumericRange r : dates)
                if (r.lowest < -1 && r.negated)
                    return true;
                else if (r.highest < -1 && !r.negated)
                    return true;

        if (!ListUtil.isEmpty(modified))
            for (NumericRange r : modified)
                if (r.lowest < -1 && r.negated)
                    return true;
                else if (r.highest < -1 && !r.negated)
                    return true;

        return false;
    }

    void checkDates() {
        checkIntervals(dates);
        checkIntervals(calStartDates);
        checkIntervals(calEndDates);
        checkIntervals(modified);
        checkIntervals(modifiedContent);
        checkIntervals(convCounts);
    }

    void checkIntervals(Collection<? extends DbSearchConstraints.NumericRange> intervals) {
        if (!ListUtil.isEmpty(intervals)) {
            for (Iterator<? extends NumericRange> iter = intervals.iterator(); iter.hasNext(); ) {
                NumericRange r = iter.next();
                if (!r.isValid())
                    iter.remove();
            }
        }
    }
}
