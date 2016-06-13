/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.property;

import java.util.ArrayList;

import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.resource.AddressObject;
import com.zimbra.cs.dav.service.DavServlet;

public class CardDavProperty extends ResourceProperty {

    public static ResourceProperty getAddressbookHomeSet(String user) {
        return new AddressbookHomeSet(user);
    }
    
    public static ResourceProperty getAddressbookData(Element prop, AddressObject contact) {
        return new AddressbookData(prop, contact);
    }
    
    protected CardDavProperty(QName name) {
        super(name);
        setProtected(true);
        setVisible(true);
    }

    private static class AddressbookHomeSet extends CardDavProperty {
        public AddressbookHomeSet(String user) {
            super(DavElements.CardDav.E_ADDRESSBOOK_HOME_SET);
            mChildren.add(createHref(DavServlet.DAV_PATH + "/" + user + "/"));
        }
    }
    
    private static class AddressbookData extends CardDavProperty {
        ArrayList<String> props;
        AddressObject contact;
        public AddressbookData(Element prop, AddressObject c) {
            super(DavElements.CardDav.E_ADDRESS_DATA);
            props = new ArrayList<String>();
            for (Object child : prop.elements()) {
                if (child instanceof Element) {
                    Element e = (Element) child;
                    if (e.getQName().equals(DavElements.CardDav.E_PROP))
                        props.add(e.attributeValue(DavElements.P_NAME));
                }
            }
            contact = c;
        }
        public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
            Element abd = super.toElement(ctxt, parent, nameOnly);
            try {
                abd.setText(contact.toVCard(ctxt, props));
            } catch (Exception e) {
                ZimbraLog.dav.warn("can't get vcard content", e);
            }
            return abd;
        }
    }
}
