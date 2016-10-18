package com.zimbra.cs.filter.jsieve;

public class ActionFileInto extends org.apache.jsieve.mail.ActionFileInto {

    private boolean copy;

    public ActionFileInto(String destination) {
        this(destination, false);
    }

    public ActionFileInto(String destination, boolean copy) {
        super(destination);
        this.copy = copy;
    }

    public boolean isCopy() {
        return copy;
    }

    public void setCopy(boolean copy) {
        this.copy = copy;
    }

}
