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
package com.zimbra.cs.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.common.util.ListUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

/**
 * @author tim
 *
 * A class which encapsulates all of the constraints we can do on a mailbox search
 * 
 * "required" entries must be set, or you won't get what you want
 * 
 * "optional" entries are ignored if the default value is passed 
 */
public class DbSearchConstraints implements DbSearchConstraintsNode, Cloneable {

    public DbSearchConstraintsNode.NodeType getNodeType() { return DbSearchConstraintsNode.NodeType.LEAF; }
    public Iterable<DbSearchConstraintsNode> getSubNodes() { return null; }
    public DbSearchConstraints getSearchConstraints() { return this; }

    public static class StringRange {
        public boolean negated = false;
        public String lowest = null;  public boolean lowestEqual = false;
        public String highest = null;  public boolean highestEqual = false;
        
        public boolean equals(Object o) {
            DbSearchConstraints.StringRange other = (DbSearchConstraints.StringRange) o;
            return (
                        StringUtil.equal(other.lowest, lowest) && other.lowestEqual == lowestEqual &&
                        StringUtil.equal(other.highest, highest) && other.highestEqual == highestEqual &&
                        other.negated == negated
            );
        }
        
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
        public long lowest = -1;  public boolean lowestEqual = false;
        public long highest = -1;  public boolean highestEqual = false;
        
        public boolean equals(Object o) {
            DbSearchConstraints.NumericRange other = (DbSearchConstraints.NumericRange) o;
            return (
                        (other.negated == negated) && (other.lowest == lowest) && (other.highest == highest) &&
                        (other.lowestEqual == lowestEqual) && (other.highestEqual == highestEqual) 
            );
        }

        boolean isValid()  { return lowest > 0 || highest > 0; }
        
        public String toString() {
            StringBuilder retVal = new StringBuilder();

            if (negated) {
                retVal.append("NOT (");
            }
            
            // special-case ">=N AND <=N" to be "=N"
            if (lowest>-1 && lowestEqual && highestEqual && lowest == highest) {
                retVal.append(lowest);
            } else {
                if (lowest > -1) {
                    retVal.append(">");
                    if (lowestEqual)
                        retVal.append('=');
                    retVal.append(lowest);
                    retVal.append(' ');
                }
                
                if (highest > -1) {
                    retVal.append("<");
                    if (highestEqual)
                        retVal.append('=');
                    retVal.append(highest);
                    retVal.append(' ');
                }
            }
            
            if (negated) {
                retVal.append(")");
            }

            return retVal.toString();
        }
    }
    
    //
    // these should all be moved OUT of DbSearchConstraints and passed as parameters 
    // to the DbMailItem.search() function!
    //
    public Mailbox mailbox;                          /* required */
    public byte sort;                                /* required */
    public int offset = -1;                 /* optional */
    public int limit = -1;                  /* optional */

    //
    // When we COMBINE the operations during query optimization, we'll need 
    // to track some state values for example "no results" 
    //
    public boolean noResults = false;

    //
    // These are the main constraints 
    //
    public Set<Tag> tags = null; /* optional - SPECIAL CASE -- ALL listed tags must be present.  NULL IS DIFFERENT THAN EMPTY SET!!! */
    public Set<Tag> excludeTags = new HashSet<Tag>(); /* optional - ALL listed tags must be NOT present*/

    public Boolean hasTags = null;                   /* optional */

    public Set<Folder> folders = new HashSet<Folder>();        /* optional - ANY of these folders are OK */
    public Set<Folder> excludeFolders = new HashSet<Folder>(); /* optional - ALL listed folders not allowed */

    public Set<RemoteFolderDescriptor> remoteFolders = new HashSet<RemoteFolderDescriptor>();         /* optional - ANY of these folders are OK */
    public Set<RemoteFolderDescriptor> excludeRemoteFolders = new HashSet<RemoteFolderDescriptor>();  /* optional - ALL listed folders are not OK */
    
    public int convId = 0;                          /* optional - must match this convId  */
    public Set<Integer> prohibitedConvIds = new HashSet<Integer>();          /* optional - ALL listed convs not allowed*/
    
