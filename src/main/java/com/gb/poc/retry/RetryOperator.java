package com.gb.poc.retry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * @author Bhadhara, Girish
 */
public class RetryOperator<T> {
    private final RetrySupplier<T> retrySupplier;
    private final int noOfRetry;
    private final int delayInterval;
    private final TimeUnit timeUnit;
    private final Predicate<T> retryPredicate;
    private final List<Class<? extends Exception>> exceptionList;
    private RetryListener listener;

    public static final class OperationBuilder<T> {
        private RetrySupplier<T> tRetrySupplier;
        private int iNoOfRetry;
        private int iDelayInterval;
        private TimeUnit iTimeUnit;
        private Predicate<T> tRetryPredicate;
        private Class<? extends Exception>[] exceptionClasses;
        private RetryListener listener;

        private OperationBuilder() {
        }

        public OperationBuilder<T> retrySupplier(final RetrySupplier<T> retrySupplier) {
            this.tRetrySupplier = retrySupplier;
            return this;
        }

        public OperationBuilder<T> noOfRetry(final int noOfRetry) {
            this.iNoOfRetry = noOfRetry;
            return this;
        }

        public OperationBuilder<T> onRetryAttempt(final RetryListener listener) {
            this.listener = listener;
            return this;
        }

        public OperationBuilder<T> delayInterval(final int delayInterval, final TimeUnit timeUnit) {
            this.iDelayInterval = delayInterval;
            this.iTimeUnit = timeUnit;
            return this;
        }

        public OperationBuilder<T> retryPredicate(final Predicate<T> retryPredicate) {
            this.tRetryPredicate = retryPredicate;
            return this;
        }

        @SafeVarargs
        public final OperationBuilder<T> retryOn(final Class<? extends Exception>... exceptionClasses) {
            this.exceptionClasses = exceptionClasses;
            return this;
        }

        public RetryOperator<T> build() {
            Objects.requireNonNull(tRetrySupplier);
            List<Class<? extends Exception>> exceptionList = new ArrayList<>();
            if (Objects.nonNull(exceptionClasses) && exceptionClasses.length > 0) {
                exceptionList = Arrays.asList(exceptionClasses);
            }

            if (iNoOfRetry == 0) {
                // always execute once at least
                iNoOfRetry = 1;
            }
            return new RetryOperator<>(tRetrySupplier, iNoOfRetry, iDelayInterval, iTimeUnit, tRetryPredicate, exceptionList, listener);
        }
    }

    public static <T> OperationBuilder<T> newBuilder() {
        return new OperationBuilder<>();
    }

    private RetryOperator(RetrySupplier<T> retrySupplier, int noOfRetry, int delayInterval, TimeUnit timeUnit,
                          Predicate<T> retryPredicate, List<Class<? extends Exception>> exceptionList, RetryListener listener) {
        this.retrySupplier = retrySupplier;
        this.noOfRetry = noOfRetry;
        this.delayInterval = delayInterval;
        this.timeUnit = timeUnit;
        this.retryPredicate = retryPredicate;
        this.exceptionList = exceptionList;
        this.listener = listener;
    }

    /**
     * Retry to evaluate the user method execution. It tries at least once and maximum of {@code noOfRetries} set,until
     * the given {@code retryPredicate}, if present, evaluates to {@code true} or any exception, if present, thrown is
     * listed in {@code exceptionList}. If {@code exceptionList} is not provided it will continue retry for any exception.
     */
    public T retry() {
        T result = null;
        int retries = 0;
        while (retries < noOfRetry) {
            try {
                fireListener(retries);
                result = retrySupplier.call();
                if (Objects.nonNull(retryPredicate)) {
                    if (retryPredicate.test(result)) {
                        retries = increaseRetryCountAndSleep(retries);
                    } else {
                        return result;
                    }
                } else {
                    // no retry condition defined, no exception thrown. This is the desired result.
                    return result;
                }
            } catch (Exception e) {
                retries = handleException(retries, e);
            }
        }
        return result;
    }

    private void fireListener(int retries) {
        if (Objects.nonNull(listener)) {
            listener.onRetry(retries);
        }
    }

    private int handleException(int retries, Exception e) {
        if (exceptionList.contains(e.getClass()) || (exceptionList.isEmpty())) {
            // exception is excepted, continue retry.
            retries = increaseRetryCountAndSleep(retries);
            if (retries == noOfRetry) {
                // evaluation is throwing exception, no more retry left. Throw it.
                throw new RetryFailureException(e);
            }
        } else {
            // unexpected exception, no retry required. Throw it.
            throw new RetryFailureException(e);
        }
        return retries;
    }

    private int increaseRetryCountAndSleep(int retries) {
        retries++;
        if (retries < noOfRetry) {
            sleepFor(delayInterval, timeUnit);
        }
        return retries;
    }

    private void sleepFor(int delayInterval, TimeUnit timeUnit) {
        if (delayInterval == 0 || Objects.isNull(timeUnit)) {
            return;
        }
        try {
            timeUnit.sleep(delayInterval);
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }
    }
}
