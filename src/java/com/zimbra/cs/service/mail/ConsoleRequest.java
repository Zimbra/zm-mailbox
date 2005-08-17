/*
 * Created on Feb 18, 2005
 */
package com.liquidsys.coco.service.mail;

import java.util.*;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.zimbra.soap.LiquidContext;
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
        LiquidContext lc = getLiquidContext(context);
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
    
    private String reindex(LiquidContext lc, Element response, 
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
    
    private String verifyIndex(LiquidContext lc, Element response, 
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
    
    
    public String executeCommand(LiquidContext lc, Element response, 
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
