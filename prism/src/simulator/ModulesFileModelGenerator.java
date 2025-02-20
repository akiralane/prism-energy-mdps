package simulator;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import parser.State;
import parser.Values;
import parser.VarList;
import parser.ast.DeclarationType;
import parser.ast.Expression;
import parser.ast.LabelList;
import parser.ast.ModulesFile;
import parser.ast.RewardStruct;
import parser.type.Type;
import parser.type.TypeClock;
import prism.ModelGenerator;
import prism.ModelType;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLangException;
import prism.RewardGenerator;

public class ModulesFileModelGenerator implements ModelGenerator, RewardGenerator
{
	// Parent PrismComponent (logs, settings etc.)
	protected PrismComponent parent;
	
	// PRISM model info
	/** The original modules file (might have unresolved constants) */
	private ModulesFile originalModulesFile;
	/** The modules file used for generating (has no unresolved constants after {@code initialise}) */
	private ModulesFile modulesFile;
	private ModelType modelType;
	private Values mfConstants;
	private VarList varList;
	private LabelList labelList;
	private List<String> labelNames;
	
	// Model exploration info
	
	// State currently being explored
	private State exploreState;
	// Updater object for model
	protected Updater updater;
	// List of currently available transitions
	protected TransitionList transitionList;
	// Has the transition list been built? 
	protected boolean transitionListBuilt;
	// Global clock invariant (conjunction of per-module invariants)
	protected Expression invariant;
	
	/**
	 * Build a ModulesFileModelGenerator for a particular PRISM model, represented by a ModuleFile instance.
	 * Throw an explanatory exception if this is not possible.
	 * @param modulesFile The PRISM model
	 */
	public ModulesFileModelGenerator(ModulesFile modulesFile) throws PrismException
	{
		this(modulesFile, null);
	}
	
	/**
	 * Build a ModulesFileModelGenerator for a particular PRISM model, represented by a ModuleFile instance.
	 * Throw an explanatory exception if this is not possible.
	 * @param modulesFile The PRISM model
	 */
	public ModulesFileModelGenerator(ModulesFile modulesFile, PrismComponent parent) throws PrismException
	{
		this.parent = parent;
		
		// No support for system...endsystem yet
		if (modulesFile.getSystemDefn() != null) {
			throw new PrismException("The system...endsystem construct is not currently supported");
		}
		
		// Store basic model info
		this.modulesFile = modulesFile;
		this.originalModulesFile = modulesFile;
		modelType = modulesFile.getModelType();
		
		// If there are no constants to define, go ahead and initialise;
		// Otherwise, setSomeUndefinedConstants needs to be called when the values are available  
		mfConstants = modulesFile.getConstantValues();
		if (mfConstants != null) {
			initialise();
		}
	}
	
	/**
	 * (Re-)Initialise the class ready for model exploration
	 * (can only be done once any constants needed have been provided)
	 */
	private void initialise() throws PrismException
	{
		// Evaluate constants on (a copy) of the modules file, insert constant values and optimize arithmetic expressions
		modulesFile = (ModulesFile) modulesFile.deepCopy().replaceConstants(mfConstants).simplify();

		// Get info
		varList = modulesFile.createVarList();
		labelList = modulesFile.getLabelList();
		labelNames = labelList.getLabelNames();
		
		// Create data structures for exploring model
		updater = new Updater(modulesFile, varList, parent);
		if(modelType == ModelType.CSG)
			updater.initialiseCSG();
		transitionList = new TransitionList();
		transitionListBuilt = false;
	}
	
	// Methods for ModelInfo interface
	
	@Override
	public ModelType getModelType()
	{
		return modelType;
	}

	@Override
	public void setSomeUndefinedConstants(Values someValues) throws PrismException
	{
		setSomeUndefinedConstants(someValues, false);
	}

	@Override
	public void setSomeUndefinedConstants(Values someValues, boolean exact) throws PrismException
	{
		// We start again with a copy of the original modules file
		// and set the constants in the copy.
		// As {@code initialise()} can replace references to constants
		// with the concrete values in modulesFile, this ensures that we
		// start again at a place where references to constants have not
		// yet been replaced.
		modulesFile = (ModulesFile) originalModulesFile.deepCopy();
		modulesFile.setSomeUndefinedConstants(someValues, exact);
		mfConstants = modulesFile.getConstantValues();
		initialise();
	}
	
	@Override
	public Values getConstantValues()
	{
		return mfConstants;
	}
	
	@Override
	public boolean containsUnboundedVariables()
	{
		return modulesFile.containsUnboundedVariables();
	}
	
