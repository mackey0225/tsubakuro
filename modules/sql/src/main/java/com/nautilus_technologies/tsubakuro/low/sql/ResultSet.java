package com.nautilus_technologies.tsubakuro.low.sql;

import java.io.Closeable;
import java.io.IOException;

/**
 * ResultSet type.
 */
public interface ResultSet extends Closeable {
    /**
     * Provides record metadata holding information about field type and nullability
     */
    public interface RecordMeta {
        /**
         * Get the field type
         * @param index field index. Must be equal to, or greater than 0. Must be less than the field count.
         * @return field type
         */
        CommonProtos.DataType at(int index) throws IOException;

        /**
         * Get the current field type
         * @return current field type
         */
        CommonProtos.DataType at() throws IOException;

        /**
         * Get the nullability for the field
         * @param index field index. Must be equal to, or greater than 0. Must be less than the field count.
         * @return true if the field is nullable
         */
        boolean nullable(int index) throws IOException;

        /**
         * Get the nullability for the field
         * @return true if the current field is nullable
         */
        boolean nullable() throws IOException;

        /**
         * Get the number of fields in the record
         * @return the number of the fields
         */
        long fieldCount();
    }

    /**
     * Get the record mata data of the ResultSet
     * @return RecordMeta subclass belonging to this class
     */
    RecordMeta getRecordMeta();

    /**
     * Move the current pointer to the next record
     * @return true if the next record exists
     */
    boolean nextRecord() throws IOException;

    /**
     * Check whether the current column is null or not
     * @return true if the current column is null
     */
    boolean isNull() throws IOException;

    /**
     * Get the current column value
     * @return the value of the current column
     */
    int getInt4() throws IOException;
    long getInt8() throws IOException;
    float getFloat4() throws IOException;
    double getFloat8() throws IOException;
    String getCharacter() throws IOException;

    /**
     * Proceed the currnet column position
     * @return true if the next column exists
     */
    boolean nextColumn();
}
