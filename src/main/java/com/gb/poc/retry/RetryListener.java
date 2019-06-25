package com.gb.poc.retry;

/**
 * @author Bhadhara, Girish
 */
public interface RetryListener {
    void onRetry(int attempt);
}