	@Override
	public int getNumVars()
	{
		return modulesFile.getNumVars();
	}
	
	@Override
	public List<String> getVarNames()
	{
		return modulesFile.getVarNames();
	}

	@Override
	public List<Type> getVarTypes()
	{
		return modulesFile.getVarTypes();
	}

	public DeclarationType getVarDeclarationType(int i) throws PrismException
	{
		return modulesFile.getVarDeclarationType(i);
	}
	
	@Override
	public int getVarModuleIndex(int i)
	{
		return modulesFile.getVarModuleIndex(i);
	}
	
	@Override
	public String getModuleName(int i)
	{
		return modulesFile.getModuleName(i);
	}
	
	@Override
	public VarList createVarList() throws PrismException
	{
		return varList;
	}
	
	@Override
	public boolean isVarObservable(int i)
	{
		return modulesFile.isVarObservable(i);
	}
	
	@Override
	public List<Object> getActions()
	{
		return modulesFile.getActions();
	}

	@Override
	public int getNumLabels()
	{
		return labelList.size();	
	}

	@Override
	public String getActionStringDescription()
	{
		return "Module/[action]";
	}
	
	@Override
	public List<String> getLabelNames()
	{
		return labelNames;
	}
	
	@Override
	public String getLabelName(int i) throws PrismException
	{
		return labelList.getLabelName(i);
	}
	
	@Override
	public int getLabelIndex(String label)
	{
		return labelList.getLabelIndex(label);
	}
	
	@Override
	public List<String> getObservableNames()
	{
		return modulesFile.getObservableNames();
	}
	
	@Override
	public List<String> getPlayerNames()
	{
		return modulesFile.getPlayerNames();
	}

	public int getEnvironmentPlayer() { return modulesFile.getEnvironmentPlayer(); }

	// Methods for ModelGenerator interface
	
	@Override
	public boolean hasSingleInitialState() throws PrismException
	{
		return modulesFile.getInitialStates() == null;
	}
	
	@Override
	public State getInitialState() throws PrismException
	{
		if (modulesFile.getInitialStates() == null) {
			return modulesFile.getDefaultInitialState();
		} else {
			// Inefficient but probably won't be called
			return getInitialStates().get(0);
		}
	}
	
	@Override
	public List<State> getInitialStates() throws PrismException
	{
		List<State> initStates = new ArrayList<State>();
		// Easy (normal) case: just one initial state
		if (modulesFile.getInitialStates() == null) {
			State state = modulesFile.getDefaultInitialState();
			initStates.add(state);
		}
		// Otherwise, there may be multiple initial states
		// For now, we handle this is in a very inefficient way
		else {
			Expression init = modulesFile.getInitialStates();
			List<State> allPossStates = varList.getAllStates();
			for (State possState : allPossStates) {
				if (init.evaluateBoolean(modulesFile.getConstantValues(), possState)) {
					initStates.add(possState);
				}
			}
		}
		return initStates;
	}

	@Override
	public void exploreState(State exploreState) throws PrismException
	{
		this.exploreState = exploreState;
		transitionListBuilt = false;
	}
	
	@Override
    public int getPlayerOwningState() throws PrismException
    {
		// Turn-based games only
		if (modelType.concurrent()) {
			return -1;
		}
		// Determine which player owns the state
		int player = -1;
		TransitionList transitions = getTransitionList();
		int nc = getNumChoices();
		for (int i = 0; i < nc; i++) {
			String modAct = transitions.getChoiceModuleOrAction(i);
			int iPlayer = modulesFile.getPlayerForModuleOrAction(modAct);
			if (player != -1 && iPlayer != -1 && iPlayer != player) {
				throw new PrismException("Choices for both player " + (player + 1) + " and " + (iPlayer + 1) + " in state " + exploreState);
			}
			if (iPlayer != -1) {
				player = iPlayer;
			}
		}
		// Assign deadlock states to player 1
		if (nc == 0) {
			player = 0;
		}
		// No assigned player: only allowed when the state is deterministic (one choice)
		// (in which case, assign the state to player 1)
		if (player == -1) {
			if (nc == 1) {
				player = 0;
			}
			// Otherwise, it's an error
			else {
				List<String> acts = new ArrayList<>();
				for (int i = 0; i < nc; i++) {
					String modAct = transitions.getChoiceModuleOrAction(i);
					if (modulesFile.getPlayerForModuleOrAction(modAct) == -1) {
						acts.add(getChoiceActionString(i));
					}
				}
				String errMsg = "There are multiple choices (" + String.join(",", acts) +  ") in state " + exploreState + " not assigned to any player"; 
				throw new PrismException(errMsg);
			}
		}
		// Make sure a valid player owns the state
		if (player < 0 || player >= getNumPlayers()) {
			throw new PrismException("State " + exploreState + " owned by invalid player (" + (player + 1) + ")");
		}
		return player;
    }
	
