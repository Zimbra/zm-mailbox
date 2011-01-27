/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010-2011 Zimbra, Inc.
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
import com.zimbra.soap.mail.message.ExportContactsRequest;
import com.zimbra.soap.mail.message.ExportContactsResponse;
import com.zimbra.soap.mail.message.GetDataSourcesRequest;
import com.zimbra.soap.mail.message.GetDataSourcesResponse;
import com.zimbra.soap.mail.message.GetFolderRequest;
import com.zimbra.soap.mail.message.GetFolderResponse;
import com.zimbra.soap.mail.message.ImportContactsRequest;
import com.zimbra.soap.mail.message.ImportContactsResponse;

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
            ExportContactsRequest.class,
            ExportContactsResponse.class,
            GetFolderRequest.class,
            GetFolderResponse.class,
            ImportContactsRequest.class,
            ImportContactsResponse.class,

            // zimbraAdmin
            com.zimbra.soap.admin.message.AddAccountAliasRequest.class,
            com.zimbra.soap.admin.message.AddAccountAliasResponse.class,
            com.zimbra.soap.admin.message.AuthRequest.class,
            com.zimbra.soap.admin.message.AuthResponse.class,
            com.zimbra.soap.admin.message.CheckHealthRequest.class,
            com.zimbra.soap.admin.message.CheckHealthResponse.class,
            com.zimbra.soap.admin.message.CheckPasswordStrengthRequest.class,
            com.zimbra.soap.admin.message.CheckPasswordStrengthResponse.class,
            com.zimbra.soap.admin.message.CopyCosRequest.class,
            com.zimbra.soap.admin.message.CopyCosResponse.class,
            com.zimbra.soap.admin.message.CountAccountRequest.class,
            com.zimbra.soap.admin.message.CountAccountResponse.class,
            com.zimbra.soap.admin.message.CreateAccountRequest.class,
            com.zimbra.soap.admin.message.CreateAccountResponse.class,
            com.zimbra.soap.admin.message.CreateCosRequest.class,
            com.zimbra.soap.admin.message.CreateCosResponse.class,
            com.zimbra.soap.admin.message.CreateDomainRequest.class,
            com.zimbra.soap.admin.message.CreateDomainResponse.class,
            com.zimbra.soap.admin.message.CreateServerRequest.class,
            com.zimbra.soap.admin.message.CreateServerResponse.class,
            com.zimbra.soap.admin.message.DelegateAuthRequest.class,
            com.zimbra.soap.admin.message.DelegateAuthResponse.class,
            com.zimbra.soap.admin.message.DeleteAccountRequest.class,
            com.zimbra.soap.admin.message.DeleteAccountResponse.class,
            com.zimbra.soap.admin.message.DeleteCosRequest.class,
            com.zimbra.soap.admin.message.DeleteCosResponse.class,
            com.zimbra.soap.admin.message.DeleteDomainRequest.class,
            com.zimbra.soap.admin.message.DeleteDomainResponse.class,
            com.zimbra.soap.admin.message.DeleteMailboxRequest.class,
            com.zimbra.soap.admin.message.DeleteMailboxResponse.class,
            com.zimbra.soap.admin.message.DeleteServerRequest.class,
            com.zimbra.soap.admin.message.DeleteServerResponse.class,
            com.zimbra.soap.admin.message.FlushCacheRequest.class,
            com.zimbra.soap.admin.message.FlushCacheResponse.class,
            com.zimbra.soap.admin.message.GetAccountInfoRequest.class,
            com.zimbra.soap.admin.message.GetAccountInfoResponse.class,
            com.zimbra.soap.admin.message.GetAccountMembershipRequest.class,
            com.zimbra.soap.admin.message.GetAccountMembershipResponse.class,
            com.zimbra.soap.admin.message.GetAccountRequest.class,
            com.zimbra.soap.admin.message.GetAccountResponse.class,
            com.zimbra.soap.admin.message.GetAllAccountsRequest.class,
            com.zimbra.soap.admin.message.GetAllAccountsResponse.class,
            com.zimbra.soap.admin.message.GetAllAdminAccountsRequest.class,
            com.zimbra.soap.admin.message.GetAllAdminAccountsResponse.class,
            com.zimbra.soap.admin.message.GetAllConfigRequest.class,
            com.zimbra.soap.admin.message.GetAllConfigResponse.class,
            com.zimbra.soap.admin.message.GetAllCosRequest.class,
            com.zimbra.soap.admin.message.GetAllCosResponse.class,
            com.zimbra.soap.admin.message.GetAllDomainsRequest.class,
            com.zimbra.soap.admin.message.GetAllDomainsResponse.class,
            com.zimbra.soap.admin.message.GetAllLocalesRequest.class,
            com.zimbra.soap.admin.message.GetAllLocalesResponse.class,
            com.zimbra.soap.admin.message.GetAllMailboxesRequest.class,
            com.zimbra.soap.admin.message.GetAllMailboxesResponse.class,
            com.zimbra.soap.admin.message.GetAllServersRequest.class,
            com.zimbra.soap.admin.message.GetAllServersResponse.class,
            com.zimbra.soap.admin.message.GetConfigRequest.class,
            com.zimbra.soap.admin.message.GetConfigResponse.class,
            com.zimbra.soap.admin.message.GetCosRequest.class,
            com.zimbra.soap.admin.message.GetCosResponse.class,
            com.zimbra.soap.admin.message.GetDomainInfoRequest.class,
            com.zimbra.soap.admin.message.GetDomainInfoResponse.class,
            com.zimbra.soap.admin.message.GetDomainRequest.class,
            com.zimbra.soap.admin.message.GetDomainResponse.class,
            com.zimbra.soap.admin.message.GetMailboxRequest.class,
            com.zimbra.soap.admin.message.GetMailboxResponse.class,
            com.zimbra.soap.admin.message.GetMailboxStatsRequest.class,
            com.zimbra.soap.admin.message.GetMailboxStatsResponse.class,
            com.zimbra.soap.admin.message.GetServerRequest.class,
            com.zimbra.soap.admin.message.GetServerResponse.class,
            com.zimbra.soap.admin.message.GetServerStatsRequest.class,
            com.zimbra.soap.admin.message.GetServerStatsResponse.class,
            com.zimbra.soap.admin.message.GetServiceStatusRequest.class,
            com.zimbra.soap.admin.message.GetServiceStatusResponse.class,
            com.zimbra.soap.admin.message.ModifyAccountRequest.class,
            com.zimbra.soap.admin.message.ModifyAccountResponse.class,
            com.zimbra.soap.admin.message.ModifyConfigRequest.class,
            com.zimbra.soap.admin.message.ModifyConfigResponse.class,
            com.zimbra.soap.admin.message.ModifyCosRequest.class,
            com.zimbra.soap.admin.message.ModifyCosResponse.class,
            com.zimbra.soap.admin.message.ModifyDomainRequest.class,
            com.zimbra.soap.admin.message.ModifyDomainResponse.class,
            com.zimbra.soap.admin.message.ModifyServerRequest.class,
            com.zimbra.soap.admin.message.ModifyServerResponse.class,
            com.zimbra.soap.admin.message.NoOpRequest.class,
            com.zimbra.soap.admin.message.NoOpResponse.class,
            com.zimbra.soap.admin.message.PingRequest.class,
            com.zimbra.soap.admin.message.PingResponse.class,
            com.zimbra.soap.admin.message.RecalculateMailboxCountsRequest.class,
            com.zimbra.soap.admin.message.RecalculateMailboxCountsResponse.class,
            com.zimbra.soap.admin.message.ReloadLocalConfigRequest.class,
            com.zimbra.soap.admin.message.ReloadLocalConfigResponse.class,
            com.zimbra.soap.admin.message.RemoveAccountAliasRequest.class,
            com.zimbra.soap.admin.message.RemoveAccountAliasResponse.class,
            com.zimbra.soap.admin.message.RenameAccountRequest.class,
            com.zimbra.soap.admin.message.RenameAccountResponse.class,
            com.zimbra.soap.admin.message.RenameCosRequest.class,
            com.zimbra.soap.admin.message.RenameCosResponse.class,
            com.zimbra.soap.admin.message.SearchDirectoryRequest.class,
            com.zimbra.soap.admin.message.SearchDirectoryResponse.class,
            com.zimbra.soap.admin.message.SetPasswordRequest.class,
            com.zimbra.soap.admin.message.SetPasswordResponse.class
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

    //  This appears to be safe but is fairly slow.
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T> T elementToJaxbUsingByteArray(Element e)
    throws ServiceException {
        try {
            Unmarshaller unmarshaller = getContext().createUnmarshaller();
            org.dom4j.Element rootElem = e.toXML();
            return (T) unmarshaller.unmarshal(new ByteArrayInputStream(rootElem.asXML().getBytes()));
        } catch (JAXBException ex) {
            throw ServiceException.FAILURE(
                    "Unable to unmarshal response for " + e.getName(), ex);
        }
    }

    // This appears to work if e is an XMLElement but sometimes fails badly if
    // e is a JSONElement - get:
    // javax.xml.bind.UnmarshalException: Namespace URIs and local names
    //      to the unmarshaller needs to be interned.
    // and that seems to make the unmarshaller unstable from then on :-(
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T> T elementToJaxbUsingDom4j(Element e)
    throws ServiceException {
        try {
            Unmarshaller unmarshaller = getContext().createUnmarshaller();
            org.dom4j.Element rootElem = e.toXML();
            DocumentSource docSrc = new DocumentSource(rootElem);
            return (T) unmarshaller.unmarshal(docSrc);
        } catch (JAXBException ex) {
            throw ServiceException.FAILURE(
                    "Unable to unmarshal response for " + e.getName(), ex);
        }
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
            // ZimbraLog.soap.warn("Dom to Xml:\n" + domToString(doc));
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
