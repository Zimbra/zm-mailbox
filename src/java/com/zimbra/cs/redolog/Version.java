/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

/*
 * Created on 2004. 8. 4.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.redolog;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author jhahm
 *
 * Versioning of redo log serialization
 */
public class Version {

	private short mMajorVer;
	private short mMinorVer;

	public Version(short major, short minor) {
		mMajorVer = major;
		mMinorVer = minor;
	}

	public Version() {
		mMajorVer = 0;
		mMinorVer = 0;
	}

	public int compareTo(Version b) {
		if (mMajorVer == b.mMajorVer) {
			if (mMinorVer == b.mMinorVer)
				return 0;
			else if (mMinorVer < b.mMinorVer)
				return -1;
			else
				return 1;
		} else if (mMajorVer < b.mMajorVer)
			return -1;
		else // mMajorVer > b.mMajorVer
			return 1;
	}

	public String toString() {
		return Integer.toString(mMajorVer) + "." + Integer.toString(mMinorVer);
	}

	public void serialize(DataOutput out) throws IOException {
		out.writeShort(mMajorVer);
		out.writeShort(mMinorVer);
	}

	public void deserialize(DataInput in) throws IOException {
		mMajorVer = in.readShort();
		mMinorVer = in.readShort();
		if (mMajorVer < 0 || mMinorVer < 0)
			throw new IOException("Negative version number: major=" + mMajorVer + ", minor=" + mMinorVer);
	}

	public boolean equals(Object obj) {
		Version b = (Version) obj;
		return b.mMajorVer == mMajorVer && b.mMinorVer == mMinorVer;
	}
}
