/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.client.soap;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.zimbra.common.soap.DomUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;

public class LmcGetNoteRequest extends LmcSoapRequest {

    private String mNoteToGet;


    /**
     * Set the ID of the note to get.
     * @param n - the ID of the note to get
     */
    public void setNoteToGet(String n) { mNoteToGet = n; }

    public String getNoteToGet() { return mNoteToGet; }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailConstants.GET_NOTE_REQUEST);
        Element note = DomUtil.add(request, MailConstants.E_NOTE, "");
        DomUtil.addAttr(note, MailConstants.A_ID, mNoteToGet);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML)
        throws ServiceException
    {
        LmcGetNoteResponse response = new LmcGetNoteResponse();
        Element noteElem = DomUtil.get(responseXML, MailConstants.E_NOTE);
        response.setNote(parseNote(noteElem));
        return response;
    }

}
