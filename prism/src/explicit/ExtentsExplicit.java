package explicit;

import parser.State;
import prism.PrismException;

import java.util.*;

/**
 * An explicit representation of the extents of the states in a model
 */
public class ExtentsExplicit extends Extents implements IExtents {

    private List<TreeMap<Double, Double>> extents;

    /**
     * Constructor: initialises an extent for each state in the model.
     */
    public ExtentsExplicit(EMDPExplicit emdp, List<State> targetStates) {
        super(emdp, targetStates);

        extents = new ArrayList<>();

        for (State state : emdp.statesList) {
            if (targetStates.contains(state)) {
                // "with no energy, you can always reach a target state (this one)"
                extents.add(new TreeMap<>(Map.of(0d, 1d)));
            } else {
                // "with no energy, it is impossible to reach a target state (for now, before calculation)"
                extents.add(new TreeMap<>(Map.of(0d, 0d)));
            }
        }
    }

    /**
     * Computes the result of merging the extents from the given state's successors (as described by
     * {@code merge} in the progress report). Requires the model for player and transition information.
     * Note that this doesn't (shouldn't) mutate the "extents" object.
     * <p>
     * <em>Player case:</em> for each key (energy value) in the successors:<ul>
     *    <li>Get the list of probabilities in the successors associated with that key and pick the highest</li>
     *    <li>If the highest value in list lower than highest value in the extent so far: omit key from computed extent</li>
     *    <li>Otherwise: put (key, max(list)) in computed extent</li>
     * </ul>
     * </p>
     * @return The extent computed as a result of the merge.
     */
    private TreeMap<Double, Double> mergeExtents(int stateIndex, EMDPSimple emdp) throws PrismException {

        var resultExtent = new TreeMap<Double, Double>();
        var transitions = emdp.getTransitions(stateIndex);

        if (emdp.getEnvironmentPlayer() == emdp.getPlayer(stateIndex)) { // probabilistic
            /*
            (look at your notebook! keep track of the "routes" somehow and use those)
            whenever we encounter an entry, update the "routes" of the successor extents which participate
            take the weighted sum of the values on the routes
            */

        } else { // controller's choice

            var energyValues = new TreeSet<Double>();
            var successorExtents = new HashSet<TreeMap<Double, Double>>();

            // for every successor state, lift and store its extent, add its keys (energy values) to a set
            for (Map.Entry<Integer, Transition> transition : transitions) {

                var targetState = transition.getKey();
                var cost = transition.getValue().value();

                if (transition.getValue().type().equals(Transition.TransitionType.Probabilistic)) {
                    throw new PrismException(
                            "Found probabilistic transition \""+
                            stateIndex+" --("+cost+")-> "+targetState+
                            "\" from Controller state while merging extents!"
                    );
                }

                // "lift" extent by the cost of the transition
                var oldExtent = extents.get(targetState);
                var liftedExtent = new TreeMap<Double, Double>();
                for (Double energy : oldExtent.keySet()) {
                    liftedExtent.put(energy + cost, oldExtent.get(energy));
                }

                successorExtents.add(liftedExtent);
                energyValues.addAll(liftedExtent.keySet());
            }

            // create the corresponding output entry for each energy value
            double highestProbInExtent = 0.0;
            for (Double energy : energyValues) {

                // find the highest probability in the successors associated with this energy
                double highestProbForThisEnergy = 0.0;
                for (TreeMap<Double, Double> extent : successorExtents) {
                    if (extent.containsKey(energy)) {
                        highestProbForThisEnergy = Double.max(highestProbForThisEnergy, extent.get(energy));
                    }
                }

                // if it's higher than the current highest probability in the extent,
                // update the new highest and put (energy, value) in the output
                if (highestProbForThisEnergy > highestProbInExtent) {
                    highestProbInExtent = highestProbForThisEnergy;
                    resultExtent.put(energy, highestProbForThisEnergy);
                }
                // otherwise, don't include this energy value in the output, since it's redundant
            }
        }

        return resultExtent;
    }

    /**
     * Sets the extent at the given index.
     */
    public void setExtent(int stateIndex, TreeMap<Double, Double> extent) {
        extents.set(stateIndex, extent);
    }

    /**
     * @return The extent at the given index.
     */
    public TreeMap<Double, Double> getExtent(int stateIndex) {
        return extents.get(stateIndex);
    }

    @Override
    public double getProbability(int stateIndex, double energy) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public double findMinEnergy(int stateIndex, double probability) {
        throw new UnsupportedOperationException(); // TODO
    }
}
