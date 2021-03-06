package com.microsoft.azure.mobile.crashes.ingestion.models;

import com.microsoft.azure.mobile.ingestion.models.AbstractLog;
import com.microsoft.azure.mobile.ingestion.models.json.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.UUID;

import static com.microsoft.azure.mobile.ingestion.models.CommonProperties.ID;

/**
 * Abstract error log.
 */
public abstract class AbstractErrorLog extends AbstractLog {

    private static final String PROCESS_ID = "process_id";

    private static final String PROCESS_NAME = "process_name";

    private static final String PARENT_PROCESS_ID = "parent_process_id";

    private static final String PARENT_PROCESS_NAME = "parent_process_name";

    private static final String ERROR_THREAD_ID = "error_thread_id";

    private static final String ERROR_THREAD_NAME = "error_thread_name";

    private static final String FATAL = "fatal";

    private static final String APP_LAUNCH_TOFFSET = "app_launch_toffset";

    private static final String ARCHITECTURE = "architecture";

    /**
     * Error identifier.
     */
    private UUID id;

    /**
     * Process identifier.
     */
    private Integer processId;

    /**
     * Process name.
     */
    private String processName;

    /**
     * Parent's process identifier.
     */
    private Integer parentProcessId;

    /**
     * Parent's process name.
     */
    private String parentProcessName;

    /**
     * Error thread identifier.
     */
    private Long errorThreadId;

    /**
     * Error thread name.
     */
    private String errorThreadName;

    /**
     * If true, this crash report is an application crash.
     */
    private Boolean fatal;

    /**
     * Corresponds to the number of milliseconds elapsed between the time the
     * error occurred and the app was launched.
     */
    private Long appLaunchTOffset;

    /**
     * CPU architecture.
     */
    private String architecture;

    /**
     * Get the id value.
     *
     * @return the id value
     */
    public UUID getId() {
        return this.id;
    }

    /**
     * Set the id value.
     *
     * @param id the id value to set
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Get the processId value.
     *
     * @return the processId value
     */
    public Integer getProcessId() {
        return this.processId;
    }

    /**
     * Set the processId value.
     *
     * @param processId the processId value to set
     */
    public void setProcessId(Integer processId) {
        this.processId = processId;
    }

    /**
     * Get the processName value.
     *
     * @return the processName value
     */
    public String getProcessName() {
        return this.processName;
    }

    /**
     * Set the processName value.
     *
     * @param processName the processName value to set
     */
    public void setProcessName(String processName) {
        this.processName = processName;
    }

    /**
     * Get the parentProcessId value.
     *
     * @return the parentProcessId value
     */
    public Integer getParentProcessId() {
        return this.parentProcessId;
    }

    /**
     * Set the parentProcessId value.
     *
     * @param parentProcessId the parentProcessId value to set
     */
    public void setParentProcessId(Integer parentProcessId) {
        this.parentProcessId = parentProcessId;
    }

    /**
     * Get the parentProcessName value.
     *
     * @return the parentProcessName value
     */
    public String getParentProcessName() {
        return this.parentProcessName;
    }

    /**
     * Set the parentProcessName value.
     *
     * @param parentProcessName the parentProcessName value to set
     */
    public void setParentProcessName(String parentProcessName) {
        this.parentProcessName = parentProcessName;
    }

    /**
     * Get the errorThreadId value.
     *
     * @return the errorThreadId value
     */
    public Long getErrorThreadId() {
        return this.errorThreadId;
    }

    /**
     * Set the errorThreadId value.
     *
     * @param errorThreadId the errorThreadId value to set
     */
    public void setErrorThreadId(Long errorThreadId) {
        this.errorThreadId = errorThreadId;
    }

    /**
     * Get the errorThreadName value.
     *
     * @return the errorThreadName value
     */
    public String getErrorThreadName() {
        return this.errorThreadName;
    }

    /**
     * Set the errorThreadName value.
     *
     * @param errorThreadName the errorThreadName value to set
     */
    public void setErrorThreadName(String errorThreadName) {
        this.errorThreadName = errorThreadName;
    }

    /**
     * Get the fatal value.
     *
     * @return the fatal value
     */
    public Boolean getFatal() {
        return this.fatal;
    }

    /**
     * Set the fatal value.
     *
     * @param fatal the fatal value to set
     */
    public void setFatal(Boolean fatal) {
        this.fatal = fatal;
    }

