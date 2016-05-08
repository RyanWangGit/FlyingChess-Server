package GameObjects;

import DataPack.DataPack;
import DataPack.DataPackUtil;
import Database.Database;
import PlayerFilter.RoomSelectingFilter;
import Server.BroadcastRunnable;
import Server.DataPackSocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages all the online objects including @ref
 */
public class ObjectManager {
    private static Logger logger = LogManager.getLogger(ObjectManager.class.getName());
    private PlayerManager playerManager = null;
    private RoomManager roomManager = null;
    private ExecutorService executor = null;
    private BroadcastRunnable broadcastRunnable = null;

    public ObjectManager(){
        this.playerManager = new PlayerManager(this);
        this.roomManager = new RoomManager(this);
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Invoked when the list of the rooms have changed,
     * this method creates a new thread to broadcast the new rooms
     * info to all players.
     */
    protected void roomListChanged(Room changedRoom){
        if(broadcastRunnable != null)
            broadcastRunnable.shutdown();

        DataPack dataPack = new DataPack(DataPack.A_ROOM_LOOKUP, DataPackUtil.getRoomsMessage(this));
        this.broadcastRunnable = new BroadcastRunnable(getAllPlayers(), new RoomSelectingFilter(), dataPack);
        executor.submit(this.broadcastRunnable);

        return;
    }

    public boolean registerUser(String userName, String password){
        User user = Database.getUser(userName);
        // user doesn't exist
        if(user == null)
            return Database.addUser(userName, password);
        else
            return false;
    }

    public Player createPlayer(String userName, String password){
        User user = Database.getUser(userName);
        if(user == null || !user.getPassword().equals(password)){
            return null;
        }
        else{
           return playerManager.createPlayer(user);
        }
    }

    public Player getPlayer(DataPackSocket socket){
        return playerManager.getPlayer(socket);
    }

    public Player getPlayer(int playerId){
        return playerManager.getPlayer(playerId);
    }

    public void removePlayer(Player player){
        playerManager.removePlayer(player);
    }

    public Room createRoom(String roomName, Player host){
        Room room = roomManager.createRoom(roomName);
        room.setHost(host);
        room.addPlayer(host);
        return room;
    }

    public Room getRoom(int roomId){
        return roomManager.getRoom(roomId);
    }

    public void removeRoom(Room room){
        roomManager.removeRoom(room);
    }

    public Collection<Room> getAllRooms(){
        return roomManager.getAllRooms();
    }

    public Collection<Player> getAllPlayers(){
        return playerManager.getAllPlayers();
    }
}

class PlayerManager {
    private static Logger logger = LogManager.getLogger(PlayerManager.class.getName());
    /**
     * 2 maps to store the player object
     * to provide O(1) look up speed by 2 unique keys
     */
    private ObjectManager parent = null;
    private Map<Integer, Player> idPlayerMap = null;
    private Map<DataPackSocket, Player> socketPlayerMap = null;

    public PlayerManager(ObjectManager parent){
        this.idPlayerMap = new ConcurrentHashMap<>(100, 0.75f);
        this.socketPlayerMap = new ConcurrentHashMap<>(100, 0.75f);
        this.parent = parent;
    }

    public Player createPlayer(User user){
        // get former connection
        Player currentPlayer = this.idPlayerMap.get(user.getId());
        if(currentPlayer == null){
            Player player = new Player(user, this);
            this.idPlayerMap.put(player.getId(), player);
            this.socketPlayerMap.put(player.getSocket(), player);
            return player;
        }
        else {
            // close the former client connection
            if(!currentPlayer.getSocket().equals(this)){
                try{
                    currentPlayer.getSocket().close();
                    currentPlayer.setSocket(null);
                } catch(IOException | NullPointerException e){
                    logger.warn("Error occured closing former connection.");
                }
                finally {
                    return currentPlayer;
                }
            }
        }
        return null;
    }

    public Player getPlayer(int id) {
        // if it is a robot
        if(id < 0 && id >= -4)
            return new Player(new User(id, "Robot", null, 0), null);
        else
            return this.idPlayerMap.get(id);
    }

    public Player getPlayer(DataPackSocket socket){
        return this.socketPlayerMap.get(socket);
    }

    public void removePlayer(Player player){
        try{
            if(player == null)
                return;
            this.idPlayerMap.remove(player.getId());
            this.socketPlayerMap.remove(player.getSocket());
            player.getSocket().close();
        } catch(IOException e){
            logger.catching(e);
        }
    }

    public Collection<Player> getAllPlayers(){
        return Collections.unmodifiableCollection(this.idPlayerMap.values());
    }
}


class RoomManager {
    private ObjectManager parent = null;
    private Map<Integer, Room> rooms = null;
    private int nextId = 0;

    public RoomManager(ObjectManager parent){
        this.parent = parent;
        this.rooms = new ConcurrentHashMap<>(100, 0.75f);
    }

    public Room getRoom(int roomId){
        return this.rooms.get(roomId);
    }

    public synchronized Room createRoom(String roomName){
        Room room = new Room(nextId, roomName, this);
        this.rooms.put(nextId, room);
        nextId++;
        return room;
    }

    public void removeRoom(Room room) { this.rooms.remove(room.getId()); }

    protected void roomListChanged(Room changedRoom){
        parent.roomListChanged(changedRoom);
    }

    public Collection<Room> getAllRooms(){
        return Collections.unmodifiableCollection(this.rooms.values());
    }
}