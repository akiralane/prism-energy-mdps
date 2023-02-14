package parser.ast;

import parser.State;

import java.util.List;

public class ExpressionProbGivenEnergy extends ExpressionEnergyReachability {

    private final List<State> targetStates;
    private final double initialEnergy;

    public ExpressionProbGivenEnergy(List<State> targetStates, double initialEnergy) {
        this.targetStates = targetStates;
        this.initialEnergy = initialEnergy;
    }

    public List<State> getTargetStates() {
        return targetStates;
    }

    public double getInitialEnergy() {
        return initialEnergy;
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public Expression deepCopy() {
        return null;
    }

}
