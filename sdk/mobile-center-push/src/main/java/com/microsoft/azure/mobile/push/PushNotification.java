package com.microsoft.azure.mobile.push;

import android.support.annotation.NonNull;

import java.util.Map;

/**
 * Object describing a received push notification.
 */
@SuppressWarnings("WeakerAccess")
public class PushNotification {

    /**
     * Notification title.
     */
    private String mTitle;

    /**
     * Notification message.
     */
    private String mMessage;

    /**
     * Custom data.
     */
    private Map<String, String> mCustomData;

    /**
     * Init.
     */
    public PushNotification(String title, String message, @NonNull Map<String, String> customData) {
        mTitle = title;
        mMessage = message;
        mCustomData = customData;
    }

    /**
     * Get notification title.
     *
     * @return notification title or null if was not specified or if push received in background.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Get notification message.
     *
     * @return notification message or null if push received in background.
     */
    public String getMessage() {
        return mMessage;
    }

    /**
     * Get custom data.
     *
     * @return custom data with the push. Can be empty but not null.
     */
    public Map<String, String> getCustomData() {
        return mCustomData;
    }
}
