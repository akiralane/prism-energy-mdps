package explicit;

import parser.State;

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
     * Note that this doesn't (shouldn't) mutate the object.
     * @return The extent computed as a result of the merge.
     */
    private TreeMap<Double, Double> mergeExtents(int stateIndex, EMDPSimple emdp) {

        if (emdp.getEnvironmentPlayer() == emdp.getPlayer(stateIndex)) { // probabilistic
            /*
            (look at your notebook! keep track of the "routes" somehow and use those)
            */
        } else { // player's choice

            var transitions = emdp.getTransitions(stateIndex);
            var energyValues = new TreeSet<Double>();
            var successorExtents = new HashSet<TreeMap<Double, Double>>();

            // for every successor state, record its extent and add its keys (energy values) to the set
            for (int i = 0; i < transitions.size(); i++) {
                if (transitions.get(i).isPresent()) {
                    successorExtents.add(extents.get(i));
                    energyValues.addAll(extents.get(i).keySet());
                }
            }

            // create the corresponding output entry for each energy value in the successors (some end up omitted)
            for (Double energy : energyValues) {
                var successorProbabilities = new ArrayList<>();

                // go through the successor extents and record its corresponding probability to this energy value
                for (TreeMap<Double, Double> extent : successorExtents) {
                    if (extent.containsKey(energy)) {
                        successorProbabilities.add(extent.get(energy));
                    }
                }

                // ...
            }

            /*
            (look at your notebook for better detail)
            - for each key (energy value) in the successors:
                - get the list of successor values associated with that key
                - list empty: panic
                - highest value in list lower than current running highest: omit key from computed extent
                - otherwise: put (key, max(list)) in computed extent
            */
        }

        return null;
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
