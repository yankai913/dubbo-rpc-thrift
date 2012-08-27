package com.sodao.dubbo.thrift.common;

import static com.sodao.dubbo.thrift.common.ThriftConstants.UTF8;

import java.io.InputStream;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

/**
 * 扩展ChannelBuffer
 * 
 * @author yankai
 * @date 2012-8-16
 */
public class ThriftBuffer {

	private final ChannelBuffer buffer;
	private final int size;
	
	public ThriftBuffer(int size) {
		this.size = size;
		this.buffer =  ChannelBuffers.dynamicBuffer(size);
	}
	
	public ThriftBuffer() {
		this.size = 256;
		this.buffer =  ChannelBuffers.dynamicBuffer();
	}
	
	public ThriftBuffer(byte[] src) {
		this();
		buffer.writeBytes(src);
	}
	
	public ThriftBuffer(InputStream input) {
		this();
		try {
			int b = input.read();
			while (b != -1) {
				buffer.writeByte((byte)b);
				b = input.read();
			}
		} catch (Exception e) {
			throw new IllegalStateException("Thirft buffer wrap inputstream error!");
		}
	}
	
	public int getSize() {
		return size;
	}
	
	public ChannelBuffer getBuffer() {
		return buffer;
	}
	
	public void writeUTF(String str) {
		try {
			byte[] arr = str.getBytes(UTF8);
			buffer.writeInt(arr.length);
			buffer.writeBytes(arr);
		} catch (Exception e) {
			throw new IllegalStateException("Thirft buffer WriteUTF error!");
		}
	}
	
	public String readUTF() {
		int leng = buffer.readInt();
		byte[] array = new byte[leng];
		buffer.readBytes(array);
		try {
			return new String(array, UTF8);
		} catch (Exception e) {
			throw new IllegalStateException("Thirft buffer readUTF error!");
		}
	}
	
	public void writeByte(byte value) {
		buffer.writeByte(value);
	}
	
	public int readByte() {
		return buffer.readByte();
	}
	
	public void writeInt(int value) {
		buffer.writeInt(value);
	}
	
	public int readInt() {
		return buffer.readInt();
	}
	
	public void writeBytes(byte[] src) {
		buffer.writeBytes(src);
	}
	
	public byte[] array() {
		byte[] dst = new byte[buffer.readableBytes()];
		buffer.readBytes(dst);
		return dst;
	}
	
	public void resetReaderIndex() {
		buffer.resetReaderIndex();
	}
	
	public void resetWriterIndex() {
		buffer.resetWriterIndex();
	}
}
