package com.zimbra.cs.client.soap;

import com.zimbra.cs.client.*;

public class LmcGetNoteResponse extends LmcSoapResponse {

    private LmcNote mNote;

    public LmcNote getNote() { return mNote; }

    public void setNote(LmcNote n) { mNote = n; }
}
