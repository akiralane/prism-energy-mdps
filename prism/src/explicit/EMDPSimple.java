package explicit;

import prism.PrismException;
import prism.PrismLog;
import prism.PrismPrintStreamLog;

import java.util.BitSet;
import java.util.List;

public class EMDPSimple extends EMDPExplicit implements ModelSimple {

    protected PrismLog mainLog = new PrismPrintStreamLog(System.out);

    protected int numTransitions;
    protected List<Integer> initialStates;
    protected List<TransitionList> transitionLists;

    /**
     * Constructor: Empty EMDP.
     */
    public EMDPSimple() {
        initialise(0);
    }

    /**
     * Constructor: EMDP with fixed number of states.
     */
    public EMDPSimple(int numStates) {
        super(numStates);
        initialise(numStates);
    }

    /**
     * Construct an EMDP from an existing one and a state index permutation,
     * i.e. in which state index i becomes index permut[i].
     */
    public EMDPSimple(EMDPSimple emdp, int[] permut) {
        super(emdp, permut);
        initialise(emdp.numStates);
        copyFrom(emdp, permut);
        // TODO - finish implementation
    }

    @Override
    public void initialise(int numStates) {
        super.initialise(numStates);
        // TODO populate our transition function
    }

    @Override
    public void clearState(int i) {
        // TODO
    }

    @Override
    public int addState() {
        // TODO
        return 0;
    }

    @Override
    public void addStates(int numToAdd) {
        // TODO
    }

    @Override
    public SuccessorsIterator getSuccessors(int s) {
        // TODO
        return null;
    }

    @Override
    public void findDeadlocks(boolean fix) throws PrismException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportToPrismExplicitTra(PrismLog log, int precision) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportToPrismLanguage(String filename, int precision) throws PrismException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void buildFromPrismExplicit(String filename) throws PrismException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkForDeadlocks(BitSet except) throws PrismException {
        throw new UnsupportedOperationException();
    }
}
