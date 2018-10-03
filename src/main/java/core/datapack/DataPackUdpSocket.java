package core.datapack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Simple wrapper for udp datagram socket for transmitting data packs in which
 * {@link DataPack} is atomic and basic unit.
 * @author Ryan Wang
 */
public class DataPackUdpSocket {
    private Logger logger = LogManager.getLogger(DataPackUdpSocket.class.getName());
    protected DatagramSocket socket = null;
    protected Gson dataPackBuilder = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create();
    protected byte[] inBuf = null;

    public DataPackUdpSocket(DatagramSocket socket){
        this.socket = socket;
        inBuf = new byte[1024];
    }
    /**
     * This method sends out the datapack immediately, in the thread
     * which calls the method.
     *
     * @param dataPack The datapack to be sent.
     * @param address The address to which the datapack is sent.
     */
    public void send(DataPack dataPack, InetSocketAddress address) throws IOException {
        byte[] bytes = dataPackBuilder.toJson(dataPack, DataPack.class).getBytes();
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address);
        socket.send(packet);
        logger.debug("Sending" + packet.toString());
    }

    /**
     * Receive one data pack from the inputstream, which
     * will be blocking until one data pack is successfully read.
     *
     * @return The data pack read.
     */
    public DataPack receive() throws IOException {
        DatagramPacket packet = new DatagramPacket(inBuf, inBuf.length);
        socket.receive(packet);
        return dataPackBuilder.fromJson(new String(packet.getData(), StandardCharsets.UTF_8), DataPack.class);
    }

    /**
     * Close the socket.
     */
    public void close() throws IOException {
        this.socket.close();
    }

    public InetSocketAddress getInetSocketAddress() {
        return null;
    }
}
