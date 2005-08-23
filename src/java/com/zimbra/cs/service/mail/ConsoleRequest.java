/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

/*
 * Created on Feb 18, 2005
 */
package com.zimbra.cs.service.mail;

import java.util.*;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.ZimbraContext;
import com.zimbra.soap.WriteOpDocumentHandler;


/**
 * @author tim
 */
public class ConsoleRequest extends WriteOpDocumentHandler 
{
    public static class Param {
        public void addValue(String value) {
            values.add(value);
        }
        public List getValues() {
            return values;
        }
        public String getFirstValue() {
            if (values.size() > 0) {
                return (String)(values.get(0));
            } else {
                return "";
            }
        }
        private List /*String*/ values = new ArrayList();
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.soap.DocumentHandler#handle(org.dom4j.Element, java.util.Map)
     */
    public Element handle(Element request, Map context)
            throws ServiceException 
    {
        ZimbraContext lc = getZimbraContext(context);
        Element response = lc.createElement(MailService.CONSOLE_RESPONSE);
        
        String name = request.getAttribute(MailService.A_NAME);
        
        Map /*<String, Param>*/ params = new HashMap();
        for (Iterator iter = request.elementIterator(MailService.E_PARAM); iter.hasNext();) {
            Element cur = (Element) iter.next();
            String pName = cur.getAttribute(MailService.A_NAME);
            String pVal = cur.getText();
            Param param = (Param) params.get(pName);
            if (param == null) {
                param = new Param();
                params.put(pName, param);
            }
            param.addValue(pVal);
        }

        String status = executeCommand(lc, response, name, params);
        
        response.addAttribute(MailService.A_STATUS, status);
        return response;
    }
    
    private String reindex(ZimbraContext lc, Element response, 
            String command, Map /*Param*/ params) throws ServiceException 
    {
        Param pMbId = (Param)(params.get("id"));

        if (pMbId != null) {
            String str = pMbId.getFirstValue();
            if (str.equals("all")) {
                int ids[] = Mailbox.getMailboxIds();
                for (int i = 0; i < ids.length; i++) {
                    response.setText("Mailbox " + ids[i] + "\n");
                    Mailbox mbx = Mailbox.getMailboxById(ids[i]);
                    mbx.reIndex();
                }
            } else {
                Mailbox mbx = getMailboxFromParameter(str);
                mbx.reIndex();
            }
        }
        return "OK";
    }
    
    private String verifyIndex(ZimbraContext lc, Element response, 
            String command, Map /*Param*/ params) throws ServiceException 
    {
        Param pMbId = (Param)(params.get("id"));

        if (pMbId != null) {
            String str = pMbId.getFirstValue();
            if (str.equals("all")) {
                int ids[] = Mailbox.getMailboxIds();
                for (int i = 0; i < ids.length; i++) {
                    response.setText("Mailbox " + ids[i] + "\n");
                    Mailbox mbx = Mailbox.getMailboxById(ids[i]);
                    mbx.getMailboxIndex().chkIndex(false);
                }
            } else {
                Mailbox mbx = getMailboxFromParameter(str);
                mbx.getMailboxIndex().chkIndex(false);
            }
        }
        return "OK";
    }
    
    
    public String executeCommand(ZimbraContext lc, Element response, 
            String command, Map /*Param*/ params) throws ServiceException
    {
        try {
            if (command.equalsIgnoreCase("reIndex")) {
                return reindex(lc, response, command, params);
            } else if (command.equalsIgnoreCase("verifyIndex")) {
                return verifyIndex(lc, response, command, params);
            }

            
        } catch(ServiceException e) {
            return e.toString();
        }
        
        return "NO_SUCH_COMMAND";
    }
    
    private Mailbox getMailboxFromParameter(String paramStr) throws ServiceException {
        Mailbox mbx = null;
        if (paramStr.indexOf('@') >= 0) {
            // account
            Account acct = Provisioning.getInstance().getAccountByName(paramStr);
            mbx = Mailbox.getMailboxByAccountId(acct.getId());
        } else {
            int mailboxId = Integer.parseInt(paramStr);
            mbx = Mailbox.getMailboxById(mailboxId);
        }
        return mbx;
    }
}
