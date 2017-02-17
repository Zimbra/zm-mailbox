/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.client;

public class LmcWiki extends LmcDocument {
	private String mWikiWord;
	private String mContents;
	
	public void setWikiWord(String w) { mWikiWord = w; setName(w); }
	public void setContents(String c) { mContents = c; }
	
	public String getWikiWord() { return mWikiWord; }
	public String getContents() { return mContents; }
	
	public String toString() {
		return "Wiki id=" + mId + " rev=" + mRev + " wikiword=" + mWikiWord +
		" folder=" + mFolder + " lastEditor=" + mLastEditedBy + 
		" lastModifiedDate=" + mLastModifiedDate;
	}
}
