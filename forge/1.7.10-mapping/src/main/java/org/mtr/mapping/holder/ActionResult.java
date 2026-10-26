package org.mtr.mapping.holder;

public enum ActionResult {
    SUCCESS, CONSUME, PASS, FAIL;
    public static ActionResult getSuccessMapped() { return SUCCESS; }
    public static ActionResult getFailMapped() { return FAIL; }
    public boolean isAccepted() { return this == SUCCESS || this == CONSUME; }
}
