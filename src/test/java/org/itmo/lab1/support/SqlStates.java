package org.itmo.lab1.support;

import java.sql.SQLException;

public final class SqlStates {

    public static final String INSUFFICIENT_PRIVILEGE = "42501";
    public static final String CHECK_VIOLATION = "23514";
    public static final String UNIQUE_VIOLATION = "23505";
    public static final String FOREIGN_KEY_VIOLATION = "23503";

    private SqlStates() {
    }

    public static String of(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && sql.getSQLState() != null) {
                return sql.getSQLState();
            }
        }
        return null;
    }
}
