package org.jkff.ire.regex;

/**
 * Created on: 01.09.2010 23:42:24
 */
public class Alternative implements RxNode {
    public final RxNode a;
    public final RxNode b;

    public Alternative(final RxNode a, final RxNode b) {
        this.a = a;
        this.b = b;
    }
}
