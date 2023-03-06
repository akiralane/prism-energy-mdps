package explicit;

import parser.EvaluateContextState;
import parser.State;
import parser.ast.Expression;
import parser.ast.ExpressionEnergyReachability;
import parser.type.*;
import prism.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EMDPModelChecker extends StateModelChecker {

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

    private Extents computeExtents(EMDPSimple emdp, Set<Integer> targetStates)
    {
        var extents = new Extents(emdp, targetStates);

        mainLog.print("\nPutting states in order of proximity to target states...");
        var orderedStates = findIntermediateStatesInOrder(emdp, targetStates);



        // TODO test extent merge algorithm
        // TODO might it be worth leaving out initial states from the ordering - put them at the end specifically?
        //  it's possible we could get a better result, but I'm not sure...

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

            // add state but don't include target states, since their extents should never be updated
            if (!targetStates.contains(thisState)) orderedStates.add(thisState);

            for (Integer successor : successors) {
                if (!orderedStates.contains(successor)) {
                    stateQueue.add(successor);
                }
            }
        }

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
