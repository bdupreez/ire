package org.jkff.ire.fa;

/**
 * Created on: 25.07.2010 13:34:11
 */
public class BiDFA<C, ST extends State> {
    private final DFA<C, ST> forward;
    private final DFA<C, ST> backward;

    public BiDFA(final DFA<C, ST> forward, final DFA<C, ST> backward) {
        this.forward = forward;
        this.backward = backward;
    }

    public DFA<C, ST> getForward() {
        return forward;
    }

    public DFA<C, ST> getBackward() {
        return backward;
    }
}
