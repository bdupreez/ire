package org.jkff.ire.util;

import java.util.*;

/**
 * Created on: 13.10.2010 21:44:19
 */
public class CoarsestPartition {
    public static int[] coarsestStablePartition(int[] p, final int[][] edges) {
        // Find the coarsest refining 'q' of the partition 'p' such that
        // for every two blocks B1 and B2 of 'q', either in(B1) = B2,
        // or in(B1) is disjoint with B2.

        // p and q are specified as a mapping to equivalence classes (blocks).
        // edges is an array of 2-element arrays.

        // An extremely naive and inefficient implementation.
        // There exists an O(m log n) one, described in "Three partitioning algorithms"
        // by Paigue and Tarjan, 1987. However I'm too lazy to implement it,

        boolean anythingChanged;
        do {
            anythingChanged = false;
            // Refine p with respect to each block of p.
            final Map<Integer, List<Integer>> pBlocks = CollectionFactory.newLinkedHashMap();
            for (int i = 0; i < p.length; ++i) {
                List<Integer> block = pBlocks.get(p[i]);
                if (block == null) {
                    pBlocks.put(p[i], block = CollectionFactory.newArrayList());
                }
                block.add(i);
            }

            final Map<Integer, List<Integer>> ins = CollectionFactory.newLinkedHashMap();
            for (final int[] edge : edges) {
                final int src = edge[0];
                final int dst = edge[1];
                List<Integer> is = ins.get(dst);
                if (is == null) {
                    ins.put(dst, is = CollectionFactory.newArrayList());
                }
                is.add(src);
            }

            int[] q = Arrays.copyOf(p, p.length);
            for (final List<Integer> block : pBlocks.values()) {
                final Set<Integer> in = CollectionFactory.newLinkedHashSet();
                for (final int member : block) {
                    final List<Integer> curIn = ins.get(member);
                    if (curIn != null) in.addAll(curIn);
                }
                final int[] newQ = refine(q, in);
                if (!Arrays.equals(q, newQ)) {
                    anythingChanged = true;
                    q = newQ;
                }
            }
            p = q;
        } while (anythingChanged);
        return p;
    }

    private static int[] refine(final int[] q, final Set<Integer> s) {
        // Make it so that every block is either contained in S
        // or doesn't intersect it.
        int maxBlock = 0;
        for (final int c : q) maxBlock = Math.max(c, maxBlock);
        final Map<Integer, Integer> block2size = CollectionFactory.newLinkedHashMap();
        final Map<Integer, Integer> block2covered = CollectionFactory.newLinkedHashMap();
        for (int i = 0; i < q.length; ++i) {
            final int block = q[i];
            if (!block2size.containsKey(block)) block2size.put(block, 0);
            block2size.put(block, block2size.get(block) + 1);
            if (s.contains(i)) {
                if (!block2covered.containsKey(block)) block2covered.put(block, 0);
                block2covered.put(block, block2covered.get(block) + 1);
            }
        }

        final int[] res = Arrays.copyOf(q, q.length);

        for (final int block : block2size.keySet()) {
            if (block2covered.containsKey(block) && !block2covered.get(block).equals(block2size.get(block))) {
                // Split this block into S and B-S: Move S to a new block.
                final int newBlock = ++maxBlock;
                s.stream().filter(i -> q[i] == block).forEach(i -> res[i] = newBlock);
            }
        }

        return res;
    }
}
