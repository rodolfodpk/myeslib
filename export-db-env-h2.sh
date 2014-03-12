#!/bin/sh
DB_DATASOURCE_CLASS_NAME=org.h2.jdbcx.JdbcDataSource
#DB_URL=jdbc:h2:mem:test;MODE=Oracle
DB_URL=jdbc:h2:file:~/myeslib-database;MODE=Oracle
DB_USER=scott
DB_PASSWORD=tiger

export DB_DATASOURCE_CLASS_NAME
export DB_URL
export DB_USER
export DB_PASSWORD
