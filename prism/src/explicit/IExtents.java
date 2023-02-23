package explicit;

public interface IExtents {

    /**
     * @return The probability of success at the given state and energy.
     */
    double getProbability(int stateIndex, double energy);

    /**
     * @return The smallest amount of energy required to succeed in the given state with the given probability.
     */
    double findMinEnergy(int stateIndex, double probability);
}
