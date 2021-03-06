package com.microsoft.azure.mobile.crashes;

import android.annotation.SuppressLint;
import android.app.Application;
import android.support.test.InstrumentationRegistry;

import com.microsoft.azure.mobile.Constants;
import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.MobileCenterPrivateHelper;
import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.crashes.ingestion.models.ManagedErrorLog;
import com.microsoft.azure.mobile.crashes.model.ErrorReport;
import com.microsoft.azure.mobile.crashes.utils.ErrorLogHelper;
import com.microsoft.azure.mobile.ingestion.models.Log;
import com.microsoft.azure.mobile.utils.HandlerUtils;
import com.microsoft.azure.mobile.utils.async.MobileCenterConsumer;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.lang.reflect.Method;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.azure.mobile.test.TestUtils.TAG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
public class CrashesAndroidTest {

    @SuppressLint("StaticFieldLeak")
    private static Application sApplication;

    private static Thread.UncaughtExceptionHandler sDefaultCrashHandler;

    private Channel mChannel;

    @BeforeClass
    public static void setUpClass() {
        sDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
        sApplication = (Application) InstrumentationRegistry.getContext().getApplicationContext();
        StorageHelper.initialize(sApplication);
        Constants.loadFromContext(sApplication);
    }

    @Before
    public void setUp() {
        Thread.setDefaultUncaughtExceptionHandler(sDefaultCrashHandler);
        StorageHelper.PreferencesStorage.clear();
        for (File logFile : ErrorLogHelper.getErrorStorageDirectory().listFiles()) {
            assertTrue(logFile.delete());
        }
        mChannel = mock(Channel.class);
    }

