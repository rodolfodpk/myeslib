package org.myeslib.hazelcast;

import java.io.IOException;

import com.google.gson.Gson;
import com.hazelcast.nio.serialization.ByteArraySerializer;

public class GsonSerializer implements ByteArraySerializer<Object> {

    final Gson gson ;
    final int type ;
    final Class<?> clazz;

    public GsonSerializer(Gson gson, int type, Class<?> clazz) {
        this.gson = gson;	
        this.type = type;
        this.clazz = clazz;
    }

	@Override
	public void destroy() {
	}

	@Override
	public int getTypeId() {
		return type;
	}

	@Override
	public Object read(byte[] data) throws IOException {
		String json = new String(data);
		Object result = gson.fromJson(json, clazz);
		return result;
	}

	@Override
	public byte[] write(Object object) throws IOException {
		String json = gson.toJson(object, clazz);
		return json.getBytes();
	}
 
}
