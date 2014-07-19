package org.jkff.ire.fa;

import org.jkff.ire.util.WrappedBitSet;

/**
 * Created on: 31.07.2010 15:16:46
 */
public class IntState implements State {
    private final int index;
    private final WrappedBitSet terminatedPatterns;

    public IntState(final int index, final WrappedBitSet terminatedPatterns) {
        this.index = index;
        this.terminatedPatterns = terminatedPatterns;
    }

    public int getIndex() {
        return index;
    }

    public WrappedBitSet getTerminatedPatterns() {
        return terminatedPatterns;
    }
}
