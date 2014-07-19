package org.jkff.ire.util;

public class WrappedBitSet implements Cloneable {
    public final static int ADDRESS_BITS_PER_WORD = 6;
    public final static int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;

    public static final long WORD_MASK = 0xffffffffffffffffL;

    private final long[] words;
    private final int offset;
    private final int numWords;
    private int numBits;

    public WrappedBitSet(final int numBits) {
        words = new long[wordIndex(numBits -1) + 1];
        this.offset = 0;
        this.numWords = words.length;
        this.numBits = numBits;
    }

    public WrappedBitSet(final long[] words, final int offset, final int numWords, final int numBits) {
        this.words = words;
        this.offset = offset;
        this.numWords = numWords;
    }

    private int wordIndex(final int bitIndex) {
        return offset + (bitIndex >> ADDRESS_BITS_PER_WORD);
    }

    public void set(final int bitIndex) {
        final int wordIndex = wordIndex(bitIndex);
    	words[wordIndex] |= (1L << bitIndex);
    }

    public void clear(final int bitIndex) {
        final int wordIndex = wordIndex(bitIndex);
        words[wordIndex] &= ~(1L << bitIndex);
    }

    public boolean get(final int bitIndex) {
	    final int wordIndex = wordIndex(bitIndex);
	    return ((words[wordIndex] & (1L << bitIndex)) != 0);
    }

    public int nextSetBit(final int fromIndex) {
        int u = (fromIndex >> ADDRESS_BITS_PER_WORD);
        if(u >= numWords)
            return -1;
    	long word = words[offset+u] & (WORD_MASK << fromIndex);
        while (true) {
            if (word != 0)
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            if (++u == numWords)
                return -1;
            word = words[offset+u];
        }
    }

    public static int nextSetBit(final long[] words, final int offset, final int length, final int fromIndex) {
        int u = (fromIndex >> ADDRESS_BITS_PER_WORD);
        if(u >= length)
            return -1;
    	long word = words[offset + u] & (WORD_MASK << fromIndex);
        while (true) {
            if (word != 0)
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            if (++u == length)
                return -1;
            word = words[offset + u];
        }
    }

    public int length() {
        return numWords;
    }

    public boolean isEmpty() {
        for(int i = offset; i < offset + numWords; ++i) {
            if(words[i] != 0)
                return false;
        }
        return true;
    }

    public int cardinality() {
        int sum = 0;
        for(int i = offset; i < offset + numWords; ++i)  {
            sum += Long.bitCount(words[i]);
        }
        return sum;
    }

    public void and(final WrappedBitSet set) {
        for (int i = 0; i < numWords; ++i)
            words[offset+i] &= set.words[set.offset+i];
    }

    public void or(final WrappedBitSet set) {
        for (int i = 0; i < numWords; ++i)
            words[offset+i] |= set.words[set.offset+i];
    }

    public String toString() {
        final StringBuilder b = new StringBuilder();

        b.append('{');
        int i = nextSetBit(0);
        if (i != -1) {
            b.append(i);
            for (i = nextSetBit(i+1); i >= 0; i = nextSetBit(i+1)) {
                b.append(", ").append(i);
            }
        }

        b.append('}');
        return b.toString();
    }

    public WrappedBitSet makeCopy() {
        final long[] words = new long[numWords];
        System.arraycopy(this.words, offset, words, 0, numWords);
        return new WrappedBitSet(words, 0, words.length, numBits);
    }

    public int numBits() {
        return numBits;
    }
}
