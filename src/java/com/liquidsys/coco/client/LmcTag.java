package com.liquidsys.coco.client;

public class LmcTag {

	private String mName;

	private String mColor;

	private String mID;

	private long mUnreadCount;

	private boolean isUnreadCountValid; // true if unreadCount has been set

	public LmcTag(String id, String name, String color, long unreadCount) {
		mID = id;
		mName = name;
		mColor = color;
		if (unreadCount == -1) {
			isUnreadCountValid = false;
		} else {
			isUnreadCountValid = true;
			mUnreadCount = unreadCount;
		}
	}

	public LmcTag(String id, String name, String color) {
		this(id, name, color, -1);
	}
    
    public LmcTag() { }

	public void setID(String i) {
		mID = i;
	}

	public void setName(String n) {
		mName = n;
	}

	public void setColor(String c) {
		mColor = c;
	}

	public void setUnreadCount(int n) {
		mUnreadCount = n;
		isUnreadCountValid = true;
	}

	public String getID() {
		return mID;
	}

	public String getName() {
		return mName;
	}

	public String getColor() {
		return mColor;
	}

	/**
	 * @return the optional unread count.  Check to see if the unread count
	 * is valid before calling this method.  If not valid, this method will
	 * throw an IllegalStateException.
	 * @throws IllegalStateException
	 */
	public long getUnreadCount() throws IllegalStateException {
		if (!isUnreadCountValid)
			throw new IllegalStateException("Unread count not valid");
		else
			return mUnreadCount;
	}
    
    public String toString() {
        return "Name=\"" + mName + " \"id=\"" + mID + "\" color=\"" + mColor + 
               "\" unreadCount=\"" + mUnreadCount + "\"";
    }
}