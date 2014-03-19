package org.myeslib.example.jdbi.modules;

import java.sql.SQLException;

import javax.inject.Singleton;
import javax.sql.DataSource;

import lombok.AllArgsConstructor;

import org.skife.jdbi.v2.DBI;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@AllArgsConstructor
public class DatabaseModule extends AbstractModule {

	int dbPoolMinConnections;
	int dbPoolMaxConnections;

	@Provides
	@Singleton
	public DataSource datasource() throws SQLException {
		HikariConfig config = new HikariConfig();
		config.setPoolName("db-pool-jdbi-example");
		config.setDataSourceClassName(System.getenv("DB_DATASOURCE_CLASS_NAME"));
		config.addDataSourceProperty("URL", System.getenv("DB_URL"));
		config.addDataSourceProperty("user", System.getenv("DB_USER"));
		config.addDataSourceProperty("password", System.getenv("DB_PASSWORD"));
		config.setMinimumPoolSize(dbPoolMinConnections);
		config.setMaximumPoolSize(dbPoolMaxConnections);
		config.setConnectionInitSql("select 1 + 1 from dual"); // oracle dialect
		config.setUseInstrumentation(true);
		config.setJdbc4ConnectionTest(true);
		return new HikariDataSource(config);
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


