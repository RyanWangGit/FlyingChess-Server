package Managers;

import DataPack.DataPack;
import DataPack.DataPackUtil;
import Database.Database;
import GameObjects.Player.Player;
import GameObjects.Player.RoomSelectingFilter;
import GameObjects.Room;
import GameObjects.User;
import Server.BroadcastThread;
import Server.DataPackSocket;
import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Ryan on 16/5/4.
 */
public class ObjectManager {
    private static Logger logger = LogManager.getLogger(ObjectManager.class.getName());
    private PlayerManager playerManager = null;
    private RoomManager roomManager = null;
    private BroadcastThread broadcastThread = null;

    public ObjectManager(){
        this.playerManager = new PlayerManager();
        this.roomManager = new RoomManager();
    }

    /**
     * Invoked when the list of the rooms have changed,
     * this method creates a new thread to broadcast the new rooms
     * info to all players.
     */
    void roomListChanged(){
        if(broadcastThread != null){
            broadcastThread.shutdown();
            this.broadcastThread = null;
        }
        DataPack dataPack = new DataPack(DataPack.A_ROOM_LOOKUP, DataPackUtil.getRoomsMessage(this));
        this.broadcastThread = new BroadcastThread(getAllPlayers(), new RoomSelectingFilter(), dataPack);
        this.broadcastThread.start();
        return ;
    }

    public Player addPlayer(String userName, String password){
        User user = Database.getUser(userName);
        if(user == null || !user.getPasswordMD5().equals(password)){
            return null;
        }
        else{
            // get former connection
            Player currentPlayer = playerManager.getPlayer(user.getId());
            if(currentPlayer == null){
                Player player = new Player(user);
                playerManager.addPlayer(player);
                return player;
            }
            else {
                // close the former client connection
                if(!currentPlayer.getSocket().equals(this)){
                    try{
                        currentPlayer.getSocket().close();
                        currentPlayer.setSocket(null);
                    } catch(IOException | NullPointerException e){
                        logger.warn("Former connection has been closed.");
                    }
                    finally {
                        return currentPlayer;
                    }
                }
            }
        }
        return null;
    }

    public boolean registerUser(String userName, String password){
        User user = Database.getUser(userName);
        // user doesn't exist
        if(user == null)
           return Database.addUser(userName, password);
        else
            return false;
    }

    public Player getPlayer(DataPackSocket socket){

    }

    public Player getPlayer(int playerId){


    }

    public void removePlayer(Player player){

    }

    public Room addRoom(String roomName, Player host){
        newRoomInfoBroadcastThread().start();
    }

    public Room getRoom(int RoomId){

    }

    public void removeRoom(Room room){

    }

    public Collection<Room> getAllRooms(){

    }

    public Collection<Player> getAllPlayers(){

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

    public void addPlayer(Player player) {
        this.idPlayerMap.put(player.getId(), player);
        this.socketPlayerMap.put(player.getSocket(), player);
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

    public Collection<Room> getAllRooms(){
        return Collections.unmodifiableCollection(this.rooms.values());
    }

    public Room getRoom(int roomId){
        return this.rooms.get(roomId);
    }

    public synchronized int addRoom(String roomName, Player host){
        Room room = new Room(nextId, roomName, host);
        host.setHost(true);
        room.addPlayer(host);
        this.rooms.put(nextId, room);
        nextId++;
        return nextId - 1;
    }

    public void removeRoom(Room room) { this.rooms.remove(room.getId()); }
}