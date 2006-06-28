/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

import java.util.List;

public interface ZMessage  {

    /**
     * @return conversation's id
     */
    public String getId();
    
    /**
     * @return comma-separated list of tag ids
     */
    public String getTagIds();
    
    public String getFlags();
    
    public String getSubject();
    
    public String getFolderId();
    
    public String getConversationId();
    
    public String getFragment();

    public long getReceivedDate();
    
    public long getSentDate();

    public String getMessageIdHeader();
    
    public List<ZEmailAddress> getEmailAddresses();
    
    public ZMimePart getMimeStructure();

    public long getSize();
    
    /** content of the message, if raw is specified. if message too big or not ASCII, a content servlet URL is returned */
    public String getContent();
    
    /** if raw is specified and message too big or not ASCII, a content servlet URL is returned */
    public String getContentURL();
    
    public interface ZMimePart {
        
        /** "" means top-level part, 1 first part, 1.1 first part of a multipart inside of 1. */
        public String getPartName();
        
        /** name attribute from the Content-Type param list */
        public String getName();

        /** MIME Content-Type */
        public String getContentType();

        /** MIME Content-Disposition */
        public String getContentDispostion();
        
        /** filename attribute from the Content-Disposition param list */
        public String getFileName();
        
        /** MIME Content-ID (for display of embedded images) */
        public String getContentId();
        
        /** MIME/Microsoft Content-Location (for display of embedded images) */
        public String getContentLocation();
        
        /** MIME Content-Description.  Note cont-desc is not currently used in the code. */
        public String getContentDescription();
        
        /** content of the part, if requested */
        public String getContent();
        
        /** set to 1, if this part is considered to be the "body" of the message for display purposes */
        public boolean isBody();
        
        /** get child parts */
        public List<ZMimePart> getChildren();
        
        public long getSize();
    }
}
