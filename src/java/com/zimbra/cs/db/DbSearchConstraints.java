package com.zimbra.cs.db;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.util.ListUtil;

/**
 * @author tim
 *
 * A class which encapsulates all of the constraints we can do on a mailbox search
 * 
 * "required" entries must be set, or you won't get what you want
 * 
 * "optional" entries are ignored if the default value is passed 
 */
public class DbSearchConstraints {
	public static class Range {
		public boolean negated = false;
		public long lowest = -1;
		public long highest = -1;

		public boolean equals(Object o) {
			DbSearchConstraints.Range other = (DbSearchConstraints.Range) o;
			return ((other.negated == negated) && (other.lowest == lowest) && (other.highest == highest));
		}

		boolean isValid()  { return lowest > 0 || highest > 0; }
	}

	//
	// these should all be moved OUT of DbSearchConstraints and passed as parameters 
	// to the DbMailItem.search() function!
	//
	public int mailboxId = 0;                        /* required */
	public byte sort;                                /* required */
	public int offset = -1;                 /* optional */
	public int limit = -1;                  /* optional */

	public Collection<Tag> tags = null;              /* optional */
	public Collection<Tag> excludeTags = null;       /* optional */
	public Boolean hasTags = null;                   /* optional */
	public Collection<Folder> folders = null;        /* optional */
	public Collection<Folder> excludeFolders = null; /* optional */
	public int convId = -1;                          /* optional */
	public Collection<Integer> prohibitedConvIds = null;          /* optional */
	public Collection<Integer> itemIds = null;                    /* optional */
	public Collection<Integer> prohibitedItemIds = null;          /* optional */
	public Collection<Integer> indexIds = null;                   /* optional */
	public Collection<Byte> types = null;                         /* optional */
	public Collection<Byte> excludeTypes = null;                  /* optional */
	public Collection<DbSearchConstraints.Range> dates = null;    /* optional */
	public Collection<DbSearchConstraints.Range> modified = null; /* optional */
	public Collection<DbSearchConstraints.Range> sizes = null;    /* optional */

	boolean automaticEmptySet() {
		// Check for tags and folders that are both included and excluded.
		Set<Integer> s = new HashSet<Integer>();
		addIdsToSet(s, tags);
		addIdsToSet(s, folders);
		assert(!(setContainsAnyId(s, excludeTags) || setContainsAnyId(s, excludeFolders)));
//		return true;
		if (hasTags == Boolean.FALSE && tags != null && tags.size() != 0)
			return true;

		// lots more optimizing we could do here...
		if (!ListUtil.isEmpty(dates))
			for (Range r : dates) 
				if (r.lowest < -1 && r.negated)
					return true;
				else if (r.highest < -1 && !r.negated)
					return true;
		
		if (!ListUtil.isEmpty(modified))
			for (Range r : modified)
				if (r.lowest < -1 && r.negated)
					return true;
				else if (r.highest < -1 && !r.negated)
					return true;
		
		return false;
	}

	void checkDates() {
		dates    = checkIntervals(dates);
		modified = checkIntervals(modified);
	}
	
	Collection<DbSearchConstraints.Range> checkIntervals(Collection<DbSearchConstraints.Range> intervals) {
		if (ListUtil.isEmpty(intervals))
			return intervals;
		
		for (Iterator<Range> iter = intervals.iterator(); iter.hasNext(); ) {
			Range r = iter.next();
			if (!r.isValid())
				iter.remove();
		}
		return intervals;
		
//		HashSet<DbSearchConstraints.Range> badDates = new HashSet<DbSearchConstraints.Range>();
//		for (DbSearchConstraints.Range range : intervals)
//			if (!range.isValid())
//				badDates.add(range);
//		
//		if (badDates.size() == 0)
//			return intervals;
//		else if (badDates.size() == intervals.size())
//			intervals = null;
//		else {
//			DbSearchConstraints.Range[] fixedDates = new DbSearchConstraints.Range[intervals.length - badDates.size()];
//			for (int i = 0, j = 0; i < intervals.length; i++)
//				if (!badDates.contains(intervals[i]))
//					fixedDates[j++] = intervals[i];
//			intervals = fixedDates;
//		}
//		return intervals;
	}
	
	private void addIdsToSet(Set<Integer> s, Collection<?> items)
	{
		if (items != null)
			for (Object obj : items)
				s.add(((MailItem)obj).getId());
	}
	
	private boolean setContainsAnyId(Set<Integer> s, Collection<?> items) {
		if (items != null)
			for (Object obj : items)
				if (s.contains(((MailItem)obj).getId()))
					return true;
		return false;
	}
}