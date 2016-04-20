package Server;

import Config.Config;
import Database.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SSLServer implements Server {
    private static Logger logger = LogManager.getLogger(SSLServer.class.getName());
    private SSLServerSocket server = null;
    private Config config = null;
    private ExecutorService socketCommunicationExecutors = null;
    private ConcurrentHashMap<Integer, UserCommunicationJob> onlineHash = null;

    public SSLServer(Config config){
        // initialize database
        Database.initialize(config);
        this.config = config;
        this.socketCommunicationExecutors = Executors.newCachedThreadPool();
        this.onlineHash = new ConcurrentHashMap<Integer, UserCommunicationJob>(1000, 0.75f);
    }

    public void start(){
        try {
            if(server == null || !server.isBound() || server.isClosed())
                this.server = createServerSocket(config.getDataPort());

            logger.info("Server started, listening for incoming connections.");

            while(true){
                Socket sock = server.accept();
                logger.info("Accepted new socket " + sock.getRemoteSocketAddress().toString());
                Runnable socketJob = new UserCommunicationJob(this, sock);
                this.socketCommunicationExecutors.submit(socketJob);
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
        InputStream keyNameStream = SSLServer.class.getClassLoader().getResourceAsStream("chaton.keystore");
        char[] keyStorePass = "ryanwang@hust".toCharArray();
        char[] keyPassword = "ryanwang@hust".toCharArray();

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(keyNameStream, keyStorePass);

        // create key manager
        KeyManagerFactory kmf=KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, keyPassword);

        // create ssl context
        SSLContext context = SSLContext.getInstance("SSLv3");
        context.init(kmf.getKeyManagers(), null, null);

        // generate ssl server socket factory
        SSLServerSocketFactory factory = context.getServerSocketFactory();

        // create server socket
        socket = (SSLServerSocket) factory.createServerSocket(port);

        return socket;
    }

    public void shutdown(){

    }

    public UserCommunicationJob getUserCommunicationJob(Integer id){
        return this.onlineHash.get(id);
    }

    public void addUserCommunicationJob(Integer id, UserCommunicationJob job){
        UserCommunicationJob curJob = this.onlineHash.get(id);
        if(curJob != null){
            // close the former socket
            curJob.shutdown();
        }
        this.onlineHash.put(id, job);
    }
}