	@Override
	public int getNumChoices() throws PrismException
	{
		return getTransitionList().getNumChoices();
	}

	@Override
	public int getNumTransitions() throws PrismException
	{
		return getTransitionList().getNumTransitions();
	}

	@Override
	public int getNumTransitions(int i) throws PrismException
	{
		return getTransitionList().getChoice(i).size();
	}

	@Override
	public int getChoiceIndexOfTransition(int index) throws PrismException
	{
		return getTransitionList().getChoiceIndexOfTransition(index);
	}
	
	@Override
	public int getChoiceOffsetOfTransition(int index) throws PrismException
	{
		return getTransitionList().getChoiceOffsetOfTransition(index);
	}
	
	@Override
	public int getTotalIndexOfTransition(int i, int offset) throws PrismException
	{
		return getTransitionList().getTotalIndexOfTransition(i, offset);
	}
	
	@Override
	public Object getTransitionAction(int i, int offset) throws PrismException
	{
		TransitionList transitions = getTransitionList();
		int index = transitions.getTotalIndexOfTransition(i, offset);
		if (!modelType.concurrent()) {
			int a = transitions.getTransitionModuleOrActionIndex(index);
			return a < 0 ? null : getActions().get(a - 1);
		} else {
			int as[] = ((ChoiceListFlexi) transitions.getChoice(index)).getActions();
			return as;
		}
	}

	@Override
	public int getTransitionActionIndex(int i, int offset) throws PrismException
	{
		TransitionList transitions = getTransitionList();
		if (!modelType.concurrent()) {
			int a = transitions.getTransitionModuleOrActionIndex(transitions.getTotalIndexOfTransition(i, offset));
			return a < 0 ? -1 : a - 1;
		} else {
			throw new PrismException("Action index info not available"); 
		}
	}

	@Override
	public String getTransitionActionString(int i, int offset) throws PrismException
	{
		TransitionList transitions = getTransitionList();
		int index = transitions.getTotalIndexOfTransition(i, offset);
		if (!modelType.concurrent()) {
			int a = transitions.getTransitionModuleOrActionIndex(index);
			return getDescriptionForModuleOrActionIndex(a);
		} else {
			int as[] = ((ChoiceListFlexi) transitions.getChoice(i)).getActions();
			return getDescriptionForActionIndexList(as);
		}
	}
	
	@Override
	public Object getChoiceAction(int index) throws PrismException
	{
		TransitionList transitions = getTransitionList();
		if (!modelType.concurrent()) {
			int a = transitions.getChoiceModuleOrActionIndex(index);
			return a < 0 ? null : getActions().get(a - 1);
		} else {
			int as[] = ((ChoiceListFlexi) transitions.getChoice(index)).getActions();
			return as;
		}
	}

	@Override
	public int getChoiceActionIndex(int index) throws PrismException
	{
		TransitionList transitions = getTransitionList();
		if (!modelType.concurrent()) {
			int a = transitions.getChoiceModuleOrActionIndex(index);
			return a < 0 ? -1 : a - 1;
		} else {
			throw new PrismException("Action index info not available"); 
		}
	}

	@Override
	public String getChoiceActionString(int index) throws PrismException
	{
		TransitionList transitions = getTransitionList();
		if (!modelType.concurrent()) {
			int a = transitions.getChoiceModuleOrActionIndex(index);
			return getDescriptionForModuleOrActionIndex(a);
		} else {
			int as[] = ((ChoiceListFlexi) transitions.getChoice(index)).getActions();
			return getDescriptionForActionIndexList(as);
		}
	}

	@Override
	public int[] getTransitionIndexes(int i) 
	{
		return transitionList.getTransitionActionIndexes(i);
	}
	
	/**
	 * Utility method to get a description for an action label:
	 * "[a]" for a synchronous action a and "M" for an unlabelled
	 * action belonging to a module M. Takes in an integer index:
	 * -i for independent in ith module, i for synchronous on ith action
	 * (in both cases, modules/actions are 1-indexed) 
	 */ 
	private String getDescriptionForModuleOrActionIndex(int a)
	{
		if (a < 0) {
			return modulesFile.getModuleName(-a - 1);
		} else if (a > 0) {
			return "[" + modulesFile.getSynchs().get(a - 1) + "]";
		} else {
			return "?";
		}
	}
	
