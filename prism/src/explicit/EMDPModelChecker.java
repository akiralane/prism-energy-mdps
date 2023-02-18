package explicit;

import parser.EvaluateContextState;
import parser.State;
import parser.ast.Expression;
import parser.ast.ExpressionEnergyReachability;
import parser.type.*;
import prism.*;

import java.util.List;

public class EMDPModelChecker extends StateModelChecker {

    /**
     * Constructor: new EMDP, inherit basic state from parent (unless null).
     * @param parent
     */
    public EMDPModelChecker(PrismComponent parent) throws PrismException {
        super(parent);
    }

    public Result check(Model model, Expression expr) throws PrismException
    {
        // verify and cast before passing to checking method
        verifyTypes(model, expr);
        var emdp = (EMDPSimple) model;
        var energyReachabilityExpr = (ExpressionEnergyReachability) expr;

        return checkEMDP(emdp, energyReachabilityExpr);
    }

    private Result checkEMDP(EMDPSimple model, ExpressionEnergyReachability expr) throws PrismException
    {
        // TODO implement actual algorithm
        var testInitial = model.initialStates.get(0);
        var testEc = new EvaluateContextState(model.statesList.get(testInitial));

        var result = expr.evaluate(testEc);
        System.out.println(result);

        return null;
    }

    /**
     * Check that the model and expression are the correct classes, and type-check the expression.
     */
    private void verifyTypes(Model model, Expression expr) throws PrismException
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
