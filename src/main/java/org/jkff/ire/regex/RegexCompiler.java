package org.jkff.ire.regex;

import org.jkff.ire.DFARopePatternSet;
import org.jkff.ire.PatternSet;
import org.jkff.ire.fa.*;
import org.jkff.ire.util.CoarsestPartition;
import org.jkff.ire.util.WrappedBitSet;
import org.jkff.ire.util.Pair;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.jkff.ire.util.CollectionFactory.*;

/**
 * Created on: 01.09.2010 23:43:07
 */
public class RegexCompiler {
    public static PatternSet compile(final List<RxNode> roots) {
        return new DFARopePatternSet(compileToBiDFA(roots));
    }

    public static PatternSet compile(final String... regexes) {
        final List<RxNode> roots = newArrayList();
        for(final String regex : regexes) {
            roots.add(RegexParser.parse(regex));
        }
        return compile(roots);
    }

    static BiDFA<Character, PowerIntState> compileToBiDFA(final List<RxNode> roots) {
        final List<RxNode> rootsAnywhere = newArrayList();
        final List<RxNode> reversedRoots = newArrayList();
        for(final RxNode root : roots) {
            final Alternative dotStar = new Alternative(new Empty(), new OnceOrMore(CharacterClass.ANY_CHAR));
            rootsAnywhere.add(new Sequence(dotStar, new Sequence(root, dotStar)));
            reversedRoots.add(new Sequence(reverse(root), dotStar));
        }
        return new BiDFA<>(compileToDFA(rootsAnywhere), compileToDFA(reversedRoots));
    }

    static DFA<Character, PowerIntState> compileToDFA(final List<RxNode> rxNodes) {
        if(rxNodes.isEmpty()) {
            throw new IllegalArgumentException("Pattern list can't be empty");
        }
        final List<RxNode> labeled = newArrayList();
        for(int i = 0; i < rxNodes.size(); ++i) {
            labeled.add(new Labeled(rxNodes.get(i), i));
        }
        RxNode alt = labeled.get(0);
        for(int i = 1; i < rxNodes.size(); ++i) {
            alt = new Alternative(alt, labeled.get(i));
        }

        return toDFA(reduceNFA(toNFA(alt)), rxNodes.size());
    }

    static DFA<Character, PowerIntState> toDFA(final NFA nfa, final int numPatterns) {
        final Pair<Set<NFA.Node>, NFA.Node> opt = optimize(nfa);

        final Set<NFA.Node> allNodes = opt.first;
        final NFA.Node newInitial = opt.second;

        final int numStates = allNodes.size();

        final Map<NFA.Node, Integer> node2id = newLinkedHashMap();
        final NFA.Node[] id2node = allNodes.toArray(new NFA.Node[allNodes.size()]);

        for(int i = 0; i < id2node.length; ++i) {
            node2id.put(id2node[i], i);
        }

        final State[] basis = new State[numStates];
        for(int i = 0; i < numStates; ++i) {
            final WrappedBitSet terminatedPatterns = new WrappedBitSet(numPatterns);
            id2node[i].patternIds.forEach(terminatedPatterns::set);
            basis[i] = new IntState(i, terminatedPatterns);
        }

        final TransferTable<Character,PowerIntState> transfer = new TransferTable<Character, PowerIntState>() {
            private final TransferFunction[] transfer = new TransferFunction[Character.MAX_VALUE+1];

            public TransferFunction<PowerIntState> forToken(final Character token) {
                final char t = token;
                TransferFunction<PowerIntState> f = transfer[t];
                if(f == null) {
                    transfer[t] = f = computeTransferFor(t);
                }
                return f;
            }

            private TransferFunction<PowerIntState> computeTransferFor(final char token) {
                final WrappedBitSet[] state2next = new WrappedBitSet[numStates];
                for(int i = 0; i < numStates; ++i) {
                    final WrappedBitSet res = new WrappedBitSet(numStates);
                    final NFA.Node node = id2node[i];
                    node.out.stream().filter(out -> out.first.acceptsChar(token)).forEach(out -> {
                        res.set(node2id.get(out.second));
                    });
                    state2next[i] = res;
                }
                return new PowerIntTable(state2next);
            }
        };

        final WrappedBitSet justInitial = new WrappedBitSet(numStates);
        justInitial.set(node2id.get(newInitial));
        final PowerIntState initial = new PowerIntState(basis, justInitial);

//        StringBuilder dot = new StringBuilder();
//        dot.append("digraph g {\n");
//        for(int i = 0; i < numStates; ++i) {
//            WrappedBitSet justThis = new WrappedBitSet(numStates);
//            justThis.set(i);
//            PowerIntState state = new PowerIntState(basis, justThis);
//            dot.append(i + " [shape=" + (state.getTerminatedPatterns().isEmpty() ? "circle" : "square") + "]\n");
//        }
//        for(int i = 0; i < numStates; ++i) {
//            WrappedBitSet justThis = new WrappedBitSet(numStates);
//            justThis.set(i);
//            PowerIntState state = new PowerIntState(basis, justThis);
//            PowerIntState nextState = transfer.forToken('t').next(state);
//            WrappedBitSet next = nextState.getSubset();
//            for(int bit = next.nextSetBit(0); bit != -1; bit = next.nextSetBit(bit+1)) {
//                dot.append(i + " -> " + bit + "\n");
//            }
//        }
//        dot.append("}\n");
//        System.out.println(dot);

        return new DFA<Character, PowerIntState>(transfer, initial, PowerIntTable.REDUCER) {
            @Override
            public PowerIntState resetTerminatedPattern(final PowerIntState state, final int pattern) {
                final WrappedBitSet reset = new WrappedBitSet(basis.length);
                reset.or(state.getSubset());
                for(int substate = reset.nextSetBit(0); substate != -1; substate = reset.nextSetBit(substate + 1)) {
                    if(basis[substate].getTerminatedPatterns().get(pattern)) {
                        reset.clear(substate);
                    }
                }
                reset.or(justInitial);
                return new PowerIntState(basis, reset);
            }
        };
    }

