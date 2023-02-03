package explicit;

import prism.ModelType;

public interface EMDP extends Model {

    @Override
    default ModelType getModelType() { return ModelType.EDMP; }

    @Override
    default String infoString()
    {
        String s = "";
        s += getNumStates() + " states (" + getNumInitialStates() + " initial)";
        s += ", " + getNumTransitions() + " transitions";
        return s;
    }

    @Override
    default String infoStringTable()
    {
        String s = "";
        s += "States:      " + getNumStates() + " (" + getNumInitialStates() + " initial)\n";
        s += "Transitions: " + getNumTransitions() + "\n";
        return s;
    }
}
