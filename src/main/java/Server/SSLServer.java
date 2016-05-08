package Server;

import Config.Config;
import DataPack.DataPack;
import Database.Database;
import GameObjects.ObjectManager;
import GameObjects.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectArrayMessage;

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


public class SSLServer implements Server {
    private static Logger logger = LogManager.getLogger(SSLServer.class.getName());
    private SSLServerSocket server = null;
    private Config config = null;
    private ExecutorService socketExecutor = null;
    private ObjectManager objectManager = null;


    public SSLServer(Config config){
        // initialize database
        Database.initialize(config);
        this.config = config;
        this.socketExecutor = Executors.newCachedThreadPool();
        this.objectManager = new ObjectManager();
    }

    public void start() {
        try {
            if(server == null || !server.isBound() || server.isClosed())
                this.server = createServerSocket(config.getDataPort());

            logger.info("Server started, listening for incoming connections.");

            while(true){
                Socket sock = server.accept();
                logger.info("Accepted new socket " + sock.getRemoteSocketAddress().toString());
                Runnable socketRunnable = new DataPackSocketRunnable(objectManager, sock);
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




    public void shutdown(){
        try{
            // send shutdown datapack to ever online users
            // and close the socket.
            for(Player player : objectManager.getAllPlayers()){
                player.getSocket().send(new DataPack(DataPack.TERMINATE));
                player.getSocket().close();
            }

        } catch(Exception e){
            logger.catching(e);
        }
    }
}
