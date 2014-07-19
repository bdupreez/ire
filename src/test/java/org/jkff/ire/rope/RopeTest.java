package org.jkff.ire.rope;

import org.jkff.ire.fa.Sequence;
import org.jkff.ire.util.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created on: 30.08.2010 23:04:07
 */
public class RopeTest {
    private static final Reducer<String> CONCAT = new Reducer<String>() {
        public String compose(final String a, final String b) {
            return a + b;
        }

        public String composeAll(final Sequence<String> ss) {
            final StringBuilder res = new StringBuilder();
            for(int i = 0; i < ss.length(); ++i) {
                res.append(ss.get(i));
            }
            return res.toString();
        }
    };
    private static final Function<Character, String> SINGLETON_STRING = c -> "" + c;
    private static final Function2<Integer,Rope<String>,Integer> ADD_LENGTH = (s, r) -> s + r.length();
    private static final Function2<Integer,Character,Integer> INCREMENT = (s, character) -> s + 1;

    @Test
    public void testToFromString() {
        final RopeFactory<String> f = new RopeFactory<>(4, CONCAT, SINGLETON_STRING);
        assertEquals("abc", Rope.fromString(f, "abc").toString());
        assertEquals("abc", Rope.fromString(f, "abc").getSum());
        assertEquals("abcd", Rope.fromString(f, "abcd").toString());
        assertEquals("abcd", Rope.fromString(f, "abcd").getSum());
        assertEquals("abcde", Rope.fromString(f, "abcde").toString());
        assertEquals("abcde", Rope.fromString(f, "abcde").getSum());
        assertEquals("abcdefgh", Rope.fromString(f, "abcdefgh").toString());
        assertEquals("abcdefgh", Rope.fromString(f, "abcdefgh").getSum());
        assertEquals("abcdefghij", Rope.fromString(f, "abcdefghij").toString());
        assertEquals("abcdefghij", Rope.fromString(f, "abcdefghij").getSum());
        assertEquals("abcdefghijklmnopqrstuvwxyz", Rope.fromString(f, "abcdefghijklmnopqrstuvwxyz").toString());
        assertEquals("abcdefghijklmnopqrstuvwxyz", Rope.fromString(f, "abcdefghijklmnopqrstuvwxyz").getSum());
    }

    @Test
    public void testAppend() {
        final RopeFactory<String> f = new RopeFactory<>(4, CONCAT, SINGLETON_STRING);
        final String s = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";
        for (int i = 0; i < s.length(); ++i) {
            final Rope<String> part1 = Rope.fromString(f, s.substring(0, i));
            final Rope<String> part2 = Rope.fromString(f, s.substring(i));
            assertEquals(s, part1.append(part2).toString());
            assertEquals(s, part1.append(part2).getSum());
        }

        for (int a = 0; a < s.length(); ++a) {
            for (int b = a; b < s.length(); ++b) {
                for (int c = b; c < s.length(); ++c) {
                    final String[] parts = new String[]{
                            s.substring(0, a),
                            s.substring(a, b),
                            s.substring(b, c),
                            s.substring(c),
                    };
                    final Rope[] ropes = new Rope[]{
                            Rope.fromString(f, parts[0]),
                            Rope.fromString(f, parts[1]),
                            Rope.fromString(f, parts[2]),
                            Rope.fromString(f, parts[3]),
                    };
                    assertEquals(s,
                            ropes[0].append(ropes[1]).append(ropes[2])
                                    .append(ropes[3]).toString());
                    assertEquals(s,
                            ropes[0].append(ropes[1]).append(ropes[2])
                                    .append(ropes[3]).getSum());
                }
            }
        }
    }

    @Test
    public void testSplitAfterRise() {
        final RopeFactory<String> f = new RopeFactory<>(4, CONCAT, SINGLETON_STRING);
        final String s = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";
        final Rope<String> r = Rope.fromString(f, s);

        for(int i = 0; i < s.length(); ++i) {
            final Pair<Rope<String>,Rope<String>> p = r.splitAfterRise(0, ADD_LENGTH, INCREMENT, greaterThan(i-1));
            assertEquals(i, p.first.length());
            assertEquals(s, p.first.append(p.second).toString());
        }
        assertNull(r.splitAfterRise(0, ADD_LENGTH, INCREMENT, greaterThan(s.length())));
    }

    @Test
    public void testSplitAfterBackRise() {
        final RopeFactory<String> f = new RopeFactory<>(4, CONCAT, SINGLETON_STRING);
        final String s = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";
        final Rope<String> r = Rope.fromString(f, s);

        for(int i = 0; i < s.length(); ++i) {
            final Pair<Rope<String>,Rope<String>> p = r.splitAfterBackRise(0, ADD_LENGTH, INCREMENT, greaterThan(i-1));
            assertEquals(i, p.second.length());
            assertEquals(s, p.first.append(p.second).toString());
        }
        assertNull(r.splitAfterBackRise(0, ADD_LENGTH, INCREMENT, greaterThan(s.length())));
    }

    private static Predicate<Integer> greaterThan(final int x) {
        return i -> i > x;
    }
}
