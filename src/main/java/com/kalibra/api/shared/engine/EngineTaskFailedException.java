package com.kalibra.api.shared.engine;

// The engine finished a task with a problem; status carries the meaning of its REST contract:
// 422 the input is bad (do not retry), 502 a provider failed (retry later), 409/500 need attention.
public class EngineTaskFailedException extends RuntimeException {

    private final int status;

    public EngineTaskFailedException(int status, String detail) {
        super(detail);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public boolean isRejectedInput() {
        return status == 422;
    }
}
