package explicit;

/**
 * Represents an immutable transition. Essentially a pair (type, value).
 */
public record TransitionWeight(Type type, double value) {

    public enum Type {
        Probabilistic,
        Energy,
    }

}
