package com.zimbra.cs.mailbox.im;

/**
 * @author tim
 *
 * Used to ensure the processor thread is woken up during shutdown 
 */
public class IMNullEvent implements IMEvent {

    public void run() {

    }

}