    public ItemId remoteConvId = null;  /* optional */
    public Set<ItemId>  prohibitedRemoteConvIds = new HashSet<ItemId>(); /* optional - ALL listed convs not allowed */

    public Set<Integer> itemIds = new HashSet<Integer>();                             /* optional - ANY of these itemIDs are OK.*/
    public Set<Integer> prohibitedItemIds = new HashSet<Integer>(); /* optional - ALL of these itemIDs are excluded*/
    
    public Set<ItemId> remoteItemIds = new HashSet<ItemId>();                             /* optional - ANY of these itemIDs are OK.*/
    public Set<ItemId> prohibitedRemoteItemIds = new HashSet<ItemId>(); /* optional - ALL of these itemIDs are excluded*/

    public Set<Integer> indexIds = new HashSet<Integer>();                   /* optional - ANY of these indexIDs are OK.  */

    public Set<Byte> types = new HashSet<Byte>();                         /* optional - ANY of these types are OK.  */
    public Set<Byte> excludeTypes = new HashSet<Byte>();                  /* optional - ALL of these types are excluded */

    //
    // A possible result must match *ALL* the range constraints below.  It might seem strange that we don't 
    // just combine the ranges -- yes this would be easy for positive ranges (foo>5 AND foo >7 and foo<10) 
    // but it quickly gets very complicated with negative ranges such as (3<foo<100 AND NOT 7<foo<20)
    //
    public Collection<NumericRange> dates = new ArrayList<NumericRange>();    /* optional */
    public Collection<NumericRange> calStartDates = new ArrayList<NumericRange>();    /* optional */
    public Collection<NumericRange> calEndDates = new ArrayList<NumericRange>();    /* optional */
    public Collection<NumericRange> modified = new ArrayList<NumericRange>(); /* optional */
    public Collection<NumericRange> sizes = new ArrayList<NumericRange>();    /* optional */
    public Collection<NumericRange> convCounts = new ArrayList<NumericRange>(); /* optional */
    public Collection<StringRange> subjectRanges = new ArrayList<StringRange>(); /* optional */
    public Collection<StringRange> senderRanges = new ArrayList<StringRange>(); /* optional */
    
    private Set<Byte> calcTypes() {
        if (excludeTypes.size() == 0)
            return types;
        
        if (types.size() == 0)
            for (byte i = 1; i < MailItem.TYPE_MAX; i++)
                types.add(i);
        
        for (Byte t : excludeTypes) {
            types.remove(t);
        }
        excludeTypes = new HashSet<Byte>();
        return types;
    }

    /**
     * @return TRUE if these constraints have a non-appointment type in them
     */
    boolean hasNonAppointmentTypes() {
        Set<Byte> fullTypes = calcTypes();
        
        HashSet<Byte> temp = new HashSet<Byte>();
        temp.addAll(fullTypes);
        temp.remove(MailItem.TYPE_APPOINTMENT);
        temp.remove(MailItem.TYPE_TASK);
        return !temp.isEmpty();
    }
    
    /**
     * @return TRUE if these constraints have a constraint that requires a join with the Appointment table
     */
    boolean hasAppointmentTableConstraints() {
        Set<Byte> fullTypes = calcTypes();
        
// testing hack:        
//        if (fullTypes.contains(MailItem.TYPE_APPOINTMENT)) {
//            NumericRange range = new NumericRange();
//            range.lowest = 110;
//            range.highest = Integer.MAX_VALUE;
//            calStartDates.add(range);
//        }
        
        return ((calStartDates.size() > 0 || calEndDates.size() > 0) &&
                    (fullTypes.contains(MailItem.TYPE_APPOINTMENT) || fullTypes.contains(MailItem.TYPE_TASK)));
    }

