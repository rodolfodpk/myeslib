package org.myeslib.hazelcast.jdbi;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.util.TypedMapper;

public class ClobMapper extends TypedMapper<Clob> {
	
	public ClobMapper(){
		super();
	}

	public ClobMapper(int index)
    {
        super(index);
    }

	public ClobMapper(String name)
    {
        super(name);
    }
	
	@Override
	protected Clob extractByName(ResultSet r, String name) throws SQLException {
		return r.getClob(name);
	}

	@Override
	protected Clob extractByIndex(ResultSet r, int index) throws SQLException {
		return r.getClob(index);
	}

    public static final ClobMapper FIRST = new ClobMapper();
}
