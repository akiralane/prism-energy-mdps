package explicit;

import parser.EvaluateContextState;
import parser.ast.Expression;
import parser.ast.ExpressionEnergyReachability;
import parser.type.*;
import prism.*;

import java.util.*;

public class EMDPModelChecker extends StateModelChecker {

    public final double DELTA_BOUND = 0; // TODO load from settings

    /**
     * Constructor: new EMDP model checker, inherit basic state from parent (unless null).
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

    /**
     * Specific EMDP checking method, called after making sure we're using the right model.
     */
    private Result checkEMDP(EMDPSimple emdp, ExpressionEnergyReachability expr) throws PrismException
    {
        mainLog.print("\nEvaluating the expression on each state in the model...");
        var targetStates = findTargetStates(emdp, expr);
        mainLog.print("\nFound "+targetStates.size()+" target states: "+targetStates);

        mainLog.print("\n\n[ Computing extents ]");
        var extents = computeExtents(emdp, targetStates);

        mainLog.print("\n\n[ Computing result ]");
        switch (expr.getReachabilityType()) {
            case ENERGY_GIVEN_PROB ->
            {
                var targetProbability = expr.getGivenValue();
                mainLog.print("\nFinding minimum energy required to reach a target state with probability "+targetProbability+"...");
                var maybeResult = findEnergyGivenProb(emdp.getInitialStates(), extents, targetProbability);
                if (maybeResult.isEmpty()) {
                    throw new PrismException("No candidate energy found in the extents - try increasing the bound");
                }
                mainLog.print("\n===> "+maybeResult.get()+"\n");
            }
            case PROB_GIVEN_ENERGY ->
            {
                var targetEnergy = expr.getGivenValue();
                mainLog.print("\nFinding probability of reaching a target state with initial energy "+expr.getGivenValue()+"...");
                var result = findProbGivenEnergy(emdp.getInitialStates(), extents, targetEnergy);
                mainLog.print("\n===> "+result+"\n");
            }
        }

        return new Result(); // TODO return an actual result, put extents in result so they can be looked at
    }

    private Extents computeExtents(EMDPSimple emdp, Set<Integer> targetStates) throws PrismException
    {
        var extents = new Extents(emdp, targetStates);

        mainLog.print("\nOrdering states by proximity to target states...");
        var orderedStates = findIntermediateStatesInOrder(emdp, targetStates);

        mainLog.print("\nPerforming value iteration with delta bound "+DELTA_BOUND+"...");
        mainLog.flush();
        double timer = System.currentTimeMillis();
        int counter = 0;
        var environmentPlayer = emdp.getEnvironmentPlayer();
        do {
//            mainLog.print("\n\niteration: "+counter);
//            mainLog.print("\ndelta: "+extents.getMaxDelta());
            extents.clearDelta();
            for (var state : orderedStates) {
                if (emdp.getPlayer(state) == environmentPlayer) {
                    extents.mergeEnvironment(state, emdp);
                } else {
                    extents.mergeController(state, emdp);
                }
            }
            counter++;
        } while (extents.getMaxDelta() > DELTA_BOUND);
//        mainLog.print("\n"+extents+"\n");

        mainLog.print(" done in "+counter+" iterations and "+((System.currentTimeMillis() - timer) / 1000)+" seconds.");
        mainLog.print("\nResulting extents have an average of "+extents.getAverageEntries()+" entries.");

        return extents;
    }

    /**
     * Performs a reverse-BFS on the states of the EMDP, starting with the target states and
     * returning a list of *non-target* states ordered by their
     * proximity to the target states. Excludes any "dead end" states which do not have any paths
     * to a target state.
     */
    private List<Integer> findIntermediateStatesInOrder(EMDPSimple emdp, Set<Integer> targetStates)
    {
        var orderedStates = new ArrayList<Integer>();

        // reverse all transitions in EMDP
        var originalTransitions = emdp.transitions;
        var reversedTransitions = Arrays.asList(new TransitionList[originalTransitions.size()]);
        reversedTransitions.replaceAll(ignored -> new TransitionList());

        for (int sourceState = 0; sourceState < originalTransitions.size(); sourceState++) {

            var transitionList = originalTransitions.get(sourceState);

            for (var transition : transitionList) {
                var targetState = transition.getKey();
                var weight = transition.getValue();

                reversedTransitions.get(targetState).addTransition(sourceState, weight);
            }
        }

        // initialise queue with target states
        Queue<Integer> stateQueue = new ArrayDeque<>(targetStates);

        // perform bfs over reversed state graph
        while (!stateQueue.isEmpty()) {

            var thisState = stateQueue.remove();
            var successors = reversedTransitions.get(thisState).getSupport();

            orderedStates.add(thisState);

            for (Integer successor : successors) {
                if (!orderedStates.contains(successor)) {
                    stateQueue.add(successor);
                }
            }
        }

        // don't include target states, since their extents should never be updated
        orderedStates.removeAll(targetStates);
        return orderedStates;
    }

    /**
     * Evaluates the expression on every model in the state and returns a list of the indexes of those which
     * satisfy it (that is, the target states).
     */
    private Set<Integer> findTargetStates(EMDPSimple model, ExpressionEnergyReachability expr) throws PrismException
    {
        var targetStates = new HashSet<Integer>();
        for (int stateIndex = 0; stateIndex < model.numStates; stateIndex++) {
            var ctx = new EvaluateContextState(model.statesList.get(stateIndex));
            if ((boolean) expr.evaluate(ctx)) {
                targetStates.add(stateIndex);
            }
        }
        return targetStates;
    }

    /**
     * For each initial state, finds the lowest energy required to succeed with the given probability.
     * Empty if no such energy is found, implying we didn't reach it yet.
     */
    private Optional<Double> findEnergyGivenProb(Iterable<Integer> initialStates, Extents computedExtents, double probability)
    {
        var smallestEnergy = Double.MAX_VALUE;
        boolean foundOne = false;

        for (var state : initialStates)
        {
            var maybeEnergy = computedExtents.findMinEnergy(state, probability);
            if (maybeEnergy.isPresent()) {
                smallestEnergy = Double.min(maybeEnergy.get(), smallestEnergy);
                foundOne = true;
            }
        }

        return foundOne ? Optional.of(smallestEnergy) : Optional.empty();
    }

    /**
     * For each initial state, finds the highest probability of success with the given energy.
     */
    private double findProbGivenEnergy(Iterable<Integer> initialStates, Extents computedExtents, double energy)
    {
        var largestProbability = 0.0;
        for (var state : initialStates)
        {
            largestProbability = Double.max(computedExtents.getProbability(state, energy), largestProbability);
        }
        return largestProbability;
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
