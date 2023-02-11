package explicit;

/**
 * Represents an immutable transition. Essentially a pair (type, value).
 */
public record Transition(explicit.Transition.TransitionType type, double value) {

    public enum TransitionType {
        Probabilistic,
        Energy,
    }

}
