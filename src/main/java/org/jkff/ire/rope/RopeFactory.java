package org.jkff.ire.rope;

import org.jkff.ire.fa.Sequence;
import org.jkff.ire.util.Function;
import org.jkff.ire.util.Reducer;

/**
 * Created on: 30.08.2010 22:42:16
 */
public class RopeFactory<M> {
    private final int blockSize;
    private final Reducer<M> reducer;
    private final Function<Character,M> map;

    public RopeFactory(final int blockSize, final Reducer<M> reducer, final Function<Character, M> map) {
        this.blockSize = blockSize;
        this.reducer = reducer;
        this.map = map;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public Reducer<M> getReducer() {
        return reducer;
    }

    public Function<Character, M> getMap() {
        return map;
    }

    public M mapReduce(final Sequence<Character> chars) {
        return reducer.composeAll(new Sequence<M>() {
            public int length() {
                return chars.length();
            }

            public M get(final int i) {
                return getMap().applyTo(chars.get(i));
            }
        });
    }
}
