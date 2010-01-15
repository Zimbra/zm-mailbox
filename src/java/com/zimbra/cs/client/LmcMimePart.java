/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.client;

import java.util.ArrayList;
import java.util.List;

public class LmcMimePart {

    private String partName;
    private String contentType;
    private String contentTypeName;
    private String contentDisp;
    private String contentDispFilename;
    private String isBody;
    private String messageID;
    private String size;
    private String convID;

    private boolean isBodyBool = false;

    // these two are part of the content element
    private String contentEncoding;
    private String content;  

    // XXX not sure where this one goes
    private String contentDesc;

    private LmcMimePart subParts[];

    public void setSubParts(LmcMimePart s[]) { subParts = s; }
    public void setConvID(String c) { convID = c; }
    public void setSize(String s) { size = s; }
    public void setPartName(String m) { partName = m; }
    public void setContentType(String c) { contentType = c; }
    public void setContentTypeName(String c) { contentTypeName = c; }
    public void setContentDisp(String c) { contentDisp = c; }
    public void setContentDispFilename(String c) { contentDispFilename = c; }
    public void setMessageID(String m) { messageID = m; }
    public void setIsBody(String b) {
        isBody = b;
        isBodyBool = (isBody != null && isBody.compareTo("1") == 0);
    }

    // these 2 are in the <content> element of a MIME part
    public void setContentEncoding(String c) { contentEncoding = c; }
    public void setContent(String c) { content = c; }

    // not sure where this 
    public void setContentDesc(String c) { contentDesc = c; }


    public LmcMimePart[] getSubParts() { return subParts; }
    public String getSize() { return size; }
    public String getPartName() { return partName; }
    public String getConvID() { return convID; }
    public String getContentType() { return contentType; }
    public String getContentTypeName() { return contentTypeName; }
    public String getContentDisp() { return contentDisp; }
    public String getContentDispFilename() { return contentDispFilename; }
    public String getContentDesc() { return contentDesc; }
    public String getContentEncoding() { return contentEncoding; }
    public String getContent() { return content; }
    public String getMessageID() { return messageID; }
    public String getIsBody() { return isBody; }
    public boolean isBody() { return isBodyBool; }

    public String getMessageBody() {
        return findBodyContent(this);
    }

    private static String findBodyContent(LmcMimePart parent) {
        if (parent.isBody())
            return parent.getContent();
        else if (parent.subParts != null) {
    		for (int i = 0; i < parent.subParts.length; i++) {
    			LmcMimePart part = parent.subParts[i];
                String c = findBodyContent(part);
                if (c != null)
                    return c;
            }
        }
        return null;
    }

    public String[] getAttachmentPartNumbers() {
    	List parts = new ArrayList();
        findAttachmentParts(this, parts);
        if (parts.size() > 0) {
            String[] array = new String[parts.size()];
            return (String[]) parts.toArray(array);
        }
        return null;
    }

    private static void findAttachmentParts(LmcMimePart parent, List parts) {
        if (parent.subParts != null) {
        	for (int i = 0; i < parent.subParts.length; i++) {
        		findAttachmentParts(parent.subParts[i], parts);
            }
        } else {
            String cd = parent.getContentDisp();
            if (cd != null &&  cd.compareToIgnoreCase("attachment") == 0) {
                String pn = parent.getPartName();
                if (pn != null)
                    parts.add(pn);
            }
        }
    }
}
