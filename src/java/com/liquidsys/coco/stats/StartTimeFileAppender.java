package com.liquidsys.coco.stats;

import org.apache.log4j.FileAppender;

public class StartTimeFileAppender extends FileAppender {
    
    public void setFile(String file) {
        long secondsSinceEpoch = System.currentTimeMillis() / 1000;
        file = file.replaceAll("%s", Long.toString(secondsSinceEpoch));
        super.setFile(file);
    }
}
