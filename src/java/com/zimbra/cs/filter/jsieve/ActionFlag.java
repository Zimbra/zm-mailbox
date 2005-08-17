/*
 * Created on Nov 8, 2004
 */
package com.liquidsys.coco.filter.jsieve;

import org.apache.jsieve.mail.Action;


/**
 * @author kchen
 */
public class ActionFlag implements Action {
    private boolean set;
    private int flagId;
    private String name;
    
    /**
     * Constructs a flag action.
     * 
     * @param flag the flag
     */
    public ActionFlag(int flagId, boolean set, String name) {
        setFlag(flagId, set, name);
    }
    
    /**
     * @return Returns the flag.
     */
    public int getFlagId() {
        return flagId;
    }
    
    public boolean isSetFlag() {
        return set;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * @param flag The flag to set.
     */
    public void setFlag(int flagId, boolean set, String name) {
        this.flagId = flagId;
        this.set = set;
        this.name = name;
    }
    
    public String toString() {
        return "ActionFlag, " + (set ? "set" : "reset") + " flag " + name;
    }
}
