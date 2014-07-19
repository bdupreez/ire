package org.jkff.ire;

import org.jkff.ire.fa.*;
import org.jkff.ire.util.WrappedBitSet;

import java.util.List;
import java.util.Map;

import static org.jkff.ire.util.CollectionFactory.newArrayList;
import static org.jkff.ire.util.CollectionFactory.newLinkedHashMap;

/**
 * Created on: 01.08.2010 13:47:21
 */
public class DFABuilder {
    private final IntState[] states;
    private final int numPatterns;
    private final int initialState;
    private final List<Transition> transitions = newArrayList();

    public DFABuilder(final int numStates, final int initialState, final int numPatterns) {
        this.states = new IntState[numStates];
        this.initialState = initialState;
        this.numPatterns = numPatterns;
    }

    public StateBuilder state(final int i, final int... termPatterns) {
        return new StateBuilder(i, termPatterns);
    }

    public DFA<Character, IntState> build() {
        final Map<Character,int[]> char2table = newLinkedHashMap();
        for(final Transition t : transitions) {
            int[] table = char2table.get(t.c);
            if(table == null)
                char2table.put(t.c, table = new int[states.length]);
            table[t.from] = t.to;
        }
        final int[][] char2state2next = new int[256][states.length];
        for(final Transition t : transitions) {
            if(t.c == null) {
                for(int i = 0; i < 256; ++i)
                    char2state2next[i][t.from] = t.to;
            }
        }
        transitions.stream().filter(t -> t.c != null).forEach(t -> {
            char2state2next[t.c][t.from] = t.to;
        });
        final TransferTable<Character, IntState> transfer = token -> new IntTable(states, char2state2next[token]);
        return new DFA<Character, IntState>(transfer, states[initialState], IntTable.REDUCER) {
            @Override
            public IntState resetTerminatedPattern(final IntState state, final int pattern) {
                return states[initialState];
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
            states[state] = new IntState(state, t);
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