	/**
	 * Utility method to get a description for list of (concurrent) actions,
	 * given as an array of (1-indexed) indices into the list of all actions.
	 * An index of -1 indicates that a player idles.
	 * The format is "[a1,b2,-,c3]" with "-" denoting idle.
	 */
	private String getDescriptionForActionIndexList(int as[])
	{
		String s = "[";
		int n = as.length;
		if (n > 0) {
			s += as[0] == -1 ? "-" : getActions().get(as[0] - 1);
		}
		for (int i = 1; i < n; i++) {
			s += ",";
			s += as[i] == -1 ? "-" : getActions().get(as[i] - 1);
		}
		s += "]";
		return s;
	}
	
	@Override
	public Expression getChoiceClockGuard(int i) throws PrismException
	{
		TransitionList transitions = getTransitionList();
		return transitions.getChoice(i).getClockGuard();
	}
	
	@Override
	public double getTransitionProbability(int i, int offset) throws PrismException
	{
		TransitionList transitions = getTransitionList();
		return transitions.getChoice(i).getProbability(offset);
	}

	@Override
	public double getChoiceProbabilitySum(int i) throws PrismException
	{
		TransitionList transitions = getTransitionList();
		return transitions.getChoice(i).getProbabilitySum();
	}
	
	@Override
	public double getProbabilitySum() throws PrismException
	{
		TransitionList transitions = getTransitionList();
		return transitions.getProbabilitySum();
	}
	
	@Override
	public String getTransitionUpdateString(int i, int offset) throws PrismException
	{
		TransitionList transitions = getTransitionList();
		return transitions.getTransitionUpdateString(transitions.getTotalIndexOfTransition(i, offset), exploreState);
	}
	
	@Override
	public String getTransitionUpdateStringFull(int i, int offset) throws PrismException
	{
		TransitionList transitions = getTransitionList();
		return transitions.getTransitionUpdateStringFull(transitions.getTotalIndexOfTransition(i, offset));
	}
	
	@Override
	public State computeTransitionTarget(int index, int offset) throws PrismException
	{
		return getTransitionList().getChoice(index).computeTarget(offset, exploreState);
	}

	@Override
	public boolean isLabelTrue(int i) throws PrismException
	{
		Expression expr = labelList.getLabel(i);
		return expr.evaluateBoolean(exploreState);
	}
	
	@Override
	public Expression getClockInvariant() throws PrismException
	{
		// Compute the conjunction of all per-module invariants, if not already done
		if (invariant == null) {
			int numModules = modulesFile.getNumModules();
			for (int m = 0; m < numModules; m++) {
				Expression invariantMod = modulesFile.getModule(m).getInvariant();
				if (invariantMod != null) {
					invariant = (invariant == null) ? invariantMod : Expression.And(invariant, invariantMod);
				}
			}
		}
		if (invariant == null) {
			return null;
		}
		// Replace non-clock variables with their values and simplify
		int numVars = varList.getNumVars();
		State stateNoClocks = new State(exploreState);
		for (int v = 0; v < numVars; v++) {
			if (varList.getType(v) instanceof TypeClock) {
				stateNoClocks.varValues[v] = null;
			}
		}
		return (Expression) invariant.deepCopy().evaluatePartially(stateNoClocks).simplify();
	}
	
	@Override
	public State getObservation(State state) throws PrismException
	{
		if (!modelType.partiallyObservable()) {
			return null;
		}
		int numObservables = getNumObservables();
		State sObs = new State(numObservables);
		for (int i = 0; i < numObservables; i++) {
			Object oObs = modulesFile.getObservable(i).getDefinition().evaluate(modulesFile.getConstantValues(), state);
			sObs.setValue(i, oObs);
		}
		return sObs;
	}
	
	// Methods for RewardGenerator interface

	@Override
	public List<String> getRewardStructNames()
	{
		return modulesFile.getRewardStructNames();
	}
	
	@Override
	public boolean rewardStructHasStateRewards(int i)
	{
		return modulesFile.rewardStructHasStateRewards(i);
	}
	
	@Override
	public boolean rewardStructHasTransitionRewards(int i)
	{
		return modulesFile.rewardStructHasTransitionRewards(i);
	}
	
