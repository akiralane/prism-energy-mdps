package explicit;

import prism.ModelType;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismPrintStreamLog;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

public class EMDPSimple extends EMDPExplicit implements ModelSimple {

    protected PrismLog mainLog = new PrismPrintStreamLog(System.out);

    protected int numTransitions;
//    protected List<Integer> initialStates;
    protected List<TransitionList> transitions;

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
        for (int i = 0; i < numStates; i++) {
            transitions.set(
                    permut[i],
                    new TransitionList(emdp.transitions.get(i), permut));
        }

        numTransitions = emdp.getNumTransitions();
    }

    @Override
    public void initialise(int numStates) {
        super.initialise(numStates);
        transitions = new ArrayList<>(numStates);
        for (int i = 0; i < numStates; i++) {
            transitions.add(new TransitionList());
        }
    }

    public void addProbabilisticTransition(int index, int target, double probability)
    {
        if (!transitions.get(index).addProbabilisticTransition(target, probability))
        {
            numTransitions++;
        }
    }

    public void addEnergyTransition(int index, int target, double energy)
    {
        if (!transitions.get(index).addEnergyTransition(target, energy))
        {
            numTransitions++;
        }
    }

    @Override
    public void clearState(int i) {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds a new player 1 state.
     * @return The index of the added state.
     */
    @Override
    public int addState() {
        super.addState();
        addStates(1);
        return numStates - 1;
    }

    /**
     * Adds a number of player-1 states.
     * @param numToAdd _
     */
    @Override
    public void addStates(int numToAdd) {
        super.addStates(numToAdd);
        for (int i = 0; i < numToAdd; i++) {
            transitions.add(new TransitionList());
            numStates++;
        }
    }

    @Override
    public Iterator<Integer> getSuccessorsIterator(final int s)
    {
        return transitions.get(s).getSupport().iterator();
    }

    @Override
    public SuccessorsIterator getSuccessors(int s) {
        return SuccessorsIterator.from(getSuccessorsIterator(s), true);
    }

    /**
     * @return The number of transitions in the model.
     */
    @Override
    public int getNumTransitions() {
        return numTransitions;
    }

    /**
     * @param s The index of the state to check.
     * @return The number of transitions from state {@code s}.
     */
    @Override
    public int getNumTransitions(int s) {
        return transitions.get(s).size();
    }

    /**
     * @param s The index of the state to check.
     * @return The transitions from state {@code s}.
     */
    public TransitionList getTransitions(int s)
    {
        return transitions.get(s);
    }

    @Override
    public ModelType getModelType()
    {
        return ModelType.EMDP;
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
