/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
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
import com.zimbra.soap.mail.message.GetDataSourcesRequest;
import com.zimbra.soap.mail.message.GetDataSourcesResponse;
import com.zimbra.soap.mail.message.GetFolderRequest;
import com.zimbra.soap.mail.message.GetFolderResponse;

public class JaxbUtil {

    private static Class<?>[] MESSAGE_CLASSES;
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
            
            // zimbraMail
            GetDataSourcesRequest.class,
            GetDataSourcesResponse.class,
            GetFolderRequest.class,
            GetFolderResponse.class
        };
        
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(MESSAGE_CLASSES);
        } catch (JAXBException e) {
            ZimbraLog.soap.error("Unable to initialize JAXB", e);
        }
    }
    
    public static Element jaxbToElement(Object o)
    throws ServiceException {
        try {
            Marshaller marshaller = getContext().createMarshaller();
            // marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            marshaller.marshal(o, out);
            byte[] content = out.toByteArray();
            return Element.parseXML(new ByteArrayInputStream(content));
        } catch (Exception e) {
            throw ServiceException.FAILURE("Unable to convert " + o.getClass().getName() + " to Element", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T elementToJaxb(Element e)
    throws ServiceException {
        try {
            Unmarshaller unmarshaller = getContext().createUnmarshaller();
            String responseXml = e.prettyPrint();
            return (T) unmarshaller.unmarshal(new ByteArrayInputStream(responseXml.getBytes()));
        } catch (JAXBException ex) {
            throw ServiceException.FAILURE("Unable to unmarshal response for " + e.getName(), ex);
        }
        
    }

    private static JAXBContext getContext()
    throws ServiceException {
        if (JAXB_CONTEXT == null) {
            throw ServiceException.FAILURE("JAXB has not been initialized", null);
        }
        return JAXB_CONTEXT;
    }
}
