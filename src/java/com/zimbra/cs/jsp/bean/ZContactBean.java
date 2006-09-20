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
package com.zimbra.cs.jsp.bean;

import java.util.Map;
import java.util.regex.Pattern;

import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.zclient.ZContact;

public class ZContactBean {

    private ZContact mContact;
    private String mFileAs;
    
    public ZContactBean(ZContact contact) {
        mContact = contact;
    }
       
    public String getId() { return mContact.getId(); }
    
    public String getTagIds() { return mContact.getTagIds(); }
    
    public String getFlags() { return mContact.getFlags(); }
    
    public boolean getHasFlags() { return mContact.hasFlags(); }
    
    public boolean getHasTags() { return mContact.hasFlags(); }
    
    public boolean getIsFlagged() { return mContact.isFlagged(); }
    
    public boolean getHasAttachment() { return mContact.hasAttachment(); }
    
    public String getFolderId() { return mContact.getFolderId(); }

    public String getRevision() { return mContact.getRevision(); }

    /**
     * @return time in msecs
     */
    public long getMetaDataChangedDate() { return mContact.getMetaDataChangedDate(); }
    
    public Map<String, String> getAttrs() { return mContact.getAttrs(); }

    // fields

    public String getAssistantPhone() { return mContact.getAttrs().get("assistantPhone"); }

    public String getBirthday() { return mContact.getAttrs().get("birthday"); }

    public String getCallbackPhone() { return mContact.getAttrs().get("callbackPhone"); }

    public String getCarPhone() { return mContact.getAttrs().get("carPhone"); }

    public String getCompany() { return mContact.getAttrs().get("company"); }

    public String getCompanyPhone() { return mContact.getAttrs().get("companyPhone"); }

    public String getDescription() { return mContact.getAttrs().get("description"); }

    public String getDepartment() { return mContact.getAttrs().get("department"); }

    public String getEmail() { return mContact.getAttrs().get("email"); }

    public String getEmail2() { return mContact.getAttrs().get("email2"); }

    public String getEmail3() { return mContact.getAttrs().get("email3"); }

    public String getFileAs() { return mContact.getAttrs().get("fileAs"); }

    public String getFirstName() { return mContact.getAttrs().get("firstName"); }

    public String getFullName() { return mContact.getAttrs().get("fullName"); }

    public String getHomeCity() { return mContact.getAttrs().get("homeCity"); }

    public String getHomeCountry() { return mContact.getAttrs().get("homeCountry"); }

    public String getHomeFax() { return mContact.getAttrs().get("homeFax"); }

    public String getHomePhone() { return mContact.getAttrs().get("homePhone"); }

    public String getHomePhone2() { return mContact.getAttrs().get("homePhone2"); }

    public String getHomePostalCode() { return mContact.getAttrs().get("homePostalCode"); }

    public String getHomeState() { return mContact.getAttrs().get("homeState"); }

    public String getHomeStreet() { return mContact.getAttrs().get("homeStreet"); }

    public String getHomeURL() { return mContact.getAttrs().get("homeURL"); }

    public String getInitials() { return mContact.getAttrs().get("initials"); }

    public String getJobTitle() { return mContact.getAttrs().get("jobTitle"); }

    public String getLastName() { return mContact.getAttrs().get("lastName"); }

    public String getMiddleName() { return mContact.getAttrs().get("middleName"); }

    public String getMobilePhone() { return mContact.getAttrs().get("mobilePhone"); }

    public String getNamePrefix() { return mContact.getAttrs().get("namePrefix"); }

    public String getNameSuffix() { return mContact.getAttrs().get("nameSuffix"); }

    public String getNickname() { return mContact.getAttrs().get("nickname"); }

    public String getNotes() { return mContact.getAttrs().get("notes"); }

    public String getOffice() { return mContact.getAttrs().get("office"); }

    public String getOtherCity() { return mContact.getAttrs().get("otherCity"); }

    public String getOtherCountry() { return mContact.getAttrs().get("otherCountry"); }

    public String getOtherFax() { return mContact.getAttrs().get("otherFax"); }

    public String getOtherPhone() { return mContact.getAttrs().get("otherPhone"); }

    public String getOtherPostalCode() { return mContact.getAttrs().get("otherPostalCode"); }

    public String getOtherState() { return mContact.getAttrs().get("otherState"); }

    public String getOtherStreet() { return mContact.getAttrs().get("otherStreet"); }

    public String getOtherURL() { return mContact.getAttrs().get("otherURL"); }

    public String getPager() { return mContact.getAttrs().get("pager"); }

    public String getWorkCity() { return mContact.getAttrs().get("workCity"); }

    public String getWorkCountry() { return mContact.getAttrs().get("workCountry"); }

    public String getWorkFax() { return mContact.getAttrs().get("workFax"); }

    public String getWorkPhone() { return mContact.getAttrs().get("workPhone"); }

    public String getWorkPhone2() { return mContact.getAttrs().get("workPhone2"); }

    public String getWorkPostalCode() { return mContact.getAttrs().get("workPostalCode"); }

    public String getWorkState() { return mContact.getAttrs().get("workState"); }

    public String getWorkStreet() { return mContact.getAttrs().get("workStreet"); }

    public String getWorkURL() { return mContact.getAttrs().get("workURL"); }

    public String getDisplayFileAs() {
        if (mFileAs == null) {
            try {
                mFileAs = Contact.getFileAsString(mContact.getAttrs());
            } catch (ServiceException e) {
                mFileAs = "";
            }
        }
        return mFileAs;
    }
    
    private static final Pattern sCOMMA_OR_SP = Pattern.compile("[, ]");
    
    public static boolean anySet(ZContactBean cbean, String s) {
        if (s == null || s.length() == 0) return false;
        String[] fields = sCOMMA_OR_SP.split(s);
        Map<String, String> attrs = cbean.getAttrs();         
        for (String field: fields) {
            if (attrs.get(field) != null) return true;
        }
        return false;
    }

}
