package com.voxflow.assistant.security;

public class OperationCancelledException extends RuntimeException {
    public OperationCancelledException() {
        super("Operation cancelled");
    }
}
