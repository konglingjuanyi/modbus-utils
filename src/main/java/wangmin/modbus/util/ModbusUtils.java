package wangmin.modbus.util;

import com.google.common.collect.Lists;

import wangmin.modbus.entity.type.ModbusDataType;
import wangmin.modbus.entity.type.ModbusByteOrderType;
import net.wimpi.modbus.io.BytesOutputStream;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.*;
import net.wimpi.modbus.net.TCPMasterConnection;
import net.wimpi.modbus.procimg.InputRegister;
import net.wimpi.modbus.procimg.Register;
import net.wimpi.modbus.procimg.SimpleInputRegister;

import java.net.InetAddress;
import java.util.List;

/**
 * Created by wm on 2017/1/3.
 */
public abstract class ModbusUtils {
    // 字节高低位转化 (该函数是本身的逆函数, 所以互转只需要实现一个)
    public static byte[] transferModbusDataBytesOrder(byte[] rowHexBytes, int startIdx, int len, ModbusByteOrderType bot) {
        if (bot == ModbusByteOrderType.HighFirstBigEndian) {   // 1234 -> 1234
            byte[] sb = new byte[len];
            for (int i = 0; i < len; i++) {
                sb[i] = rowHexBytes[startIdx + i];
            }
            return sb;
        } else if (bot == ModbusByteOrderType.LowFirstBigEndian) { // 3412 -> 1234
            byte[] sb = new byte[len];
            for (int i = 0; i < len; i++) {
                if ((i & 1) == 0) {
                    sb[i] = rowHexBytes[startIdx + (len - (i + 2))];
                } else {
                    sb[i] = rowHexBytes[startIdx + (len - i)];
                }
            }
            return sb;
        } else if (bot == ModbusByteOrderType.HighFirstLittleEndian) { // 2143 -> 1234
            byte[] sb = new byte[len];
            for (int i = 0; i < len; i+=2) {
                sb[i] = rowHexBytes[startIdx + i+1];
                sb[i+1] = rowHexBytes[startIdx + i];
            }
            return sb;
        } else {//if (bot == ModbusByteOrderType.LowFirstLittleEndian) {   // 4321 -> 1234
            byte[] sb = new byte[len];
            for (int i = 0; i < len; i++) {
                sb[i] = rowHexBytes[startIdx + (len - (i + 1))];
            }
            return sb;
        }
    }
    private static String generateDataStrFromMsg(ModbusResponse msg, int byteCount, ModbusDataType dtype, ModbusByteOrderType bot) {
        // 读取消息
        BytesOutputStream byteOut = new BytesOutputStream(9+byteCount);
        try {
            msg.writeTo(byteOut);
        } catch (Exception e) {
            return "";
        }
        byte[] msgBytes = byteOut.getBuffer();

        int dataBytesCount = msgBytes.length - 9;
        if (dataBytesCount > 0) {
            byte[] rawDataBytes = new byte[dataBytesCount];
            for (int i = 0; i < dataBytesCount; i++) {
                rawDataBytes[i] = msgBytes[i+9];
            }

            byte[] dataBytes = transferModbusDataBytesOrder(rawDataBytes, 0, rawDataBytes.length, bot);
            return ModbusDataUtils.convertDataToStr(dtype, dataBytes, true);
        }

        return "";
    }


    /**
     * 连接modbus服务器
     * */
    public static TCPMasterConnection connectionModbus(String ip, int port) throws Exception {
        InetAddress inetAddress = InetAddress.getByName(ip);
        TCPMasterConnection conn = new TCPMasterConnection(inetAddress);
        conn.setPort(port);
        conn.connect();
        return conn;
    }

    /**
     * 读取一个bit位
     * 返回 boolean
     */
    public static boolean readOneInputDigital(TCPMasterConnection conn, int slaveId, int address) throws Exception {
        ReadInputDiscretesRequest req = new ReadInputDiscretesRequest(address, 1);
        req.setUnitID(slaveId);
        ModbusTCPTransaction trans = new ModbusTCPTransaction(conn);
        trans.setRequest(req);
        trans.execute();

        ReadInputDiscretesResponse res = (ReadInputDiscretesResponse) trans.getResponse();

        return res.getDiscretes().getBit(0);
    }
    public static boolean readOneInputDigital(String ip, int port, int slaveId, int address) throws Exception {
        TCPMasterConnection conn = connectionModbus(ip, port);

        boolean result = readOneInputDigital(conn, slaveId, address);

        conn.close();

        return result;
    }

