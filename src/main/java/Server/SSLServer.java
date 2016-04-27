package Server;

import Config.Config;
import DataPack.DataPack;
import Database.Database;
import GameObjects.Player;
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
    private ExecutorService socketExecutor = null;
    private ConcurrentHashMap<Integer, Player> onlinePlayers = null;
    private Map<Integer, Room> roomMap = null;
    private int nextRoomId = 0;

    public SSLServer(Config config){
        // initialize database
        Database.initialize(config);
        this.config = config;
        this.socketExecutor = Executors.newCachedThreadPool();
        this.onlinePlayers = new ConcurrentHashMap<>(1000, 0.75f);
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
                Runnable socketRunnable = new DataPackSocketRunnable(this, sock);
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
        SSLContext context = SSLContext.getInstance("SSLv3");
        context.init(kmf.getKeyManagers(), null, null);

        // generate ssl server socket factory
        SSLServerSocketFactory factory = context.getServerSocketFactory();

        // create server socket
        socket = (SSLServerSocket) factory.createServerSocket(port);

        return socket;
    }


    public Player getPlayer(int id) { return this.onlinePlayers.get(id); }

    public void addPlayer(Player player){
        Player currentPlayer = this.onlinePlayers.get(player.getId());
        if(currentPlayer != null){
            if(currentPlayer.getSocket().equals(player.getSocket()))
                return;
            // remove current
            removePlayer(currentPlayer);
        }

        this.onlinePlayers.put(player.getId(), player);
        return;
    }

    public void removePlayer(Player player){
        try{
            player.getSocket().send(new DataPack(DataPack.TERMINATE));
            player.getSocket().close();
            this.onlinePlayers.remove(player.getId());
        } catch(IOException e){
            logger.catching(e);
        }
    }

    public Collection<Room> getRooms(){
        return this.roomMap.values();
    }

    public Room getRoom(int roomId){
        return this.roomMap.get(roomId);
    }

    public synchronized int addRoom(String roomName, Player host){
        Room room = new Room(nextRoomId, roomName, host);
        host.setHost(true);
        room.addPlayer(host);
        this.roomMap.put(nextRoomId, room);
        nextRoomId++;
        return nextRoomId - 1;
    }

    public void removeRoom(Room room) { this.roomMap.remove(room.getId()); }

    public void shutdown(){
        try{
            // send shutdown datapack to ever online users
            // and close the socket.
            for(Player player : this.onlinePlayers.values()){
                player.getSocket().send(new DataPack(DataPack.TERMINATE));
                player.getSocket().close();
            }

        } catch(Exception e){
            logger.catching(e);
        }
    }
}
