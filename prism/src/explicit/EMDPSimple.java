package explicit;

import prism.PrismException;
import prism.PrismLog;

import java.util.BitSet;

public class EMDPSimple extends EMDPExplicit implements ModelSimple {

    // TODO some sort of energy-distribution? where some maps *may* be labelled with energy costs
    //  analogous to the Distribution in the MDP implementation

    /**
     * Constructor: empty EMDP.
     */
    public EMDPSimple() {
        // TODO
    }

    /**
     * Constructor: new EMDP with fixed number of states.
     */
    public EMDPSimple(int numStates) {
        // TODO
    }

    /**
     * Construct an EMDP from an existing one and a state index permutation,
     * i.e. in which state index i becomes index permut[i].
     */
    public EMDPSimple(EMDPSimple emdp, int[] permut) {
        // TODO
    }

    @Override
    public void clearState(int i) {
        // TODO
    }

    @Override
    public int addState() {
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
