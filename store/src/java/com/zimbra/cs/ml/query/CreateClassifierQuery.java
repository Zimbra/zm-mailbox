package com.zimbra.cs.ml.query;

import com.zimbra.cs.ml.schema.ClassifierInfo;
import com.zimbra.cs.ml.schema.ClassifierSpec;


/**
 * Create a classifier based on a ClassifierSpec
 */
public class CreateClassifierQuery extends MLQuery<ClassifierInfo> {

    private ClassifierSpec classifierSpec;

    public CreateClassifierQuery(ClassifierSpec classifierSpec) {
        this.classifierSpec = classifierSpec;
    }

    public ClassifierSpec getClassifierSpec() {
        return classifierSpec;
    }
}
