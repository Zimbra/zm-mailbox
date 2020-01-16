package com.zimbra.cs.service.account;

import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.soap.ZimbraSoapContext;

public class GetVCMeetingInfo extends AccountDocumentHandler {

	private static Hashtable<String, String> env = new Hashtable<>();
	
	@Override
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        
        String emailAddr = request.getAttribute(MailConstants.A_EMAIL, null);
        
        String uid = fetchUIDFromSunLdap(emailAddr);
		
        getVCMeetingInfo(uid);
        
		return null;
	}

	private void getVCMeetingInfo(String uid) {
		// TODO Auto-generated method stub
		
	}

	private String fetchUIDFromSunLdap(String email) throws ServiceException {
		
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        if (LC.sun_ssl_enable.booleanValue()) {
        	env.put(Context.PROVIDER_URL, "ldaps://" + LC.sun_host.value() + ":" + LC.sun_port.intValue());
        	env.put(Context.SECURITY_AUTHENTICATION, "ssl");
        } else {
        	env.put(Context.PROVIDER_URL, "ldap://" + LC.sun_host.value() + ":" + LC.sun_port.intValue());
        }
        env.put(Context.SECURITY_PRINCIPAL, LC.sun_binddn.value());
        env.put(Context.SECURITY_CREDENTIALS, LC.sun_bindpass.value());
		
        String uid = "";
        DirContext ctx = null;
        
		String[] attr = {"uid"};
		SearchControls sc = new SearchControls();
		sc.setReturningAttributes(attr);
		sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
		String searchFilter = "";
		searchFilter = "(|(mail=" + email + "))";
		try {
			ctx = new InitialDirContext(env);
			NamingEnumeration<SearchResult> enumerator = ctx.search(LC.sun_directory_base.value(), searchFilter, sc);
			while (enumerator.hasMoreElements()) {
				SearchResult searchResult = (SearchResult) enumerator.nextElement();
				uid = (String) searchResult.getAttributes().get("uid").get();
				if (uid != null && uid.length() > 0) {
					break;
				}
			}
			ctx.close();
		} catch (NamingException e) {
			ZimbraLog.mailbox.error("Unable to fetch data from sun ldap", e);
			throw ServiceException.INVALID_REQUEST("Sun attributes are not set or not correct", e);
		}
		return uid;
	}
}