	@Override
	public double getStateReward(int r, State state) throws PrismException
	{
		RewardStruct rewStr = modulesFile.getRewardStruct(r);
		int n = rewStr.getNumItems();
		double d = 0;
		for (int i = 0; i < n; i++) {
			if (!rewStr.getRewardStructItem(i).isTransitionReward()) {
				Expression guard = rewStr.getStates(i);
				if (guard.evaluateBoolean(modulesFile.getConstantValues(), state)) {
					double rew = rewStr.getReward(i).evaluateDouble(modulesFile.getConstantValues(), state);
					// Check reward is finite/non-negative (would be checked at model construction time,
					// but more fine grained error reporting can be done here)
					// Note use of original model since modulesFile may have been simplified
					if (!Double.isFinite(rew)) {
						throw new PrismLangException("Reward structure is not finite at state " + state, originalModulesFile.getRewardStruct(r).getReward(i));
					}
					// NB: for now, disable negative reward check for CSGs 
					if (modelType != ModelType.CSG && rew < 0) {
						throw new PrismLangException("Reward structure is negative + (" + rew + ") at state " + state, originalModulesFile.getRewardStruct(r).getReward(i));
					}
					d += rew;
				}
			}
		}
		return d;
	}

	@Override
	public double getStateActionReward(int r, State state, Object action) throws PrismException
	{
		double d = 0;
		RewardStruct rewStr = modulesFile.getRewardStruct(r);
		int n = rewStr.getNumItems();
		Expression guard;

		if (modelType != ModelType.CSG) {
			String cmdAction;
			for (int i = 0; i < n; i++) {
				if (rewStr.getRewardStructItem(i).isTransitionReward()) {
					guard = rewStr.getStates(i);
					cmdAction = rewStr.getSynch(i);
					if (action == null ? (cmdAction.isEmpty()) : action.equals(cmdAction)) {
						if (guard.evaluateBoolean(modulesFile.getConstantValues(), state)) {
							double rew = rewStr.getReward(i).evaluateDouble(modulesFile.getConstantValues(), state);
							// Check reward is finite/non-negative (would be checked at model construction time,
							// but more fine grained error reporting can be done here)
							// Note use of original model since modulesFile may have been simplified
							if (!Double.isFinite(rew)) {
								throw new PrismLangException("Reward structure is not finite at state " + state, originalModulesFile.getRewardStruct(r).getReward(i));
							}
							if (rew < 0) {
								throw new PrismLangException("Reward structure is negative + (" + rew + ") at state " + state, originalModulesFile.getRewardStruct(r).getReward(i));
							}
							d += rew;
						}
					}
				}
			}
		}
		else {
			BitSet active = new BitSet();
			BitSet indexes = new BitSet();
			BitSet tmp;
			int[] actions = (int[]) action;
			for (int i = 0; i < actions.length; i++) {
				if (actions[i] != -1)
					active.set(actions[i]);
			}
			for (int i = 0; i < n; i++) {
				if (rewStr.getRewardStructItem(i).isTransitionReward()) {
					guard = rewStr.getStates(i);
					indexes.clear();
					for (int j : rewStr.getRewardStructItem(i).getSynchIndices()) {
						if (j != 0)
							indexes.set(j);
					}
					tmp = (BitSet) indexes.clone();
					tmp.andNot(active);
					if (indexes.isEmpty() || (!indexes.isEmpty() && tmp.isEmpty())) {
						if (guard.evaluateBoolean(modulesFile.getConstantValues(), state)) {
							double rew = rewStr.getReward(i).evaluateDouble(modulesFile.getConstantValues(), state);
							// Check reward is finite/non-negative (would be checked at model construction time,
							// but more fine grained error reporting can be done here)
							// Note use of original model since modulesFile may have been simplified
							if (!Double.isFinite(rew)) {
								throw new PrismLangException("Reward structure is not finite at state " + state, originalModulesFile.getRewardStruct(r).getReward(i));
							}
							// NB: for now, disable negative reward check for CSGs 
//							if (rew < 0) {
//								throw new PrismLangException("Reward structure is negative + (" + rew + ") at state " + state, originalModulesFile.getRewardStruct(r).getReward(i));
//							}
							d += rew;
						}
					}
				}
			}
		}

		return d;
	}

	// Local utility methods
	
	/**
	 * Returns the current list of available transitions, generating it first if this has not yet been done.
	 */
	private TransitionList getTransitionList() throws PrismException
	{
		// Compute the current transition list, if required
		if (!transitionListBuilt) {
			if(modelType == ModelType.CSG) {
				updater.calculateTransitionsCSG(exploreState, transitionList);
			}
			else {
 				updater.calculateTransitions(exploreState, transitionList);
			}				
			transitionListBuilt = true;
		}

		return transitionList;
	}
}
