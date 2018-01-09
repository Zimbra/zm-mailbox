package com.zimbra.cs.ml.query;

import com.zimbra.cs.ml.schema.ClassifierInfo;

public class GetClassifierQuery extends MLQuery<ClassifierInfo> {

    private String classifierId;

    public GetClassifierQuery(String classifierId) {
        this.classifierId = classifierId;
    }

    public String getClassifierId() {
        return classifierId;
    }
}
