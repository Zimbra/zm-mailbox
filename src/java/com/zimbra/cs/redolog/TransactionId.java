/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2004. 7. 21.
 */
package com.zimbra.cs.redolog;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author jhahm
 *
 * A transaction ID is a long value whose high 32 bits is a time
 * component and low 32 bits is a sequence component.
 */
public class TransactionId {

	private int mTime;
	private int mCounter;

	public TransactionId(int time, int count) {
		mTime = time;
		mCounter = count;
	}

	public TransactionId() {
		mTime = 0;
		mCounter = 0;
	}

    /**
     * Compares this transaction ID against another one to see which
     * transaction occurred earlier.
     * @param b transaction ID being compared against
     * @return negative number if this transaction is earlier than b;
     *         0 if this transaction ID and b are the same;
     *         positive number if this transaction is later than b
     */
	public int compareTo(TransactionId b) {
		if (mTime == b.mTime) {
			if (mCounter < b.mCounter)
				return -1;
			else if (mCounter > b.mCounter)
				return 1;
			return 0;
		} else if (mTime < b.mTime)
			return -1;
		else // mTime > b.mTime
			return 1;
	}

	public String toString() {
		return Integer.toString(mTime) + "." + Integer.toString(mCounter);
	}

	public void serialize(DataOutput out) throws IOException {
		out.writeInt(mTime);
		out.writeInt(mCounter);
	}

	public void deserialize(DataInput in) throws IOException {
		mTime = in.readInt();
		mCounter = in.readInt();
	}

	public boolean equals(Object obj) {
		TransactionId b = (TransactionId) obj;
		return b.mTime == mTime && b.mCounter == mCounter;
	}

	public int hashCode() {
		return mCounter;
	}
}
