package com.multios.core;

/**
 * Callback that receives process output lines as they are streamed.
 */
@FunctionalInterface
public interface ProcessOutputListener {

    void onLine(ProcessOutputStream stream, String line);
}
