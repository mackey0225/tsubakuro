package com.nautilus_technologies.tsubakuro;

/**
 * ResultSet type.
 */
public interface ResultSet {
    /**
     * Describes field type
     */
    enum FieldType {
        INT4("INT4", 1),
        INT8("INT8", 2),
        FLOAT4("FLOAT4", 3),
        FLOAT8("FLOAT8", 4),
        STRING("STRING", 5);

        private String label;
        private int type;

        FieldType(String label, int type) {
            this.label = label;
            this.type = type;
        }

        String getLabel() {
            return label;
        }

        int getType() {
            return type;
        }
    }
    /**
     * Provides record metadata holding information about field type and nullability
     */
    public interface RecordMeta {
        /**
         * Get the field type
         * @param index field index. Must be equal to, or greater than 0. Must be less than the field count.
         * @return field type
         */
        FieldType at(int index);

        /**
         * Get the nullability for the field
         * @param index field index. Must be equal to, or greater than 0. Must be less than the field count.
         * @return true if the field is nullable
         */
        boolean nullable(int index);

        /**
         * Get the number of fields in the record
         * @return the number of the fields
         */
        long fieldCount();
    }

    /**
     * Provides record object in the result set
     */
    public interface Cursor {
	/**
	 * Move the current pointer to the next record
	 * @return true if the next record exists
	 */
	boolean next();

	/**
	 * Check whether the current column is null or not
	 * @return true if the current column is null
	 */
        boolean isNull();

	/**
	 * Get the current column value and proceed the currnet column position
	 * @return the value of the current column
	 */
	int getInt4();
	long getInt8();
	float getFloat4();
	double getFloat8();
	String getCharacter();
    }

    /**
     * Get the record mata data of the ResultSet
     * @return RecordMeta subclass belonging to this class
     */
    RecordMeta getRecordMeta();

    /**
     * Get the cursor of the ReaultSet
     * @return Cursor subclass belonging to this class
     */
    Cursor getCursor();
}
