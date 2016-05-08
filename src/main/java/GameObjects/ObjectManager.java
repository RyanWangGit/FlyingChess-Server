package GameObjects;

import DataPack.DataPack;
import DataPack.DataPackUtil;
import Database.Database;
import GameObjects.Player.Player;
import GameObjects.Player.PlayerManager;
import GameObjects.Player.RoomSelectingFilter;
import GameObjects.Room.Room;
import GameObjects.Room.RoomManager;
import GameObjects.Player.User;
import Server.BroadcastThread;
import Server.DataPackSocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Collection;

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

    public Player createPlayer(String userName, String password){
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

    public Room createRoom(String roomName, Player host){
        roomListChanged();

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