    private static Pair<Set<NFA.Node>, NFA.Node> optimize(final NFA nfa) {
        final Pair<Set<NFA.Node>, NFA.Node> eClosure = computeEClosure(nfa);
        final Pair<Set<NFA.Node>, NFA.Node> groupedLeft = groupEquivalentStates(eClosure, true);
        final Pair<Set<NFA.Node>, NFA.Node> groupedRight = groupEquivalentStates(groupedLeft, false);

        return groupedRight;
    }

    private static Pair<Set<NFA.Node>, NFA.Node> groupEquivalentStates(
            final Pair<Set<NFA.Node>, NFA.Node> nfa, final boolean leftNotRight)
    {
        // See paper "On NFA reductions".
        final Set<NFA.Node> nodes = nfa.first;
        final NFA.Node initial = nfa.second;

        // Nodes terminating different patterns are different.
        final Map<Set<Integer>, Integer> patIds2block = newLinkedHashMap();
        final Map<NFA.Node, Integer> node2block = newLinkedHashMap();
        for(final NFA.Node node : nodes) {
            Integer block = patIds2block.get(node.patternIds);
            if(block == null) {
                patIds2block.put(node.patternIds, block = patIds2block.size());
            }
            node2block.put(node, block);
        }

        final NFA.Node[] id2node = nodes.toArray(new NFA.Node[nodes.size()]);
        final Map<NFA.Node, Integer> node2id = newLinkedHashMap();
        for(int i = 0; i < id2node.length; ++i) {
            node2id.put(id2node[i], i);
        }

        int[] p = new int[id2node.length];
        for(int i = 0; i < id2node.length; ++i) {
            p[i] = node2block.get(id2node[i]);
        }

        // Instead of iterating over the whole unicode alphabet,
        // let us iterate over the distinct labels of the automaton.
        final Set<CharacterClass> alphabet = newLinkedHashSet();
        for(final NFA.Node node : nodes) {
            alphabet.addAll(node.out.stream().map(out -> out.first).collect(Collectors.toList()));
        }

        boolean anythingChanged;
        do {
            anythingChanged = false;
            for(final CharacterClass c : alphabet) {
                final List<int[]> edges = newArrayList();
                for(int i = 0; i < id2node.length; ++i) {
                    final NFA.Node node = id2node[i];
                    for (final Pair<CharacterClass, NFA.Node> out : node.out) {
                        // When splitting by a particular label, say, [agc],
                        // we should take into account all edges that might
                        // be triggered by any of the characters accepted
                        // by this label. For example, a "." edge should be used.
                        if(out.first.intersects(c)) {
                            final int j = node2id.get(out.second);
                            edges.add(leftNotRight ? new int[] {i,j} : new int[] {j, i});
                        }
                    }
                }
                final int[] newP = CoarsestPartition.coarsestStablePartition(p, edges.toArray(new int[edges.size()][]));
                if(!Arrays.equals(p, newP)) {
                    anythingChanged = true;
                    p = newP;
                }
            }
        } while(anythingChanged);

        // Group nodes of the nfa according to 'p'.
        final Map<Integer, NFA.Node> block2newNode = newLinkedHashMap();
        final Map<Integer,List<Integer>> block2oldNodeIds = newLinkedHashMap();
        for (int i = 0; i < p.length; i++) {
            final int b = p[i];
            if (!block2newNode.containsKey(b)) {
                final NFA.Node newNode = new NFA.Node();
                block2newNode.put(b, newNode);
                block2oldNodeIds.put(b, new ArrayList<>());
            }
            block2oldNodeIds.get(b).add(i);
        }
        for(final int b : block2newNode.keySet()) {
            final NFA.Node newNode = block2newNode.get(b);
            for(final int oldNodeId : block2oldNodeIds.get(b)) {
                final NFA.Node oldNode = id2node[oldNodeId];
                newNode.patternIds.addAll(oldNode.patternIds);
                for (final Pair<CharacterClass, NFA.Node> out : oldNode.out) {
                    final CharacterClass cc = out.first;
                    final NFA.Node dest = out.second;
                    final NFA.Node newDest = block2newNode.get(p[node2id.get(dest)]);
                    final Pair<CharacterClass, NFA.Node> edge = Pair.of(cc, newDest);
                    if(!newNode.out.contains(edge))
                        newNode.out.add(edge);
                }
            }
        }
        final NFA.Node newInitial = block2newNode.get(p[node2id.get(initial)]);
        final Set<NFA.Node> newNodes = new HashSet<>(block2newNode.values());
        return Pair.of(newNodes, newInitial);
    }

