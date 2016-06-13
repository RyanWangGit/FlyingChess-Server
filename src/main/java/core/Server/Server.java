package core.Server;

import core.DataPack.DataPackProcessor;

/**
 * Created by Ryan on 16/4/2.
 */
public interface Server {

    /**
     * Start the server with the defined data pack processor.
     * @param processor The processor to process the incoming data pack.
     */
    void start(DataPackProcessor processor);

    /**
     * Shutdown the server.
     */
    void shutdown();

}
