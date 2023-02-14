package explicit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class EMDPExplicit extends ModelExplicit implements EMDP {

    protected Map<Integer, String> playerNames;
    protected StateOwnersSimple stateOwners;
    protected int environmentPlayer = -1;

    /**
     * Constructor: Empty EMDP.
     */
    public EMDPExplicit()
    {
        super();
        playerNames = new HashMap<>();
        stateOwners = new StateOwnersSimple();
    }

    /**
     * Constructor: EMDP with fixed number of states.
     * @param numStates _
     */
    public EMDPExplicit(int numStates)
    {
        this.numStates = numStates;
        playerNames = new HashMap<>();
        stateOwners = new StateOwnersSimple(numStates);
    }

    /**
     * Constructor: EMDP from an existing EMDP and a state index permutation.
     * @param emdp The EMDP to construct from.
     * @param permut State index {@code i} becomes index {@code permut[i]}
     */
    public EMDPExplicit(EMDPExplicit emdp, int permut[])
    {
        this(emdp.numStates);

        environmentPlayer = emdp.environmentPlayer;
        playerNames = emdp.playerNames;
        stateOwners = new StateOwnersSimple(emdp.numStates);

        // Create blank array of correct size
        for (int i = 0; i < numStates; i++) {
            stateOwners.addState(0);
        }
        // Copy permuted player info
        for (int i = 0; i < numStates; i++) {
            stateOwners.setPlayer(permut[i], emdp.stateOwners.getPlayer(i));
        }
    }

    /**
     * Constructor: Copy constructor.
     * @param emdp The EMDP to copy from.
     */
    public EMDPExplicit(EMDPExplicit emdp)
    {
        playerNames = new HashMap<>(emdp.playerNames);
        stateOwners = new StateOwnersSimple(emdp.stateOwners);
    }


    /**
     * Adds a new player 1 state.
     * @return The index of the added state.
     */
    public int addState()
    {
        return addState(1);
    }

    /**
     * Add a new (player {@code p}) state and return its index.
     * !! Note that if you're calling this from outside an EMDP implementation, it won't actually add a real state !!
     * @param player The player who owns the new state (0-indexed).
     */
    public int addState(int player)
    {
        stateOwners.addState(player);
        return numStates - 1;
    }

    /**
     * Add a number of new player-1 states.
     * @param numToAdd _
     */
    public void addStates(int numToAdd)
    {
        for (int i = 0; i < numToAdd; i++) {
            stateOwners.addState(1);
        }
    }

    /**
     * Set player {@code p} to own state {@code s}.
     * It is not checked whether {@code s} or {@code p} are in the correct range.
     */
    public void setPlayer(int s, int p)
    {
        stateOwners.setPlayer(s, p);
    }

    /**
     * Set the info about players, i.e., the (integer) index and name of each one.
     * This is given as a mapping from indices to names.
     * Indices can be arbitrary and do not need to be contiguous.
     * Names are optional and can be null or "" if undefined,
     * but all normally defined names should be unique.
     */
    public void setPlayerInfo(Map<Integer, String> playerNames)
    {
        this.playerNames = new HashMap<>(playerNames);
    }

    /**
     * Set the info about players, provided as a list of names.
     * Names are optional and can be null or "" if undefined,
     * but all normally defined names should be unique.
     */
    public void setPlayerInfo(List<String> playerNamesList)
    {
        this.playerNames = new HashMap<>();
        int numPlayers = playerNamesList.size();
        for (int i = 0; i < numPlayers; i++) {
            this.playerNames.put(i, playerNamesList.get(i));
        }
    }

    /**
     * Sets the info about players, additionally identifying an environment player.
     * The extra information shouldn't be necessary for functionality (as yet) but allows
     * for richer logging.
     */
    public void setPlayerInfo(List<String> playerNamesList, int environmentPlayer)
    {
        setPlayerInfo(playerNamesList);
        this.environmentPlayer = environmentPlayer;
    }

    /**
     * Get the player that owns state {@code s}.
     */
    public int getPlayer(int s)
    {
        return stateOwners.getPlayer(s);
    }

    @Override
    public String infoStringTable()
    {
        // Build rich(er) representation of states
        StringBuilder statesListRep = new StringBuilder("[");
        for (int i = 0; i < statesList.size(); i++) {
            statesListRep
                    .append(statesList.get(i))
                    .append(initialStates.contains(i) ? "*" : "")
                    .append(", ");
        }
        var len = statesListRep.length();
        statesListRep.replace(len - 2, len, "]");

        String s = "";
        s += "States:      " + statesListRep + "\n";
        s += "Transitions: " + getNumTransitions() + "\n";
        s += "Players:     " + playerNames + " with environment \""+playerNames.get(environmentPlayer)+"\" (player "+environmentPlayer+")\n";
        return s;
    }
}
