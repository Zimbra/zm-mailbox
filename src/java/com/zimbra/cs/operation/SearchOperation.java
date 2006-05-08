/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.operation;

import java.io.IOException;
import com.zimbra.cs.index.queryparser.ParseException;

import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.Session;

public class SearchOperation extends Operation {
	SearchParams mParams;
	ZimbraQueryResults mResults;
	
	private static final int BASE_LOAD = 5;
	
	public SearchOperation(Session session, OperationContext oc, Mailbox mbox, Requester req, SearchParams params) throws ServiceException {
		super(session, oc, mbox, req, req.getPriority(), BASE_LOAD);
		mParams = params;
		
		schedule();
	}
	
	public String toString() {
		return super.toString()+" offset="+mParams.getOffset()+" limit="+mParams.getLimit();
	}
	
	protected void callback() throws ServiceException {
		try {
			byte[] types = MailboxIndex.parseGroupByString(mParams.getTypesStr());
			
			mResults = getMailbox().search(getOpCtxt(), mParams.getQueryStr(), types, mParams.getSortBy(), mParams.getLimit() + mParams.getOffset());
			
		} catch (IOException e) {
			throw ServiceException.FAILURE("IO error", e);
		} catch (ParseException e) {
			if (e.currentToken != null)
				throw MailServiceException.QUERY_PARSE_ERROR(mParams.getQueryStr(), e, e.currentToken.image, e.currentToken.beginLine, e.currentToken.beginColumn);
			else 
				throw MailServiceException.QUERY_PARSE_ERROR(mParams.getQueryStr(), e, "", -1, -1);
		}
	}

	public ZimbraQueryResults getResults() { return mResults; }
}
