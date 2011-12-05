package com.zimbra.qa;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class QA {
    
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Bug {
        int bug();
    }
}
