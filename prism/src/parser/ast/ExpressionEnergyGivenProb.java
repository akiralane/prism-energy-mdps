package parser.ast;
import parser.State;

import java.util.List;

public class ExpressionEnergyGivenProb extends ExpressionEnergyReachability {

    private final List<State> targetStates;
    private final double targetProbability;

    public ExpressionEnergyGivenProb(List<State> targetStates, double targetProbability) {
        this.targetStates = targetStates;
        this.targetProbability = targetProbability;
    }

    public List<State> getTargetStates() { return targetStates; }

    public double getTargetProbability() { return targetProbability; }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public Expression deepCopy() {
        return null;
    }

}
