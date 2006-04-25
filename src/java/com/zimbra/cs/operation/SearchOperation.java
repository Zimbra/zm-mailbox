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
		
		run();
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
