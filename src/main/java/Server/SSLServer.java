package Server;

import Config.Config;
import DataPack.DataPack;
import Database.Database;
import GameObjects.Room;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SSLServer implements Server {
    private static Logger logger = LogManager.getLogger(SSLServer.class.getName());
    private SSLServerSocket server = null;
    private Config config = null;
    private ExecutorService userSocketExecutor = null;
    private ConcurrentHashMap<Integer, UserSocket> onlineUserSockets = null;
    private Map<Integer, Room> roomMap = null;
    private int nextRoomId = 0;

    public SSLServer(Config config){
        // initialize database
        Database.initialize(config);
        this.config = config;
        this.userSocketExecutor = Executors.newCachedThreadPool();
        this.onlineUserSockets = new ConcurrentHashMap<>(1000, 0.75f);
        this.roomMap = new ConcurrentHashMap<>(100, 0.75f);
    }

    public void start() {
        try {
            if(server == null || !server.isBound() || server.isClosed())
                this.server = createServerSocket(config.getDataPort());

            logger.info("Server started, listening for incoming connections.");

            while(true){
                Socket sock = server.accept();
                logger.info("Accepted new socket " + sock.getRemoteSocketAddress().toString());
                Runnable socketJob = new UserSocketRunnable(this, sock);
                this.userSocketExecutor.submit(socketJob);
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
        SSLContext context = SSLContext.getInstance("SSLv3");
        context.init(kmf.getKeyManagers(), null, null);

        // generate ssl server socket factory
        SSLServerSocketFactory factory = context.getServerSocketFactory();

        // create server socket
        socket = (SSLServerSocket) factory.createServerSocket(port);

        return socket;
    }


    public UserSocket getUserSocket(Integer id) { return this.onlineUserSockets.get(id); }

    public void addUserSocket(Integer id, UserSocket socket){
        try{
            UserSocket currentSocket = this.onlineUserSockets.get(id);
            if(currentSocket != null){
                // close the former socket
                currentSocket.close();
            }
            this.onlineUserSockets.put(id, socket);
        } catch(IOException e){
            logger.catching(e);
        }
    }

    public void removeUserSocket(Integer id){
        this.onlineUserSockets.remove(id);
    }

    public Collection<Room> getRooms(){
        return this.roomMap.values();
    }

    public Room getRoom(Integer roomId){
        return this.roomMap.get(roomId);
    }

    public synchronized int addRoom(String roomName, Integer hostId){
        Room room = new Room(nextRoomId, roomName, hostId);
        this.roomMap.put(nextRoomId, room);
        nextRoomId++;
        return nextRoomId - 1;
    }

    public void shutdown(){
        try{
            // send shutdown datapack to ever online users
            for(UserSocket socket : this.onlineUserSockets.values())
                socket.send(new DataPack(DataPack.TERMINATE));

        } catch(Exception e){
            logger.catching(e);
        }
    }
}
