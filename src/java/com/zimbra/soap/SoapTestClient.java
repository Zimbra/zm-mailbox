/*
 * Created on May 26, 2004
 */
package com.zimbra.soap;

import java.io.IOException;

import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.cs.servlet.ZimbraServlet;
import com.zimbra.cs.util.Liquid;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.SoapHttpTransport;

/**
 * @author schemers
 */
public class SoapTestClient {

    public static void main(String args[]) 
    {
        Liquid.toolSetup();
    	
        SoapHttpTransport trans = null;
        try {
            trans = new SoapHttpTransport("http://localhost:7070" + ZimbraServlet.USER_SERVICE_URI);
            Element request = Element.XMLElement.mFactory.createElement(AccountService.AUTH_REQUEST);
            request.addAttribute(AccountService.E_ACCOUNT, "user1@liquidsys.com", Element.DISP_CONTENT);
            request.addAttribute(AccountService.E_PASSWORD, "test123", Element.DISP_CONTENT);
            Element response = trans.invoke(request);

            System.out.println(response.prettyPrint());

            // get the auth token out, no default, must be present or a service exception is thrown
            String authToken = response.getAttribute(AccountService.E_AUTH_TOKEN);
            // get the session id, if not present, default to null
            String sessionId = response.getAttribute(ZimbraContext.E_SESSION_ID, null);

            // set the auth token and session id in the transport for future requests to use
            trans.setAuthToken(authToken);
            if (sessionId != null)
                trans.setSessionId(sessionId);
            
            Element tagsRequest = Element.XMLElement.mFactory.createElement(MailService.GET_TAG_REQUEST);
            Element tagsResponse = trans.invoke(tagsRequest);

            System.out.println(tagsResponse.prettyPrint());
            
        } catch (SoapFaultException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (trans != null)
                trans.shutdown();   
        }
		
    }
}
