package explicit;

import prism.PrismException;

import java.util.*;

/**
 * An explicit representation of the extents of the states in a model
 */
public class Extents {

    /** Indexed by state. An extent is a TreeMap of energy -> probability of success. */
    private final Map<Integer, Extent> extents;
    /** The highest change in probability over every extent. */
    private double maxDelta = 1.0;

    /**
     * Constructor: initialises an extent for each state in the model.
     */
    public Extents(EMDPExplicit emdp, Set<Integer> targetStates)
    {
        // create and populate extents
        extents = new HashMap<>();
        for (int stateIndex = 0; stateIndex < emdp.numStates; stateIndex++) {
            if (targetStates.contains(stateIndex)) {
                // "with no energy, you can always reach a target state (this one)"
                extents.put(stateIndex, new Extent(Extent.StateType.Target));
            } else {
                // "with no energy, it is impossible to reach a target state (for now, before calculation)"
                extents.put(stateIndex, new Extent(Extent.StateType.Intermediate));
            }
        }
    }

    /** Computes the result of merging the extents from the given environment state's successors (as described by
     * {@code merge} in the progress report) and updates the corresponding extent. Requires the model for player and transition information.
     * <p></p>
     * <em>Environment case: </em>Weight the probabilities, then for each key (energy value) in the combined successors:
     * <ul>
     *      <li>Sum over the associated weighted probabilities in the successors for that key</li>
     *      <li>If the energy value doesn't exist, then take the next lowest energy value's probability (which can be thought
     *      of as the "current" probability of that extent)</li>
     *      <li>Put (energy, sum) in computed extent</li>
     * </ul>
     */
    public void mergeEnvironment(int stateIndex, EMDPSimple emdp) throws PrismException
    {
        var resultExtent = extents.get(stateIndex).deepCopy();
        if (resultExtent.getType().equals(Extent.StateType.Target)) { throw new PrismException("Tried to merge values for target state "+stateIndex+"!"); }

        if (emdp.getEnvironmentPlayer() != emdp.getPlayer(stateIndex)) {
            throw new PrismException("Tried to use environment merge on controller state (index "+stateIndex+")!");
        }

        var transitions = emdp.getTransitions(stateIndex);
        var energyValues = new TreeSet<Double>();
        var successorExtents = new HashSet<Extent>();

        // 1. for each successor state, weight and store its extent, add its keys (energy values) to a set
        for (Map.Entry<Integer, TransitionWeight> transition : transitions) {

            var targetState = transition.getKey();
            var weight      = transition.getValue();
            if (weight.type().equals(TransitionWeight.Type.Energy)) {
                throw new PrismException(
                        "Found energy transition \""+
                                stateIndex+" --("+weight.value()+")-> "+targetState+
                                "\" from Environment state while merging extents!"
                );
            }

            // 1.1 weight extent by the probability of its transition
            var targetExtent = extents.get(targetState);
            var weightedExtent = new Extent();
            for (Double energy : targetExtent.getEnergySet()) {
                weightedExtent.set(
                        energy,
                        targetExtent.getProbabilityFor(energy) * weight.value()
                );
            }
            successorExtents.add(weightedExtent);
            energyValues.addAll(weightedExtent.getEnergySet());
        }

        // 2. now create the corresponding output entry for each energy value
        for (Double energy : energyValues) {

            // 2.1 get a list of probability values for this energy
            var probValues = new ArrayList<Double>();
            for (var extent : successorExtents) {
                // get the energy value, or the next lowest if it doesn't exist
                // see notebook for the reasoning behind this
                var entry = extent.floorEntry(energy);
                var probability = (entry != null) ? entry.getValue() : 0;
                probValues.add(probability);
            }

            // 2.2 sum them and place them in the output
            var oldProbability = resultExtent.getProbabilityFor(energy); // used for delta
            var weightedSum = probValues.stream().reduce(0.0, Double::sum);
            resultExtent.set(energy, weightedSum);

            // 2.3 compute delta for this energy
            maxDelta = Double.max(weightedSum - oldProbability, maxDelta);

        }

        extents.put(stateIndex, resultExtent);
    }

