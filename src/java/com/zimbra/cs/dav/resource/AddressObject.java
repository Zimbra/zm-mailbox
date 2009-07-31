/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.resource;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.service.formatter.VCard;

public class AddressObject extends MailItemResource {

    public static final String VCARD_EXTENSION = ".vcf";
    
    private VCard mVCard;
    
    public AddressObject(DavContext ctxt, Contact item) throws ServiceException {
        super(ctxt, item);
        mVCard = VCard.formatContact(item);
    }
    
    public boolean isCollection() {
        return false;
    }
    
    public String toVCard() {
        return mVCard.formatted;
    }
}
