package com.zimbra.cs.ml.schema;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.ml.Classifiable;
import com.zimbra.cs.ml.feature.ComputedFeatures;

public abstract class AbstractClassificationInput<T extends Classifiable> {

    protected List<Float> encodedFeatures;

    public AbstractClassificationInput() {
        encodedFeatures = new ArrayList<>();
    }

    public AbstractClassificationInput(ComputedFeatures<T> computedFeatures) throws ServiceException {
        this();
        init(computedFeatures);
    }

    protected abstract void init(ComputedFeatures<T> computedFeatures) throws ServiceException;
}
