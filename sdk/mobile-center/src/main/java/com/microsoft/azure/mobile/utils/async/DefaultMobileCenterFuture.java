package com.microsoft.azure.mobile.utils.async;

import com.microsoft.azure.mobile.utils.HandlerUtils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link MobileCenterFuture}.
 *
 * @param <T> result type.
 */
public class DefaultMobileCenterFuture<T> implements MobileCenterFuture<T> {

    /**
     * Lock used to wait or monitor result.
     */
    private final CountDownLatch mLatch = new CountDownLatch(1);

    /**
     * Result.
     */
    private T mResult;

    /**
     * Callbacks from thenAccept waiting for result.
     */
    private Collection<MobileCenterConsumer<T>> mConsumers;

    @Override
    public T get() {
        while (true) {
            try {
                mLatch.await();
                break;
            } catch (InterruptedException ignored) {
            }
        }
        return mResult;
    }

    @Override
    public boolean isDone() {
        while (true) {
            try {
                return mLatch.await(0, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public synchronized void thenAccept(final MobileCenterConsumer<T> function) {
        if (isDone()) {
            HandlerUtils.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    function.accept(mResult);
                }
            });
        } else {
            if (mConsumers == null) {
                mConsumers = new LinkedList<>();
            }
            mConsumers.add(function);
        }
    }

    /**
     * Set result.
     *
     * @param value result.
     */
    public synchronized void complete(final T value) {
        if (!isDone()) {
            mResult = value;
            mLatch.countDown();
            if (mConsumers != null) {
                HandlerUtils.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        /* No need to synchronize anymore as consumers cannot be modified anymore. */
                        for (MobileCenterConsumer<T> function : mConsumers) {
                            function.accept(value);
                        }
                        mConsumers = null;
                    }
                });
            }
        }
    }

}
