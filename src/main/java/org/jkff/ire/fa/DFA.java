package org.jkff.ire.fa;

import org.jkff.ire.util.Reducer;

/**
 * Created on: 22.07.2010 23:54:27
 */
public abstract class DFA<C, S extends State> {
    private final TransferTable<C,S> transfer;
    private final S initialState;
    private final Reducer<TransferFunction<S>> transferFunctionsReducer;

    public DFA(final TransferTable<C, S> transfer, final S initialState,
               final Reducer<TransferFunction<S>> transferFunctionsReducer)
    {
        this.transfer = transfer;
        this.initialState = initialState;
        this.transferFunctionsReducer = transferFunctionsReducer;
    }

    public S getInitialState() {
        return initialState;
    }

    public TransferFunction<S> transfer(final C token) {
        return transfer.forToken(token);
    }

    public Reducer<TransferFunction<S>> getTransferFunctionsReducer() {
        return transferFunctionsReducer;
    }

    public abstract S resetTerminatedPattern(S state, int pattern);
}
