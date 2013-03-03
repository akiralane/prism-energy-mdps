package strat;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Collections;
import java.util.Comparator;

import prism.PrismException;
import explicit.STPG;
import explicit.Distribution;
import explicit.Model;
import explicit.STPGExplicit;
import explicit.MDP;
import explicit.SMG;
import explicit.MDPSimple;
import explicit.SMGModelChecker;
import explicit.PPLSupport;
import explicit.rewards.STPGRewards;
import parser.State;

import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.linear.Relationship;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.fraction.BigFraction;
import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;
import parma_polyhedra_library.*;

public class MultiObjectiveStrategy implements Strategy
{

    // number of states
    //protected int n;

    String info = "No information available.";

    // last state in history
    protected int lastState;
    // last memory element in history (paired with lastState)
    protected int lastCorner;

    // the MDP obtained from applying the strategy
    MDPSimple mdp;

    // memory size
    int memorySize = 0;

    protected Integer sample(Map<Integer,Double> distribution) throws PrismException
    {
	double r = Math.random();
	for(Map.Entry<Integer,Double> kv : distribution.entrySet()) {
	    r -= kv.getValue();
	    if(r <= 0)
		return kv.getKey();
	}
	throw new PrismException("Distribution invalid.");
    }

    @Override
    public void init(int state) throws InvalidStrategyStateException
    {
	// TODO
    }

    @Override
    public Distribution getNextMove(int state) throws InvalidStrategyStateException
    {
	return null; // TODO
    }

    @Override
    public void reset()
    {
	lastCorner = -1;
	lastState = -1;
    }

    @Override
    public void exportToFile(String file)
    {
	System.out.println("Exporting to file not supported yet.");
    }

    
    @Override
    public Model buildProduct(Model model) throws PrismException
    {
	return mdp;
    }

    @Override
    public void setInfo(String info)
    {
	this.info = info;
    }

    @Override
    public String getInfo()
    {
	return info;
    }
    
    @Override
    public int getMemorySize()
    {
	return memorySize;
    }
    
    @Override
    public String getType()
    {
	return "rCMQ HR strategy.";
    }
    
    @Override
    public Object getCurrentMemoryElement()
    {
	return null; // TODO
    }
    
    @Override
    public void setMemory(Object memory) throws InvalidStrategyStateException
    {
	System.out.println("Setting memory not supported.");
    }

    @Override
    public String getStateDescription()
    {
	return null;
    }

    @Override
    public int getInitialStateOfTheProduct(int s)
    {
	return 0;
    }


    private List<List<double[]>> selectGenerators(List<double[]> gs, List<double[]> tuple, int l, List<double[]> not)
    {
	if(not == null) {
	    not = new ArrayList<double[]>();
	}

	List<List<double[]>> output = new ArrayList<List<double[]>>();
	if(l==0){
	    if(tuple != null) {
		output.add(tuple);
	    }
	} else {
	    outer:
	    for(double[] g : gs) {
		if(not != null) {
		    for(double[] n : not) {
			if(g == n) { // ref comparison should be fine here
			    continue outer;
			}
		    }
		}
		List<double[]> new_tuple = new ArrayList<double[]>();
		if(tuple != null) {
		    new_tuple.addAll(tuple);
		}
		new_tuple.add(g);
		not.add(g);
		List<double[]> new_not = new ArrayList<double[]>();
		new_not.addAll(not);
		output.addAll(selectGenerators(gs, new_tuple, l-1, new_not));
	    }
	}
	
	return output;
    }


    // select all combinations of q^u_i from a list of length l of tuples
    private List<List<List<double[]>>> selectMultiGenerators(List<List<List<double[]>>> tuples, int successor_number, List<List<double[]>> multiTuple)
    {
	List<List<List<double[]>>> output = new ArrayList<List<List<double[]>>>();

	if(successor_number >= tuples.size()) { // base case
	    // generate new list of multituples
	    // put in currently computed tuple - if it exists
	    if(multiTuple != null) {
		output.add(multiTuple);
	    }
	} else {
	    for(int i = 0; i < tuples.get(successor_number).size(); i++) {
		List<List<double[]>> new_multiTuple = new ArrayList<List<double[]>>();
		if(multiTuple != null) {
		    new_multiTuple.addAll(multiTuple);
		}
		new_multiTuple.add(tuples.get(successor_number).get(i));
		output.addAll(selectMultiGenerators(tuples, successor_number+1, new_multiTuple));
	    }
	}

	return output;
    }

