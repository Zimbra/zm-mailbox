/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 VMware, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.dom4j.Document;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.account.message.AuthRequest;
import com.zimbra.soap.account.message.AuthResponse;
import com.zimbra.soap.account.message.ChangePasswordRequest;
import com.zimbra.soap.account.message.ChangePasswordResponse;
import com.zimbra.soap.account.message.GetIdentitiesRequest;
import com.zimbra.soap.account.message.GetIdentitiesResponse;
import com.zimbra.soap.account.message.GetInfoRequest;
import com.zimbra.soap.account.message.GetInfoResponse;
import com.zimbra.soap.account.message.GetSignaturesRequest;
import com.zimbra.soap.account.message.GetSignaturesResponse;
import com.zimbra.soap.account.message.ModifyPropertiesRequest;
import com.zimbra.soap.account.message.ModifyPropertiesResponse;
import com.zimbra.soap.admin.message.ReloadLocalConfigRequest;
import com.zimbra.soap.admin.message.ReloadLocalConfigResponse;
import com.zimbra.soap.mail.message.GetDataSourcesRequest;
import com.zimbra.soap.mail.message.GetDataSourcesResponse;
import com.zimbra.soap.mail.message.GetFolderRequest;
import com.zimbra.soap.mail.message.GetFolderResponse;

public final class JaxbUtil {

    private static final Class<?>[] MESSAGE_CLASSES;
    private static JAXBContext JAXB_CONTEXT;

    static {
        MESSAGE_CLASSES = new Class<?>[] {
            // zimbraAccount
            AuthRequest.class,
            AuthResponse.class,
            ChangePasswordRequest.class,
            ChangePasswordResponse.class,
            GetIdentitiesRequest.class,
            GetIdentitiesResponse.class,
            GetInfoRequest.class,
            GetInfoResponse.class,
            GetSignaturesRequest.class,
            GetSignaturesResponse.class,
            ModifyPropertiesRequest.class,
            ModifyPropertiesResponse.class,

            // zimbraMail
            GetDataSourcesRequest.class,
            GetDataSourcesResponse.class,
            GetFolderRequest.class,
            GetFolderResponse.class,

            // zimbraAdmin
            ReloadLocalConfigRequest.class,
            ReloadLocalConfigResponse.class
        };

        try {
            JAXB_CONTEXT = JAXBContext.newInstance(MESSAGE_CLASSES);
        } catch (JAXBException e) {
            ZimbraLog.soap.error("Unable to initialize JAXB", e);
        }
    }

    private JaxbUtil() {
    }

    /**
     * @param o
     * @param factory - e.g. XmlElement.mFactory or JSONElement.mFactory 
     * @return
     * @throws ServiceException
     */
    public static Element jaxbToElement(Object o, Element.ElementFactory factory)
    throws ServiceException {
        try {
            Marshaller marshaller = getContext().createMarshaller();
            // marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            DocumentResult dr = new DocumentResult();
            marshaller.marshal(o, dr); 
            Document theDoc = dr.getDocument();
            org.dom4j.Element rootElem = theDoc.getRootElement();
            return Element.convertDOM(rootElem, factory);
        } catch (Exception e) {
            throw ServiceException.FAILURE("Unable to convert " +
                    o.getClass().getName() + " to Element", e);
        }
    }

    public static Element jaxbToElement(Object o)
    throws ServiceException {
        return jaxbToElement(o, XMLElement.mFactory);
    }

    /**
     * Return a JAXB object.  This implementation uses a org.w3c.dom.Document 
     * as an intermediate representation.  This appears to be more reliable
     * than using a DocumentSource based on org.dom4j.Element
     */
    @SuppressWarnings("unchecked")
    public static <T> T elementToJaxb(Element e)
    throws ServiceException {
        try {
            Unmarshaller unmarshaller = getContext().createUnmarshaller();
            org.w3c.dom.Document doc = e.toW3cDom();
            return (T) unmarshaller.unmarshal(doc);
        } catch (JAXBException ex) {
            throw ServiceException.FAILURE(
                    "Unable to unmarshal response for " + e.getName(), ex);
        }
    }

    public static String domToString(org.w3c.dom.Document document) {
        try {
            Source xmlSource = new DOMSource(document);
            StreamResult result = new StreamResult(new ByteArrayOutputStream());
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty("indent", "yes"); //Java XML Indent
            transformer.transform(xmlSource, result);
            return result.getOutputStream().toString();
        } catch (TransformerFactoryConfigurationError factoryError) {
            ZimbraLog.soap.error("Error creating TransformerFactory", factoryError);
        } catch (TransformerException transformerError) {
            ZimbraLog.soap.error( "Error transforming document", transformerError);
        }
        return null;
    }

    private static JAXBContext getContext()
    throws ServiceException {
        if (JAXB_CONTEXT == null) {
            throw ServiceException.FAILURE("JAXB has not been initialized", null);
        }
        return JAXB_CONTEXT;
    }
}
