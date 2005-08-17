package com.liquidsys.coco.client.soap;

import com.liquidsys.coco.client.*;

public class LmcCreateFolderResponse extends LmcSoapResponse {

    private LmcFolder mFolder;

    public LmcFolder getFolder() { return mFolder; }

    public void setFolder(LmcFolder f) { mFolder = f; }
}
