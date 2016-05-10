package Server;

import DataPack.DataPack;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;

/**
 * Created by Ryan on 16/4/26.
 */
public class DataPackSocket {
    private static Logger logger = LogManager.getLogger(DataPackSocket.class.getName());
    protected Socket socket = null;
    protected DataInputStream is = null;
    protected DataOutputStream os = null;
    protected Gson dataPackGson = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create();

    public DataPackSocket(Socket socket) throws IOException{
        this.socket = socket;
        this.socket.setTcpNoDelay(true);
        this.os = new DataOutputStream(socket.getOutputStream());
        this.is = new DataInputStream(socket.getInputStream());
    }

    /**
     * Receive one data pack from the inputstream, which
     * will be blocking until one data pack is successfully read.
     *
     * @return The data pack read.
     */
    protected DataPack receive() throws IOException{
        int blockSize = this.is.readInt();

        byte[] bytes = new byte[blockSize];
        this.is.readFully(bytes);

        String json = new String(bytes, "UTF-8");

        logger.debug(json);

        // parse the datapack and return
        return dataPackGson.fromJson(json, DataPack.class);
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
    public synchronized void send(DataPack dataPack) {
        try{
            byte[] sendBytes = dataPackGson.toJson(dataPack, DataPack.class).getBytes(Charset.forName("UTF-8"));
            int bytesSize = sendBytes.length;

            this.os.writeInt(bytesSize);
            this.os.write(sendBytes);
            this.os.flush();
            logger.debug(new String(sendBytes));

        } catch(SocketException e){
            logger.warn("Socket has been shutdown.");
        } catch(IOException e){
            logger.catching(e);
        }
    }
}
