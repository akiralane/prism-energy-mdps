package explicit;

import java.util.*;

/**
 * Explicit representation of a list of transitions from a single state in an EMDP. Since a transition
 * can either be a probabilistic or an energy transition, we keep track of its 'type' here.
 * This class is essentially a mapping: successor state -> (weight type, weight value). Each entry represents
 * one transition.
 */
public class TransitionList implements Iterable<Map.Entry<Integer, TransitionWeight>>
{
    private final HashMap<Integer, TransitionWeight> transitionMap;

    /**
     * Constructor: Empty transition.
     */
    public TransitionList()
    {
        transitionMap = new HashMap<>();
    }

    /**
     * Construct a trans list from an existing one and an index permutation,
     * i.e. in which index i becomes index permut[i].
     */
    public TransitionList(TransitionList trans, int permut[])
    {
        this();
        for (Map.Entry<Integer, TransitionWeight> entry : trans) {
            addTransition(permut[entry.getKey()], entry.getValue());
        }
    }

    /**
     * Adds probabilistic transition at {@code index} with value {@code value}.
     * @return True if there was already a value at {@code index}.
     */
    public boolean addProbabilisticTransition(int index, double value)
    {
        return addTransition(index, new TransitionWeight(TransitionWeight.Type.Probabilistic, value));
    }

    /**
     * Adds energy transition at {@code index} with value {@code value}.
     * @return True if there was already a value at {@code index}.
     */
    public boolean addEnergyTransition(int index, double value)
    {
        return addTransition(index, new TransitionWeight(TransitionWeight.Type.Energy, value));
    }

    /**
     * Adds a given transition to the map.
     * @param index The index of the target state.
     * @param weight The weight of the transition.
     * @return True if there was already a value at {@code index}.
     */
    public boolean addTransition(int index, TransitionWeight weight)
    {
        var result = transitionMap.put(index, weight);
        return result != null;
    }

    /**
     * Get the transition value at index {@code index}.
     * @param index The index of the transition to get.
     * @return The transition, empty if undefined.
     */
    public Optional<TransitionWeight> get(int index)
    {
        var transition = transitionMap.get(index);
        return transition == null ? Optional.empty() : Optional.of(transition);
    }

    public int size()
    {
        return transitionMap.size();
    }

    public boolean containsState(int s) { return transitionMap.containsKey(s); }

    @Override
    public Iterator<Map.Entry<Integer, TransitionWeight>> iterator() {
        return transitionMap.entrySet().iterator();
    }

    public Set<Integer> getSupport()
    {
        return transitionMap.keySet();
    }

    @Override
    public String toString()
    {
        return transitionMap.toString();
    }
}