    /**
     * Returns true if query is for a single folder, item type list
     * includes <code>MaliItem.TYPE_MESSAGE</code>, and no other conditions
     * are specified.
     * @return
     */
    boolean isSimpleSingleFolderMessageQuery() {
        boolean typeListIncludesMessage = false;
        if (types.size() > 0) {
            for (Byte type : types) {
                if (type == MailItem.TYPE_MESSAGE) {
                    typeListIncludesMessage = true;
                    break;
                }
            }
        }
        if (typeListIncludesMessage && excludeTypes.size() > 0) {
            for (Byte type : excludeTypes) {
                if (type == MailItem.TYPE_MESSAGE) {
                    typeListIncludesMessage = false;
                    break;
                }
            }
        }
        return
            folders.size() == 1 && excludeFolders.isEmpty() &&
            typeListIncludesMessage &&
            
// bug: 23985
// tagConstraints is not initialized when this check is made, don't count on it
// instead use the tags/flags from the query itself            
//            (tagConstraints == null ||
//             (tagConstraints.searchFlagsets == null &&
//              tagConstraints.searchTagsets == null &&
//              tagConstraints.unread == null)) &&
            hasTags == null &&
            (excludeTags == null || excludeTags.isEmpty()) &&
            (tags == null || tags.isEmpty()) &&
            
            convId == 0 && prohibitedConvIds.isEmpty() &&
            itemIds.isEmpty() && prohibitedItemIds.isEmpty() &&
            indexIds.isEmpty() &&
            dates.isEmpty() &&
            calStartDates.isEmpty() &&
            calEndDates.isEmpty() &&
            modified.isEmpty() &&
            sizes.isEmpty() &&
            convCounts.isEmpty() &&
            subjectRanges.isEmpty() &&
            senderRanges.isEmpty();
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
    
    public Object clone() throws CloneNotSupportedException {
        DbSearchConstraints toRet = (DbSearchConstraints)super.clone();
        
        toRet.tags = SetHelper.cloneHashSet(tags);
        toRet.excludeTags = SetHelper.cloneHashSet(excludeTags);

        toRet.folders = SetHelper.cloneHashSet(folders);
        toRet.excludeFolders = SetHelper.cloneHashSet(excludeFolders);

        toRet.remoteFolders = SetHelper.cloneHashSet(remoteFolders);
        toRet.excludeRemoteFolders = SetHelper.cloneHashSet(excludeRemoteFolders);

        toRet.convId = convId;
        toRet.prohibitedConvIds = SetHelper.cloneHashSet(prohibitedConvIds);

        toRet.remoteConvId = remoteConvId;
        toRet.prohibitedRemoteConvIds = SetHelper.cloneHashSet(prohibitedRemoteConvIds);

        toRet.itemIds = SetHelper.cloneHashSet(itemIds);
        toRet.prohibitedItemIds = SetHelper.cloneHashSet(prohibitedItemIds);

        toRet.remoteItemIds = SetHelper.cloneHashSet(remoteItemIds);
        toRet.prohibitedRemoteItemIds = SetHelper.cloneHashSet(prohibitedRemoteItemIds);

        toRet.indexIds = SetHelper.cloneHashSet(indexIds);

        toRet.types = SetHelper.cloneHashSet(types);
        toRet.excludeTypes = SetHelper.cloneHashSet(excludeTypes);

        toRet.dates = SetHelper.cloneHashSet(dates);
        toRet.calStartDates = SetHelper.cloneHashSet(calStartDates);
        toRet.calEndDates = SetHelper.cloneHashSet(calEndDates);

        toRet.modified = SetHelper.cloneHashSet(modified);
        toRet.sizes = SetHelper.cloneHashSet(sizes);
        toRet.convCounts = SetHelper.cloneHashSet(convCounts);
        toRet.subjectRanges = SetHelper.cloneHashSet(subjectRanges);
        toRet.senderRanges = SetHelper.cloneHashSet(senderRanges);
        
        return toRet;
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
                str.append(")");
            }
        }

