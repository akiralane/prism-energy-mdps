package explicit;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Extent {

    public enum StateType {
        Target,
        Intermediate,
    }

    private TreeMap<Double, Double> extent;
    private TreeMap<Double, Integer> sourceMap; // (energy -> state that the corresponding probability came from)
    private final StateType type;

    /**
     * Constructor: create an extent with initial values determined by its type.
     * @param type Whether this extent belongs to a target or intermediate state.
     */
    public Extent(StateType type) {
        this.type = type;
        switch (type) {
            // "with no energy, you can always reach a target state (this one)"
            case Target -> extent = new TreeMap<>(Map.of(0d, 1d));
            // "with no energy, it is impossible to reach a target state (for now, before calculation)"
            case Intermediate -> extent = new TreeMap<>(Map.of(0d, 0d));
        }
        sourceMap = new TreeMap<>();
    }

    /**
     * Constructor: create an *empty* extent, to be filled out in the merge operation.
     * Since target states should never have merges calculated for them, this is an
     * intermediate extent by default.
     */
    public Extent() {
        type = StateType.Intermediate;
        extent = new TreeMap<>();
        sourceMap = new TreeMap<>();
    }

    public Extent deepCopy() {
        var copy = new Extent(type);
        copy.extent = new TreeMap<>(extent);
        copy.sourceMap = new TreeMap<>(sourceMap);
        return copy;
    }

    public void set(Double energy, Double probability) { extent.put(energy, probability); }

    public void setSource(Double energy, Integer sourceState) { sourceMap.put(energy, sourceState); }

    /** @return The index of the state which this energy-probability pair came from. */
    public Integer getSource(Double energy) { return sourceMap.get(energy); }

    public Double getProbabilityFor(Double energy) { return extent.get(energy); }

    public boolean hasEnergy(Double energy) { return extent.containsKey(energy); }

    public Map.Entry<Double, Double> floorEntry(Double energy) { return extent.floorEntry(energy); }

    public Set<Double> getEnergySet() { return extent.keySet(); }

    public Set<Map.Entry<Double, Double>> entrySet() { return extent.entrySet(); }

    public StateType getType() { return type; }

    public Map<Double, Integer> sourceMap() { return sourceMap; }

    /**
     * Clears the extent data, but *not* the source data. This means that
     * extents can remain minimal while strategies can still see how the extents
     * got their numbers.
     */
    public void softClear() {
        extent.clear();
//        sourceMap.clear();
    }

    @Override
    public String toString() {
        return extent.toString();
    }
}
