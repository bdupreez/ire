package org.jkff.ire;

import org.jkff.ire.fa.BiDFA;
import org.jkff.ire.fa.PowerIntState;
import org.jkff.ire.rope.RopeBasedIS;

/**
 * Created on: 01.09.2010 23:44:51
 */
public class DFARopePatternSet implements PatternSet {
    private final BiDFA<Character, PowerIntState> bidfa;

    public DFARopePatternSet(final BiDFA<Character, PowerIntState> bidfa) {
        this.bidfa = bidfa;
    }

    public IndexedString match(final String s) {
        return new RopeBasedIS<>(bidfa, s);
    }

    public IndexedString match(final String s, final int blockSize) {
        return new RopeBasedIS<>(bidfa, s, blockSize);
    }
}
