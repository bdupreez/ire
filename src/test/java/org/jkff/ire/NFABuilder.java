package org.jkff.ire;

import org.jkff.ire.fa.*;
import org.jkff.ire.util.WrappedBitSet;

import java.util.List;

import static org.jkff.ire.util.CollectionFactory.newArrayList;

/**
 * Created on: 01.08.2010 13:47:21
 */
public class NFABuilder {
    private final IntState[] basisStates;
    private final int numPatterns;
    private final int initialState;
    private final List<Transition> transitions = newArrayList();

    public NFABuilder(final int numBasisStates, final int initialState, final int numPatterns) {
        this.basisStates = new IntState[numBasisStates];
        this.initialState = initialState;
        this.numPatterns = numPatterns;
    }

    public StateBuilder state(final int i, final int... termPatterns) {
        return new StateBuilder(i, termPatterns);
    }

    public DFA<Character, PowerIntState> build() {
        final WrappedBitSet[][] char2state2next = new WrappedBitSet[256][basisStates.length];
        for(int i = 0; i < 256; ++i)
            for(int j = 0; j < basisStates.length; ++j)
                char2state2next[i][j] = new WrappedBitSet(basisStates.length);

        transitions.stream().filter(t -> t.c != null).forEach(t -> {
            char2state2next[t.c][t.from].set(t.to);
        });
        for(final Transition t : transitions) {
            if(t.c == null) {
                for(int i = 0; i < 256; ++i)
                    if(char2state2next[i][t.from].isEmpty())
                        char2state2next[i][t.from].set(t.to);
            }
        }
        final TransferTable<Character, PowerIntState> transfer = token -> new PowerIntTable(char2state2next[token]);

        final WrappedBitSet justInitial = new WrappedBitSet(basisStates.length);
        justInitial.set(initialState);
        return new DFA<Character, PowerIntState>(transfer,
                new PowerIntState(basisStates, justInitial), PowerIntTable.REDUCER)
        {
            @Override
            public PowerIntState resetTerminatedPattern(final PowerIntState state, final int pattern) {
                final WrappedBitSet reset = new WrappedBitSet(basisStates.length);
                reset.or(state.getSubset());
                for(int substate = reset.nextSetBit(0); substate != -1; substate = reset.nextSetBit(substate + 1)) {
                    if(basisStates[substate].getTerminatedPatterns().get(pattern)) {
                        reset.clear(substate);
                    }
                }
                reset.or(justInitial);
                return new PowerIntState(basisStates, reset);
            }
        };
    }

    public class StateBuilder {
        private final int state;
        private final int[] termPatterns;

        public StateBuilder(final int state, final int... termPatterns) {
            this.state = state;
            this.termPatterns = termPatterns;
        }

        public void transitions(final Object... char2state) {
            for(int i = 0; i < char2state.length; i += 2) {
                transitions.add(new Transition(this.state, (Character)char2state[i], (Integer)char2state[i+1]));
            }
            final WrappedBitSet t = new WrappedBitSet(numPatterns);
            for(final int tp : termPatterns)
                t.set(tp);
            basisStates[state] = new IntState(state, t);
        }
    }

    private static class Transition {
        int from;
        Character c;
        int to;

        private Transition(final int from, final Character c, final int to) {
            this.from = from;
            this.c = c;
            this.to = to;
        }
    }
}