package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.soap.ZimbraSoapContext;

import  javax.naming.*;
import  javax.naming.directory.*;

import  java.util.Hashtable;

public class CheckDomainMXRecord extends AdminDocumentHandler {

    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    @Override
	public Element handle(Element request, Map<String, Object> context)
			throws ServiceException {

		ZimbraSoapContext zsc = getZimbraSoapContext(context);
	    Provisioning prov = Provisioning.getInstance();     
		Element d = request.getElement(AdminConstants.E_DOMAIN);
	    String key = d.getAttribute(AdminConstants.A_BY);
        String value = d.getText();
	    
	    Domain domain = prov.get(DomainBy.fromString(key), value);	
	    if (isDomainAdminOnly(zsc) && !canAccessDomain(zsc, domain))
	    	throw ServiceException.PERM_DENIED("can not access domain");   
	       
	    String SMTPHost = domain.getAttr(Provisioning.A_zimbraDNSCheckHostname, true);
	    String domainName = domain.getName();
	    if(SMTPHost == null || SMTPHost.length()<1)
	    	SMTPHost = domain.getAttr(Provisioning.A_zimbraSmtpHostname, false);

	    if(SMTPHost == null || SMTPHost.length()<1)
	    	SMTPHost = prov.getLocalServer().getAttr(Provisioning.A_zimbraSmtpHostname);
	
	    if(SMTPHost == null || SMTPHost.length()<1)
	    	SMTPHost = prov.getConfig().getAttr(Provisioning.A_zimbraSmtpHostname);
	    
		if(SMTPHost == null || SMTPHost.length()<1)
			SMTPHost = domain.getName();

		String SMTPHostMatch = String.format("^[0-9]{2}\\s%s\\.$", SMTPHost);
		ZimbraLog.soap.info("checking domain mx record");
		Hashtable env = new Hashtable();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
		String message = String.format("Domain is configured to use SMTP host: %s. None of the MX records match this name.", SMTPHost);
		Element response = zsc.createElement(AdminConstants.CHECK_DOMAIN_MX_RECORD_RESPONSE);
		boolean found = false;
		try {
			DirContext ictx = new InitialDirContext(env);
			Attributes attrs = ictx.getAttributes(domainName, new String[] {"MX"});
			if(attrs.size()<1) {
				throw ServiceException.FAILURE(String.format("Failed to retreive MX record for %s from DNS", domainName), null);
			}
	        for (NamingEnumeration ne = attrs.getAll(); ne.hasMore(); ) {
	            Attribute attr = (Attribute) ne.next();
	            if (attr.size() == 1) {
	            	ZimbraLog.soap.info("single attribute");
	                Object o = attr.get();
	                if (o instanceof String) {
	                	String rec = o.toString();
	                	ZimbraLog.soap.info("found MX record " + rec);
	                	if(rec.matches(SMTPHostMatch)) {
	                		found = true;
	                		break;
	                	}
	                	response.addElement(AdminConstants.E_ENTRY).addText(rec);
	                } else { 
	                	String rec = new String((byte[])o);
	                	ZimbraLog.soap.info("found MX attribute " + attr.getID() + " = "+ rec);
	                	if(rec.matches(SMTPHostMatch)) {
	                		found = true;
	                		break;
	                	}
	                	response.addElement(AdminConstants.E_ENTRY).addText(rec);
	                }

	            } else {
	            	ZimbraLog.soap.info("multivalued attribute");
	                for (int i=0; i < attr.size(); i++) {
	                    Object o = attr.get(i);
	                    if (o instanceof String) {
	                    	String rec = o.toString();
	                    	ZimbraLog.soap.info("found MX record " + attr.getID() + "-" + Integer.toString(i) + " = " + rec);
	                    	if(rec.matches(SMTPHostMatch)) {
		                		found = true;
		                		break;
		                	}
	                    	response.addElement(AdminConstants.E_ENTRY).addText(rec);
	                    } else { 
	                    	String rec = new String((byte[])o);
	                    	ZimbraLog.soap.info("found MX attribute " + attr.getID() + "-" + Integer.toString(i) + " = "+ rec);
	                    	if(rec.matches(SMTPHostMatch)) {
		                		found = true;
		                		break;
		                	}
	                    	response.addElement(AdminConstants.E_ENTRY).addText(rec);
		                	//message = String.format("%s %s", message,rec);
	                    }
	                }
	                
	            }
	        }
	        if(found)
	        	response.addElement(AdminConstants.E_CODE).addText("Ok");
	        else {
	        	response.addElement(AdminConstants.E_CODE).addText("Failed");
	        	
	        	response.addElement(AdminConstants.E_MESSAGE).addText(message);
	        }
		} catch (NameNotFoundException e) {
			throw ServiceException.FAILURE("NameNotFoundException", e);
		}
		catch (NamingException e) {
			// TODO Auto-generated catch block
			throw ServiceException.FAILURE("Failed to verify domain's MX record", e);
		}
		return response;
	}

}

	
	