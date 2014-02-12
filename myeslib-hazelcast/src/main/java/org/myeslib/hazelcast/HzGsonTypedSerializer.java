package org.myeslib.hazelcast;

import java.io.IOException;
import java.lang.reflect.Type;

import lombok.AllArgsConstructor;

import com.google.gson.Gson;
import com.hazelcast.nio.serialization.ByteArraySerializer;

@AllArgsConstructor
public class HzGsonTypedSerializer implements ByteArraySerializer<Object> {

    final Gson gson ;
    final int typeId ;
    final Type type;;

	@Override
	public void destroy() {
	}

	@Override
	public int getTypeId() {
		return typeId;
	}

	@Override
	public Object read(byte[] data) throws IOException {
		String json = new String(data);
		Object result = gson.fromJson(json, type);
		return result;
	}

	@Override
	public byte[] write(Object object) throws IOException {
		String json = gson.toJson(object, type);
		return json.getBytes();
	}
 
}
