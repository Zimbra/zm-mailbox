/*
 * Created on Nov 2, 2004
 *
 */
package com.zimbra.cs.filter.jsieve;

import org.apache.jsieve.mail.Action;

/**
 * @author kchen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ActionTag implements Action {

    private String mTagName;
    
    public ActionTag(String tagName) {
        mTagName = tagName;
    }
    /**
     * @return Returns the tagName.
     */
    public String getTagName() {
        return mTagName;
    }
    /**
     * @param tagName The tagName to set.
     */
    public void setTagName(String tagName) {
        this.mTagName = tagName;
    }
    
    public String toString() {
        return "ActionTag, tag=" + mTagName;
    }
}
