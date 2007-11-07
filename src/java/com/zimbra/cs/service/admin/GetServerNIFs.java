package com.zimbra.cs.service.admin;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.ServerBy;
import com.zimbra.cs.rmgmt.RemoteCommands;
import com.zimbra.cs.rmgmt.RemoteManager;
import com.zimbra.cs.rmgmt.RemoteResult;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author Greg Solovyev
 */
public class GetServerNIFs extends AdminDocumentHandler {
    private static final Pattern ADDR_PATTERN = Pattern.compile("(addr):([0-9\\.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MASK_PATTERN = Pattern.compile("(mask):([0-9\\.]+)", Pattern.CASE_INSENSITIVE);    
    private static final int KEY_GROUP = 1;
    private static final int VALUE_GROUP = 2;
    
	
	public Element handle(Element request, Map<String, Object> context)
			throws ServiceException {
		
		
		ZimbraSoapContext lc = getZimbraSoapContext(context);

	    Element serverEl = request.getElement(AdminConstants.E_SERVER);
	    String method = serverEl.getAttribute(AdminConstants.A_BY);
	    String serverName = serverEl.getText();
	    Provisioning prov = Provisioning.getInstance();
		Server server = prov.get(ServerBy.fromString(method), serverName);
		if (server == null) {
			throw ServiceException.INVALID_REQUEST("Cannot find server record for the host: " + serverName, null);
		}	    
		
		RemoteManager rmgr = RemoteManager.getRemoteManager(server);
		RemoteResult rr = rmgr.execute(RemoteCommands.ZM_SERVER_IPS);
		BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(rr.getMStdout())));
        String line;
        
        Element response = lc.createElement(AdminConstants.GET_SERVER_NIFS_RESPONSE);
        try {
			while ((line = in.readLine()) != null) {
			    Element elNIF = response.addElement(AdminConstants.E_NI);

			    Matcher IPmatcher = ADDR_PATTERN.matcher(line);
			    if (IPmatcher.find()) {
			    	elNIF.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, IPmatcher.group(KEY_GROUP).toLowerCase()).setText(IPmatcher.group(VALUE_GROUP));
			    }
			    
			    Matcher maskMatcher = MASK_PATTERN.matcher(line);
			    if (maskMatcher.find()) {
			    	elNIF.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, maskMatcher.group(KEY_GROUP).toLowerCase()).setText(maskMatcher.group(VALUE_GROUP));
			    }			    
			}
		} catch (IOException e) {
			throw ServiceException.FAILURE("exception occurred handling CLI command", e);
		}		
		
		
		return response;
	}

}
