package com.liquidsys.coco.client;

public class LmcConversation {

    // attributes
    private String mID;
    private String mDate;
    private String mTags;
    private String mFlags;
    private String mFolder;
    private long mNumMsgs;

    // contained as elements inside the <c> element
    private String mFragment;
    private String mSubject;
    private LmcEmailAddress[] mParticipants;
    private LmcMessage[] mMsgs;

    public void setID(String id) { mID = id; }
    public void setDate(String d) { mDate = d; }
    public void setTags(String t) { mTags = t; }
    public void setFlags(String f) { mFlags = f; }
    public void setFolder(String f) { mFolder = f; }
    public void setNumMessages(long n) { mNumMsgs = n; }
    public void setSubject(String s) { mSubject = s; }
    public void setFragment(String f) { mFragment = f; }
    public void setParticipants(LmcEmailAddress e[]) { mParticipants = e; }
    public void setMessages(LmcMessage m[]) { mMsgs = m; }

    public String getDate() { return mDate; }
    public String getID() { return mID; }
    public String getTags() { return mTags; }
    public String getFlags() { return mFlags; }
    public String getSubject() { return mSubject; }
    public String getFragment() { return mFragment; }
    public String getFolder() { return mFolder; }
    public LmcEmailAddress[] getParticipants() { return mParticipants; }
    public LmcMessage[] getMessages() { return mMsgs; }

    private static String getCount(Object o[]) {
    	return (o == null) ? "0" : Integer.toString(o.length);
    }
    
    public String toString() {
    	return "Conversation: id=\"" + mID + "\" date=\"" + mDate + "\" tags=\"" + mTags +
            "\" flags=\"" + mFlags + "\" folder=\"" + mFolder + "\" numMsgs=\"" + mNumMsgs + 
			"\" subject=\"" + mSubject + "\" fragment=\"" + mFragment + "\" and " + 
            getCount(mParticipants) + " participants";
    }
}
