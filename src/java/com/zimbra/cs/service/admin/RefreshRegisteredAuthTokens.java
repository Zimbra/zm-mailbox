package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.RefreshRegisteredAuthTokensRequest;
import com.zimbra.soap.admin.message.RefreshRegisteredAuthTokensResponse;

/**
 * This admin SOAP handler receives a list of authtokens that have been deregistered on another server and
 * reloads corresponding accounts from LDAP in order to have updated registry of authtokens for the accounts
 * @author gsolovyev
 *
 */
public class RefreshRegisteredAuthTokens extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        checkRight(zsc, context, null, AdminRight.PR_SYSTEM_ADMIN_ONLY);

        Provisioning prov = Provisioning.getInstance();
        Server localServer = prov.getLocalServer();
        if(localServer.getLowestSupportedAuthVersion() < 2) {
            return JaxbUtil.jaxbToElement( new RefreshRegisteredAuthTokensResponse());
        }

        RefreshRegisteredAuthTokensRequest req = JaxbUtil.elementToJaxb(request);
        List<String> tokens = req.getTokens();
        if(tokens != null && !tokens.isEmpty()) {
            for(String token : tokens) {
                try {
                    AuthToken zt = ZimbraAuthToken.getAuthToken(token);
                    if(zt.isRegistered()) {
                        Account acc = zt.getAccount();
                        Provisioning.getInstance().reload(acc);
                        ZimbraLog.soap.debug("Refreshed token %s for account %s", token, acc.getName());
                    }
                } catch (AuthTokenException | ServiceException e) {
                    ZimbraLog.soap.error("Failed to refresh deregistered authtoken %s", token, e);
                }
            }
        }
        return JaxbUtil.jaxbToElement( new RefreshRegisteredAuthTokensResponse());
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.SYSTEM_ADMINS_ONLY);
    }
}
