package com.sodao.dubbo.thrift.common;

import static com.sodao.dubbo.thrift.common.ThriftConstants.VERSION_1;
import static com.sodao.dubbo.thrift.common.ThriftConstants._ARGS;
import static com.sodao.dubbo.thrift.common.ThriftConstants._RESULT;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.thrift.TApplicationException;
import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMessageType;

import com.sodao.dubbo.thrift.transport.ThriftTransport;

/**
 * TBase帮助类
 * @author yankai
 * @date 2012-3-31
 */
public class TBaseTools {
	
	public static TBase<?,?> buffer2TBase_args(String iface, String methodName, byte[] buffer) throws Exception {
		TBase<?,?> _args = getTBaseObj(iface, methodName, new Class<?>[0], new Object[0], _ARGS);
		TDeserializer des = new TDeserializer();
		des.deserialize(_args, buffer);
		return _args;
	}
	
	public static byte[] tBase2buffer(TBase<?, ?> base) throws Exception {
		try {
			TSerializer ser = new TSerializer();
			byte[] arr = ser.serialize(base);
			return arr;
		} catch (TException e) {
			throw new IOException(e);
		}
	}
	
	//Map<serviceName, Class>, 减少loader class
	static final ConcurrentHashMap<String, Class<?>> cachedClass = new ConcurrentHashMap<String, Class<?>>();
	
	public static TBase<?, ?> getTBaseObj(String iface, String methodName, Class<?>[] parameterTypes, 
			Object[] initargs, String tag) throws Exception {
		String newIface = iface.substring(0, iface.lastIndexOf("$"))+ "$" + methodName + tag;
		Class<?> baseClazz = null;
		if (cachedClass.get(newIface) == null ) {
			baseClazz = Class.forName(newIface);
			cachedClass.putIfAbsent(newIface, baseClazz);
		} else {
			baseClazz = cachedClass.get(newIface);
		}
		
		Constructor<?> constructor = baseClazz.getConstructor(parameterTypes);
		return (TBase<?, ?>) constructor.newInstance(initargs);
	}
	
	public static List<Field> getTBase_argsFields(Class<?> clazz) throws Exception {
		Field[] fields = clazz.getDeclaredFields();
		List<Field> result = new ArrayList<Field>();
		for (Field f : fields) {
			f.setAccessible(true);
			if (f.getModifiers() == Modifier.PUBLIC) {
				result.add(f);
			}
		}
		return result;
	}
	
	public static Class<?>[] getParamterType(Class<?> clazz) throws Exception {
		List<Field> fields = getTBase_argsFields(clazz);
		Class<?>[]  paramTypes = new Class<?>[fields.size()];
		for (int i = 0; i < fields.size(); i++) {
			fields.get(i).setAccessible(true);
			paramTypes[i] = fields.get(i).getType();
		}
		return paramTypes;
	}
	
	public static Object[] getArgs(TBase<?, ?> obj) throws Exception {
		List<Field> fields = getTBase_argsFields(obj.getClass());
		Object[]  args = new Object[fields.size()];
		for (int i = 0; i < fields.size(); i++) {
			fields.get(i).setAccessible(true);
			args[i] = fields.get(i).get(obj);
		}
		return args;
	}
	
	public static Object getResult(byte[] buffer, String iface, String name, int seqId) throws Exception {
		TDeserializer deserializer = new TDeserializer();
		//获取空的result对象
		TBase<?, ?> _result = getTBaseObj(iface, name, new Class<?>[0], new Object[0], _RESULT);
		//通过byte数组放入成员变量
		deserializer.deserialize(_result, buffer);
		//有异常则抛异常，只有在方法声明有抛异常时，才有err1这个字段，这个异常是Thrift文件定义异常，不是普通java异常
		
		Field[] fields = _result.getClass().getDeclaredFields();
		//查找含有异常的字段
		for (Field f : fields) {
			if (f.getModifiers() == Modifier.PUBLIC
					&& TBase.class.isAssignableFrom(f.getType())
					&& Exception.class.isAssignableFrom(f.getType())) {
				f.setAccessible(true);
				if (f.get(_result) != null) {
					return f.get(_result);
				}
			}
		}
		
		Field success = null;
		try {
			success = _result.getClass().getDeclaredField("success");
			success.setAccessible(true);
			return success.get(_result);
		} catch (Exception e) {//没有success字段说明方法是void
			return void.class;
		}
	}
	
	public static void createErrorTMessage(ThriftBuffer buffer, String methodName, int seqId, String errMsg) {
		buffer.writeInt((VERSION_1 | TMessageType.EXCEPTION));
		buffer.writeUTF(methodName);
		buffer.writeInt(seqId);
		ThriftTransport transport = new ThriftTransport(null, buffer.getBuffer());
		TBinaryProtocol protocol = new TBinaryProtocol(transport);
		TApplicationException ex = new TApplicationException(TApplicationException.INTERNAL_ERROR, errMsg); 
		try {
			ex.write(protocol);
		} catch (TException e) {
			e.printStackTrace();
		}
	}
}