    /**
     * 读取多个bit位
     * 返回 boolean
     */
    public static List<Boolean> readInputDigitals(TCPMasterConnection conn, int slaveId, int address, int bitCount) throws Exception {
        ReadInputDiscretesRequest req = new ReadInputDiscretesRequest(address, bitCount);
        req.setUnitID(slaveId);
        ModbusTCPTransaction trans = new ModbusTCPTransaction(conn);
        trans.setRequest(req);
        trans.execute();

        ReadInputDiscretesResponse response = (ReadInputDiscretesResponse) trans.getResponse();

        List<Boolean> result = Lists.newArrayListWithCapacity(bitCount);
        for (int i=0; i<bitCount; ++i)
            result.add(response.getDiscretes().getBit(i));
        return result;
    }
    public static List<Boolean> readInputDigitals(String ip, int port, int slaveId, int address, int bitCount) throws Exception {
        TCPMasterConnection conn = connectionModbus(ip, port);

        List<Boolean> result = readInputDigitals(conn, slaveId, address, bitCount);

        conn.close();

        return result;
    }

    /**
     * 读取一个bit位
     * 返回 boolean
     */
    public static boolean readOneOutputDigital(TCPMasterConnection conn, int slaveId, int address) throws Exception {
        ReadCoilsRequest req = new ReadCoilsRequest(address, 1);
        req.setUnitID(slaveId);
        ModbusTCPTransaction trans = new ModbusTCPTransaction(conn);
        trans.setRequest(req);
        trans.execute();
        ReadCoilsResponse res = ((ReadCoilsResponse) trans.getResponse());

        return res.getCoils().getBit(0);
    }
    public static boolean readOneOutputDigital(String ip, int port, int slaveId, int address) throws Exception {
        TCPMasterConnection conn = connectionModbus(ip, port);

        boolean result = readOneOutputDigital(conn, slaveId, address);

        conn.close();

        return result;
    }

    /**
     * 读取多个bit位
     * 返回 boolean
     */
    public static List<Boolean> readOutputDigitals(TCPMasterConnection conn, int slaveId, int address, int bitCount) throws Exception {
        ReadCoilsRequest req = new ReadCoilsRequest(address, 1);
        req.setUnitID(slaveId);
        ModbusTCPTransaction trans = new ModbusTCPTransaction(conn);
        trans.setRequest(req);
        trans.execute();
        ReadCoilsResponse response = ((ReadCoilsResponse) trans.getResponse());

        List<Boolean> result = Lists.newArrayListWithCapacity(bitCount);
        for (int i=0; i<bitCount; ++i)
            result.add(response.getCoils().getBit(i));
        return result;
    }
    public static List<Boolean> readOutputDigitals(String ip, int port, int slaveId, int address, int bitCount) throws Exception {
        TCPMasterConnection conn = connectionModbus(ip, port);

        List<Boolean> result = readOutputDigitals(conn, slaveId, address, bitCount);

        conn.close();

        return result;
    }


    /**
     * 读取一个unsigned short
     * 返回 int
     */
    public static int readOneInputWord(TCPMasterConnection conn, int slaveId, int address) throws Exception {
        ReadInputRegistersRequest req = new ReadInputRegistersRequest(address, 1);
        req.setUnitID(slaveId);
        ModbusTCPTransaction trans = new ModbusTCPTransaction(conn);
        trans.setRequest(req);
        trans.execute();
        ReadInputRegistersResponse res = (ReadInputRegistersResponse) trans.getResponse();

        return res.getRegisterValue(0);
    }
    public static int readOneInputWord(String ip, int port, int slaveId, int address) throws Exception {
        TCPMasterConnection conn = connectionModbus(ip, port);

        int result = readOneInputWord(conn, slaveId, address);

        conn.close();

        return result;
    }


    /**
     * 根据数据类型 读取
     * dtype 字符显示形式
     * 返回 String
     */
    public static String readInputWordsStr(TCPMasterConnection conn, int slaveId, int address, ModbusDataType dtype, ModbusByteOrderType bot) throws Exception {
        int wordCount = dtype.getByteLength()/2;
        if (wordCount <= 0)
            wordCount = 1;

        ReadInputRegistersRequest req = new ReadInputRegistersRequest(address, wordCount);
        req.setUnitID(slaveId);
        ModbusTCPTransaction trans = new ModbusTCPTransaction(conn);
        trans.setRequest(req);
        trans.execute();
        ReadInputRegistersResponse res = (ReadInputRegistersResponse) trans.getResponse();

        return generateDataStrFromMsg(res, res.getByteCount(), dtype, bot);
    }
    public static String readInputWordsStr(String ip, int port, int slaveId, int address, ModbusDataType dtype, ModbusByteOrderType bot) throws Exception {
        TCPMasterConnection conn = connectionModbus(ip, port);

        String result = readInputWordsStr(conn, slaveId, address, dtype, bot);

        conn.close();

        return result;
    }


