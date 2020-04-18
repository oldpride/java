package com.tpsup.fileCopy;

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

    public byte[]xor(byte[]plain_bytes, int size) {
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
        for (byte b : plain_bytes){
            i ++;
            if (size >= 0 && i > size) {
                 break;
            }
                
            xored_bytes[i] = (byte) (b ^ this.key_array[this.index]);  

            this.index ++;
            if (this.index == this.key_length) {
                this.index = 0;
            }
         }
              
         return xored_bytes;
    }

    // java doesn't support optional parameters. we have to delegate
    byte[]xor(byte[]plain_bytes) {
       return this.xor(plain_bytes, -1);
    }
}