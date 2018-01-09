package com.zimbra.cs.ml.query;

import com.zimbra.cs.ml.Classifiable;
import com.zimbra.cs.ml.schema.AbstractClassificationInput;
import com.zimbra.cs.ml.schema.ClassificationResult;

public abstract class AbstractClassificationQuery<T extends Classifiable> extends MLQuery<ClassificationResult> {

    protected AbstractClassificationInput<T> input;

    public AbstractClassificationQuery(AbstractClassificationInput<T> input) {
        this.input = input;
    }

    public AbstractClassificationInput<T> getInput() {
        return input;
    }
}
