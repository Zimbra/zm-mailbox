/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

	public boolean equals(Object obj) {
		TransactionId b = (TransactionId) obj;
		return b.mTime == mTime && b.mCounter == mCounter;
	}

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
