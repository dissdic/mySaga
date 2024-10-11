package com.example.demo.distributedTransaction.saga;

public class SagaWrapperException extends RuntimeException {

    private Exception wrappedException;
    public SagaWrapperException(Exception wrappedException) {
      this.wrappedException = wrappedException;
    }
    public SagaWrapperException(String message) {
        super(message);
    }

    public Exception getWrappedException() {
      return wrappedException;
    }

    public void setWrappedException(Exception wrappedException) {
      this.wrappedException = wrappedException;
    }
}
