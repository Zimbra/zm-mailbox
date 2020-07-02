package com.zimbra.cs.ml.query;


/**
 * Delete a classifier by its ID
 */
public class DeleteClassifierQuery extends MLQuery<Boolean> {

    private String classifierId;

    public DeleteClassifierQuery(String classifierId) {
        this.classifierId = classifierId;
    }

    public String getClassifierId() {
        return classifierId;
    }
}
