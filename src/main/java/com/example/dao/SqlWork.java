package com.example.dao;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface SqlWork<T> {
    T execute(Connection conn) throws SQLException;
}
