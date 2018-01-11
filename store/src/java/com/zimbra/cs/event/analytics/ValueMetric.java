package com.zimbra.cs.event.analytics;


/**
 * IncrementableMetric to be used when the underlying data is an integer
 */
public class ValueMetric implements IncrementableMetric<Integer, ValueMetric.IntIncrement> {
    private int val;

    public ValueMetric(int val) {
        this.val = val;
    }
    @Override
    public Integer getValue() {
        return val;
    }

    @Override
    public void increment(IntIncrement increment) {
        val += increment.getIncrement();
    }

    public static class IntIncrement implements IncrementableMetric.Increment {
        private int inc;

        public IntIncrement(int inc) {
            this.inc = inc;
        }

        private int getIncrement() {
            return inc;
        }

        @Override
        public String toString() {
            return String.valueOf(inc);
        }
    }
}