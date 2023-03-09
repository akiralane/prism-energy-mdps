package explicit;

import explicit.graphviz.Decorator;
import prism.*;

import java.util.*;

public class EMDPSimple extends EMDPExplicit implements ModelSimple {

    protected PrismLog mainLog = new PrismPrintStreamLog(System.out);

    /** state index -> [(next state index, transition weight)] */
    protected List<TransitionList> transitions;
    protected int numTransitions;

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
     * Copy constructor: construct from existing EMDP.
     */
    public EMDPSimple(EMDPSimple emdp) {
        super(emdp);
        transitions = emdp.transitions;
        numTransitions = emdp.numTransitions;
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
    public void exportToDotFile(PrismLog out, Iterable<explicit.graphviz.Decorator> decorators, int precision)
    {
        explicit.graphviz.Decoration defaults = new explicit.graphviz.Decoration();
        defaults.attributes().put("shape", "circle");
        defaults.attributes().put("fontname", "Helvetica");

        // Header
        out.print("digraph " + getModelType() + " {\nnode " + defaults.toString() + ";\n");
        int i, numStates;
        for (i = 0, numStates = getNumStates(); i < numStates; i++) {
            // initialize
            explicit.graphviz.Decoration d = new explicit.graphviz.Decoration(defaults);
            d.setLabel(Integer.toString(i));
            d.attributes().put("shape", environmentPlayer == getPlayer(i) ? "circle" : "diamond");
            d.attributes().put("color", environmentPlayer == getPlayer(i) ? "black" : "red");
            // run any decorators
            if (decorators != null) {
                for (Decorator decorator : decorators) {
                    d = decorator.decorateState(i, d);
                }
            }

            String decoration = d.toString();
            out.println(i + " " + decoration + ";");

            // Transitions for state i
            if (initialStates.contains(i)) {
                drawInitialTransitionTo(i, out);
            }
            exportTransitionsToDotFile(i, out, decorators, precision);
        }

        // Footer
        out.print("}\n");
    }

    private void drawInitialTransitionTo(int i, PrismLog out) {

        // state
        explicit.graphviz.Decoration d = new explicit.graphviz.Decoration();
        d.setLabel("");
        d.attributes().put("shape", "point");
        d.attributes().put("color", environmentPlayer == getPlayer(i) ? "black" : "red");
        out.println("\""+i + "i\" " + d + ";");

        // transition
        out.print("\""+i + "i\" -> " + i);
        explicit.graphviz.Decoration dInit = new explicit.graphviz.Decoration();
        dInit.attributes().put("color", environmentPlayer == getPlayer(i) ? "black" : "red");
        dInit.setLabel("");
        out.println(" " + dInit + ";");
    }

    @Override
    public void exportTransitionsToDotFile(int i, PrismLog out, Iterable<explicit.graphviz.Decorator> decorators, int precision)
    {
        // Iterate through outgoing transitions for this state
        for (Map.Entry<Integer, TransitionWeight> e : transitions.get(i)) {
            // Print a new dot file line for the arrow for this transition
            out.print(i + " -> " + e.getKey());
            // Annotate this arrow with the probability
            explicit.graphviz.Decoration decoration = new explicit.graphviz.Decoration();
            decoration.setLabel(PrismUtils.formatDouble(precision, e.getValue().value()));
            decoration.attributes().put("color", e.getValue().type() == TransitionWeight.Type.Energy ? "red" : "black");
            decoration.attributes().put("fontcolor", e.getValue().type() == TransitionWeight.Type.Energy ? "red" : "black");
            decoration.attributes().put("fontname", "Helvetica");
            // Apply any other decorators requested
            if (decorators != null) {
                for (Decorator decorator : decorators) {
                    decoration = decorator.decorateProbability(i, e.getKey(), e.getValue(), decoration);
                }
            }
            // Append to the dot file line for this transition
            out.println(" " + decoration.toString() + ";");
        }
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
