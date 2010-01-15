/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2010 Zimbra, Inc.
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
 * Created on 2004. 7. 23.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.redolog.TransactionId;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class ControlOp extends RedoableOp {

	public ControlOp() {
	}

	public ControlOp(TransactionId txnId) {
		setTransactionId(txnId);
        setTimestamp(System.currentTimeMillis());
	}

	public void redo() throws Exception {
		// do nothing
	}

	public void commit() {
		// do nothing
	}

	public void abort() {
		// do nothing
	}

	protected String getPrintableData() {
		return null;
	}

	protected void serializeData(RedoLogOutput out) throws IOException {
		// nothing to do
	}

	protected void deserializeData(RedoLogInput in) throws IOException {
		// nothing to do
	}
}
