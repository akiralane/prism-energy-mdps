package explicit;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * Explicit representation of a transition in an EMDP. Since a transition
 * can either be a probabilistic or an energy transition, we keep track of
 * its 'type' here.
 */
public class TransitionList implements Iterable<Map.Entry<Integer, Double>>
{
    private enum TransitionType
    {
        Probabilistic,
        Energy,
    }

    private final HashMap<Integer, Double> transitionMap;
    private final HashMap<Integer, TransitionType> typeMap;

    /**
     * Constructor: Empty transition.
     */
    public TransitionList()
    {
        transitionMap = new HashMap<>();
        typeMap = new HashMap<>();
    }

    /**
     * Adds probabilistic transition at {@code index} with value {@code value}.
     * @return True if there was already a value at {@code index}.
     */
    public boolean addProbabilisticTransition(int index, double value)
    {
        typeMap.put(index, TransitionType.Probabilistic);
        return set(index, value);
    }

    /**
     * Adds energy transition at {@code index} with value {@code value}.
     * @return True if there was already a value at {@code index}.
     */
    public boolean addEnergyTransition(int index, double value)
    {
        typeMap.put(index, TransitionType.Energy);
        return set(index, value);
    }

    /**
     * Sets transition at {@code index} to {@code value}.
     * @return True if there was already a value at {@code index}.
     */
    private boolean set(int index, double value)
    {
        var result = transitionMap.put(index, value);
        return result != null;
    }

    /**
     * Get the value of the transition at index {@code index}.
     * @param index The index of the transition to get.
     * @return The value of the transition, empty if undefined.
     */
    public Optional<Double> get(int index)
    {
        var value = transitionMap.get(index);
        return value == null ? Optional.empty() : Optional.of(value);
    }

    /**
     * Get the type of the transition at index {@code index}.
     * @param index The index of the transition to get.
     * @return True if probabilistic, false if energy, empty if undefined.
     */
    public Optional<Boolean> isProbabilistic(int index)
    {
        var value = typeMap.get(index);
        return value == null ?
                Optional.empty() :
                Optional.of(value == TransitionType.Probabilistic);
    }

    /**
     * Get the type of the transition at index {@code index}.
     * @param index The index of the transition to get.
     * @return True if energy, false if probabilistic, empty if undefined.
     */
    public Optional<Boolean> isEnergy(int index)
    {
        return isProbabilistic(index).map(x -> !x);
    }

    public int size()
    {
        return transitionMap.size();
    }

    @Override
    public Iterator<Map.Entry<Integer, Double>> iterator() {
        return transitionMap.entrySet().iterator();
    }

    @Override
    public String toString()
    {
        return "Transitions: "+ transitionMap +" of types: "+ typeMap;
    }
}
