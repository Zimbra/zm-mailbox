package com.zimbra.cs.client.soap;

import com.zimbra.cs.client.*;

public class LmcCreateFolderResponse extends LmcSoapResponse {

    private LmcFolder mFolder;

    public LmcFolder getFolder() { return mFolder; }

    public void setFolder(LmcFolder f) { mFolder = f; }
}
