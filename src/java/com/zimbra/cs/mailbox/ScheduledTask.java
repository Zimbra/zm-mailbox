/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.mailbox;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public abstract class ScheduledTask<V>
implements Callable<V> {

    private long mMailboxId;
    private Map<String, String> mProperties = new HashMap<String, String>();
    private long mIntervalMillis;
    private Date mExecTime;

    /**
     * Returns the task name.
     */
    abstract public String getName();
    
    /**
     * Returns the mailbox id, or <tt>0</tt> if this task is not
     * specific to a mailbox.
     */
    public long getMailboxId() { return mMailboxId; }
    
    public void setMailboxId(long mailboxId) {
        mMailboxId = mailboxId;
    }
    
    /**
     * Returns the timestamp at which this task will execute next.
     */
    public Date getExecTime() { return mExecTime; }
    
    public void setExecTime(Date execTime) {
        mExecTime = execTime;
    }
    
    /**
     * Returns the recurrence interval, or <tt>0</tt> if this is not
     * a recurring task.
     */
    public long getIntervalMillis() { return mIntervalMillis; }

    public void setIntervalMillis(long intervalMillis) {
        mIntervalMillis = intervalMillis;
    }
    
    /**
     * Returns <tt>true</tt> if this is a recurring task.
     */
    public boolean isRecurring() { return (mIntervalMillis > 0); }
    
    /**
     * Returns the value of a property associated with this task.
     */
    public String getProperty(String key) {
        return mProperties.get(key);
    }

    /**
     * Sets the value of a property associated with this task.
     */
    public void setProperty(String key, String value) {
        mProperties.put(key, value);
    }
    
    /**
     * Returns the names of all properties set for this <tt>ScheduledTask</tt>.
     */
    public Set<String> getPropertyNames() {
        return mProperties.keySet();
    }
    
    public String toString() {
        return String.format("%s: { name=%s, mailboxId=%d, execTime=%s, intervalMillis=%d }",
            this.getClass().getSimpleName(), getName(), mMailboxId, mExecTime, mIntervalMillis);
    }
}
