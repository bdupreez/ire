package org.jkff.ire.rope;

import org.jkff.ire.DFAIndexedString;
import org.jkff.ire.*;
import org.jkff.ire.IndexedString;
import org.jkff.ire.fa.BiDFA;
import org.jkff.ire.fa.Sequence;
import org.jkff.ire.fa.State;
import org.jkff.ire.fa.TransferFunction;
import org.jkff.ire.util.*;

/**
 * Created on: 21.08.2010 21:10:19
 */
@SuppressWarnings("unchecked")
public class RopeBasedIS<ST extends State> implements DFAIndexedString<ST> {
    private static final int DEFAULT_BLOCK_SIZE = 128;

    private final BiDFA<Character,ST> bidfa;
    private final Rope<TransferFunctions<ST>> rope;

    public RopeBasedIS(final BiDFA<Character,ST> bidfa, final String value) {
        this(bidfa, value, DEFAULT_BLOCK_SIZE);
    }

    public RopeBasedIS(final BiDFA<Character,ST> bidfa, final String value, final int blockSize) {
        this(bidfa, Rope.fromString(
                new RopeFactory<>(
                        blockSize,
                        new TFProduct<>(
                                bidfa.getForward().getTransferFunctionsReducer(),
                                bidfa.getBackward().getTransferFunctionsReducer()),
                        new TFMap<>(bidfa)),
                value));
    }

    private RopeBasedIS(final BiDFA<Character,ST> bidfa, final Rope<TransferFunctions<ST>> rope) {
        this.bidfa = bidfa;
        this.rope = rope;
    }

    public TransferFunction<ST> getForward() {
        return rope.getSum().forward;
    }

    public TransferFunction<ST> getBackward() {
        return rope.getSum().backward;
    }

    public Iterable<Match> getMatches() {
        return DFAMatcher.getMatches(bidfa, this);
    }

    public Pair<IndexedString, IndexedString> splitBefore(final int index) {
        final Function2<Integer, Rope<TransferFunctions<ST>>, Integer> addRopeLength = (len, rope1) -> len + rope1.length();
        final Function2<Integer, Character, Integer> inc = (len, c) -> len + 1;
        final Predicate<Integer> isAfterIndex = x -> x >= index;
        final Pair<Rope<TransferFunctions<ST>>, Rope<TransferFunctions<ST>>> p =
                rope.splitAfterRise(0, addRopeLength, inc, isAfterIndex);
        return Pair.of(
                (IndexedString)new RopeBasedIS<>(bidfa, p.first),
                (IndexedString)new RopeBasedIS<>(bidfa, p.second));
    }

    public <T> Pair<IndexedString, IndexedString> splitAfterRise(
            final T seed,
            final Function2<T, IndexedString, T> addChunk,
            final Function2<T, Character, T> addChar,
            final Predicate<T> toBool)
    {
        final Pair<Rope<TransferFunctions<ST>>, Rope<TransferFunctions<ST>>> p = rope.splitAfterRise(
                seed, toRopeAddChunkFun(addChunk), addChar, toBool);
        return (p == null) ? null : Pair.of(
                (IndexedString) new RopeBasedIS<>(bidfa, p.first),
                (IndexedString) new RopeBasedIS<>(bidfa, p.second));
    }

    public <T> Pair<IndexedString, IndexedString> splitAfterBackRise(
            final T seed,
            final Function2<T, IndexedString, T> addChunk,
            final Function2<T, Character, T> addChar, final Predicate<T> toBool)
    {
        final Pair<Rope<TransferFunctions<ST>>, Rope<TransferFunctions<ST>>> p = rope.splitAfterBackRise(
                seed, toRopeAddChunkFun(addChunk), addChar, toBool);
        return (p == null) ? null : Pair.of(
                (IndexedString) new RopeBasedIS<>(bidfa, p.first),
                (IndexedString) new RopeBasedIS<>(bidfa, p.second));
    }

