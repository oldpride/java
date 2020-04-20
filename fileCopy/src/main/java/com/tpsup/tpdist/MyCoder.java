package com.tpsup.tpdist;

import java.util.ArrayList;
import java.util.HashMap;

public final class MyCoder {
	private int index = 0; // current char position in key_str
	public String key_str = null;
	private int key_length = 0;
	private byte[] key_array;

	public MyCoder(String key_str) {
		this.key_str = key_str;

		if (key_str != null && !key_str.isEmpty()) {
			this.key_array = key_str.getBytes();
			this.key_length = this.key_array.length;
			this.index = 0;
		}
	}

	public byte[] xor(byte[] plain_bytes, int size) {
		if (this.key_str == null || key_str.isEmpty()) {
			return plain_bytes;
		}

		byte[] xored_bytes = null;
		if (size >= 0) {
			xored_bytes = new byte[size];
		} else {
			xored_bytes = new byte[plain_bytes.length];
		}

		int i = 0;
		for (byte b : plain_bytes) {
			i++;
			if (size >= 0 && i > size) {
				break;
			}

			xored_bytes[i - 1] = (byte) (b ^ this.key_array[this.index]);

			this.index++;
			if (this.index == this.key_length) {
				this.index = 0;
			}
		}

		return xored_bytes;
	}

	// java doesn't support optional parameters. we have to delegate
	byte[] xor(byte[] plain_bytes) {
		return this.xor(plain_bytes, -1);
	}

	public int xorInplace(byte[] buffer, int size) {
		if (buffer.length < size ) {
			size = buffer.length;
		}
		if (this.key_str == null || key_str.isEmpty() || size <= 0) {
			return size;
		}

		for (int i=0; i<size; i++) {
			buffer[i] = (byte) (buffer[i] ^ this.key_array[this.index]);

			this.index++;
			if (this.index == this.key_length) {
				this.index = 0;
			}
		}

		return size;
	}

	public static void main(String[] args) {
		String plain = "hello world";
		MyCoder encoder = new MyCoder("abc");
		MyCoder decoder = new MyCoder("abc");
		byte[] xored = encoder.xor(plain.getBytes());
		MyLog.append(MyGson.gson.toJson(xored));
		byte[] xored2 = decoder.xor(xored);
		String plain2 = new String(xored2);
		MyLog.append(MyGson.gson.toJson(plain2));
		
		byte[] buffer = "hello2".getBytes();
		MyLog.append("original = " + MyGson.gson.toJson(buffer));
		int size = encoder.xorInplace(buffer, 3);
		MyLog.append("size = " + size + ", encoded = " + MyGson.gson.toJson(buffer));
		size = decoder.xorInplace(buffer, size);
		MyLog.append("size = " + size + ", decoded = " + MyGson.gson.toJson(buffer));
	}
}