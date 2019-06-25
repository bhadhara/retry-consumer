package com.gb.poc.retry;

import java.util.concurrent.Callable;

/**
 * @author Bhadhara, Girish
 */
@FunctionalInterface
public interface RetrySupplier<T> extends Callable<T> {
}
