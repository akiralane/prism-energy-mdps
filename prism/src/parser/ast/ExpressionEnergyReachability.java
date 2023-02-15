package parser.ast;

import parser.EvaluateContext;
import parser.visitor.ASTVisitor;
import prism.PrismLangException;

public class ExpressionEnergyReachability extends Expression {

    public enum EnergyReachabilityType {
        PROB_GIVEN_ENERGY("P|E="),
        ENERGY_GIVEN_PROB("Emin|P=");

        private String name;
        EnergyReachabilityType(String name) { this.name = name; }

        @Override
        public String toString() { return name; }
    }

    /** The main operand carried by this expression: i.e. "[s > 0]" in "Emin|P=0.3 [s > 0]" */
    private final Expression expression;
    /** The type of energy-reachability expression this is. */
    private final EnergyReachabilityType reachabilityType;
    /** The probability of success if the type is ENERGY_GIVEN_PROB, or the initial energy if it's PROB_GIVEN_ENERGY */
    private final double givenValue;

    /**
     * Constructor: creates a new energy-reachability expression with a given type and value.
     * @param expression The main operand carried by this expression: i.e. "[s > 0]" in "Emin|P=0.3 [s > 0]"
     * @param reachabilityType The type of energy-reachability expression this is.
     * @param givenValue The probability of success if the type is ENERGY_GIVEN_PROB, or the initial energy if it's PROB_GIVEN_ENERGY
     */
    public ExpressionEnergyReachability(Expression expression, EnergyReachabilityType reachabilityType, double givenValue) {
        this.expression = expression;
        this.reachabilityType = reachabilityType;
        this.givenValue = givenValue;
    }

    @Override
    public Object accept(ASTVisitor v) throws PrismLangException {
        return v.visit(this);
    }

    @Override
    public Object evaluate(EvaluateContext ec) throws PrismLangException {
        return expression.evaluate(ec);
    }

    public EnergyReachabilityType getReachabilityType() {
        return reachabilityType;
    }

    public double getGivenValue() {
        return givenValue;
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public boolean isProposition() {
        return false;
    }

    @Override
    public boolean returnsSingleValue() {
        return false;
    }

    @Override
    public String toString() {
        return reachabilityType +
                Double.toString(givenValue) + " " +
                "[" + expression + "]";
    }

    @Override
    public Expression deepCopy() {
        ExpressionEnergyReachability expr = new ExpressionEnergyReachability(expression, reachabilityType, givenValue);
        expr.setType(type);
        expr.setPosition(this);
        return expr;
    }
}
