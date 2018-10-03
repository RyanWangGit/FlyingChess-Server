package core.datapack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

/**
 * Simple wrapper for tcp stream socket for transmitting data packs in which
 * {@link DataPack} is atomic and basic unit.
 * @author Ryan Wang
 */
public class DataPackTcpSocket {
    private static Logger logger = LogManager.getLogger(DataPackTcpSocket.class.getName());
    protected Socket socket = null;
    protected DataInputStream is = null;
    protected DataOutputStream os = null;
    protected Gson dataPackBuilder = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create();

    public DataPackTcpSocket(Socket socket) throws IOException{
        this.socket = socket;
        this.socket.setTcpNoDelay(true);
        this.socket.setKeepAlive(true);
        this.os = new DataOutputStream(socket.getOutputStream());
        this.is = new DataInputStream(socket.getInputStream());
    }

    /**
     * Receive one data pack from the inputstream, which
     * will be blocking until one data pack is successfully read.
     *
     * @return The data pack read.
     */
    public DataPack receive() throws IOException{
        int blockSize = this.is.readInt();

        byte[] bytes = new byte[blockSize];
        this.is.readFully(bytes);

        // parse the datapack and return
        return dataPackBuilder.fromJson(new String(bytes, StandardCharsets.UTF_8), DataPack.class);
    }

    /**
     * Close the socket.
     */
    public void close() throws IOException{
        send(new DataPack(DataPack.TERMINATE));
        this.os.close();
        this.is.close();
        this.socket.close();
    }

    /**
     * This method sends out the datapack immediately, in the thread
     * which calls the method.
     *
     * @param dataPack The datapack to be sent.
     */
    public synchronized void send(DataPack dataPack) throws IOException {
        try{
            byte[] sendBytes = dataPackBuilder.toJson(dataPack, DataPack.class).getBytes(StandardCharsets.UTF_8);
            int bytesSize = sendBytes.length;

            this.os.writeInt(bytesSize);
            this.os.write(sendBytes);
            this.os.flush();

        } catch(SocketException e) {
            logger.warn("Socket has been shutdown.");
        }
    }

    public InetSocketAddress getInetSocketAddress(){
        return new InetSocketAddress(socket.getInetAddress(), socket.getPort());
    }
}
