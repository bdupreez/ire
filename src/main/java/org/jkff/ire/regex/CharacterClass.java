package org.jkff.ire.regex;

/**
 * Created on: 01.09.2010 23:47:14
 */
public abstract class CharacterClass implements RxNode {
    public abstract boolean acceptsChar(char c);

    public static CharacterClass ANY_CHAR = new CharacterClass() {
        @Override
        public boolean acceptsChar(final char c) {
            return true;
        }

        @Override
        public boolean intersects(final CharacterClass c) {
            return true;
        }

        public String toString() {
            return ".";
        }
    };

    public static CharacterClass oneOf(final String s) {
        return new OneOf(s);
    }

    public abstract boolean intersects(CharacterClass c);

    private static class OneOf extends CharacterClass {
        private final String s;

        public OneOf(final String s) {
            this.s = s;
        }

        @Override
        public boolean acceptsChar(final char c) {
            return s.indexOf(c) > -1;
        }

        @Override
        public boolean intersects(final CharacterClass c) {
            if(c instanceof OneOf) {
                final OneOf other = (OneOf) c;
                for(int i = 0; i < s.length(); ++i) {
                    final char ch = s.charAt(i);
                    if(other.s.indexOf(ch) != -1) {
                        return true;
                    }
                }
                return false;
            } else if(c == ANY_CHAR) {
                return true;
            } else {
                throw new UnsupportedOperationException();
            }
        }

        public String toString() {
            return "[" + s + "]";
        }

        public boolean equals(final Object other) {
            if(other == this) return true;
            if(other == null) return false;
            return other instanceof OneOf && s.equals(((OneOf) other).s);
        }

        public int hashCode() {
            return s.hashCode();
        }
    }
}
