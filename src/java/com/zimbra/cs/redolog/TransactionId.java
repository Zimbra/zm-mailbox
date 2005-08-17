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
