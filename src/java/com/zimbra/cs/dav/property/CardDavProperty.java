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
package com.zimbra.cs.dav.property;

import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.service.DavServlet;

public class CardDavProperty extends ResourceProperty {

    public static ResourceProperty getAddressbookHomeSet(String user) {
        return new AddressbookHomeSet(user);
    }
    
    protected CardDavProperty(QName name) {
        super(name);
        setProtected(true);
    }

    private static class AddressbookHomeSet extends CardDavProperty {
        public AddressbookHomeSet(String user) {
            super(DavElements.CardDav.E_ADDRESSBOOK_HOME_SET);
            Element e = org.dom4j.DocumentHelper.createElement(DavElements.E_HREF);
            e.setText(DavServlet.DAV_PATH + "/" + user + "/");
            mChildren.add(e);
        }
    }
}