    private static Pair<Set<NFA.Node>, NFA.Node> computeEClosure(final NFA nfa) {
        final Map<NFA.Node, Set<NFA.Node>> node2closure = newLinkedHashMap();

        final Set<NFA.Node> allOldNodes = dfs(nfa.begin, true);
        for(final NFA.Node node : allOldNodes) {
            node2closure.put(node, dfs(node, false));
        }

        final Map<NFA.Node, Set<NFA.Node>> newNode2contents = newLinkedHashMap();
        final Map<Set<NFA.Node>, NFA.Node> contents2newNode = newLinkedHashMap();
        final Set<NFA.Node> newNodesToVisit = newLinkedHashSet();
        final Set<NFA.Node> initialEC = node2closure.get(nfa.begin);
        final NFA.Node newInitial = new NFA.Node();
        for(final NFA.Node subNode : initialEC) {
            newInitial.patternIds.addAll(subNode.patternIds);
        }
        newNodesToVisit.add(newInitial);
        newNode2contents.put(newInitial, initialEC);
        contents2newNode.put(initialEC, newInitial);
        while(!newNodesToVisit.isEmpty()) {
            final NFA.Node newNode = newNodesToVisit.iterator().next();
            newNodesToVisit.remove(newNode);
            final Map<CharacterClass, Set<NFA.Node>> class2dest = newLinkedHashMap();
            for(final NFA.Node subNode : newNode2contents.get(newNode)) {
                for(final Pair<CharacterClass, NFA.Node> out : subNode.out) {
                    if(out.first == null) {
                        // Skip epsilon transitions: we're operating on epsilon closures
                        continue;
                    }
                    Set<NFA.Node> dest = class2dest.get(out.first);
                    if(dest == null) {
                        class2dest.put(out.first, dest = newLinkedHashSet());
                    }
                    dest.addAll(node2closure.get(out.second));
                }
            }
            for(final CharacterClass cc : class2dest.keySet()) {
                final Set<NFA.Node> dest = class2dest.get(cc);
                NFA.Node newDest = contents2newNode.get(dest);
                if(newDest == null) {
                    newDest = new NFA.Node();
                    for(final NFA.Node subNode : dest) {
                        newDest.patternIds.addAll(subNode.patternIds);
                    }
                    newNode2contents.put(newDest, dest);
                    contents2newNode.put(dest, newDest);
                    newNodesToVisit.add(newDest);
                }
                newNode.transition(cc, newDest);
            }
        }

        return Pair.of(newNode2contents.keySet(), newInitial);
    }

