package com.zimbra.cs.filter.jsieve;

public class ActionRedirect extends org.apache.jsieve.mail.ActionRedirect {

    private boolean copy;

    public ActionRedirect(String address) {
        super(address);
    }

    public ActionRedirect(String address, boolean copy) {
        super(address);
        this.setCopy(copy);
    }

    public boolean isCopy() {
        return copy;
    }

    public void setCopy(boolean copy) {
        this.copy = copy;
    }

}
