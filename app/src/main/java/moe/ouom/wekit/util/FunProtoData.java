package moe.ouom.wekit.util;

import static cn.hutool.core.convert.Convert.hexToBytes;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import moe.ouom.wekit.util.log.WeLogger;


public class FunProtoData {
    private final HashMap<Integer, List<Object>> values = new HashMap<>();

    public static byte[] getUnpPackage(byte[] b) {
        if (b == null) {
            return null;
        }
        if (b.length < 4) {
            return b;
        }
        if ((b[0] & 0xFF) == 0) {
            return Arrays.copyOfRange(b, 4, b.length);
        } else {
            return b;
        }
    }

    public void fromJSON(JSONObject json){
        try {
            Iterator<String> key_it = json.keys();
            while (key_it.hasNext()){
                String key = key_it.next();
                int k = Integer.parseInt(key);
                Object value = json.get(key);
                if (value instanceof JSONObject){
                    FunProtoData newProto = new FunProtoData();
                    newProto.fromJSON((JSONObject) value);
                    putValue(k, newProto);
                } else if (value instanceof JSONArray arr){
                    for (int i = 0; i < arr.length(); i++){
                        Object arr_obj = arr.get(i);
                        if (arr_obj instanceof JSONObject){
                            FunProtoData newProto = new FunProtoData();
                            newProto.fromJSON((JSONObject) arr_obj);
                            putValue(k, newProto);
                        } else {
                            putValue(k, arr_obj);
                        }
                    }
                } else {
                    putValue(k, value);
                }
            }
        } catch (Exception ignored){ }
    }

    private void putValue(int key, Object value){
        List<Object> list = values.computeIfAbsent(key, k -> new ArrayList<>());
        list.add(value);
    }

    public void fromBytes(byte[] b) throws IOException {
        CodedInputStream in = CodedInputStream.newInstance(b);
        while (in.getBytesUntilLimit() > 0) {
            int tag;
            try {
                tag = in.readTag();
            } catch (InvalidProtocolBufferException e) {
//                WeLogger.i(b.length + " - " + bytesToHex(b));
                throw new InvalidProtocolBufferException(e);
            }

            int fieldNumber = tag >>> 3;
            int wireType = tag & 7;
            if (wireType == 4 || wireType == 3 || wireType > 5) throw new IOException("Unexpected wireType: " + wireType);
            switch (wireType) {
                case 0:
                    putValue(fieldNumber, in.readInt64());
                    break;
                case 1:
                    putValue(fieldNumber, in.readRawVarint64());
                    break;
                case 2: {
                    byte[] subBytes = in.readByteArray();
                    try {
                        FunProtoData sub_data = new FunProtoData();
                        sub_data.fromBytes(subBytes);
                        putValue(fieldNumber, sub_data);
                    } catch (Exception e) {
                        try {
                            String decoded = new String(subBytes, StandardCharsets.UTF_8);
                            byte[] reEncoded = decoded.getBytes(StandardCharsets.UTF_8);
                            if (arraysEqual(subBytes, reEncoded)) {
                                putValue(fieldNumber, decoded);
                            } else {
                                putValue(fieldNumber, "hex->" + bytesToHex(subBytes));
                            }
                        } catch (Exception e2) {
                            putValue(fieldNumber, "hex->" + bytesToHex(subBytes));
                        }
                    }
                    break;
                }
                case 5:
                    putValue(fieldNumber, in.readFixed32());
                    break;
                default:
                    putValue(fieldNumber, "Unknown wireType: " + wireType);
                    break;
            }
        }
    }

    private static boolean arraysEqual(byte[] a1, byte[] a2) {
        if (a1.length != a2.length) return false;
        for (int i = 0; i < a1.length; i++) {
            if (a1[i] != a2[i]) return false;
        }
        return true;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }

    public JSONObject toJSON()throws Exception{
        JSONObject obj = new JSONObject();
        for (Integer k_index : values.keySet()){
            List<?> list = values.get(k_index);
            assert list != null;
            if (list.size() > 1){
                JSONArray arr = new JSONArray();
                for (Object o : list){
                    arr.put(valueToText(o));
                }
                obj.put(String.valueOf(k_index), arr);
            }else {
                for (Object o : list){
                    obj.put(String.valueOf(k_index), valueToText(o));
                }
            }
        }
        return obj;
    }
    private Object valueToText(Object value) throws Exception {
        if (value instanceof FunProtoData data){
            return data.toJSON();
        }else {
            return value;
        }
    }
    public byte[] toBytes() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        CodedOutputStream out = CodedOutputStream.newInstance(bos);
        try {
            for (Integer k_index : values.keySet()){
                List<?> list = values.get(k_index);
                if (list == null) continue;
                for (Object o : list){
                    if (o instanceof Number num) {
                        long val = num.longValue();
                        if (val < 0) {
                            out.writeInt64(k_index, val);
                        } else {
                            out.writeInt32(k_index, (int) val);
                        }
                    }
                    else if (o instanceof String s) {
                        if (s.startsWith("hex->")) {
                            byte[] hexBytes = hexToBytes(s.substring(5));
                            out.writeByteArray(k_index, hexBytes);
                        } else {
                            out.writeByteArray(k_index , s.getBytes(StandardCharsets.UTF_8));
                        }
                    }
                    else if (o instanceof FunProtoData data){
                        byte[] subBytes = data.toBytes();
                        out.writeTag(k_index, 2);
                        out.writeUInt32NoTag(subBytes.length);
                        out.writeRawBytes(subBytes);
                    }
                    else {
                        WeLogger.w("FunProtoData.toBytes Unknown type: " + o.getClass().getName());
                    }
                }
            }
            out.flush();
            return bos.toByteArray();
        } catch (Exception e){
            WeLogger.e("FunProtoData - toBytes", e);
            return new byte[0];
        }
    }
}
