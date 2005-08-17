package com.zimbra.cs.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.Part;
import javax.mail.internet.ContentDisposition;
import javax.mail.internet.ParseException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ContactCSV;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.servlet.LiquidServlet;

/**
 * simple iCal servlet on a mailbox. URL is:
 * 
 *  http://server/service/ical/cal.ics[?...support-range-at-some-point...]
 *  
 *  need to support a range query at some point, right now get -30 thorugh +90 days from today
 *
 */

public class CsvServlet extends LiquidServlet {

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        try {
            AuthToken authToken = getAuthTokenFromCookie(req, resp);
            if (authToken == null) 
                return;

            Account account = Provisioning.getInstance().getAccountById(authToken.getAccountId());
            if (account == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "no such account");
                return;
            }
            
            if (!account.isCorrectHost()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "wrong server");
                return;
            }
        
            Mailbox mbox = Mailbox.getMailboxByAccount(account);
            List contacts = mbox.getContactList(-1);
            StringBuffer sb = new StringBuffer();
            if (contacts == null)
                contacts = new ArrayList();
            ContactCSV.toCSV(contacts, sb);

            ContentDisposition cd = new ContentDisposition(Part.ATTACHMENT);
            cd.setParameter("filename", "contacts.csv");
            resp.addHeader("Content-Disposition", cd.toString());
            resp.setContentType("text/plain");
            resp.getOutputStream().print(sb.toString());
        } catch (ServiceException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (ParseException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

}