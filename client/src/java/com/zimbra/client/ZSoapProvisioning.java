package com.zimbra.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpException;

import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.util.Constants;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.admin.message.GetDomainInfoRequest;
import com.zimbra.soap.admin.message.GetDomainInfoResponse;
import com.zimbra.soap.admin.type.DomainInfo;
import com.zimbra.soap.admin.type.DomainSelector;

public class ZSoapProvisioning {

    private SoapHttpTransport transport;
    private ZAuthToken authToken;
    private String csrfToken;

    public ZDomain getDomainInfo(DomainBy keyType, String key)
    throws ServiceException {
        DomainSelector domSel = new DomainSelector(toJaxb(keyType), key);

        try {
            GetDomainInfoResponse resp = invokeJaxb(new GetDomainInfoRequest(domSel, null));
            DomainInfo domainInfo = resp.getDomain();
            return domainInfo == null ? null : new ZDomain(domainInfo);
        } catch (ServiceException e) {
            if (e.getCode().equals(Constants.ERROR_CODE_NO_SUCH_DOMAIN))
                return null;
            else
                throw e;
        }
    }

    private static DomainSelector.DomainBy toJaxb(DomainBy provDomainBy) throws ServiceException {
        return DomainSelector.DomainBy.fromString(provDomainBy.toString());
    }

    @SuppressWarnings("unchecked")
    public <T> T invokeJaxb(Object jaxbObject)
    throws ServiceException {
        Element req = JaxbUtil.jaxbToElement(jaxbObject);
        Element res = invoke(req);
        return (T) JaxbUtil.elementToJaxb(res);
    }

    public synchronized Element invoke(Element request) throws ServiceException {
        checkTransport();
        try {
            return invokeRequest(request);
        } catch (SoapFaultException e) {
            throw e;
        } catch (IOException e) {
            throw ZClientException.IO_ERROR("invoke "+e.getMessage()+", server: "+serverName(), e);
        }
    }

    private String serverName() {
        try {
            return new URI(transport.getURI()).getHost();
        } catch (URISyntaxException e) {
            return transport.getURI();
        }
    }

    private void checkTransport() throws ServiceException {
        if (transport == null)
            throw ServiceException.FAILURE("transport has not been initialized", null);
    }

    private Element invokeRequest(Element request) throws ServiceException, IOException{
            return transport.invokeWithoutSession(request);
    }

    /**
     * @param uri URI of server we want to talk to
     */
    public void soapSetURI(String uri) {
        if (transport != null) {
           transport.shutdown();
        }
        transport = new SoapHttpTransport(uri);
    }

    public ZAuthToken getAuthToken() {
        return authToken;
    }

    public void setAuthToken(ZAuthToken authToken) {
       this.authToken = authToken;
        if (transport != null)
            transport.setAuthToken(authToken);
    }

    public void setCsrfToken(String csrfToken) {
        this.csrfToken = csrfToken;
        if (transport != null)
            transport.setCsrfToken(csrfToken);
    }
}
