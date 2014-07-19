package org.jkff.ire.regex;

/**
 * Created on: 04.09.2010 13:12:26
 */
public class RegexParser {
    public static RxNode parse(final String regex) {
        return parseAlt(new Tokenizer(regex));
    }

    private static boolean expect(final Tokenizer t, final char c) {
        final Character p = t.peek();
        return p != null && p == c;
    }

    private static RxNode parseAlt(final Tokenizer t) {
        final RxNode a = parseSequence(t);
        if (!expect(t, '|')) {
            return a;
        }
        t.next();
        final RxNode b = parseAlt(t);
        return new Alternative(a, b);
    }

    private static RxNode parseSequence(final Tokenizer t) {
        if(expect(t, '|') || expect(t, ')')) {
            return new Empty();
        }
        final RxNode a = parseUnary(t);
        if(expect(t, '|') || expect(t, ')') || t.peek() == null) {
            return a;
        }
        final RxNode b = parseSequence(t);
        return new Sequence(a, b);
    }

    private static RxNode parseUnary(final Tokenizer t) {
        RxNode a = parseAtom(t);
        while(true) {
            if(expect(t, '+')) {
                t.next();
                a = new OnceOrMore(a);
            } else if(expect(t, '?')) {
                t.next();
                a = new Alternative(new Empty(), a);
            } else if(expect(t, '*')) {
                t.next();
                a = new Alternative(new Empty(), new OnceOrMore(a));
            } else {
                return a;
            }
        }
    }

    private static RxNode parseAtom(final Tokenizer t) {
        if(expect(t, '(')) {
            t.next();
            return parseParen(t);
        } else if(expect(t, '[')) {
            t.next();
            return parseCharacterRange(t);
        } else if(expect(t, '.')) {
            t.next();
            return CharacterClass.ANY_CHAR;
        } else {
            return CharacterClass.oneOf(""+parseChar(t));
        }
    }

    private static RxNode parseParen(final Tokenizer t) {
        final RxNode a = parseAlt(t);
        if(!expect(t, ')')) {
            throw new IllegalArgumentException("Expected ')', got " + t.peek());
        }
        t.next();
        return a;
    }

    private static RxNode parseCharacterRange(final Tokenizer t) {
        final StringBuilder s = new StringBuilder();
        Character last = null;
        while(!expect(t, ']')) {
            Character c = parseChar(t);
            if(c != null && c == '-' && last != null) {
                c = parseChar(t);
                for(char i = last; i <= c; ++i) {
                    s.append(i);
                }
            } else {
                s.append(c);
            }
            last = c;
        }
        t.next();
        return CharacterClass.oneOf(s.toString());
    }

    private static Character parseChar(final Tokenizer t) {
        if(expect(t, '\\')) {
            t.next();
        }
        return t.next();
    }

    private static class Tokenizer {
        private final String s;
        private int pos;

        public Tokenizer(final String s) {
            this.s = s;
        }

        public Character peek() {
            return (pos < s.length()) ? s.charAt(pos) : null;
        }

        public Character next() {
            return (pos < s.length()) ? s.charAt(pos++) : null;
        }
    }
}
