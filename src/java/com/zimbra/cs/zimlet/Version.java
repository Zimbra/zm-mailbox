/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.zimlet;

import java.util.ArrayList;

/**
 * Handles version strings such as "1.0", "2.5.1", "1.24.3"
 * 
 * The comparision of two versions are not the same as floating point
 * arithmetics.  Rather it is done by iteratively comparing each 
 * version nodes as whole numbers.
 * 
 * 1.0    < 1.1
 * 1.0    < 2.0
 * 1.0    < 1.0.1
 * 1.2    < 1.15  (because 2 < 15)
 * 1.31.5 < 1.135 (because 31 < 135)
 * 
 * @author jylee
 *
 */
public class Version implements Comparable<Version> {
	
	ArrayList<Integer> mTokens;
	String mVer;
	
	public Version(Version v) {
	    set(v);
	}
	
	public Version(String str) {
		parse(str);
	}
	
	public Version(int[] v) {
		int depth = v.length;
		mTokens = new ArrayList<Integer>();
		for (int i = 0; i < depth; i++) {
			mTokens.add(v[i]);
		}
	}
	
	public void parse(String str) {
		String[] tokens = str.split("\\.|_");
		int depth = tokens.length;
		mTokens = new ArrayList<Integer>();
		for (int i = 0; i < depth; i++) {
			mTokens.add(Integer.parseInt(tokens[i]));
		}
        mVer = str;
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof Version)) {
			return false;
		}
		Version v = (Version) obj;
		if (mTokens.size() != v.mTokens.size()) {
			return false;
		}
		return (compareTo(v) == 0);
	}
	
	public int compareTo(Version v) {
		int d = 0;
		int myDepth = mTokens.size();
		int yourDepth = v.mTokens.size();
		while (true) {
			if (myDepth == d || yourDepth == d) {
				return (myDepth - yourDepth);
			}

			Integer mine = mTokens.get(d);
			Integer yours = v.mTokens.get(d);
			if (!mine.equals(yours)) {
				return mine - yours;
			}
			d++;
		}
	}
	
	public String toString() {
	    if (mVer != null)
	        return mVer;
		StringBuffer buf = new StringBuffer("");
		for (int i = 0; i < mTokens.size(); i++) {
			if (buf.length() > 0) buf.append(".");
			buf.append(mTokens.get(i));
		}
		return buf.toString();
	}
	
	public void set(Version v) {
		this.mTokens = (ArrayList<Integer>)v.mTokens.clone();
		this.mVer = v.mVer;
	}

	/*
	 * 1.5 -> 1.4
	 */
	public void decrement() {
	    mVer = null;
		int lastToken = mTokens.size() - 1;
		if (lastToken >= 0) {
			int tok = mTokens.remove(lastToken);
			tok--;
			mTokens.add(lastToken, tok);
		}
	}

	/*
	 * 1.5 -> 1.6
	 */
	public void increment() {
        mVer = null;
		int lastToken = mTokens.size() - 1;
		if (lastToken >= 0) {
			int tok = mTokens.remove(lastToken);
			tok++;
			mTokens.add(lastToken, tok);
		}
	}

	/*
	 * 1.5 -> 1.5.1
	 */
	public void incrementMinor() {
        mVer = null;
		mTokens.add(1);
	}
	
	/*
	 * Increments this version while maintaining the constraint this.compareTo(before) < 0
	 * 
	 * current		before		incremented to
	 * 1.5			1.8			1.6
	 * 1.5			1.6			1.5.1
	 * 1.5			1.5.1		1.5.0.1
	 */
	public void increment(Version before) {
		if (compareTo(before) >= 0) {
			throw new IllegalArgumentException(toString() + " >= " + before.toString());
		}
		Version v = new Version(this);
		v.increment();
		if (v.compareTo(before) > 0) {
			v.set(before);
			v.decrement();
			v.incrementMinor();
			this.set(v);
		} else if (v.equals(before)) {
			this.incrementMinor();
		} else {
			this.set(v);
		}
	}
	
	public Version createNext() {
		Version instance = new Version(this);
		instance.increment();
		return instance;
	}

	public Version createNext(Version before) {
		Version instance = new Version(this);
		instance.increment(before);
		return instance;
	}
}
