package explicit;

import explicit.rewards.Rewards;
import parser.ast.Expression;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismNotSupportedException;

import java.util.BitSet;

public class EMDPModelChecker extends ProbModelChecker {


    /**
     * Create a new ProbModelChecker, inherit basic state from parent (unless null).
     * @param parent
     */
    public EMDPModelChecker(PrismComponent parent) throws PrismException {
        super(parent);
    }

    /**
     * Compute probabilities for an LTL path formula
     */
    @Override
    protected StateValues checkProbPathFormulaLTL(Model model, Expression expr, boolean qual, MinMax minMax, BitSet statesOfInterest) throws PrismException
    {
        // This probably isn't what I actually want...? I should extend the PRISM language first
        throw new PrismNotSupportedException("Probability computation for EMDPs not implemented");
    }

    /**
     * Compute rewards for a co-safe LTL reward operator.
     */
    @Override
    protected StateValues checkRewardCoSafeLTL(Model model, Rewards modelRewards, Expression expr, MinMax minMax, BitSet statesOfInterest) throws PrismException
    {
        // Reward structures are beyond this project's scope - this method should be safe to delete
        throw new PrismException("Reward computation for EMDPs not implemented");
    }

}
