package GameObjects;

import DataPack.DataPack;
import DataPack.DataPackUtil;
import Database.Database;
import PlayerFilter.RoomSelectingFilter;
import Server.BroadcastRunnable;
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
     * this method creates a new thread to broadcastToAll the new rooms
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

    public Player getPlayer(int playerId){
        return playerManager.getPlayer(playerId);
    }

    public void removePlayer(Player player){
        if(player == null)
            return;

        StringBuilder builder = new StringBuilder();
        builder.append("Player " + player.toString() + " got disconnetd");

        Room playerRoom = player.getRoom();

        if(playerRoom != null){
            // send disconnected datapack
            DataPack dataPack = new DataPack(DataPack.E_GAME_PLAYER_DISCONNECTED, DataPackUtil.getPlayerInfoMessage(player));
            if(player.getStatus() != Player.PLAYING)
                dataPack.setCommand(DataPack.E_ROOM_EXIT);
            playerRoom.broadcastToOthers(player, dataPack);

            builder.append(" in room " + playerRoom.toString() + ". ");
            if(player.isHost()){
                builder.append(player.toString() + " is host, prepare to remove the room.");

                removeRoom(playerRoom);
            }
        }
        logger.info(builder.toString());

        playerManager.removePlayer(player);
    }

    public Room createRoom(String roomName, Player host){
        Room room = roomManager.createRoom(roomName);
        room.addPlayer(host);
        room.setHost(host);
        return room;
    }

    public Room getRoom(int roomId){
        return roomManager.getRoom(roomId);
    }

    public void removeRoom(Room room){
        for(Player roomPlayer : room.getPlayers())
            roomPlayer.setStatus(Player.ROOM_SELECTING);

        // notify other players that host has exited
        Player host = room.getHost();
        if(host != null)
            room.broadcastToOthers(host, new DataPack(DataPack.E_ROOM_EXIT, DataPackUtil.getPlayerInfoMessage(host)));

        roomManager.removeRoom(room);
        logger.info("Room removed: " + room.getId() + " " + room.getName());
        roomListChanged(room);
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
    private Map<Integer, Player> playerMap = null;

    public PlayerManager(ObjectManager parent){
        this.playerMap = new ConcurrentHashMap<>(100, 0.75f);
        this.parent = parent;
    }

    public Player createPlayer(User user){
        // get former connection
        Player currentPlayer = this.playerMap.get(user.getId());
        if(currentPlayer == null){
            Player player = new Player(user, this);
            this.playerMap.put(player.getId(), player);
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
            return this.playerMap.get(id);
    }

    public void removePlayer(Player player){
        try{
            this.playerMap.remove(player.getId());
            player.getSocket().close();
        } catch(IOException e){

        }
    }

    public Collection<Player> getAllPlayers(){
        return Collections.unmodifiableCollection(this.playerMap.values());
    }
}


class RoomManager {
    private Logger logger = LogManager.getLogger(RoomManager.class.getName());
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
        parent.roomListChanged(room);
        logger.info("Room created: " + room.getId() + " " + room.getName());
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