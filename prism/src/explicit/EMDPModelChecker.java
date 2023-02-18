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
     * Check that the model and expression are the correct types, and typecheck the expression.
     */
    private void verifyTypes(Model model, Expression expr) throws PrismException
    {
        if (!(model instanceof EMDPSimple))
            throw new PrismException("EMDPModelChecker only supports EMDPs, unsurprisingly");

        if (!(expr instanceof ExpressionEnergyReachability energyReachabilityExpr))
            throw new PrismException("Only energy reachability expressions are supported for EMDPs, got "+expr+" instead");

        // TODO: type check energy reachability by type checking the *inner expression*
        //  (look at how the other expressions do it - it can't just be passed a generic Expression to check)
        //  so there needs to be a special case for EnergyReachability, but I'm not sure how to visitPost or whatever on the inner Expression

//        energyReachabilityExpr.typeCheck();
//        var exprType = energyReachabilityExpr.getType();
//        if (!(exprType instanceof TypeBool))
//            throw new PrismException("Energy reachability properties must be propositions");
    }

}
