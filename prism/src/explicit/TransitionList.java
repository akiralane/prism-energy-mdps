package explicit;

import java.util.*;

/**
 * Explicit representation of a transition in an EMDP. Since a transition
 * can either be a probabilistic or an energy transition, we keep track of
 * its 'type' here.
 * This class is essentially a mapping from indices to pairs of (type, value),
 * where each pair represents a transition.
 */
public class TransitionList implements Iterable<Map.Entry<Integer, Transition>>
{
    private final HashMap<Integer, Transition> transitionMap;

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
        for (Map.Entry<Integer, Transition> entry : trans) {
            addTransition(permut[entry.getKey()], entry.getValue());
        }
    }

    /**
     * Adds probabilistic transition at {@code index} with value {@code value}.
     * @return True if there was already a value at {@code index}.
     */
    public boolean addProbabilisticTransition(int index, double value)
    {
        var result = transitionMap.put(index, new Transition(Transition.TransitionType.Probabilistic, value));
        return result != null;
    }

    /**
     * Adds energy transition at {@code index} with value {@code value}.
     * @return True if there was already a value at {@code index}.
     */
    public boolean addEnergyTransition(int index, double value)
    {
        var result = transitionMap.put(index, new Transition(Transition.TransitionType.Energy, value));
        return result != null;
    }

    /**
     * Adds a given transition to the map.
     * @return True if there was already a value at {@code index}.
     */
    public boolean addTransition(int index, Transition transition)
    {
        var result = transitionMap.put(index, transition);
        return result != null;
    }

    /**
     * Get the transition at index {@code index}.
     * @param index The index of the transition to get.
     * @return The transition, empty if undefined.
     */
    public Optional<Transition> get(int index)
    {
        var transition = transitionMap.get(index);
        return transition == null ? Optional.empty() : Optional.of(transition);
    }

    public int size()
    {
        return transitionMap.size();
    }

    @Override
    public Iterator<Map.Entry<Integer, Transition>> iterator() {
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
