package core.datapack;

import java.io.IOException;

/**
 * Provides essential interface to process an incoming data pack.
 * It is passed into the server on starting, and may store any types of objects for specific applications.
 * @author Ryan Wang
 */
public interface DataPackProcessor {

    /**
     * Defines the behavior when connection has started.
     * @param socket The socket of the connection.
     */
    void started(DataPackTcpSocket socket);

    /**
     * Defines the behavior when received a datapack.
     * @param dataPack
     */
    void process(DataPack dataPack) throws IOException;

    /**
     * Defines the behavior when the connection has stopped.
     */
    void stopped();
}
