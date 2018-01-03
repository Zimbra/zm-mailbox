package com.zimbra.cs.event.analytics;

import com.zimbra.common.util.Pair;
import com.zimbra.cs.event.analytics.IncrementableMetric.Increment;

/**
 * Used for EventMetrics representing ratios.
 */
public class RatioMetric extends Pair<Double, Integer> implements IncrementableMetric<Double, RatioMetric.RatioIncrement> {

    public RatioMetric(Double numerator, Integer denominator) {
        super(numerator, denominator);
    }

    protected Double getNumerator() {
        return getFirst();
    }

    protected Integer getDenominator() {
        return getSecond();
    }

    protected void setNumerator(Double numerator) {
        setFirst(numerator);
    }

    protected void setDenominator(int denominator) {
        setSecond(denominator);
    }

    @Override
    public void increment(RatioIncrement increment) {
        setNumerator(getNumerator() + increment.getNumeratorInc());
        setDenominator(getDenominator() + increment.getDenominatorInc());
    }

    @Override
    public Double getValue() {
        if (getDenominator() == 0) {
            return 0d; //this should be sufficient; no need to introduce NaN handling for division by zero cases
        }
        return getNumerator() / getDenominator();
    }

    public static class RatioIncrement implements Increment {
        private Double numeratorInc;
        private int denominatorInc;

        public RatioIncrement(Double numInc, int denomInc) {
            this.numeratorInc =numInc;
            this.denominatorInc = denomInc;
        }

        public Double getNumeratorInc() {
            return numeratorInc;
        }

        public int getDenominatorInc() {
            return denominatorInc;
        }

        @Override
        public String toString() {
            return String.format("num: %s, den:%s", numeratorInc, denominatorInc);
        }
    }
}