    /**
     * 读取一个 unsigned short
     * 返回 int
     */
    public static int readOneWord(TCPMasterConnection conn, int slaveId, int address) throws Exception {
        ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(address, 1);
        req.setUnitID(slaveId);
        ModbusTCPTransaction trans = new ModbusTCPTransaction(conn);
        trans.setRequest(req);
        trans.execute();
        ReadMultipleRegistersResponse res = (ReadMultipleRegistersResponse) trans.getResponse();

        return res.getRegisterValue(0);
    }
    public static int readOneWord(String ip, int port, int slaveId, int address) throws Exception {
        TCPMasterConnection conn = connectionModbus(ip, port);

        int result = readOneWord(conn, slaveId, address);

        conn.close();

        return result;
    }

    /**
     * 根据数据类型 读取
     * 返回 String
     */
    public static String readWordsStr(TCPMasterConnection conn, int slaveId, int address, ModbusDataType dtype, ModbusByteOrderType bot) throws Exception {
        int wordCount = dtype.getByteLength()/2;
        if (wordCount <= 0)
            wordCount = 1;

        ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(address, wordCount);
        req.setUnitID(slaveId);
        ModbusTCPTransaction trans = new ModbusTCPTransaction(conn);
        trans.setRequest(req);
        trans.execute();
        ReadMultipleRegistersResponse res = (ReadMultipleRegistersResponse) trans.getResponse();

        return generateDataStrFromMsg(res, res.getByteCount(), dtype, bot);
    }
    public static String readWordsStr(String ip, int port, int slaveId, int address, ModbusDataType dtype, ModbusByteOrderType bot) throws Exception {
        TCPMasterConnection conn = connectionModbus(ip, port);

        String result = readWordsStr(conn, slaveId, address, dtype, bot);

        conn.close();
        
        return result;
    }


    /**
     * 返回 byte[]
     */
    public static byte[] readInputWordsBytes(TCPMasterConnection conn, int slaveId, int address, int wordCount) throws Exception {
        ReadInputRegistersRequest req = new ReadInputRegistersRequest(address, wordCount);
        req.setUnitID(slaveId);
        ModbusTCPTransaction trans = new ModbusTCPTransaction(conn);
        trans.setRequest(req);
        trans.execute();
        ReadInputRegistersResponse response = (ReadInputRegistersResponse) trans.getResponse();

        InputRegister[] data = response.getRegisters();
        if (data != null && data.length > 0) {
            byte[] bytes = new byte[wordCount * 2];
            for (int i = 0; i < wordCount; i++) {
                byte[] registerBytes = data[i].toBytes();
                bytes[i * 2] = registerBytes[0];
                bytes[i * 2 + 1] = registerBytes[1];
            }
            return bytes;
        } else {
            throw new Exception();
        }
    }
    public static byte[] readInputWordsBytes(String ip, int port, int slaveId, int address, int wordCount) throws Exception {
        TCPMasterConnection conn = connectionModbus(ip, port);

        byte[] result = readInputWordsBytes(conn, slaveId, address, wordCount);

        conn.close();

        return result;
    }

    /**
     */
    public static byte[] readWordsBytes(TCPMasterConnection conn, int slaveId, int address, int wordCount) throws Exception {
        ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(address, wordCount);
        req.setUnitID(slaveId);
        ModbusTCPTransaction trans = new ModbusTCPTransaction(conn);
        trans.setRequest(req);
        trans.execute();
        ReadMultipleRegistersResponse response = (ReadMultipleRegistersResponse) trans.getResponse();

        Register[] data = response.getRegisters();
        if (data != null && data.length > 0) {
            byte[] bytes = new byte[wordCount * 2];
            for (int i = 0; i < wordCount; i++) {
                byte[] registerBytes = data[i].toBytes();
                bytes[i * 2] = registerBytes[0];
                bytes[i * 2 + 1] = registerBytes[1];
            }
            return bytes;
        } else {
            throw new Exception();
        }
    }
    public static byte[] readWordsBytes(String ip, int port, int slaveId, int address, int wordCount) throws Exception {
        TCPMasterConnection conn = connectionModbus(ip, port);

        byte[] result = readWordsBytes(conn, slaveId, address, wordCount);

        conn.close();

        return result;
    }


