package com.gb.poc.retry;

/**
 * @author Bhadhara, Girish
 */
public class RetryFailureException extends RuntimeException {
    public RetryFailureException(Exception e) {
        super(e);
    }
}
