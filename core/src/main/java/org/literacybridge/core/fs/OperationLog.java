package org.literacybridge.core.fs;

import java.util.HashMap;
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
        <T> void put(String key, T value);
        void end(Map<String,String> info);
        void end();
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

    /**
     * Applications call this to start a timed operation.
     * @param name String that's meaningful to the application.
     * @return An Object implementing Operation.
     */
    public static Operation startOperation(String name) {
        return new OperationEvent(name);
    }

    /**
     * Implementation of Operation.
     */
    private static class OperationEvent implements Operation {
        private Map<String, String> info = new HashMap<>();
        private long startTime;
        private String name;

        private OperationEvent(String name) {
            this.name = name;
            this.startTime = System.currentTimeMillis();
        }

        /**
         * Put a key:value pair. Anything with a toString() for the value
         * @param key The key; any name that's meaningful to the application. Note that another
         *            put to the same name replaces the earlier value.
         * @param value Anything with a toString() as the value.
         */
        @Override
        public <T> void put(String key, T value) {
            info.put(key, value.toString());
        }

        /**
         * End the operation, and provide more info.
         * @param info A Map<String, String> of additional key:value pairs. Any keys here will overwrite
         *             any keys set through 'put()'.
         */
        @Override
        public void end(Map<String, String> info) {
            this.info.putAll(info);
            end();
        }

        /**
         * Simply end the operation.
         */
        @Override
        public void end() {
            info.put("time", Long.toString(System.currentTimeMillis()-startTime));
            OperationLog.logEvent(name, info);
        }
    }

}
