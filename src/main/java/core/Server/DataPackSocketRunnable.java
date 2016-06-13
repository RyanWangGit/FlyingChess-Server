package core.Server;

import core.DataPack.DataPackProcessor;
import core.DataPack.DataPackTcpSocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;

/**
 * Created by Ryan on 16/4/12.
 */
class DataPackSocketRunnable implements Runnable {
    private static Logger logger = LogManager.getLogger(DataPackSocketRunnable.class.getName());
    private DataPackProcessor processor = null;
    private DataPackTcpSocket socket = null;

    public DataPackSocketRunnable(DataPackProcessor processor, DataPackTcpSocket socket) throws IOException{
        this.processor = processor;
        this.socket = socket;
        processor.started(socket);
    }

    public void run(){
        try{

            logger.info("Connection established with " + socket.getInetSocketAddress().toString());

            // enter process loop
            while(true){
                processor.process(socket.receive());
            }

        } catch(EOFException e){
            logger.warn("Found EOF when reading from " + socket.getInetSocketAddress().toString() + ".Drop the connection.");
        } catch(SocketException e){
            logger.info("Socket shutdown due to external request.");
        } catch (Exception e){
            logger.catching(e);
        }
        finally {
            processor.stopped();
        }
    }
}
