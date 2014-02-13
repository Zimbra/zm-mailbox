/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.servlet.continuation;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationListener;

import com.zimbra.common.util.ZimbraLog;

/**
 * ContinuationListener implementation to handle internal details of when and when not to attempt resume
 * Application code which implements timeout + explicit resume should do so via this class
 *
 */
public class ResumeContinuationListener implements ContinuationListener {

    private Continuation continuation;
    private AtomicBoolean readyToResume;

    public ResumeContinuationListener(Continuation continuation) {
        this.continuation = continuation;
        this.readyToResume = new AtomicBoolean(true);
        continuation.addContinuationListener(this);
    }


    @Override
    public void onComplete(Continuation continuation) {
        readyToResume.set(false);
    }

    @Override
    public void onTimeout(Continuation continuation) {
        readyToResume.set(false);
    }

    /**
     * Attempt to resume continuation if it is currently suspended.
     */
    public void resumeIfSuspended() {
        if (readyToResume.compareAndSet(true, false)) {
            try {
                continuation.resume();
            } catch (IllegalStateException ise) {
                if (!(continuation.isExpired() || continuation.isResumed())) {
                    //narrow race here; timeout could occur just after compareAndSet
                    //not a problem as long as it is expired or resumed
                    throw ise;
                } else {
                    ZimbraLog.misc.debug("ignoring IllegalStateException during resume; already resumed/expired", ise);
                }
            }
        }
    }

    public Continuation getContinuation() {
        return continuation;
    }

}