    /**
     * Get the appLaunchTOffset value.
     *
     * @return the appLaunchTOffset value
     */
    public Long getAppLaunchTOffset() {
        return this.appLaunchTOffset;
    }

    /**
     * Set the appLaunchTOffset value.
     *
     * @param appLaunchTOffset the appLaunchTOffset value to set
     */
    public void setAppLaunchTOffset(Long appLaunchTOffset) {
        this.appLaunchTOffset = appLaunchTOffset;
    }

    /**
     * Get the architecture value.
     *
     * @return the architecture value
     */
    public String getArchitecture() {
        return this.architecture;
    }

    /**
     * Set the architecture value.
     *
     * @param architecture the architecture value to set
     */
    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        super.read(object);
        setId(UUID.fromString(object.getString(ID)));
        setProcessId(JSONUtils.readInteger(object, PROCESS_ID));
        setProcessName(object.optString(PROCESS_NAME, null));
        setParentProcessId(JSONUtils.readInteger(object, PARENT_PROCESS_ID));
        setParentProcessName(object.optString(PARENT_PROCESS_NAME, null));
        setErrorThreadId(JSONUtils.readLong(object, ERROR_THREAD_ID));
        setErrorThreadName(object.optString(ERROR_THREAD_NAME, null));
        setFatal(JSONUtils.readBoolean(object, FATAL));
        setAppLaunchTOffset(JSONUtils.readLong(object, APP_LAUNCH_TOFFSET));
        setArchitecture(object.optString(ARCHITECTURE, null));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        super.write(writer);
        JSONUtils.write(writer, ID, getId());
        JSONUtils.write(writer, PROCESS_ID, getProcessId());
        JSONUtils.write(writer, PROCESS_NAME, getProcessName());
        JSONUtils.write(writer, PARENT_PROCESS_ID, getParentProcessId());
        JSONUtils.write(writer, PARENT_PROCESS_NAME, getParentProcessName());
        JSONUtils.write(writer, ERROR_THREAD_ID, getErrorThreadId());
        JSONUtils.write(writer, ERROR_THREAD_NAME, getErrorThreadName());
        JSONUtils.write(writer, FATAL, getFatal());
        JSONUtils.write(writer, APP_LAUNCH_TOFFSET, getAppLaunchTOffset());
        JSONUtils.write(writer, ARCHITECTURE, getArchitecture());
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        AbstractErrorLog that = (AbstractErrorLog) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (processId != null ? !processId.equals(that.processId) : that.processId != null)
            return false;
        if (processName != null ? !processName.equals(that.processName) : that.processName != null)
            return false;
        if (parentProcessId != null ? !parentProcessId.equals(that.parentProcessId) : that.parentProcessId != null)
            return false;
        if (parentProcessName != null ? !parentProcessName.equals(that.parentProcessName) : that.parentProcessName != null)
            return false;
        if (errorThreadId != null ? !errorThreadId.equals(that.errorThreadId) : that.errorThreadId != null)
            return false;
        if (errorThreadName != null ? !errorThreadName.equals(that.errorThreadName) : that.errorThreadName != null)
            return false;
        if (fatal != null ? !fatal.equals(that.fatal) : that.fatal != null) return false;
        if (appLaunchTOffset != null ? !appLaunchTOffset.equals(that.appLaunchTOffset) : that.appLaunchTOffset != null)
            return false;
        return architecture != null ? architecture.equals(that.architecture) : that.architecture == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (processId != null ? processId.hashCode() : 0);
        result = 31 * result + (processName != null ? processName.hashCode() : 0);
        result = 31 * result + (parentProcessId != null ? parentProcessId.hashCode() : 0);
        result = 31 * result + (parentProcessName != null ? parentProcessName.hashCode() : 0);
        result = 31 * result + (errorThreadId != null ? errorThreadId.hashCode() : 0);
        result = 31 * result + (errorThreadName != null ? errorThreadName.hashCode() : 0);
        result = 31 * result + (fatal != null ? fatal.hashCode() : 0);
        result = 31 * result + (appLaunchTOffset != null ? appLaunchTOffset.hashCode() : 0);
        result = 31 * result + (architecture != null ? architecture.hashCode() : 0);
        return result;
    }
}