    /**
     * 写入 一个bit位数据 到 真机的DO类型的寄存器上面, 线圈
     *
     * @param conn
     * @param address
     * @param slaveId
     * @param value
     */
    public static boolean writeOneDigitalOutput(TCPMasterConnection conn, int slaveId, int address, boolean value) throws Exception {
        ModbusTCPTransaction trans = new ModbusTCPTransaction(conn);
        WriteCoilRequest req = new WriteCoilRequest(address, value);
        req.setUnitID(slaveId);
        trans.setRequest(req);
        trans.execute();
        return true;
    }
    public static boolean writeOneDigitalOutput(String ip, int port, int slaveId, int address, boolean value) throws Exception {
        TCPMasterConnection conn = connectionModbus(ip, port);

        writeOneDigitalOutput(conn, slaveId, address, value);

        conn.close();
        return true;
    }

    /**
     * 写入 一个字节数据 到 真机，数据类型是RE 保持寄存器
     *
     * @param conn
     * @param address
     * @param slaveId
     * @param value
     */
    public static boolean writeOneWord(TCPMasterConnection conn, int slaveId, int address, int value) throws Exception {
        ModbusTCPTransaction trans = new ModbusTCPTransaction(conn);
        SimpleInputRegister register = new SimpleInputRegister();
        register.setValue(value);
        WriteSingleRegisterRequest req = new WriteSingleRegisterRequest(address, register);
        req.setUnitID(slaveId);
        trans.setRequest(req);
        trans.execute();
        return true;
    }
    public static boolean writeOneWord(String ip, int port, int slaveId, int address, int value) throws Exception {
        TCPMasterConnection conn = connectionModbus(ip, port);

        writeOneWord(conn, slaveId, address, value);

        conn.close();
        return true;
    }

    /**
     * 写入 多个字节数据 到 真机，数据类型是RE 保持寄存器
     *
     * @param conn
     * @param address
     * @param slaveId
     * @param wordValues
     */
    public static boolean writeWords(TCPMasterConnection conn, int slaveId, int address, List<Integer> wordValues) throws Exception {
        ModbusTCPTransaction trans = new ModbusTCPTransaction(conn);

        SimpleInputRegister[] registers = new SimpleInputRegister[wordValues.size()];
        for (int i=0; i<wordValues.size(); ++i) {
            SimpleInputRegister register = new SimpleInputRegister();
            register.setValue(wordValues.get(i));

            registers[i] = register;
        }

        WriteMultipleRegistersRequest req = new WriteMultipleRegistersRequest(address, registers);
        req.setUnitID(slaveId);
        trans.setRequest(req);

        trans.execute();

        return true;
    }
    public static boolean writeWords(String ip, int port, int slaveId, int address, List<Integer> wordValues) throws Exception {
        TCPMasterConnection conn = connectionModbus(ip, port);

        writeWords(conn, slaveId, address, wordValues);

        conn.close();
        return true;
    }

    /**
     * 写入 一条数据 到 真机，数据类型是RE 保持寄存器
     *
     * @param conn
     * @param address
     * @param slaveId
     * @param dataStr
     * @param dtype
     */
    public static boolean writeWordsByDataStr(TCPMasterConnection conn, int slaveId, int address, String dataStr, ModbusDataType dtype, ModbusByteOrderType bot) throws Exception {
        byte[] bytes = ModbusDataUtils.convertStrToData(dtype, dataStr, false);
        if (bytes.length <= 0)
            return false;
        bytes = transferModbusDataBytesOrder(bytes, 0, bytes.length, bot);

        int wordCount = (bytes.length/2) + (bytes.length & 1);

        List<Integer> wordValues = Lists.newArrayListWithCapacity(wordCount);
        for (int i=0; i<bytes.length; i+=2) {
            int intValue;
            if (i+2 <= bytes.length)    // 两个字节
                intValue = (int) BinaryUtils.bytesToShort(bytes, i);
            else    // 只有一个字节
                intValue = (0xff & bytes[i]);
            wordValues.add(intValue);
        }

        return writeWords(conn, slaveId, address, wordValues);
    }
    public static boolean writeWordsByDataStr(String ip, int port, int slaveId, int address, String dataStr, ModbusDataType dtype, ModbusByteOrderType bot) throws Exception {
        TCPMasterConnection conn = connectionModbus(ip, port);

        writeWordsByDataStr(conn, slaveId, address, dataStr, dtype, bot);

        conn.close();
        return true;
    }
}