/*
 * Soap11Protocol.java
 */

package com.liquidsys.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;

import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;

/**
 * Interface to Soap 1.1 Protocol
 */

class Soap11Protocol extends SoapProtocol {

    private static final String NS_STR =
        "http://schemas.xmlsoap.org/soap/envelope/";
    private static final Namespace NS = Namespace.get(NS_PREFIX, NS_STR);
    private static final QName FAULTCODE = new QName("faultcode", NS);
    private static final QName FAULTSTRING = new QName("faultstring", NS);
    private static final QName DETAIL = new QName("detail", NS);
    private static final QName SENDER_CODE = new QName("Client", NS);
    private static final QName RECEIVER_CODE = new QName("Server", NS);

    /** empty package-private constructor */
    Soap11Protocol() { 
        super();
    }

    public Element.ElementFactory getFactory() {
        return Element.XMLElement.mFactory;
    }

    /**
     * Return the namespace String
     */
    public Namespace getNamespace() {
        return NS;
    }
    
    /* (non-Javadoc)
     * @see com.liquidsys.soap.shared.SoapProtocol#soapFault(org.dom4j.Element)
     */
    public SoapFaultException soapFault(Element fault) {
        if (!isFault(fault))
            return new SoapFaultException("not a soap fault ", fault);
        
        Element code = fault.getOptionalElement(FAULTCODE);
        boolean isReceiversFault = RECEIVER_CODE.equals(code == null ? null : code.getQName());

        String reasonValue;
        Element faultString = fault.getOptionalElement(FAULTSTRING);
        if (faultString != null)
            reasonValue = faultString.getTextTrim();
        else
            reasonValue = "unknown reason";

        Element detail = fault.getOptionalElement(DETAIL);

        return new SoapFaultException(reasonValue, detail, isReceiversFault, fault);
    }

    /* (non-Javadoc)
     * @see com.liquidsys.soap.SoapProtocol#soapFault(com.liquidsys.coco.service.ServiceException)
     */
    public Element soapFault(ServiceException e) {
        String reason = e.getMessage();
        if (reason == null)
            reason = e.toString();
        QName code;
        
        if (e.isReceiversFault())
            code = RECEIVER_CODE;
        else 
            code = SENDER_CODE;

        Element eFault = mFactory.createElement(mFaultQName);
        eFault.addUniqueElement(FAULTCODE).setText(code.getQualifiedName());
        eFault.addUniqueElement(FAULTSTRING).setText(reason);
        Element eDetail = eFault.addUniqueElement(DETAIL);
        Element error = eDetail.addUniqueElement(LiquidNamespace.E_ERROR);
        // FIXME: should really be a qualified "attribute"
        error.addUniqueElement(LiquidNamespace.E_CODE).setText(e.getCode());
        return eFault;
    }

    /** Return Content-Type header */
    public String getContentType() {
        return "text/xml; charset=utf-8";
    }

    /** Whether or not to include a SOAPActionHeader */
    public boolean hasSOAPActionHeader() {
        return true;
    }

    public String getVersion() {
        return "1.1.";
    }
}
