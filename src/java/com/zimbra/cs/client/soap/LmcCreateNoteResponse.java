package com.zimbra.cs.client.soap;

import com.zimbra.cs.client.*;

public class LmcCreateNoteResponse extends LmcSoapResponse {

    private LmcNote mNote;

    public LmcNote getNote() { return mNote; }

    public void setNote(LmcNote n) { mNote = n; }
}
