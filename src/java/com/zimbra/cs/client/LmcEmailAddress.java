package com.liquidsys.coco.client;

public class LmcEmailAddress {

    private String type;
    private String emailID;
    private String personalName;
    private String emailAddress;
    private String displayName;
    private String content;
    private String referencedID;

    public void setType(String t) { type = t; }
    public void setEmailID(String e) { emailID = e; }
    public void setReferencedID(String r) { referencedID = r; }
    public void setPersonalName(String p) { personalName = p; }
    public void setEmailAddress(String e) { emailAddress = e; }
    public void setDisplayName(String d) { displayName = d; }
    public void setContent(String c) { content = c; }

    public String getReferencedID() { return referencedID; }
    public String getType() { return type; }
    public String getEmailID() { return emailID; }
    public String getPersonalName() { return personalName; }
    public String getEmailAddress() { return emailAddress; }
    public String getDisplayName() { return displayName; }
    public String getContent() { return content; }

}