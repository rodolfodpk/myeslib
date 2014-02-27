package org.myeslib.example.jdbi.modules;

import javax.inject.Singleton;
import javax.sql.DataSource;

import org.h2.jdbcx.JdbcConnectionPool;
import org.skife.jdbi.v2.DBI;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class DatabaseModule extends AbstractModule {

	@Provides
	@Singleton
	public DataSource datasource() {
	    JdbcConnectionPool pool = JdbcConnectionPool.create("jdbc:h2:mem:test;MODE=Oracle", "scott", "tiger");
		//JdbcConnectionPool pool = JdbcConnectionPool.create("jdbc:h2:file:~/temp/testdb;MODE=Oracle", "scott", "tiger");
		pool.setMaxConnections(100);
		return pool;
	}

	@Provides
	@Singleton
	public DBI dbi(DataSource ds) {
		return new DBI(ds);
	}
		
	@Override
	protected void configure() {
	}
	
}


