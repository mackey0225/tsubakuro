package com.nautilus_technologies.tsubakuro.channel.stream;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nautilus_technologies.tsubakuro.channel.stream.sql.ResponseBox;
import com.nautilus_technologies.tsubakuro.channel.stream.sql.ResultSetBox;

public class StreamWire {
    public byte[] bytes;

    private Socket socket;
    private DataOutputStream outStream;
    private DataInputStream inStream;
    private ResponseBox responseBox;
    private ResultSetBox resultSetBox;
    private byte[] header;

    private byte info;
    private boolean valid;
    private boolean closed;

    private static final byte REQUEST_SESSION_HELLO = 1;
    private static final byte REQUEST_SESSION_PAYLOAD = 2;
    private static final byte REQUEST_RESULT_SET_BYE_OK = 3;

    public static final byte RESPONSE_SESSION_PAYLOAD = 1;
    public static final byte RESPONSE_RESULT_SET_PAYLOAD = 2;
    public static final byte RESPONSE_SESSION_HELLO_OK = 3;
    public static final byte RESPONSE_SESSION_HELLO_NG = 4;
    public static final byte RESPONSE_RESULT_SET_HELLO = 5;
    public static final byte RESPONSE_RESULT_SET_BYE = 6;

    final Logger logger = LoggerFactory.getLogger(StreamWire.class);

    public StreamWire(String hostname, int port) throws IOException {
        socket = new Socket(hostname, port);
        outStream = new DataOutputStream(socket.getOutputStream());
        inStream = new DataInputStream(socket.getInputStream());
        responseBox = new ResponseBox(this);
        resultSetBox = new ResultSetBox(this);
        this.header = new byte[6];
        this.valid = false;
        this.closed = false;
    }

    public void hello() throws IOException {
        send(REQUEST_SESSION_HELLO, 0);
    }
    public boolean pull() throws IOException {
        return receive();
    }
    public ResponseBox getResponseBox() {
        return responseBox;
    }
    public ResultSetBox getResultSetBox() {
        return resultSetBox;
    }
    public void sendResutSetByeOk(int slot) throws IOException {
	send(REQUEST_RESULT_SET_BYE_OK, slot);
    }

    private void send(byte i, int s) throws IOException {  // SESSION_HELLO, RESULT_SET_BYE_OK
        header[0] = i;  // info
        header[1] = (byte) s;  // slot
        header[2] = 0;
        header[3] = 0;
        header[4] = 0;
        header[5] = 0;

        synchronized (this) {
            outStream.write(header, 0, header.length);
        }
        logger.trace("send SESSION_HELLO");
    }
    public void send(int s, byte[] payload) throws IOException {  // SESSION_PAYLOAD
        int length = (int) payload.length;

        header[0] = REQUEST_SESSION_PAYLOAD;
        header[1] = strip(s);  // slot
        header[2] = strip(length);
        header[3] = strip(length >> 8);
        header[4] = strip(length >> 16);
        header[5] = strip(length >> 24);

        synchronized (this) {
	    outStream.write(header, 0, header.length);

            if (length > 0) {
                // payload送信
                outStream.write(payload, 0, length);
            }
        }
	    logger.trace("send SESSION_PAYLOAD, length = " + length + ", slot = ", s);
    }

    public void send(int s, byte[] first, byte[] payload) throws IOException {  // SESSION_PAYLOAD
        int length = (int) (first.length + payload.length);

        header[0] = REQUEST_SESSION_PAYLOAD;
        header[1] = strip(s);  // slot
        header[2] = strip(length);
        header[3] = strip(length >> 8);
        header[4] = strip(length >> 16);
        header[5] = strip(length >> 24);

        synchronized (this) {
	    outStream.write(header, 0, header.length);

            if (length > 0) {
                // payload送信
                outStream.write(first, 0, first.length);
                outStream.write(payload, 0, payload.length);
            }
        }
        logger.trace("send SESSION_PAYLOAD, length = " + length + ", slot = ", s);
    }

    byte strip(int i) {
        return (byte) (i & 0xff);
    }

    public boolean receive() throws IOException {
        if (valid) {
            System.err.println("previous data is alive");
        }
        try {
            byte writer = 0;

            // info受信
            info = inStream.readByte();

            // slot受信
            byte slot = inStream.readByte();

            if (info ==  RESPONSE_RESULT_SET_PAYLOAD) {
                writer = inStream.readByte();
            }

            // length受信
            int length = 0;
            for (int i = 0; i < 4; i++) {
                int inData = inStream.readByte() & 0xff;
                length |= inData << (i * 8);
            }
            if (length > 0) {
                // payload受信
                bytes = new byte[length];
                int size = 0;
                while (size < length) {
                    size += inStream.read(bytes, size, length - size);
                }
            } else {
                bytes = null;
            }
            if (info == RESPONSE_SESSION_PAYLOAD) {
                logger.trace("receive SESSION_PAYLOAD, length = " + length + ", slot = ", slot);
                responseBox.push(slot, bytes);
            } else if (info == RESPONSE_RESULT_SET_PAYLOAD) {
                logger.trace("receive RESULT_SET_PAYLOAD, length = " + length + ", slot = ", slot, ", writer = ", writer);
                resultSetBox.push(slot, writer, bytes);
            } else if (info == RESPONSE_RESULT_SET_HELLO) {
                try {
                    resultSetBox.pushHello(new String(bytes, "UTF-8"), slot);
                } catch (UnsupportedEncodingException e) {
                    throw new IOException(e);
                }
            } else if (info == RESPONSE_RESULT_SET_BYE) {
                resultSetBox.pushBye(slot);
            } else if ((info == RESPONSE_SESSION_HELLO_OK) || (info == RESPONSE_SESSION_HELLO_NG)) {
                logger.trace("receive SESSION_HELLO_" + ((info == RESPONSE_SESSION_HELLO_OK) ? "OK" : "NG"));
                valid = true;
            } else {
                throw new IOException("invalid info in the response");
            }
        } catch (EOFException e) {  // imply session close
            socket.close();
            closed = true;
            return false;
        }
        return true;
    }

    public byte getInfo() {  // used only by FutureSessionWireImpl
        if (!valid) {
            System.err.println("received data has been disposed");
        }
        return info;
    }
    public String getString() {  // used only by FutureSessionWireImpl
        if (!valid) {
            System.err.println("received data has been disposed");
        }
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // As long as only alphabetic and numeric characters are received,
            // this exception will never occur.
            System.err.println(e);
            e.printStackTrace();
        }
        return "";
    }

    public void release() {  // used only by FutureSessionWireImpl
        if (!valid) {
            System.err.println("already released");
        }
        valid = false;
    }

    public void close() throws IOException {
        if (!closed) {
            socket.close();
            closed = true;
        }
    }
}
