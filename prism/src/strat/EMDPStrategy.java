package strat;

import explicit.*;
import prism.PrismException;
import prism.PrismLog;

import java.util.TreeMap;

public class EMDPStrategy implements Strategy {

    private final EMDPSimple emdp;
    private final Extents extents;

    public EMDPStrategy(EMDPSimple emdp, Extents extents) {
        this.extents = extents;
        this.emdp = emdp;
    }

    @Override
    public Object getChoiceAction(int s, int m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getChoiceIndex(int s, int m) {
        return extents.getExtent(s).getSource((double) m);
    }

    @Override
    public int getNumStates() {
        return extents.size();
    }

    @Override
    public void exportActions(PrismLog out) throws PrismException {
        System.out.println("EMDPs do not have action labels; switching to exporting strategy as indices...");
        exportIndices(out);
    }

    @Override
    public void exportIndices(PrismLog out) throws PrismException {
        for (int i = 0; i < emdp.getNumStates(); i++) {
            if (emdp.getPlayer(i) == emdp.getEnvironmentPlayer()) continue; // environment players do not have strategies

            out.println("State "+i+":");
            var sources = new TreeMap<>(extents.getExtent(i).sourceMap());
            if (sources.isEmpty()) {
                out.println("    (empty)");
                continue;
            }

//            var initialState = emdp.getInitialStates().iterator().next(); // TODO support multiple initial states
            for (var pair : sources.entrySet()) {
                var energy = pair.getKey();
                var target = pair.getValue();
                var probSuccessHere = extents.getExtent(i).getProbabilityFor(energy);
//                var probSuccessFromInitial = extents.getExtent(initialState).getProbabilityFor(energy);
                out.println("    "+energy.intValue()+" energy -> State "+target+" ("+probSuccessHere+")");
            }
        }
    }

    @Override
    public void exportInducedModel(PrismLog out, int precision) throws PrismException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exportDotFile(PrismLog out, int precision) throws PrismException {
        throw new UnsupportedOperationException("TODO?");
    }

    @Override
    public void clear() { extents.clear(); }
}
