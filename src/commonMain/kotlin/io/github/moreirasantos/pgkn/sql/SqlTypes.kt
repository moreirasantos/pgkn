package io.github.moreirasantos.pgkn.sql

/**
 * The class that defines the constants that are used to identify generic SQL types.
 */
@Suppress("MagicNumber")
enum class SqlTypes(val value: Int) {
    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * `BIT`.
    </P> */
    BIT (-7),

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * `TINYINT`.
    </P> */
    TINYINT(-6) ,
    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * `SMALLINT`.
    </P> */
    SMALLINT (5),

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * `INTEGER`.
    </P> */
    INTEGER (4),

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * `BIGINT`.
    </P> */
    BIGINT (-5),

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * `FLOAT`.
    </P> */
    FLOAT (6),

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * `REAL`.
    </P> */
    REAL (7),

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * `DOUBLE`.
    </P> */
    DOUBLE (8),

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * `NUMERIC`.
    </P> */
    NUMERIC (2),

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * `DECIMAL`.
    </P> */
    DECIMAL (3),

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * `CHAR`.
    </P> */
    CHAR (1),

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * `VARCHAR`.
    </P> */
    VARCHAR (12),

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * `LONGVARCHAR`.
    </P> */
    LONGVARCHAR (-1),

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * `DATE`.
    </P> */
    DATE (91),

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * `TIME`.
    </P> */
    TIME (92),

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * `TIMESTAMP`.
    </P> */
    TIMESTAMP (93),

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * `BINARY`.
    </P> */
    BINARY (-2),

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * `VARBINARY`.
    </P> */
    VARBINARY (-3),

    /**
     * <P>The constant in the Java programming language, sometimes referred
     * to as a type code, that identifies the generic SQL type
     * `LONGVARBINARY`.
    </P> */
    LONGVARBINARY (-4),

    /**
     * <P>The constant in the Java programming language
     * that identifies the generic SQL value
     * `NULL`.
    </P> */
    NULL (0),

    /**
     * The constant in the Java programming language that indicates
     * that the SQL type is database-specific and
     * gets mapped to a Java object that can be accessed via
     * the methods `getObject` and `setObject`.
     */
    OTHER (1111),

    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type
     * `JAVA_OBJECT`.
     * @since 1.2
     */
    JAVA_OBJECT (2000),

    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type
     * `DISTINCT`.
     * @since 1.2
     */
    DISTINCT (2001),

    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type
     * `STRUCT`.
     * @since 1.2
     */
    STRUCT (2002),

    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type
     * `ARRAY`.
     * @since 1.2
     */
    ARRAY (2003),

    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type
     * `BLOB`.
     * @since 1.2
     */
    BLOB (2004),

    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type
     * `CLOB`.
     * @since 1.2
     */
    CLOB (2005),

    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type
     * `REF`.
     * @since 1.2
     */
    REF (2006),

    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type `DATALINK`.
     *
     * @since 1.4
     */
    DATALINK (70),

    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type `BOOLEAN`.
     *
     * @since 1.4
     */
    BOOLEAN (16),
    //------------------------- JDBC 4.0 -----------------------------------
    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type `ROWID`
     *
     * @since 1.6
     */
    ROWID (-8),

    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type `NCHAR`
     *
     * @since 1.6
     */
    NCHAR (-15),

    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type `NVARCHAR`.
     *
     * @since 1.6
     */
    NVARCHAR (-9),

    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type `LONGNVARCHAR`.
     *
     * @since 1.6
     */
    LONGNVARCHAR (-16),

    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type `NCLOB`.
     *
     * @since 1.6
     */
    NCLOB (2011),

    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type `XML`.
     *
     * @since 1.6
     */
    SQLXML (2009),
    //--------------------------JDBC 4.2 -----------------------------
    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type `REF CURSOR`.
     *
     * @since 1.8
     */
    REF_CURSOR (2012),

    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type
     * `TIME WITH TIMEZONE`.
     *
     * @since 1.8
     */
    TIME_WITH_TIMEZONE (2013),

    /**
     * The constant in the Java programming language, sometimes referred to
     * as a type code, that identifies the generic SQL type
     * `TIMESTAMP WITH TIMEZONE`.
     *
     * @since 1.8
     */
    TIMESTAMP_WITH_TIMEZONE (2014)
}