    /** Computes the result of merging the extents from the given controller state's successors (as described by
     * {@code merge} in the progress report) and updates the corresponding extent. Requires the model for player and transition information.
     * <p></p>
     * <em>Controller case:</em> For each lifted energy value in the combined successors:
     * <ul>
     *    <li>Get the list of probabilities in the successors associated with that key and pick the highest</li>
     *    <li>If the highest value in the list is lower than highest value in the extent so far, omit that key from computed extent
     *    since there's no probability improvement. I don't think this should ever happen, but...</li>
     *    <li>Otherwise: put (energy, max(list)) in computed extent</li>
     * </ul>
     */
    public void mergeController(int stateIndex, EMDPSimple emdp) throws PrismException
    {
        var resultExtent = extents.get(stateIndex);
        if (resultExtent.getType().equals(Extent.StateType.Target)) { throw new PrismException("Tried to merge values for target state "+stateIndex+"!"); }
        resultExtent.clear();

        if (emdp.getEnvironmentPlayer() == emdp.getPlayer(stateIndex)) {
            throw new PrismException("Tried to use controller merge on environment state (index "+stateIndex+")!");
        }

        var transitions = emdp.getTransitions(stateIndex);
        var energyValues = new TreeSet<Double>();
        var successorExtents = new HashMap<Integer, Extent>(); // source state -> extent

        // 1. for each successor state, lift and store its extent, add its keys (energy values) to a set
        for (Map.Entry<Integer, TransitionWeight> transition : transitions) {

            var targetState = transition.getKey();
            var weight      = transition.getValue();
            if (weight.type().equals(TransitionWeight.Type.Probabilistic)) {
                throw new PrismException(
                        "Found probabilistic transition \""+
                                stateIndex+" --("+weight.value()+")-> "+targetState+
                                "\" from Controller state while merging extents!"
                );
            }

            // 1.1. "lift" extent by the cost of the transition
            var targetExtent = extents.get(targetState);
            var liftedExtent = new Extent();
            for (Double energy : targetExtent.getEnergySet()) {
                liftedExtent.set(
                        energy + weight.value(),
                        targetExtent.getProbabilityFor(energy)
                );
            }
            successorExtents.put(targetState, liftedExtent);
            energyValues.addAll(liftedExtent.getEnergySet());
        }

        // 2. now create the corresponding output entry for each energy value that we want
        double highestProbInExtent = 0.0;
        for (Double energy : energyValues) {

            // 2.1 find the highest probability in the successors associated with this energy
            var highestProbForThisEnergy = 0.0;
            var sourceOfHighestProb = 0;
            for (Map.Entry<Integer, Extent> entry : successorExtents.entrySet()) {
                var sourceState = entry.getKey();
                var extent = entry.getValue();
                if (extent.hasEnergy(energy)) {
                    var probability = extent.getProbabilityFor(energy);
                    if (probability > highestProbForThisEnergy) { // set new highest if we find one
                        highestProbForThisEnergy = probability;
                        sourceOfHighestProb = sourceState;
                    }
                }
            }

            // 2.2 if it's higher than the current highest probability in the extent,
            // update the new highest and put (energy, value) in the output
            if (highestProbForThisEnergy > highestProbInExtent) {
                highestProbInExtent = highestProbForThisEnergy;
                var oldProbabiity = resultExtent.getProbabilityFor(energy);
                resultExtent.set(energy, highestProbForThisEnergy);
                resultExtent.setSource(energy, sourceOfHighestProb);

                // also update the delta if necessary
                maxDelta = Double.max(oldProbabiity - highestProbForThisEnergy, maxDelta);
            }
            // otherwise, don't bother including this energy value in the output, since it's redundant
        }

        extents.put(stateIndex, resultExtent);
    }

    /**
     * @return The extent at the given index.
     */
    public Extent getExtent(int stateIndex) {
        return extents.get(stateIndex);
    }

    /**
     * @return The probability of success from the state at {@code stateIndex} with the given energy.
     */
    public double getProbability(int stateIndex, double energy)
    {
        var entry = extents.get(stateIndex).floorEntry(energy);
        return entry == null ? 0 : entry.getValue();
    }

    /**
     * @return The smallest amount of energy that guarantees the given probability of success.
     * The optional is empty if there is no such probability in the extent, which means that
     * we didn't search for long enough.
     */
    public Optional<Double> findMinEnergy(int stateIndex, double probability)
    {
        // "find the first energy whose probability is above the threshold"
        return extents.get(stateIndex)
                .entrySet().stream()
                .dropWhile(entry -> entry.getValue() < probability)
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public double getAverageEnergy() {
        return extents.values().stream()
                .map(Extent::getEnergySet)
                .flatMap(Set::stream)
                .mapToDouble(d -> d)
                .average()
                .orElse(Double.NaN);
    }

    public double getAverageEntries() {
        return extents.values().stream()
                .map(Extent::getEnergySet)
                .mapToDouble(Set::size)
                .average()
                .orElse(Double.NaN);
    }

    public void clearDelta() {
        maxDelta = 0;
    }

    public double getMaxDelta() { return maxDelta; }

    @Override
    public String toString() {
        return extents.toString();
    }
}
