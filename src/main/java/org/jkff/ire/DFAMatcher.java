package org.jkff.ire;

import org.jkff.ire.fa.BiDFA;
import org.jkff.ire.fa.State;
import org.jkff.ire.util.Function2;
import org.jkff.ire.util.Pair;
import org.jkff.ire.util.Predicate;

import org.jkff.ire.util.WrappedBitSet;
import java.util.List;

import static org.jkff.ire.util.CollectionFactory.newArrayList;

/**
 * Created on: 31.07.2010 12:19:28
 */
public class DFAMatcher {
    @SuppressWarnings("unchecked")
    public static <ST extends State>
        Iterable<Match> getMatches(
            final BiDFA<Character, ST> bidfa, final DFAIndexedString<ST> string)
    {
        final ST initial = bidfa.getForward().getInitialState();

        final Function2<SP<ST>, IndexedString, SP<ST>> addString = (sp, s) -> new SP<ST>(((DFAIndexedString<ST>) s).getForward().next(sp.state), sp.pos+s.length());

        final Function2<SP<ST>, Character, SP<ST>> addChar = (sp, c) -> new SP<ST>(bidfa.getForward().transfer(c).next(sp.state), sp.pos+1);

        final List<Match> res = newArrayList();

        final int shift = 0;

        SP<ST> matchStartState = new SP<>(initial, 0);
        IndexedString rem = string;
        IndexedString seen = string.subSequence(0,0);

        while(true) {
            final Pair<IndexedString, IndexedString> p = rem.splitAfterRise(
                    matchStartState, addString, addChar, DFAMatcher.<ST>hasForwardMatchAfter(shift));
            if(p == null)
                break;

            final DFAIndexedString<ST> matchingPrefix = (DFAIndexedString<ST>) p.first;
            rem = p.second;
            seen = seen.append(matchingPrefix);

            final ST stateAfterMatch = matchingPrefix.getForward().next(matchStartState.state);
            final WrappedBitSet term = stateAfterMatch.getTerminatedPatterns();

            final ST backwardInitial = bidfa.getBackward().getInitialState();

            ST nextMatchStart = stateAfterMatch;

            for(int bit = term.nextSetBit(0); bit >= 0; bit = term.nextSetBit(bit+1)) {
                final int bit2 = bit;

                final Function2<ST, IndexedString, ST> addStringBack = (st, s) -> ((DFAIndexedString<ST>) s).getBackward().next(st);

                final Function2<ST, Character, ST> addCharBack = (st, c) -> bidfa.getBackward().transfer(c).next(st);

                final Predicate<ST> startsThisMatch = state -> {
                    final WrappedBitSet tp = state.getTerminatedPatterns();
                    return tp!=null && tp.get(bit2);
                };

                final int len = seen.splitAfterBackRise(
                        backwardInitial, addStringBack, addCharBack, startsThisMatch).second.length();
                final int startPos = seen.length() - len;
                res.add(new Match(bit, startPos, len));

                nextMatchStart = bidfa.getForward().resetTerminatedPattern(nextMatchStart, bit);
            }

            matchStartState = new SP<>(nextMatchStart, matchingPrefix.length() + 1);
        }

        return res;
    }

    private static <ST extends State> Predicate<SP<ST>> hasForwardMatchAfter(final int pos) {
        return sp -> !sp.state.getTerminatedPatterns().isEmpty() && sp.pos >= pos;
    }

    // State and position.
    private static class SP<ST extends State> {
        ST state;
        int pos;

        SP(final ST state, final int pos) {
            this.state = state;
            this.pos = pos;
        }
    }
}
