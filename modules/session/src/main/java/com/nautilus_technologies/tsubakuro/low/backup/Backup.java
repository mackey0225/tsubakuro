package com.nautilus_technologies.tsubakuro.low.backup;

import java.util.Collection;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Backup type.
 */
public interface Backup extends Closeable {
    /**
     * Get a list of file path
     * @return List of file path to be backuped
     */
    Collection<? extends Path> files() throws IOException;
}
