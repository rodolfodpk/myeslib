package org.myeslib.hazelcast.jdbi;

import java.io.IOException;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.util.TypedMapper;

import com.google.common.io.CharStreams;

public class ClobMapperToString extends TypedMapper<String> {
	
	public ClobMapperToString(){
		super();
	}

	public ClobMapperToString(int index)
    {
        super(index);
    }

	public ClobMapperToString(String name)
    {
        super(name);
    }
	
	@Override
	protected String extractByName(ResultSet r, String name) throws SQLException {
		Clob clob = r.getClob(name);
		try {
			return CharStreams.toString(clob.getCharacterStream());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			clob.free();
		}
		return null;	
	}

	@Override
	protected String extractByIndex(ResultSet r, int index) throws SQLException {
		Clob clob = r.getClob(index);
		try {
			return CharStreams.toString(clob.getCharacterStream());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			clob.free();
		}
		return null;		
	}

    public static final ClobMapperToString FIRST = new ClobMapperToString();
}
