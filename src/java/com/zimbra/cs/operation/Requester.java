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
package com.zimbra.cs.operation;

import com.zimbra.cs.operation.Scheduler.Priority;

/**
 * The Requester is used to denote what subsystem is calling a particular 
 * Operation.  Each Requester has a default Priority level, which can be used
 * or ignored by a particular Operation implementation.
 */
public enum Requester {
    ADMIN(Priority.ADMIN),
    SOAP(Priority.INTERACTIVE_HIGH),
    REST(Priority.INTERACTIVE_LOW),
    IMAP(Priority.BATCH),
    SYNC(Priority.BATCH),
    POP(Priority.BATCH);

    private Priority mDefaultPrio;

    private Requester(Priority defaultPrio) { mDefaultPrio = defaultPrio; }
    public Priority getPriority() { return mDefaultPrio; }
}