    private static Comparator<double[]> COMPARATOR = new Comparator<double[]>()
    {
	public int compare(double[] g1, double[] g2)
	{
	    double l1 = 0.0;
	    double l2 = 0.0;
	    for(int i = 0; i < g1.length; i++) {
		l1 += g1[i]*g1[i];
		l2 += g2[i]*g2[i];
	    }
	    return Double.compare(l2,l1);
	}
    };


    private List<double[]> gsToList(Generator_System gs, int dim)
    {
	List<double[]> result = new ArrayList<double[]>(gs.size());
	for(Generator g : gs) {
	    double[] p = new double[dim];
	    Linear_Expression le = g.linear_expression();
	    BigInteger d = g.divisor().getBigInteger();
	    Map<Variable, BigInteger> map = new HashMap<Variable, BigInteger>();
	    PPLSupport.getCoefficientsFromLinearExpression(le, false, BigInteger.ONE, map);
	    for(Variable k : map.keySet()) {
		if(k!=null) {
		    BigFraction value = new BigFraction(map.get(k), d);
		    p[k.id()] = value.doubleValue();
		}
	    }
	    result.add(p);
	}
	
	// exclude all non-pareto points
	boolean changed = true;
	while(changed) {
	    changed = false;
	    List<double[]> to_delete = new ArrayList<double[]>();
	    for(double[] p : result) {
		look_for_larger_point:
		for(double [] q : result) { // if there is a point (q) which is larger than p in all dimensions
		    if(p != q) {
			for(int i = 0; i < p.length; i++) {
			    if(p[i] > q[i]) {
				continue look_for_larger_point;
			    }
			}
			// fall through only if q is larger than p in all dimensions
			to_delete.add(p);
			break look_for_larger_point;
		    }
		}
	    }
	    if(to_delete.size() > 0) {
		result.removeAll(to_delete);
		changed = true;
	    }
	}
	
	// now sort the result to put in generators with decreasing length
	Collections.sort(result, COMPARATOR);

	return result;
    }

    public List<List<State>> simulateMDP(int samples) // returns a list of paths through the original game
    {
	List<List<State>> paths = new ArrayList<List<State>>(samples);

	// get initial state
	int initial_state = mdp.getFirstInitialState();

	// sample paths
	for(int sample = 0; sample < samples; sample++) {
	}

	return paths;
    }