        abstract void printOne(StringBuilder s, T t);
    }
    
    private static class FolderPrinter {
        void run(StringBuilder str, boolean truthiness, Collection<Folder> collect) {
            if (!ListUtil.isEmpty(collect)) {
                str.append("(");
                boolean atFirst = true;
                for (Folder f: collect) {
                    if (!atFirst) 
                        str.append(" ");
                    
                    if (!truthiness)
                        str.append("-");

                    if (f instanceof Mountpoint) {
                        str.append("INID:");
                        Mountpoint mpt = (Mountpoint)f;
                        str.append(mpt.getRemoteId());
                    } else {
                        str.append("IN:").append(f.getName());
                    }
                    atFirst = false;
                }
                str.append(")");
            }
        }
    }

    private static class ItemIdPrinter {
        void run(StringBuilder str, boolean truthiness, Collection<ItemId> collect, String intro) {
            if (!ListUtil.isEmpty(collect)) {
                str.append("(");
                boolean atFirst = true;
                for (ItemId iid: collect) {
                    if (!atFirst) 
                        str.append(" ");

                    if (!truthiness)
                        str.append("-");
                    str.append(intro).append(":\"").append(iid.toString()).append("\"");
                    atFirst = false;
                }
                str.append(") ");
            }
        }
    }
    
    private static class RemoteFolderPrinter {
        void run(StringBuilder str, boolean truthiness, Collection<RemoteFolderDescriptor> collect) {
            if (!ListUtil.isEmpty(collect)) {
                str.append("(");
                boolean atFirst = true;
                for (RemoteFolderDescriptor rf: collect) {
                    if (!atFirst) 
                        str.append(" ");

                    if (!truthiness)
                        str.append("-");
                    
                    String intro = rf.getIncludeSubfolders() ? "UNDERID" : "INID";
                    
                    str.append(intro).append(":\"").append(rf.getFolderId().toString());
                    if (rf.getSubfolderPath() != null && rf.getSubfolderPath().length() > 0) 
                        str.append('/').append(rf.getSubfolderPath());
                    str.append("\"");
                    atFirst = false;
                }
                str.append(") ");
            }
        }
    }
    
    private static class ObjectPrinter<T extends Object> extends Printer<T> {
        void printOne(StringBuilder s, T t) {
            s.append(t.toString());
        }
    }
    
    public String toString()
    {
        StringBuilder retVal = new StringBuilder("");
        
        if (noResults) {
            retVal.append("-is:anywhere ");
        }

        //
        // all this pain could have been eliminated with a simple preprocessor macro...fucking java...
        //
        Printer<Tag> tp = new Printer<Tag>()         { void printOne(StringBuilder s, Tag t)     { s.append(t.getName()); } };
        Printer<Integer> ip = new Printer<Integer>() { void printOne(StringBuilder s, Integer i) { s.append(i); } };
        Printer<Byte> bp = new Printer<Byte>()       { void printOne(StringBuilder s, Byte b)    { s.append(b); } };
        
        FolderPrinter fp = new FolderPrinter();
        RemoteFolderPrinter rfp = new RemoteFolderPrinter();
        ItemIdPrinter iip = new ItemIdPrinter();

        // tags
        tp.run(retVal, tags, "TAG");
        tp.run(retVal, excludeTags, "-TAG");

        // hasTags?
        if (hasTags != null) {
            if (hasTags) {
                retVal.append("HAS_TAG ");
            } else {
                retVal.append("-HAS_TAG ");
            }
        }

        // folders
        fp.run(retVal, true, folders);
        fp.run(retVal, false, excludeFolders);

        // remote folders
        rfp.run(retVal, true, remoteFolders);
        rfp.run(retVal, false, excludeRemoteFolders);
        
        // convId
        if (convId != 0) {
            retVal.append("CONV:(").append(convId).append(") ");
        }
        ip.run(retVal, prohibitedConvIds, "-CONVID");
        
        // remoteConvId
        if (remoteConvId != null) {
            retVal.append("CONV:\"").append(remoteConvId).append("\" ");
        }
        iip.run(retVal, false, prohibitedRemoteConvIds, "CONV");

        // itemId
        ip.run(retVal, itemIds, "ITEM");
        ip.run(retVal, prohibitedItemIds, "-ITEM");
        
        // remoteItemId
        iip.run(retVal, true, remoteItemIds, "ITEM");
        iip.run(retVal, false, prohibitedRemoteItemIds, "ITEM");
        
        // indexId
        ip.run(retVal, indexIds, "INDEXID");

        // type
        bp.run(retVal, types, "TYPE"); 
        bp.run(retVal, excludeTypes, "-TYPE"); 

        if (!dates.isEmpty())
            new ObjectPrinter<NumericRange>().run(retVal, dates, "DATE");
        
        if (!calStartDates.isEmpty())
            new ObjectPrinter<NumericRange>().run(retVal, calStartDates, "APPT-START"); 
        if (!calEndDates.isEmpty())
            new ObjectPrinter<NumericRange>().run(retVal, calEndDates, "APPT-END"); 

        if (!modified.isEmpty())
            new ObjectPrinter<NumericRange>().run(retVal, modified, "MOD") ;
        
        if (!sizes.isEmpty()) 
            new ObjectPrinter<NumericRange>().run(retVal, sizes, "SIZE");
        
        if (!convCounts.isEmpty())
            new ObjectPrinter<NumericRange>().run(retVal, convCounts, "CONV-COUNT");

        if (!subjectRanges.isEmpty())
            new ObjectPrinter<StringRange>().run(retVal, subjectRanges, "SUBJECT");
        
        if (!senderRanges.isEmpty())
            new ObjectPrinter<StringRange>().run(retVal, senderRanges, "FROM");

        return retVal.toString();
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
        
        static void addIdsToSet(Set<Integer> s, Collection<?> items)
        {
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
            
            if (lhs.size() == 0)
                return clone(rhs);
            if (rhs.size() == 0)
                return lhs;
            

            if (rhs == null) {
                return lhs;
            } else if (lhs == null) {
                return clone(rhs);
            } else {
                Set<T> newSet = new HashSet<T>();
                for (T t : rhs) {
                    if (lhs.contains(t))
                        newSet.add(t);
                }
                return newSet;
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
     * 
     * @param other
     */
    public void andConstraints(DbSearchConstraints other) 
    {
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
        if (hasTags == null)
            hasTags = other.hasTags;
        else if (other.hasTags != null) {
            if (!hasTags.equals(other.hasTags)) {
                noResults = true;
                ZimbraLog.index.debug("Adding a HAS_NO_TAGS constraint to a HAS_TAGS one, this is a NO_RESULTS result");
                return;
            }
        }

        // folders
        //
        // these we have to intersect:
        //
        //   Folder{A or B or C} AND Folder{B or C or D} --> Folder{IN-BOTH}
        {
            // if both sets are empty going in, then an empty set means 
            // "no constraint"....on the other hand if either set is nonempty
            // going in, then an empty set coming out means "no results".
            // ugly.  Should modify this so folders=null means "no constraint" and
            // folders=[] means "no results".  TODO...
            if (folders.size() >  0 ||  other.folders.size() > 0) {
                folders = SetHelper.intersectIfNonempty(folders, other.folders);
                if (folders.size() == 0)
                    noResults = true;
            }
        }
        
        
        excludeFolders = SetHelper.union(excludeFolders, other.excludeFolders);
        
        
        //
        // remoteFolders
        //
        {
//            // if both sets are empty going in, then an empty set means 
//            // "no constraint"....on the other hand if either set is nonempty
//            // going in, then an empty set coming out means "no results".
//            // ugly.  Should modify this so folders=null means "no constraint" and
//            // folders=[] means "no results".  TODO...
//            if (remoteFolders.size() >  0 ||  other.remoteFolders.size() > 0) {
//                remoteFolders = SetHelper.intersectIfNonempty(remoteFolders, other.remoteFolders);
//                if (remoteFolders.size() == 0)
//                    noResults = true;
//            }
            
            // bug 20640
            // include-remote-folders searches cannot be properly intersected,
            // so don't try to intersect the sets right now -- just union (bug 
            remoteFolders = SetHelper.union(remoteFolders, other.remoteFolders);
            
        }
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
        {
            // these we have to intersect:
            //
            //   Item{A or B or C} AND Item{B or C or D} --> Item{IN-BOTH}
            boolean prevNonempty = false;
            if (itemIds.size() > 0 || other.itemIds.size() > 0)
                prevNonempty = true;
            itemIds = SetHelper.intersectIfNonempty(itemIds, other.itemIds);
            if (itemIds.size() == 0 && prevNonempty)
                noResults = true;
        }
        
        // these we can just union, since:
        //
        // -Item{A or B} AND -Item{C or D} --> 
        //   (-Item(A) AND -Item(B)) AND (-C AND -D) -->
        //     (A AND B AND C AND D)
        prohibitedItemIds = SetHelper.union(prohibitedItemIds, other.prohibitedItemIds);
        
        
        // remoteItemId
        {
            // these we have to intersect:
            //
            //   Item{A or B or C} AND Item{B or C or D} --> Item{IN-BOTH}
            boolean prevNonempty = false;
            if (remoteItemIds.size() > 0 || other.remoteItemIds.size() > 0)
                prevNonempty = true;
            remoteItemIds = SetHelper.intersectIfNonempty(remoteItemIds, other.remoteItemIds);
            if (itemIds.size() == 0 && prevNonempty)
                noResults = true;
        }
        
        // remote prohibitedItemId
        // these we can just union, since:
        //
        // -Item{A or B} AND -Item{C or D} --> 
        //   (-Item(A) AND -Item(B)) AND (-C AND -D) -->
        //     (A AND B AND C AND D)
        prohibitedRemoteItemIds = SetHelper.union(prohibitedRemoteItemIds, other.prohibitedRemoteItemIds);
        

        // indexId
        //   IndexId{A or B or C} AND IndexId{B or C or D} --> IndexId{IN-BOTH}
        {
            // see comment above at folders 
            if (indexIds.size() > 0 || other.indexIds.size() > 0) {
                indexIds = SetHelper.intersectIfNonempty(indexIds, other.indexIds);
                if (indexIds.size() == 0)
                    noResults = true;
            }
        }

        // types
        // see comments above
        {
            if (types.size() > 0 || other.types.size() > 0) {
                types = SetHelper.intersectIfNonempty(types, other.types);
                if (types.size() ==  0)
                    noResults = true;
            }
        }
            
        // see comments above
        excludeTypes = SetHelper.union(excludeTypes, other.excludeTypes);

        // dates
        if (other.dates != null) {
            if (dates == null) 
                dates = new ArrayList<NumericRange>();
            dates.addAll(other.dates);
        }
        
        // calStartDates
        if (other.calStartDates != null) {
            if (calStartDates == null) 
                calStartDates = new ArrayList<NumericRange>();
            calStartDates.addAll(other.calStartDates);
        }
        
        // calEndDates
        if (other.calEndDates != null) {
            if (calEndDates == null) 
                calEndDates = new ArrayList<NumericRange>();
            calEndDates.addAll(other.calEndDates);
        }
        
        // modified
        if (other.modified != null) {
            if (modified == null) 
                modified = new ArrayList<NumericRange>();
            modified.addAll(other.modified);
        }
        
        // sizes
        if (other.sizes!= null) {
            if (sizes == null) 
                sizes = new ArrayList<NumericRange>();
            sizes.addAll(other.sizes);
        }

        // conv-counts
        if (other.convCounts != null) {
            if (convCounts == null)
                convCounts = new ArrayList<NumericRange>();
            convCounts.addAll(other.convCounts);
        }

        // subjectRanges
        if (other.subjectRanges!= null) {
            if (subjectRanges== null) 
                subjectRanges = new ArrayList<StringRange>();
            subjectRanges.addAll(other.subjectRanges);
        }
        
        // senderRanges
        if (other.senderRanges!= null) {
            if (senderRanges== null) 
                senderRanges = new ArrayList<StringRange>();
            senderRanges.addAll(other.senderRanges);
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
        
        /* @see java.lang.Object#hashCode() */
        @Override
        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + ((folderId == null) ? 0 : folderId.hashCode());
            result = PRIME * result + ((subfolderPath == null) ? 0 : subfolderPath.hashCode());
            return result;
        }
        /* @see java.lang.Object#equals(java.lang.Object) */
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