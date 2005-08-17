package com.liquidsys.coco.service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.mail.internet.MailDateFormat;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.index.LiquidHit;
import com.liquidsys.coco.index.LiquidQueryResults;
import com.liquidsys.coco.index.MailboxIndex;
import com.liquidsys.coco.index.MessageHit;
import com.liquidsys.coco.index.queryparser.ParseException;
import com.liquidsys.coco.mailbox.MailItem;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.mailbox.Message;

/**
 * simple RSS feed servlet of a mailbox. URL is:
 * 
 *  http://server/service/rss/index.rss[?query={search-query}]
 *  
 *  default query is "is:unread"
 */

public class RssServlet extends LiquidBasicAuthServlet {

    private static final String  WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
    private static final String  WWW_AUTHENTICATE_VALUE = "BASIC realm=\"LiquidSystems New Mail Feed\"";
    
    private static final String PARAM_QUERY = "query";

    private static Log mLog = LogFactory.getLog(RssServlet.class);

    public void doAuthGet(HttpServletRequest req, HttpServletResponse resp, Account acct, Mailbox mailbox)
    throws ServiceException, IOException
    {
        //resp.setContentType("text/xml");
        resp.setContentType("application/rss+xml");
            
        StringBuffer sb = new StringBuffer();

        sb.append("<?xml version=\"1.0\"?>");
            
        Element.XMLElement rss = new Element.XMLElement("rss");
        rss.addAttribute("version", "2.0");

        Element channel = rss.addElement("channel");
        channel.addElement("title").setText("Liquid Mail: "+acct.getName());
            
        channel.addElement("generator").setText("Liquid Systems RSS Feed Servlet");
            
        LiquidQueryResults results;
        try {
            String query = req.getParameter(PARAM_QUERY);
            if (query == null) query = "is:unread in:inbox";

            channel.addElement("description").setText(query);
                
            results = mailbox.search(query, new byte[] { MailItem.TYPE_MESSAGE }, MailboxIndex.SEARCH_ORDER_DATE_DESC);
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
            MailDateFormat mdf = new MailDateFormat();                
            while (results.hasNext()) {
                LiquidHit hit = results.getNext();
                if (hit instanceof MessageHit) {
                    MessageHit mh = (MessageHit) hit;
                    Message m = mh.getMessage();
                    Element item = channel.addElement("item");
                    item.addElement("title").setText(m.getSubject());
                    item.addElement("description").setText(m.getFragment());
                    item.addElement("author").setText(m.getSender());
                    item.addElement("pubDate").setText(sdf.format(new Date(m.getDate())));
                    /* TODO: guid, links, etc */
                    /*
                     Element guid = item.addElement("guid");
                     guid.setText(acct.getName()+m.getId());
                     guid.addAttribute("isPermaLink", "false");
                     */
                }
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        } catch (ParseException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        }
        sb.append(rss.toString());
        resp.getOutputStream().write(sb.toString().getBytes());
    }
}
