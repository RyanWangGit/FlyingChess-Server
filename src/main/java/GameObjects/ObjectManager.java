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
 * Manages all the online objects including Player/Room
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
            // get former connection
            Player currentPlayer = playerManager.getPlayer(user.getId());
            if(currentPlayer == null){
                Player player = playerManager.createPlayer(user);
                logger.info(player.toString() + " logged in.");
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
                        logger.info(currentPlayer.toString() + " logged in.");
                        return currentPlayer;
                    }
                }
            }
            return null;
        }
    }

    public Player getPlayer(int playerId){
        // if it is a robot
        if(playerId < 0 && playerId >= -4){
            return new Player(new User(playerId, "Robot", null, 0), null);
        }
        else
            return playerManager.getPlayer(playerId);
    }

    public void removePlayer(Player player){
        if(player == null)
            return;

        StringBuilder builder = new StringBuilder();
        builder.append("Player " + player.toString() + " got disconnetd");

        Room playerRoom = player.getRoom();

        if(playerRoom != null){
            builder.append(" in room " + playerRoom.toString() + ". ");
            if(player.isHost()){
                builder.append(player.toString() + " is host, prepare to remove the room.");
                removeRoom(playerRoom);
            }
            else{
                // send disconnected datapack
                DataPack dataPack = new DataPack(DataPack.INVALID, DataPackUtil.getPlayerInfoMessage(player));

                if(!player.isInStatus(Player.PLAYING)){
                    dataPack.setCommand(DataPack.E_ROOM_EXIT);
                    playerManager.removePlayer(player);
                }
                else{
                    dataPack.setCommand(DataPack.E_GAME_PLAYER_DISCONNECTED);
                    player.setStatus(Player.DISCONNECTED);
                }
                playerRoom.broadcastToOthers(player, dataPack);
            }
        }
        else {
            playerManager.removePlayer(player);
        }
        logger.info(builder.toString());
    }

    public Room createRoom(String roomName, Player host){
        Room room = roomManager.createRoom(roomName);
        room.addPlayer(host);
        room.setHost(host);
        return room;
    }

    public Room getRoom(int roomId){
        Room room = roomManager.getRoom(roomId);
        if(room == null)
            throw new NullPointerException();

        return room;
    }

    public void removeRoom(Room room){
        // notify other players that host has exited
        Player host = room.getHost();
        if(host != null)
            room.broadcastToOthers(host, new DataPack(DataPack.E_ROOM_EXIT, DataPackUtil.getPlayerInfoMessage(host)));

        for(Player roomPlayer : room.getPlayers()){
            roomPlayer.setStatus(Player.ROOM_SELECTING);
            roomPlayer.setRoom(null);
        }
        host.setHost(false);
        roomManager.removeRoom(room);
        logger.info("Room removed: " + room.toString());
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

    PlayerManager(ObjectManager parent){
        this.playerMap = new ConcurrentHashMap<>(100, 0.75f);
        this.parent = parent;
    }

    Player createPlayer(User user){
        Player player = new Player(user, this);
        this.playerMap.put(player.getId(), player);
        return player;
    }

    Player getPlayer(int id) {
        return this.playerMap.get(id);
    }

    void removePlayer(Player player){
        try{
            this.playerMap.remove(player.getId());
            player.getSocket().close();
        } catch(IOException e){

        }
    }

    Collection<Player> getAllPlayers(){
        return Collections.unmodifiableCollection(this.playerMap.values());
    }
}


class RoomManager {
    private Logger logger = LogManager.getLogger(RoomManager.class.getName());
    private ObjectManager parent = null;
    private Map<Integer, Room> rooms = null;
    private int nextId = 0;

    RoomManager(ObjectManager parent){
        this.parent = parent;
        this.rooms = new ConcurrentHashMap<>(100, 0.75f);
    }

    Room getRoom(int roomId){
        return this.rooms.get(roomId);
    }

    synchronized Room createRoom(String roomName){
        Room room = new Room(nextId, roomName, this);
        this.rooms.put(nextId, room);
        nextId++;
        parent.roomListChanged(room);
        logger.info("Room created: " + room.toString());
        return room;
    }

    void removeRoom(Room room) { this.rooms.remove(room.getId()); }

    protected void roomListChanged(Room changedRoom){
        parent.roomListChanged(changedRoom);
    }

    Collection<Room> getAllRooms(){
        return Collections.unmodifiableCollection(this.rooms.values());
    }
}