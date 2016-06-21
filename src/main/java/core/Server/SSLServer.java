package core.Server;

import core.Config.Config;
import core.Database.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SSLServer main class.
 * @author Ryan Wang
 */
public class SSLServer implements Server {
    private static Logger logger = LogManager.getLogger(SSLServer.class.getName());
    private SSLServerSocket server = null;
    private Config config = null;
    private ExecutorService socketExecutor = null;


    /**
     * Constructs the server with {@link core.Config.Config Config}.
     * @param config The config to setup the server, provides all essential configurations the server needs.
     */
    public SSLServer(Config config){
        // initialize database
        Database.initialize(config);
        this.config = config;
        this.socketExecutor = Executors.newCachedThreadPool();
    }

    /**
     * Server starts listening for incoming connections with the defined {@link DataPackProcessor},
     * note that it is a blocking method and needs to be run in separate thread
     * if you don't want your application to be non-responsible.
     * @param processor The processor to process the incoming data pack.
     */
    public void start(DataPackProcessor processor) {
        try {
            if(server == null || !server.isBound() || server.isClosed())
                this.server = createServerSocket(config.getDataPort());

            logger.info("Server started, listening for incoming connections.");

            while(true){
                Socket sock = server.accept();
                logger.info("Accepted new socket " + sock.getRemoteSocketAddress().toString());
                Runnable socketRunnable = new DataPackSocketRunnable(processor, new DataPackTcpSocket(sock));
                this.socketExecutor.submit(socketRunnable);
            }

        } catch (Exception e){
            logger.catching(e);
        }
    }

    /**
     * Create an SSL server socket if it doesn't exist.
     * @param port The port the server listens on.
     * @return SSL server socket.
     */
    private SSLServerSocket createServerSocket(int port) throws Exception{
        SSLServerSocket socket = null;

        // key name and password
        InputStream keyNameStream = new FileInputStream(config.getKeyStorePath());
        char[] keyStorePass = config.getKeyStorePassword().toCharArray();
        char[] keyPassword = config.getKeyPassword().toCharArray();

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(keyNameStream, keyStorePass);

        // create key manager
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, keyPassword);

        // create ssl context
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(kmf.getKeyManagers(), null, null);

        // generate ssl server socket factory
        SSLServerSocketFactory factory = context.getServerSocketFactory();

        // create server socket
        socket = (SSLServerSocket) factory.createServerSocket(port);

        return socket;
    }

    /**
     * Shutdown the server, stops listening.
     */
    public void shutdown(){
        //TODO: wake the server up and stops listening.
    }
}