    private <T> Function2<T, Rope<TransferFunctions<ST>>, T> toRopeAddChunkFun(
            final Function2<T, IndexedString, T> addChunk) {
        return new Function2<T, Rope<TransferFunctions<ST>>, T>() {
            public T applyTo(final T st, final Rope<TransferFunctions<ST>> r) {
                return addChunk.applyTo(st, new RopeBasedIS<>(bidfa, r));
            }
        };
    }

    public RopeBasedIS<ST> append(final IndexedString s) {
        return new RopeBasedIS<>(bidfa, rope.append(((RopeBasedIS<ST>) s).rope));
    }

    public RopeBasedIS<ST> subSequence(final int start, final int end) {
        return (RopeBasedIS<ST>) splitBefore(start).second.splitBefore(end-start).first;
    }

    public int length() {
        return rope.length();
    }

    public char charAt(final int index) {
        return rope.charAt(index);
    }

    public String toString() {
        return rope.toString();
    }

    private static class TransferFunctions<ST> {
        TransferFunction<ST> forward;
        TransferFunction<ST> backward;

        private TransferFunctions(final TransferFunction<ST> forward, final TransferFunction<ST> backward) {
            this.forward = forward;
            this.backward = backward;
        }
    }
    private static class TFProduct<ST> implements Reducer<TransferFunctions<ST>> {
        private static final TransferFunction UNIT_TF = x -> x;
        private static final TransferFunctions UNIT = new TransferFunctions(UNIT_TF, UNIT_TF);

        private final Reducer<TransferFunction<ST>> forwardReducer;
        private final Reducer<TransferFunction<ST>> backwardReducer;

        private TFProduct(
                final Reducer<TransferFunction<ST>> forwardReducer,
                final Reducer<TransferFunction<ST>> backwardReducer)
        {
            this.forwardReducer = forwardReducer;
            this.backwardReducer = backwardReducer;
        }

        public TransferFunctions<ST> compose(final TransferFunctions<ST> a, final TransferFunctions<ST> b) {
            return new TransferFunctions<>(
                    a.forward==UNIT_TF ? b.forward : b.forward == UNIT_TF ? a.forward :
                    forwardReducer.compose(a.forward, b.forward),
                    a.backward==UNIT_TF ? b.backward : b.backward == UNIT_TF ? a.backward :
                    backwardReducer.compose(b.backward, a.backward));
        }

        public TransferFunctions<ST> composeAll(final Sequence<TransferFunctions<ST>> tfs) {
            if(tfs.length() == 0) {
                return UNIT;
            }

            final TransferFunction sumForward = forwardReducer.composeAll(new Sequence<TransferFunction<ST>>() {
                public int length() {
                    return tfs.length();
                }

                public TransferFunction<ST> get(final int i) {
                    return tfs.get(i).forward;
                }
            });

            final TransferFunction sumBackward = backwardReducer.composeAll(new Sequence<TransferFunction<ST>>() {
                public int length() {
                    return tfs.length();
                }

                public TransferFunction<ST> get(final int i) {
                    return tfs.get(length()-i-1).backward;
                }
            });

            return new TransferFunctions<ST>(sumForward, sumBackward);
        }

        public TransferFunctions<ST> unit() {
            return UNIT;
        }
    }

    private static class TFMap<ST extends State> implements Function<Character, TransferFunctions<ST>> {
        private final BiDFA<Character, ST> bidfa;

        private final TransferFunctions[] cache = new TransferFunctions[1 + Character.MAX_VALUE];

        private TFMap(final BiDFA<Character, ST> bidfa) {
            this.bidfa = bidfa;
        }

        public TransferFunctions<ST> applyTo(final Character ch) {
            final char c = ch;
            if(cache[c] == null) {
                cache[c] = new TransferFunctions<>(
                    bidfa.getForward().transfer(c),
                    bidfa.getBackward().transfer(c));
            }
            return cache[c];
        }
    }
}
