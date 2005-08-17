package com.zimbra.cs.client.soap;

import com.zimbra.cs.client.*;

public class LmcGetFolderResponse extends LmcSoapResponse {

    private LmcFolder mFolder;

    public LmcFolder getRootFolder() { return mFolder; }

    public void setRootFolder(LmcFolder s) { mFolder = s; }
}
