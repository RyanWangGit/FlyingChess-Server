package core.DataPack;

import java.io.IOException;

/**
 * Created by Ryan on 16/6/13.
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
