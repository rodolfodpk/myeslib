package org.myeslib.jdbi;

import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Clob;
import java.util.Map;

import org.myeslib.core.data.UnitOfWork;

import com.google.common.base.Function;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;

public class DefaultFromMapToUow implements Function<Map<String, Object>, UnitOfWork>{

	private final Gson gson;
	
	//@Inject
	public DefaultFromMapToUow(Gson gson) {
		super();
		this.gson = gson;
	}
	
	@Override
	public UnitOfWork apply(Map<String, Object> input) {
		Clob uowAsClob1 = (Clob) input.get("uow_data") ;
		try {
			Reader r = new InputStreamReader( uowAsClob1.getAsciiStream(), "UTF-8" ) ;
			String uowAsString1 = CharStreams.toString( r );
			r.close();
			UnitOfWork uow = gson.fromJson(uowAsString1, UnitOfWork.class);
			return uow;
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid value from database");
		}
	}

}
