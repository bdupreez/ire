package org.jkff.ire.rope;

import org.jkff.ire.DFABuilder;
import org.jkff.ire.Match;
import org.jkff.ire.NFABuilder;
import org.jkff.ire.fa.BiDFA;
import org.jkff.ire.fa.IntState;
import org.jkff.ire.fa.PowerIntState;
import org.junit.Test;

import java.util.List;

import static org.jkff.ire.util.CollectionFactory.newArrayList;
import static org.junit.Assert.assertEquals;

/**
 * Created on: 31.08.2010 8:20:00
 */
public class RopeBasedISTest {
    @Test
    public void testABConDFA() {
        final DFABuilder forward = new DFABuilder(4, 0, 1);
        forward.state(0).transitions('a', 1, null, 0);
        forward.state(1).transitions('b', 2, null, 0);
        forward.state(2).transitions('c', 3, null, 0);
        forward.state(3, 0).transitions(null, 3);

        final DFABuilder backward = new DFABuilder(4, 0, 1);
        backward.state(0).transitions('c', 1, null, 0);
        backward.state(1).transitions('b', 2, null, 0);
        backward.state(2).transitions('a', 3, null, 0);
        backward.state(3, 0).transitions(null, 3);

        final BiDFA<Character,IntState> bidfa = new BiDFA<>(forward.build(), backward.build());
        final int blockSize = 3;
        final RopeBasedIS<?> is = new RopeBasedIS<>(bidfa, "xxxcabccccc", blockSize);

        final List<Match> matches = newArrayList();
        for(final Match m : is.getMatches()) {
            matches.add(m);
        }
        assertEquals(1, matches.size());
        assertEquals(3, matches.get(0).length());
        assertEquals(0, matches.get(0).whichPattern());
        assertEquals(4, matches.get(0).startPos());
    }

    @Test
    public void testABConNFA() {
        final NFABuilder forward = new NFABuilder(4, 0, 1);
        forward.state(0).transitions('a', 1, null, 0);
        forward.state(1).transitions('b', 2, null, 0);
        forward.state(2).transitions('c', 3, null, 0);
        forward.state(3, 0).transitions(null, 3);

        final NFABuilder backward = new NFABuilder(4, 0, 1);
        backward.state(0).transitions('c', 1, null, 0);
        backward.state(1).transitions('b', 2, null, 0);
        backward.state(2).transitions('a', 3, null, 0);
        backward.state(3, 0).transitions(null, 3);

        final BiDFA<Character, PowerIntState> bidfa = new BiDFA<>(
                forward.build(), backward.build());
        final int blockSize = 3;
        final RopeBasedIS<?> is = new RopeBasedIS<>(bidfa, "xxxcabccccc", blockSize);

        final List<Match> matches = newArrayList();
        for(final Match m : is.getMatches()) {
            matches.add(m);
        }
        assertEquals(1, matches.size());
        assertEquals(3, matches.get(0).length());
        assertEquals(0, matches.get(0).whichPattern());
        assertEquals(4, matches.get(0).startPos());
    }

    @Test
    public void testABorAConNFA() {
        final NFABuilder forward = new NFABuilder(5, 0, 2);
        forward.state(0).transitions('a', 1, 'a', 3, null, 0);
        forward.state(1).transitions('b', 2, null, 0);
        forward.state(2, 0).transitions(null, 2);
        forward.state(3).transitions('c', 4, null, 0);
        forward.state(4, 1).transitions(null, 4);

        final NFABuilder backward = new NFABuilder(5, 0, 2);
        backward.state(0).transitions('b', 1, 'c', 3, null, 0);
        backward.state(1).transitions('a', 2, null, 0);
        backward.state(2, 0).transitions(null, 2);
        backward.state(3).transitions('a', 4, null, 0);
        backward.state(4, 1).transitions(null, 4);

        final BiDFA<Character,PowerIntState> bidfa = new BiDFA<>(
                forward.build(), backward.build());
        final int blockSize = 3;
        final RopeBasedIS<?> is = new RopeBasedIS<>(bidfa, "xxxcabcacccc", blockSize);

        final List<Match> matches = newArrayList();
        for(final Match m : is.getMatches()) {
            matches.add(m);
        }
        assertEquals(2, matches.size());
        assertEquals(2, matches.get(0).length());
        assertEquals(0, matches.get(0).whichPattern());
        assertEquals(4, matches.get(0).startPos());

        assertEquals(2, matches.get(1).length());
        assertEquals(1, matches.get(1).whichPattern());
        assertEquals(7, matches.get(1).startPos());
    }
}
