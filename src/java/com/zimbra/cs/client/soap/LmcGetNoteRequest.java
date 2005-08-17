package com.zimbra.cs.client.soap;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.zimbra.soap.DomUtil;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;

public class LmcGetNoteRequest extends LmcSoapRequest {

    private String mNoteToGet;
    

    /**
     * Set the ID of the note to get.
     * @param n - the ID of the note to get
     */
    public void setNoteToGet(String n) { mNoteToGet = n; }
    
    public String getNoteToGet() { return mNoteToGet; }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailService.GET_NOTE_REQUEST);  
        Element note = DomUtil.add(request, MailService.E_NOTE, "");
        DomUtil.addAttr(note, MailService.A_ID, mNoteToGet);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException
    {
        LmcGetNoteResponse response = new LmcGetNoteResponse();
        Element noteElem = DomUtil.get(responseXML, MailService.E_NOTE);
        response.setNote(parseNote(noteElem));
        return response;
    }

}