    static Set<NFA.Node> dfs(final NFA.Node origin, final boolean acceptNonEps) {
        final Set<NFA.Node> res = newLinkedHashSet();
        final Stack<NFA.Node> toVisit = new Stack<>();
        toVisit.add(origin);
        while(!toVisit.isEmpty()) {
            final NFA.Node node = toVisit.pop();
            if(!res.add(node))
                continue;
            node.out.stream().filter(out -> out.first == null || acceptNonEps).forEach(out -> {
                toVisit.push(out.second);
            });
        }
        return res;
    }

    static NFA reduceNFA(final NFA nfa) {
        //
        return nfa;
        // TODO
    }

    static NFA toNFA(final RxNode rxNode) {
        if(rxNode instanceof Alternative) {
            final Alternative x = (Alternative) rxNode;
            final NFA res = new NFA(new NFA.Node(), new NFA.Node());
            final NFA a = toNFA(x.a);
            final NFA b = toNFA(x.b);
            res.begin.transition(null, a.begin);
            res.begin.transition(null, b.begin);
            a.end.transition(null, res.end);
            b.end.transition(null, res.end);
            return res;
        } else if(rxNode instanceof CharacterClass) {
            final NFA res = new NFA(new NFA.Node(), new NFA.Node());
            res.begin.transition((CharacterClass) rxNode, res.end);
            return res;
        } else if(rxNode instanceof Empty) {
            final NFA res = new NFA(new NFA.Node(), new NFA.Node());
            res.begin.transition(null, res.end);
            return res;
        } else if(rxNode instanceof OnceOrMore) {
            final OnceOrMore x = (OnceOrMore) rxNode;
            final NFA res = toNFA(x.a);
            res.end.transition(null, res.begin);
            return res;
        } else if(rxNode instanceof Sequence) {
            final Sequence x = (Sequence) rxNode;
            final NFA a = toNFA(x.a);
            final NFA b = toNFA(x.b);
            final NFA res = new NFA(a.begin, b.end);
            a.end.transition(null, b.begin);
            return res;
        } else if(rxNode instanceof Labeled) {
            final Labeled x = (Labeled) rxNode;
            final NFA a = toNFA(x.a);
            a.end.patternIds.add(x.patternId);
            return a;
        } else {
            throw new UnsupportedOperationException("Unsupported node type " + rxNode.getClass());
        }
    }

    static class NFA {
        final Node begin, end;

        public NFA(final Node begin, final Node end) {
            this.begin = begin;
            this.end = end;
        }

        static class Node {
            static AtomicInteger nextId = new AtomicInteger(0);

            final List<Pair<CharacterClass, Node>> out = newArrayList();
            final Set<Integer> patternIds = newLinkedHashSet();
            final int id = nextId.incrementAndGet();

            void transition(final CharacterClass cc, final Node dest) {
                out.add(Pair.of(cc, dest));
            }

            public String toString() {
                return ""+id;
            }

            public boolean equals(final Object o) {
                return id == ((Node) o).id;
            }
            public int hashCode() {
                return id;
            }
        }
    }

    private static RxNode reverse(final RxNode rxNode) {
        if(rxNode instanceof Alternative) {
            final Alternative x = (Alternative) rxNode;
            return new Alternative(reverse(x.a), reverse(x.b));
        } else if(rxNode instanceof CharacterClass) {
            return rxNode;
        } else if(rxNode instanceof Empty) {
            return rxNode;
        } else if(rxNode instanceof OnceOrMore) {
            final OnceOrMore x = (OnceOrMore) rxNode;
            return new OnceOrMore(reverse(x.a));
        } else if(rxNode instanceof Sequence) {
            final Sequence x = (Sequence) rxNode;
            // The only interesting case.
            return new Sequence(reverse(x.b), reverse(x.a));
        } else if(rxNode instanceof Labeled) {
            final Labeled x = (Labeled) rxNode;
            return new Labeled(reverse(x.a), x.patternId);
        } else {
            throw new UnsupportedOperationException("Unsupported node type " + rxNode.getClass());
        }
    }
}
