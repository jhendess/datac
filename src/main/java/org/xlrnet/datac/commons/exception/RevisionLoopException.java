package org.xlrnet.datac.commons.exception;

/**
 * Exception which indicates a revision loop.
 */
public class RevisionLoopException extends DatacTechnicalException {

    private String internalId;

    public RevisionLoopException(String internalId) {
        super("Encountered loop in revision " + internalId);
        this.internalId = internalId;
    }

    public String getInternalId() {
        return internalId;
    }
}
