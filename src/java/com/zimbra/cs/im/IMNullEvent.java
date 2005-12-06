package com.zimbra.cs.im;

/**
 * @author tim
 *
 * Used to ensure the processor thread is woken up during shutdown 
 */
public class IMNullEvent extends IMEvent {
    
    IMNullEvent() {
        super((IMAddr)null);
    }

    public void run() { }
}
