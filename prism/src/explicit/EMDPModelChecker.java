package explicit;

import parser.EvaluateContextState;
import parser.State;
import parser.ast.Expression;
import parser.ast.ExpressionEnergyReachability;
import parser.type.*;
import prism.*;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class EMDPModelChecker extends StateModelChecker {

    /**
     * Constructor: new EMDP model checker, inherit basic state from parent (unless null).
     * @param parent
     */
    public EMDPModelChecker(PrismComponent parent) throws PrismException {
        super(parent);
    }

    public Result check(Model model, Expression expr) throws PrismException
    {
        // verify and cast before passing to checking method
        verify(model, expr);
        var emdp = (EMDPSimple) model;
        var energyReachabilityExpr = (ExpressionEnergyReachability) expr;

        return checkEMDP(emdp, energyReachabilityExpr);
    }

    private Result checkEMDP(EMDPSimple model, ExpressionEnergyReachability expr) throws PrismException
    {
        var targetStates = findTargetStates(model, expr);
        mainLog.print("\nFound "+targetStates.size()+" target states: "+targetStates);

        mainLog.print("\nComputing extents...");
        var extents = computeExtents(model, targetStates);

        mainLog.print(switch (expr.getReachabilityType()) {
            case ENERGY_GIVEN_PROB -> "\nComputing minimum energy required to reach a target state with probability "+expr.getGivenValue()+"...";
            case PROB_GIVEN_ENERGY -> "\nComputing probability of reaching a target state with initial energy "+expr.getGivenValue()+"...";
        });

        return new Result(); // TODO return an actual result
    }

    private ExtentsExplicit computeExtents(EMDPSimple model, List<State> targetStates)
    {
        var extents = new ExtentsExplicit(model, targetStates);

        model.

        return extents;
    }



    /**
     * Evaluates the expression on every model in the state and returns a list of those which
     * satisfy it (that is, the target states).
     */
    private List<State> findTargetStates(EMDPSimple model, ExpressionEnergyReachability expr) throws PrismException
    {
        var targetStates = new ArrayList<State>();
        for (State state : model.statesList)
        {
            var ctx = new EvaluateContextState(state);
            if ((boolean) expr.evaluate(ctx)) {
                targetStates.add(state);
            }
        }

        return targetStates;
    }

    /**
     * Check that the model and expression are the correct classes, and type-check the expression.
     */
    private void verify(Model model, Expression expr) throws PrismException
    {
        if (!(model instanceof EMDPSimple))
            throw new PrismException("EMDPModelChecker only supports EMDPs, unsurprisingly");

        if (!(expr instanceof ExpressionEnergyReachability))
            throw new PrismException("Only energy reachability expressions are supported for EMDPs, got "+expr.getClass()+" instead");

        expr.typeCheck();
        if (!(expr.getType() instanceof TypeBool))
            throw new PrismException("Energy reachability properties must be propositions, but \""+((ExpressionEnergyReachability) expr).getExpression()+"\" is of type "+expr.getType());
    }
}
