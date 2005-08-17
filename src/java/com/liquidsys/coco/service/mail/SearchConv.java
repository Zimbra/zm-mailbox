/*
 * Created on Nov 30, 2004
 */
package com.liquidsys.coco.service.mail;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.liquidsys.coco.index.LiquidHit;
import com.liquidsys.coco.index.LiquidQueryResults;
import com.liquidsys.coco.index.MailboxIndex;
import com.liquidsys.coco.index.MessageHit;
import com.liquidsys.coco.index.SearchParams;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.mailbox.Message;
import com.liquidsys.coco.mailbox.Mailbox.OperationContext;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.session.SoapSession;
import com.liquidsys.coco.stats.StopWatch;
import com.liquidsys.soap.LiquidContext;

/**
 * @author tim
 */
public class SearchConv extends Search {
    private static Log sLog = LogFactory.getLog(Search.class);

    private static StopWatch sWatch = StopWatch.getInstance("SearchConv"); 

    public Element handle(Element request, Map context) throws ServiceException {
        long startTime = sWatch.start();
        
        try {
            if (sLog.isDebugEnabled())
                sLog.debug("**Start SearchConv");

            LiquidContext lc = getLiquidContext(context);
            Mailbox mbox = getRequestedMailbox(lc);
            OperationContext octxt = lc.getOperationContext();

            SoapSession session = lc.getSession();
            SearchParams params = parseCommonParameters(request, lc);

            String cidStr = request.getAttribute(MailService.A_CONV_ID);
            int cid = 0;
            try {
                cid = Integer.parseInt(cidStr);
            } catch(NumberFormatException e) {}

            //
            // append (conv:(convid)) onto the beginning of the queryStr
            //
            StringBuffer queryBuffer = new StringBuffer("conv:\"");
            queryBuffer.append(cid);
            queryBuffer.append("\" (");
            queryBuffer.append(params.getQueryStr());
            queryBuffer.append(")");
            params.setQueryStr(queryBuffer.toString());
            
            // 
            // force to group-by-message
            // 
            params.setTypesStr(MailboxIndex.GROUP_BY_MESSAGE);
            
            LiquidQueryResults results = this.getResults(mbox, session, params);
            
            Element response = lc.createElement(MailService.SEARCH_CONV_RESPONSE);
            response.addAttribute(MailService.A_QUERY_OFFSET, Integer.toString(params.getOffset()));

            Message[] msgs = mbox.getMessagesByConversation(cid, MailboxIndex.getDbMailItemSortByte(params.getSortBy()));
            
            Element retVal = putHits(octxt, response, msgs, results, params);
            if (sLog.isDebugEnabled())
                sLog.debug("************ElementHandler Finished " + (System.currentTimeMillis() - startTime));

            return retVal;
        } finally {
            sWatch.stop(startTime);
        }
    }
    
    /**
     * NOTE - this version will only work for messages.  That's OK since we force GROUP_BY_MESSAGE here
     * 
     * @param response - soap container to put response data in
     * @param msgs - list of messages in this conversation
     * @param results - set of HITS for messages in this conv which match the search
     * @param offset - offset in conv to start at 
     * @param limit - number to return
     * @return
     * @throws ServiceException
     */
    Element putHits(OperationContext octxt, Element response, Message[] msgs, LiquidQueryResults results, SearchParams params)
    throws ServiceException {
        int offset = params.getOffset();
        int limit  = params.getLimit();
        EmailElementCache eecache = new EmailElementCache();

        if (sLog.isDebugEnabled()) {
            sLog.debug("SearchConv beginning with offset "+offset);
        }

        int iterLen = limit;
//        boolean hasMoreHits = false;

        if (msgs.length > iterLen+offset) {
//            hasMoreHits = true;
        } else {
            // iterLen+offset <= msgs.length
            iterLen = msgs.length - offset;
        }
         
        if (iterLen > 0) {

            //
            // Array of LiquidHit ptrs for matches, 1 entry for every message we might return from conv.
            // NULL means no LiquidHit presumably b/c the message didn't match the search
            //
            // ***Note that the match for msgs[i] is matched[i-offset]!!!!
            //
            LiquidHit matched[] = new LiquidHit[iterLen];
            for (int i = 0; i < matched.length; i++) {
                matched[i] = null;
            }
            
            //
            // Foreach hit, see if the hit message is in msgs[] (list of msgs in this conv), and if so 
            //
            HitIter: 
                for (LiquidHit curHit = results.getFirstHit(); curHit != null; curHit = results.getNext()) {
                    // we only bother checking the messages between offset and offset+iterLen, since only they
                    // are getting returned.
                    for (int i = offset; i < offset+iterLen; i++) {
                        if (curHit.getItemId() == msgs[i].getId()) {
                            matched[i-offset] = curHit;
                            continue HitIter; 
                        }
                    }
                }
            
            //
            // Okay, we've built the matched[] array.  Now iterate through all the messages, and put the message
            // or the MATCHED entry into the result
            //
            boolean inline = params.getFetchFirst();
            for (int i = offset; i < offset+iterLen; i++) {
                if (matched[i-offset] != null) {
                    addMessageHit(octxt, response, (MessageHit) matched[i-offset], eecache, inline, params);
                    inline = false;
                } else {
                    addMessageHit(response, msgs[i], eecache, params);
                }
            }
        }

        response.addAttribute(MailService.A_QUERY_MORE, results.hasNext());

        return response;
    }
    
}