    @After
    public void tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(sDefaultCrashHandler);
    }

    private void startFresh(CrashesListener listener) throws Exception {

        /* Configure new instance. */
        MobileCenterPrivateHelper.unsetInstance();
        Crashes.unsetInstance();
        MobileCenter.setLogLevel(android.util.Log.VERBOSE);
        MobileCenter.configure(sApplication, "a");

        /* Clean logs. */
        MobileCenter.setEnabled(false);
        MobileCenter.setEnabled(true).get();

        /* Replace channel. */
        Method method = MobileCenter.class.getDeclaredMethod("getInstance");
        method.setAccessible(true);
        MobileCenter mobileCenter = (MobileCenter) method.invoke(null);
        method = MobileCenter.class.getDeclaredMethod("setChannel", Channel.class);
        method.setAccessible(true);
        method.invoke(mobileCenter, mChannel);

        /* Set listener. */
        Crashes.setListener(listener);

        /* Start crashes. */
        MobileCenter.start(Crashes.class);

        /* Wait for start. */
        assertTrue(Crashes.isEnabled().get());
    }

    @Test
    public void getLastSessionCrashReport() throws Exception {

        /* Null before start. */
        Crashes.unsetInstance();
        assertNull(Crashes.getLastSessionCrashReport().get());
        assertFalse(Crashes.hasCrashedInLastSession().get());

        /* Crash on 1st process. */
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = mock(Thread.UncaughtExceptionHandler.class);
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
        startFresh(null);
        assertNull(Crashes.getLastSessionCrashReport().get());
        assertFalse(Crashes.hasCrashedInLastSession().get());
        final Error exception = generateStackOverflowError();
        assertTrue(exception.getStackTrace().length > ErrorLogHelper.FRAME_LIMIT);
        final Thread thread = new Thread() {

            @Override
            public void run() {
                throw exception;
            }
        };
        thread.start();
        thread.join();

        /* Get last session crash on 2nd process. */
        startFresh(null);
        ErrorReport errorReport = Crashes.getLastSessionCrashReport().get();
        assertNotNull(errorReport);
        assertTrue(Crashes.hasCrashedInLastSession().get());
    }

    @Test
    public void testNoDuplicateCallbacksOrSending() throws Exception {

        /* Crash on 1st process. */
        assertFalse(Crashes.hasCrashedInLastSession().get());
        android.util.Log.i(TAG, "Process 1");
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = mock(Thread.UncaughtExceptionHandler.class);
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
        CrashesListener crashesListener = mock(CrashesListener.class);

        /* While testing should process, call methods that require the handler to test we avoid a dead lock and run directly. */
        when(crashesListener.shouldProcess(any(ErrorReport.class))).thenAnswer(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                assertNotNull(MobileCenter.getInstallId().get());
                return MobileCenter.isEnabled().get() && Crashes.isEnabled().get();
            }
        });
        when(crashesListener.shouldAwaitUserConfirmation()).thenReturn(true);
        startFresh(crashesListener);
        final Error exception = generateStackOverflowError();
        assertTrue(exception.getStackTrace().length > ErrorLogHelper.FRAME_LIMIT);
        final Thread thread = new Thread() {

            @Override
            public void run() {
                throw exception;
            }
        };
        thread.start();
        thread.join();
        assertEquals(ErrorLogHelper.FRAME_LIMIT, exception.getStackTrace().length);
        verify(uncaughtExceptionHandler).uncaughtException(thread, exception);
        assertEquals(2, ErrorLogHelper.getErrorStorageDirectory().listFiles().length);
        verifyZeroInteractions(crashesListener);

        /* Second process: enqueue log but network is down... */
        android.util.Log.i(TAG, "Process 2");
        startFresh(crashesListener);

        /* Check last session error report. */
        Crashes.getLastSessionCrashReport().thenAccept(new MobileCenterConsumer<ErrorReport>() {

            @Override
            public void accept(ErrorReport errorReport) {
                assertNotNull(errorReport);
                Throwable lastThrowable = errorReport.getThrowable();
                assertTrue(lastThrowable instanceof StackOverflowError);
                assertEquals(ErrorLogHelper.FRAME_LIMIT, lastThrowable.getStackTrace().length);
            }
        });
        assertTrue(Crashes.hasCrashedInLastSession().get());

        /* Wait U.I. thread callback (shouldAwaitUserConfirmation). */
        final Semaphore semaphore = new Semaphore(0);
        HandlerUtils.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                semaphore.release();
            }
        });
        semaphore.acquire();

        /* Waiting user confirmation so no log sent yet. */
        ArgumentMatcher<Log> matchCrashLog = new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object o) {
                return o instanceof ManagedErrorLog;
            }
        };
        verify(mChannel, never()).enqueue(argThat(matchCrashLog), anyString());
        assertEquals(2, ErrorLogHelper.getErrorStorageDirectory().listFiles().length);
        verify(crashesListener).shouldProcess(any(ErrorReport.class));
        verify(crashesListener).shouldAwaitUserConfirmation();
        verifyNoMoreInteractions(crashesListener);

        /* Confirm to resume processing. */
        final AtomicReference<Log> log = new AtomicReference<>();
        doAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                log.set((Log) invocationOnMock.getArguments()[0]);
                return null;
            }
        }).when(mChannel).enqueue(argThat(matchCrashLog), anyString());
        Crashes.notifyUserConfirmation(Crashes.ALWAYS_SEND);
        assertTrue(Crashes.isEnabled().get());
        verify(mChannel).enqueue(argThat(matchCrashLog), anyString());
        assertNotNull(log.get());
        assertEquals(1, ErrorLogHelper.getErrorStorageDirectory().listFiles().length);

        verify(crashesListener).getErrorAttachments(any(ErrorReport.class));
        verifyNoMoreInteractions(crashesListener);

        /* Third process: sending succeeds. */
        android.util.Log.i(TAG, "Process 3");
        mChannel = mock(Channel.class);
        ArgumentCaptor<Channel.GroupListener> groupListener = ArgumentCaptor.forClass(Channel.GroupListener.class);
        startFresh(crashesListener);
        verify(mChannel).addGroup(anyString(), anyInt(), anyInt(), anyInt(), groupListener.capture());
        groupListener.getValue().onBeforeSending(log.get());
        groupListener.getValue().onSuccess(log.get());

        /* Wait callback to be processed in background thread (file manipulations) then called back in UI. */

        /*
         * Wait background thread to process the 2 previous commands,
         * to do we check if crashed in last session, since we restarted process again after crash,
         * it's false even if we couldn't send the log yet.
         */
        assertFalse(Crashes.hasCrashedInLastSession().get());
        assertNull(Crashes.getLastSessionCrashReport().get());

        /* Wait U.I. thread callbacks. */
        HandlerUtils.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                semaphore.release();
            }
        });
        semaphore.acquire();

        assertEquals(0, ErrorLogHelper.getErrorStorageDirectory().listFiles().length);
        verify(mChannel, never()).enqueue(argThat(matchCrashLog), anyString());
        verify(crashesListener).onBeforeSending(any(ErrorReport.class));
        verify(crashesListener).onSendingSucceeded(any(ErrorReport.class));
        verifyNoMoreInteractions(crashesListener);

        /* Verify log was truncated to 256 frames. */
        assertTrue(log.get() instanceof ManagedErrorLog);
        ManagedErrorLog errorLog = (ManagedErrorLog) log.get();
        assertNotNull(errorLog.getException());
        assertNotNull(errorLog.getException().getFrames());
        assertEquals(ErrorLogHelper.FRAME_LIMIT, errorLog.getException().getFrames().size());
    }

    @Test
    public void cleanupFilesOnDisable() throws Exception {

        /* Crash. */
        android.util.Log.i(TAG, "Process 1");
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = mock(Thread.UncaughtExceptionHandler.class);
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
        startFresh(null);
        assertTrue(Crashes.isEnabled().get());
        final RuntimeException exception = new RuntimeException();
        final Thread thread = new Thread() {

            @Override
            public void run() {
                throw exception;
            }
        };
        thread.start();
        thread.join();
        verify(uncaughtExceptionHandler).uncaughtException(thread, exception);
        assertEquals(2, ErrorLogHelper.getErrorStorageDirectory().listFiles().length);

        /* Disable, test waiting for disable to finish. */
        Crashes.setEnabled(false).get();
        assertFalse(Crashes.isEnabled().get());
        assertEquals(0, ErrorLogHelper.getErrorStorageDirectory().listFiles().length);
    }

    @Test
    public void setEnabledWhileAlreadyEnabledShouldNotDuplicateCrashReport() throws Exception {

        /* Test the fix of the duplicate crash sending bug. */
        android.util.Log.i(TAG, "Process 1");
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = mock(Thread.UncaughtExceptionHandler.class);
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
        startFresh(null);
        Crashes.setEnabled(true);
        assertTrue(Crashes.isEnabled().get());
        final RuntimeException exception = new RuntimeException();
        final Thread thread = new Thread() {

            @Override
            public void run() {
                throw exception;
            }
        };
        thread.start();
        thread.join();
        verify(uncaughtExceptionHandler).uncaughtException(thread, exception);

        /* Check there are only 2 files: the throwable and the json one. */
        assertEquals(2, ErrorLogHelper.getErrorStorageDirectory().listFiles().length);
    }

    private Error generateStackOverflowError() {
        try {
            return generateStackOverflowError();
        } catch (StackOverflowError error) {
            return error;
        }
    }
}
