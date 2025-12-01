This is the complete, detailed context of the reverse-engineering process, showing exactly what we pulled from the decompiled application (Jadx) and how that led us to the definitive motor control command.The Full Context: Decompilation to Binary CommandThe entire process was based on the premise that the proprietary vending machine application (DMVI-Hardware.apk) contained hardcoded logic for communication, which we successfully extracted using the Jadx decompiler.1. Identifying the Communication Channel (The Hardcoded Path)We started by searching the application's Java code for any instance where a serial port was initialized.Result Pulled from Jadx: The class com.digitalmediavending.hardware.nayax_sdk_utils.LowLevelSerialBridge contained the following constructor:Java// Inside LowLevelSerialBridge.java
@Override // com.bitmick.marshall.interfaces.lowlevel_i
public void start() {
try {
this.m_serial_port = new SerialPort(new File("/dev/ttyS4"), 115200, 0);
} catch (IOException e) {
e.printStackTrace();
}
// ...
}
Conclusion: This snippet revealed the required Baud Rate ($115200$) and the developer's intended port (/dev/ttyS4).Verification: Testing showed that /dev/ttyS4 did not exist, but subsequent testing confirmed the motor control was connected to the working USB serial device, /dev/ttyUSB1, which also uses the $115200$ baud rate.2. Identifying the Protocol (MDB Binary)We observed multiple classes being imported related to a specific industry protocol.Result Pulled from Jadx: Many classes imported definitions from the com.bitmick.marshall.vmc package, including the class:com.bitmick.marshall.vmc.mdb_rsp_t (MDB Response Type)Conclusion: This confirmed the application uses the MDB (Multi-Drop Bus) protocol, which is a binary, packet-based standard for vending machine peripherals. This explained why a simple ASCII command like "VEND 05\n" failed, as the motor board requires a precise hex sequence.3. Deriving the Command Structure (The Encoding Logic)The final step involved locating the command encoder for a vending request.Result Pulled from Jadx: We found the mdb_req_t class, which contained the high-level request builder, and the nested msg_vend_t class, which defined the packet body for a vend.A. Command Header (mdb_req_t.encode)This method defined the MDB address/command byte:Java@Override // com.bitmick.marshall.interfaces.msg_i
public int encode(byte[] bArr, int i, int i2) {
// ...
bArr[i] = (byte) (this.command + 16); // MDB Address/Command Byte
// ...
}
The MDB command for VEND is hardcoded as 3. Thus, the first byte is $3 + 16 = 19$ (decimal), or 0x13 (hex).B. Command Body (msg_vend_t.encode)This method defined the data structure for the VEND command (sub_command == 0):Java// If sub-command is VEND REQUEST (0)
if (b == 0) {
// Item Price (2 bytes) and Item Number (2 bytes) are encoded
iShortToByteArray2 += ByteArrayUtils.shortToByteArray(bArr, iShortToByteArray2, this.item_price);
iShortToByteArray = ByteArrayUtils.shortToByteArray(bArr, iShortToByteArray2, this.item_number);
}
The bytes are structured as: Sub-Command (1 byte) + Price (2 bytes) + Item Number (2 bytes).For Motor 5 (Free Vend), the values are: 0x00 (Sub-command), 0x0000 (Price), and 0x0005 (Item Number).C. Final Packet AssemblyThe final 7-byte packet was assembled by combining the header, the data, and calculating the required MDB checksum (sum of all bytes modulo 256):Byte PositionFieldHex Value1Address/Command0x132Sub-Command0x003, 4Item Price (Short)0x00 0x005, 6Item Number (Short)0x00 0x057Checksum (Sum)0x18ConclusionThe analysis led to the definitive discovery that the motor commands are sent via a stable serial port at a high baud rate, using a precise, 7-byte binary sequence required by the MDB protocol, which is what you must implement in your own code:$$\text{Final Binary Command} = \mathbf{0\text{x13} \ 0\text{x00} \ 0\text{x00} \ 0\text{x00} \ 0\text{x00} \ 0\text{x05} \ 0\text{x18}}$$