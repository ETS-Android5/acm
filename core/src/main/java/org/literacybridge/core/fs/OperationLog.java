package org.literacybridge.core.fs;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Log operations of the applications. Uploaded to server, to extract app metrics, usage, and updates.
 */

public class OperationLog {

    /**
     * OperationLog's required implementation. Applications implement this, then call 'setImplementation()'
     */
    public interface Implementation {
        void logEvent(String name, Map<String, String> info);
        void closeLogFile();
    }

    /**
     * For an operation with a time (ie, not simply a point in time), an application can get one of
     * these when the operation starts, put properties as they become available, and end it when it finishes.
     */
    public interface Operation {
        /**
         * Put a key:value pair. Anything with a toString() for the value
         * @param key The key; any name that's meaningful to the application. Note that another
         *            put to the same name replaces the earlier value.
         * @param value Anything with a toString() as the value.
         * @return this so that calls can be chained.
         */
        <T> Operation put(String key, T value);

        /**
         * Marks the time, by recording the value of the elapsed milliseconds as key. Note: only
         * applies to timed events; ignored otherwise.
         * @param key The name of the timer.
         * @return The Operation, so this can be chained with put()
         */
        Operation split(String key);

        /**
         * End the operation, and provide more info.
         * @param info A Map<String, String> of additional key:value pairs. Any keys here will overwrite
         *             any keys set through 'put()'.
         */
        void finish(Map<String,String> info);

        /**
         * Ends any timer, and saves the event.
         */
        void finish();
    }

    /**
     * Where applications set the implementation.
     */
    private static Implementation implementation;
    public static void setImplementation(Implementation implementation) {
        OperationLog.implementation = implementation;
    }

    /**
     * And application can call this and if there is an implementation, we'll forward the call.
     */
    public synchronized static void close() {
        if (implementation != null) {
            implementation.closeLogFile();
        }
    }

    /**
     * And application can call this and if there is an implementation, we'll forward the call.
     */
    public synchronized static void logEvent(String name, Map<String, String> info) {
        if (implementation != null) {
            implementation.logEvent(name, info);
        }
    }
    public synchronized static void logEvent(String name) {
        logEvent(name, null);
    }

    public static Operation log(String name) {
        return new OperationEvent(name);
    }

    /**
     * Applications call this to start a timed operation.
     * @param name String that's meaningful to the application.
     * @return An Object implementing Operation.
     */
    public static Operation startOperation(String name) {
        return new TimedOperationEvent(name);
    }

    /**
     * Implementation of Operation.
     */
    private static class OperationEvent implements Operation {
        private Map<String, String> info = new LinkedHashMap<>();
        private String name;

        private OperationEvent(String name) {
            this.name = name;
            assert name != null && name.length() > 0 : "Must provide a name for log entry.";
        }

        @Override
        public <T> Operation put(String key, T value) {
            info.put(key, value.toString());
            return this;
        }

        /**
         * Marks the time, by recording the value of the elapsed milliseconds as key.
         *
         * @param key The name of the timer.
         * @return The Operation, so this can be chained with put()
         */
        @Override
        public Operation split(String key) {
            return this;
        }

        @Override
        public void finish(Map<String, String> info) {
            this.info.putAll(info);
            finish();
        }
        @Override
        public void finish() {
            // Only record once.
            if (name != null) {
                OperationLog.logEvent(name, info);
                name = null;
            }
        }
    }
    private static class TimedOperationEvent extends OperationEvent {
        private static final String ELAPSED = "elapsedTime";
        private long startTime;
        private long splitStart;

        private TimedOperationEvent(String name) {
            super(name);
            this.startTime = System.currentTimeMillis();
            this.splitStart = startTime;
            this.put(ELAPSED, 0);
        }

        @Override
        public Operation split(String name) {
            long t = System.currentTimeMillis();
            this.put(name, Long.toString(t-splitStart));
            splitStart = t; // for next split
            return this;
        }
        @Override
        public void finish() {
            // Only record once.
            if (startTime > 0) {
                this.put(ELAPSED, Long.toString(System.currentTimeMillis() - startTime));
                startTime = -1;
                super.finish();
            }
        }
    }

}
