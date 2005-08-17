package com.liquidsys.coco.client.soap;

import com.liquidsys.coco.client.*;

public class LmcCreateNoteResponse extends LmcSoapResponse {

    private LmcNote mNote;

    public LmcNote getNote() { return mNote; }

    public void setNote(LmcNote n) { mNote = n; }
}
