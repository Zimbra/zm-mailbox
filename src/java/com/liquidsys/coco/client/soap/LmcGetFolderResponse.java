package com.liquidsys.coco.client.soap;

import com.liquidsys.coco.client.*;

public class LmcGetFolderResponse extends LmcSoapResponse {

    private LmcFolder mFolder;

    public LmcFolder getRootFolder() { return mFolder; }

    public void setRootFolder(LmcFolder s) { mFolder = s; }
}
