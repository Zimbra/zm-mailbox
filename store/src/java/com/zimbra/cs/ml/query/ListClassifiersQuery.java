package com.zimbra.cs.ml.query;

import java.util.List;

import com.zimbra.cs.ml.schema.ClassifierInfo;

/**
 * Query to return a list of all registered classifiers
 */
public class ListClassifiersQuery extends MLQuery<List<ClassifierInfo>> {

    public ListClassifiersQuery() {}
}
