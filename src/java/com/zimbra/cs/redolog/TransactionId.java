/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on 2004. 7. 21.
 */
package com.zimbra.cs.redolog;

import java.io.IOException;

import com.zimbra.common.service.ServiceException;

/**
 * @author jhahm
 *
 * A transaction ID is a long value whose high 32 bits is a time
 * component and low 32 bits is a sequence component.
 */
public class TransactionId {

	private int mTime;
	private int mCounter;

	public TransactionId(int time, int counter) {
		mTime = time;
		mCounter = counter;
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

	@Override
    public String toString() {
		return Integer.toString(mTime) + "." + Integer.toString(mCounter);
	}

	public void serialize(RedoLogOutput out) throws IOException {
		out.writeInt(mTime);
		out.writeInt(mCounter);
	}

	public void deserialize(RedoLogInput in) throws IOException {
		mTime = in.readInt();
		mCounter = in.readInt();
	}

	@Override
    public boolean equals(Object obj) {
		TransactionId b = (TransactionId) obj;
		return b != null && b.mTime == mTime && b.mCounter == mCounter;
	}

	@Override
    public int hashCode() {
		return mCounter;
	}

    public int getTime() {
        return mTime;
    }

    public int getCounter() {
        return mCounter;
    }

    public String encodeToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(mTime).append('-').append(mCounter);
        return sb.toString();
    }

    public static TransactionId decodeFromString(String str)
    throws ServiceException {
        Throwable cause = null;
        if (str != null) {
            String[] fields = str.split("-", 2);
            if (fields != null && fields.length == 2) {
                try {
                    int time = Integer.parseInt(fields[0]);
                    int counter = Integer.parseInt(fields[1]);
                    return new TransactionId(time, counter);
                } catch (NumberFormatException e) {
                    cause = e;
                }
            }
        }
        throw ServiceException.PARSE_ERROR("Invalid TransactionId " + str, cause);
    }
}
