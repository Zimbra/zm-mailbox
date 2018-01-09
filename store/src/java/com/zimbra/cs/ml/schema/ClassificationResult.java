package com.zimbra.cs.ml.schema;

/**
 * The result of classifying an email
 */
public class ClassificationResult {

    private String textUrl;
    private String exclusiveClass;
    private OverlappingClassification[] overlappingClasses;

    public String getUrl() {
        return textUrl;
    }
    public void setUrl(String textUrl) {
        this.textUrl = textUrl;
    }

    public OverlappingClassification[] getOverlappingClasses() {
        return overlappingClasses;
    }

    public void setOverlappingClasses(OverlappingClassification[] overlappingClasses) {
        this.overlappingClasses = overlappingClasses;
    }

    public String getExclusiveClass() {
        return exclusiveClass;
    }

    public void setExclusiveClass(String exclusiveClass) {
        this.exclusiveClass = exclusiveClass;
    }
}