    // directly construct MDP
    public MultiObjectiveStrategy(STPG G, int initial_state, double[] v, Map<Integer, Polyhedron> X, List<List<Polyhedron>> Y, List<STPGRewards> stpgRewards) throws PrismException
    {
	// store size of game and goal
	int N = G.getNumStates(); // number of states in game (excluding stochastic states)
	int L = ((int) X.get(0).space_dimension()); // total number of goals
	int M = L - stpgRewards.size(); // total number of probabilistic goals
	
	// need LP solver to get convex combinations
	SimplexSolver solver = new SimplexSolver(1.0e-5, 10);
	
	// store sets of points for successors
	List<List<double[]>> LIST_tuples;
	
	System.out.println("-------- CANONICAL ORDER -------------");
	// establish canonical order of corners
	List<double[]>[] LIST_gsX = new List[N];
	for(int t = 0; t < N; t++) {
	    LIST_gsX[t] = gsToList(X.get(t).minimized_generators(), L);
	    System.out.printf("\n %d[", t);
	    for(double[] g : LIST_gsX[t]) {
		System.out.printf("[");
		for(int ii = 0; ii < g.length; ii++) {
		    if(ii>0) System.out.printf(", ");
		    System.out.printf("%.4f", g[ii]);
		}
		System.out.printf("]");
	    }
	    System.out.printf("]");
	}


	System.out.println("-------------- BUILDING STATE SPACE ----------------");
	List<State> S = G.getStatesList();
	List<State> newS = new ArrayList<State>();

	// initial state - has two fields
	// 0: state in original game
	// 1: memory element, i.e. a corner of the polyhedron of field 0
	State s_init = new State(2);
	s_init.setValue(0, "init"); // special name
	s_init.setValue(1, 0);
	newS.add(s_init);

	Map<Integer,Map<Integer,State>> oldSandCornerToNewS = new HashMap<Integer,Map<Integer,State>>();
	// create new states
	// need to precompute, as the indices are later needed
	for(int s = 0; s < S.size(); s++) {
	    State state = S.get(s);
	    oldSandCornerToNewS.put(s, new HashMap<Integer,State>());
	    for(int corner = 0; corner < LIST_gsX[s].size(); corner++) {
		State new_state = new State(2);
		new_state.setValue(0, state);
		new_state.setValue(1, corner);
		newS.add(new_state);
		oldSandCornerToNewS.get(s).put(corner, new_state);
	    }
	}
	// printing state space
	for(int s = 0; s < newS.size(); s++) {
	    if(s!=0) System.out.printf(", ");
	    System.out.printf("%d: %s", s, newS.get(s));
	}
	System.out.println();

	System.out.println("-------------- INITIAL DISTRIBUTION ----------------");
	Distribution d_init = new Distribution();


	// put value of v - reward(t) into bounds
	double[] bounds = new double[L];
	for(int i = 0; i < L; i++) {
	    // lower bound on sum of betas
	    if(i < M) { // probability
		bounds[i] = v[i];
	    } else { // reward
		bounds[i] = v[i] - stpgRewards.get(i-M).getStateReward(initial_state);
	    }
	}

	boolean nothingfound = true;
	
	search_for_distribution:
	for(int l = 1; l < L+1; l++) { // first find l
	    // compute all possible combinations of q_i^u
	    LIST_tuples = selectGenerators(LIST_gsX[initial_state], null, l, null);
	    
	    // preparation for LP
	    double[] coeffs_beta = new double[l];
	    for(int i = 0; i < l; i++) {
		coeffs_beta[i] = 1;
	    }
	    // check for each such tuple
	    nothingfound = true;
	    iteration_through_tuples:
	    for(List<double[]> tuple : LIST_tuples) {
		// now formulate an LP for beta_i
		
		// max_{beta_i} sum_i beta
		// s.t. sum_i beta_i q_i^u >= v - rewards(t)
		//      sum_i beta_i <= 1
		
		// optimization function - maximize betas
		LinearObjectiveFunction f = new LinearObjectiveFunction(coeffs_beta, 0);
		
		// constraints
		List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
		double[][] coeffs_q = new double[L][l];
		for(int i = 0; i < l; i++) {
		    // get coefficients from tuple.get(i)
		    for(int k = 0; k < L; k++) {
			coeffs_q[k][i] = tuple.get(i)[k];
		    }
		}
		
		// put value of v - reward(t) into bounds
		for(int i = 0; i < L; i++) {
		    constraints.add(new LinearConstraint(coeffs_q[i], Relationship.GEQ, bounds[i]));
		}
		for(int i = 0; i < l; i++) {
		    // lower bound on beta_i
		    double[] onlyone = new double[l];
		    onlyone[i] = 1.0;
		    constraints.add(new LinearConstraint(onlyone, Relationship.GEQ, 0.0));
		}
		// upper bound on sum of beta
		constraints.add(new LinearConstraint(coeffs_beta, Relationship.LEQ, 1));
		
		PointValuePair solution;
		try{
		    solution = solver.optimize(f,
					       new LinearConstraintSet(constraints),
					       GoalType.MAXIMIZE,
					       new MaxIter(10000));
		} catch ( NoFeasibleSolutionException e) {
		    // tuple not feasible, try a different one
		    continue iteration_through_tuples;
		}
		nothingfound = false;
		
		// there has been no exception, so the problem was fasible
		// can extract the distribution now from the solution
		for(int i = 0; i < l; i++) {
		    State new_s_init = oldSandCornerToNewS.get(initial_state).get(LIST_gsX[initial_state].indexOf(tuple.get(i)));
		    d_init.add(newS.indexOf(new_s_init), solution.getPoint()[i]);
		}
		break search_for_distribution;
	    }
	}

	if(nothingfound) {
	    System.out.printf("Goal %s not realizable.\n", v);
	    throw new PrismException("Goal not realizable.\n");
	}

	// print initial distribution
	System.out.printf("initial distribution (at %d): %s\n", initial_state, d_init.toString());

	// now create a new MDP
	mdp = new MDPSimple(newS.size());
	mdp.setStatesList(newS);
	mdp.addChoice(0, d_init);
	mdp.addInitialState(0);


	System.out.println("------------- BUILDING TRANSITIONS ----------------");

	for (int t = 0; t < S.size(); t++) { // go through all states in S
	    // for each action need to build a new distribution

	    // indicate if searching for action
	    // true by default, and only turned off if for Player 1 a next move has been found
	    boolean search_for_action = true; // searching for action

	    // using search_for_action seems to cause a bug
	    for (int u = 0; u < G.getNumChoices(t); u++) { // for each stochastic successor (i.e. action u)
		System.out.printf("%d --%d--> \n", t, u);

		List<double[]> gsYtu = gsToList(Y.get(t).get(u).minimized_generators(), L);
		int ntu = G.getNumTransitions(t,u);
		Iterator<Entry<Integer,Double>> dtu;

		///// STOCHASTIC //////		
		// temporarily store results for stochastic states
		// specific to u
		Map<Integer,Map<Integer, Double>>[] stochastic = new Map[ntu];

		if(ntu >= 1) { // if the distribution is "interesting"
		    // interpret u as a stochastic state, and look at all its successors w
		    // first get tuples for each successor
		    List<List<List<double[]>>> LIST_succ_tuples = new ArrayList<List<List<double[]>>>();
		    dtu = G.getTransitionsIterator(t,u);
		    for(int w = 0; w < ntu; w++) {
			int key_w = dtu.next().getKey();
			stochastic[w] = new HashMap<Integer, Map<Integer,Double>>(); // initialize for each successor w of u
			List<List<double[]>> LIST_succ_tuple;
			look_for_nonempty_tuple:
			for(int l = L; l >= 1; l--) { // look for a tuple of sufficient size
			    LIST_succ_tuple = selectGenerators(LIST_gsX[key_w], null, l, null);
			    if(LIST_succ_tuple.size()!=0) {
				if(l < L) {
				    for(List<double[]> point : LIST_succ_tuple) {
					for(int ll = point.size(); ll < L; ll++) {
					    point.add(point.get(0));
					}
				    }
				}
				System.out.printf("LIST_succ_tuple %d length: %d\n", l, LIST_succ_tuple.size());
				LIST_succ_tuples.add(LIST_succ_tuple);
				break look_for_nonempty_tuple;
			    }
			}
			
		    }
		    
		    //List<List<List<double[]>>> LIST_multiTuples = selectMultiGenerators(LIST_succ_tuples, 0, null);
		    //System.out.printf("number of multituples: %d\n", LIST_multiTuples.size());

		    // here choose the distributions of the stochastic states
		    for (int p = 0; p < gsYtu.size(); p++) { // for each corner point in u
			//System.out.printf("Corner (p): %d\n", p);
			// bounds are p - reward
			bounds = new double[L];
			for(int k = 0; k < L; k++) {
			    if(k < M) { // probabilities
				bounds[k] = gsYtu.get(p)[k];
			    } else {
				bounds[k] = gsYtu.get(p)[k] - stpgRewards.get(k-M).getStateReward(t);
			    }
			}

			double[] coeffs_beta = new double[ntu*L];
			double[][] coeffs_beta_indiv = new double[ntu][L*ntu];
			for(int i = 0; i < L; i++) {
			    for(int w = 0; w < ntu; w++){
				coeffs_beta_indiv[w][w*L+i] = 1;
				coeffs_beta[w*L+i] = 1;
			    }
			}
			List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
			
			boolean multituples_left = true;
			Map<Integer,Integer> tuple_counters = new HashMap<Integer,Integer>();
			for(int i = 0; i < ntu; i++) {
			    tuple_counters.put(i, 0); // initialize all tuple_counters to zero
			}

			int multi_counter = 0;

			nothingfound = true;
			iteration_through_multi_tuples:
			while(multituples_left) {
			    multi_counter += 1;
			    //System.out.printf("Working on multi tuple %d\n", multi_counter);
			//for(List<List<double[]>> LIST_multiTuple : LIST_multiTuples) { // for each combination of tuples
			    
			    List<List<double[]>> LIST_multiTuple = new ArrayList<List<double[]>>();
			    // from each list of succ_tuple pick one succ_tuple and put it in the multi tuple
			    for(int i = 0; i < LIST_succ_tuples.size(); i++) { // for each successor
				List<List<double[]>> LIST_succ_tuple = LIST_succ_tuples.get(i);
				if(tuple_counters.get(i) == LIST_succ_tuple.size()) { // tuple counter reached bound
				    tuple_counters.put(i, 0); // reset tuple_counter
				    if(i+1 < LIST_succ_tuples.size()) {
					tuple_counters.put(i+1, tuple_counters.get(i+1)+1); // increase next tuplecounter
				    } else { // the last tuple counter reached the bound
					multituples_left = false;
		                        break iteration_through_multi_tuples;
				    }
				}
				LIST_multiTuple.add(LIST_succ_tuple.get(tuple_counters.get(i)));
			    }
			    //System.out.printf("MultiCounters: ");
	                    //for(int i = 0; i < LIST_succ_tuples.size(); i++) {
	                    //    System.out.printf("%d, ", tuple_counters.get(i));
                            //}
	                    //System.out.printf("\nMultituple size: %d\n", LIST_multiTuple.size());
                            

			    // increase the first tuple counter
			    tuple_counters.put(0, tuple_counters.get(0)+1);
			    
			    // formulate an LP that contains the following constraints
			    // sum_{w} /\(u,w) sum_i beta^w_i q^w_i
			    
			    // the objective function is sum_w sum_i beta^w_i
			    
			    // The LP then optimizes for all successors simultaneously,
			    // so get q^w_i and beta^w_i for each successor w
			    
			    // build objective function
			    // note: For now take L points in successor and don't try to optimize yet.
			    //       Would get a lot of optimization problems to actually calculate the
			    //       smallest number of points necessary.
			    
			    LinearObjectiveFunction f = new LinearObjectiveFunction(coeffs_beta, 0);
			    
			    // constraints
			    constraints.clear();
			    // now that all combinations of tuples are computed, can build the constraints
			    // first dimension is constraint
			    // second dimension is beta^w_i index
			    double[][] coeffs_q = new double[L][ntu*L];
			    dtu = G.getTransitionsIterator(t,u);
			    for(int w = 0; w < ntu; w++) { // for each successor w
				double delta_uw = dtu.next().getValue();
				for(int i = 0; i < L; i++) { // for each component
				    // get coefficients from tuple.get(i)
				    for(int k = 0; k < L; k++) {
					coeffs_q[k][w*L+i] = delta_uw * LIST_multiTuple.get(w).get(i)[k];
				    }
				}
			    }
			    

			    //System.out.printf("bounds: %s\n, coeffs: %s\n bounds_indiv: %s", Arrays.toString(bounds), Arrays.deepToString(coeffs_q), Arrays.deepToString(coeffs_beta_indiv));
			    for(int i = 0; i < L; i++) {
				// lower bound on sum of betas
				constraints.add(new LinearConstraint(coeffs_q[i], Relationship.GEQ, bounds[i]));
			    }
			    for(int i = 0; i < L*ntu; i++) {
				// lower bound on beta^w_i
				double[] onlyone = new double[L*ntu];
				onlyone[i] = 1.0;
				constraints.add(new LinearConstraint(onlyone, Relationship.GEQ, 0.0));
			    }
			    // upper bound on sums of betas
			    for(int w = 0; w < ntu; w++) { // for each successor w
				constraints.add(new LinearConstraint(coeffs_beta_indiv[w], Relationship.LEQ, 1.0));
			    }
			    
			    PointValuePair solution;
			    try{
				solution = solver.optimize(f,
							   new LinearConstraintSet(constraints),
							   GoalType.MAXIMIZE,
							   new MaxIter(10000));
			    } catch ( NoFeasibleSolutionException e) {
				// tuple not feasible, try a different one
				//System.out.println("infeasible.");
				continue iteration_through_multi_tuples;
			    }
			    nothingfound = false;
			    
			    // there has been no exception, so the problem was feasible
			    // can extract the distribution now from the solution
			    dtu = G.getTransitionsIterator(t,u);
			    for(int w = 0; w < ntu; w++) { // for each successor
				stochastic[w].put(p, new HashMap<Integer, Double>()); // initialize for each corner p of u
				int key_w = dtu.next().getKey();
				for(int i = 0; i < L; i++) { // for each dimension
				    Integer index = LIST_gsX[key_w].indexOf(LIST_multiTuple.get(w).get(i));
			            //System.out.printf("%s\n", LIST_gsX[key_w].toString());
	                            //System.out.printf("%s\n", Arrays.toString(LIST_multiTuple.get(w).get(i)));
				    double prob = solution.getPoint()[L*w+i];
				    prob = prob > 1.0 ? 1.0 : prob;
				    //System.out.printf("put in %f to w%d, p%d, index%d, i%d\n", prob, w, p, index, i);
			            if(!stochastic[w].get(p).containsKey(index)) { // if no multiple, just put in what probability is
				        stochastic[w].get(p).put(index, prob);
                                    } else { // if multiple, add probabilities, because don't know which one the LP-solver has assigned the probability mass to
                                        stochastic[w].get(p).put(index, prob + stochastic[w].get(p).get(index));
                                    }
				}
			    }
			    break iteration_through_multi_tuples;
			}
			//System.out.println(nothingfound);
		    } 
		} else if (ntu==1) { // distribution assigns only 1 to one successor
		    System.out.println("Simple Successors.");
		    stochastic[0] = new HashMap<Integer, Map<Integer,Double>>(); // initialize for each successor w of u
		    dtu = G.getTransitionsIterator(t,u);
		    int key_w = dtu.next().getKey(); // only one anyway
		    for (int p = 0; p < gsYtu.size(); p++) { // for each corner point in u
			stochastic[0].put(p, new HashMap<Integer, Double>()); // initialize for each corner p of u
			bounds = new double[L];
			for(int k = 0; k < L; k++) {
			    if(k < M) { // probabilities
				bounds[k] = gsYtu.get(p)[k];
			    } else {
				bounds[k] = gsYtu.get(p)[k] - stpgRewards.get(k-M).getStateReward(t);
			    }
			}
			
			search_for_point:
			for(double[] point : LIST_gsX[key_w]) {
			    for(int i = 0; i < L; i++) {
				if(point[i] < bounds[i]){
				    continue search_for_point;
				}
			    }
			    // here a point was found that is large enough - pick it w.p. 1.
			    stochastic[0].get(p).put(LIST_gsX[key_w].indexOf(point), 1.0);
			    // point found - no more necessary
			    break search_for_point;
			}
		    }
		}

		//System.out.println(Arrays.deepToString(stochastic));

		///// STOCHASTIC END ////
		
		// now for each corner point p for t, need to find a distribution
		// that is, find l, and l coefficients beta_i summing to one such that
		// for good and bad states: sum_i beta_i q_i^u >= p - rewards(t)
		// and for stochastic states: ...
		// for q^i_u in Y(t,u) - need to actually find these
		for (int p = 0; p < LIST_gsX[t].size(); p++) { // for each corner point in t
		    State origin = oldSandCornerToNewS.get(t).get(p); // specific to s and p only
		    Distribution d = new Distribution(); // specific to s, u and p
		    
		    // put value of p - reward(t) into bounds
		    bounds = new double[L];
		    for(int k = 0; k < L; k++) {
			if(k < M) { // probabilities
			    bounds[k] = LIST_gsX[t].get(p)[k];
			} else { // rewards
			    bounds[k] = LIST_gsX[t].get(p)[k] - stpgRewards.get(k-M).getStateReward(t);
			}
			
		    }
		    // find q_i^u and beta_i in Y(t,u)
		    search_for_distribution:
		    for(int l = 1; l < L+1; l++) { // first find l
			// compute all possible combinations of q_i^u
			LIST_tuples = selectGenerators(gsYtu, null, l, null);
			
			// preparation for LP
			double[] coeffs_beta = new double[l];
			for(int i = 0; i < l; i++) {
			    coeffs_beta[i] = 1;
			}
			// check for each such tuple
			iteration_through_tuples:
			for(List<double[]> LIST_tuple : LIST_tuples) {
			    //System.out.printf("tuple: %s\n", tuple.toString());
			    // now formulate an LP for beta_i
			    
			    // max_{beta_i} sum_i beta
			    // s.t. sum_i beta_i q_i^u >= p - rewards(t)
			    //      sum_i beta_i <= 1
			    
			    // describe the optimization problem
			    // optimization function - maximize betas
			    LinearObjectiveFunction f = new LinearObjectiveFunction(coeffs_beta, 0);
			    
			    // constraints
			    List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
			    double[][] coeffs_q = new double[L][l];
			    for(int i = 0; i < l; i++) {
				// get coefficients from tuple.get(i)
				for(int k = 0; k < L; k++) {
				    coeffs_q[k][i] = LIST_tuple.get(i)[k];
				}
			    }

			    //System.out.printf("coeffs (%d): %s\n", p, Arrays.deepToString(coeffs_q));

			    for(int i = 0; i < L; i++) {
				// lower bound on sum of betas
				constraints.add(new LinearConstraint(coeffs_q[i], Relationship.GEQ, bounds[i]));
			    }
			    for(int i = 0; i < l; i++) {
				// lower bound on beta_i
				double[] onlyone = new double[l];
				onlyone[i] = 1.0;
				constraints.add(new LinearConstraint(onlyone, Relationship.GEQ, 0.0));
			    }
			    // upper bound on sum of beta
			    constraints.add(new LinearConstraint(coeffs_beta, Relationship.LEQ, 1));
			    
			    PointValuePair solution;
			    try{
				solution = solver.optimize(f,
							   new LinearConstraintSet(constraints),
							   GoalType.MAXIMIZE,
							   new MaxIter(10000));
			    } catch ( NoFeasibleSolutionException e) {
				// tuple not feasible, try a different one
				continue iteration_through_tuples;
			    }
			    
			    // there has been no exception, so the problem was fasible
			    // can extract the distribution now from the solution
			    dtu = G.getTransitionsIterator(t,u);
			    for(int w = 0; w < ntu; w++) {
				Entry<Integer,Double> e_w = dtu.next();
				int key_w = e_w.getKey();
				double val_w = e_w.getValue();
				for(int i = 0; i < l; i++) {
				    Integer q_index = gsYtu.indexOf(LIST_tuple.get(i));
				    Map<Integer,Double> action = stochastic[w].get(q_index);
				    if(G.getPlayer(t) != STPGExplicit.PLAYER_1 || action != null) {
					double beta = solution.getPoint()[i]; // beta^u_i
					//System.out.printf("t:%d, p:%d, u:%d, w:%d, q_index:%d\n", t, p, u, w, q_index);
					for(Integer j : action.keySet()) {
					    double prob = beta*action.get(j);
					    prob = prob > 1.0 ? 1.0 : prob;
					    if(prob!=0.0) {
						State destination = oldSandCornerToNewS.get(key_w).get(j);
						d.add(newS.indexOf(destination), prob*val_w);
					    }
					}
				    }
				}
			    }
			    System.out.printf("%d --%d--> %d: %s\n", t, u, p, d.toString());

			    mdp.addChoice(newS.indexOf(origin), d);
			    // moreover, if good guy state, know that now u can be picked in t for p
			    if(G.getPlayer(t) == STPGExplicit.PLAYER_1) { // next move only defined for good guy
				search_for_action = false; // found a next move for the good guy
				// but can't break out of the loop because need to continue assigning
				// choices for al ather corners
			    }
			    break search_for_distribution; // a distribution for this l has been found
			}
		    }
		}
	    }
	}

	System.out.println("------------- RESULTING MDP ----------------");

	System.out.println(mdp.toString());

    }



    @Override
    public void updateMemory(int action, int state) throws InvalidStrategyStateException
    {
	// TODO
    }

}
