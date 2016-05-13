package GameObjects;

import DataPack.DataPack;
import DataPack.DataPackUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ryan on 16/4/21.
 */
public class Room {
    private Logger logger = LogManager.getLogger(Room.class.getName());
    private RoomManager parent = null;
    private int id = -1;
    private String name = null;
    private Player[] readyPlayers = null;
    private boolean isPlaying = false;
    private Map<Integer, Player>  players = null;

    Room(int id, String name, RoomManager parent){
        this.id = id;
        this.name = name;
        this.readyPlayers = new Player[4];
        this.isPlaying = false;
        this.players = new HashMap<>();
        this.parent = parent;
    }

    public boolean isPlaying() { return this.isPlaying; }

    public int getId(){
        return this.id;
    }

    public String getName(){
        return this.name;
    }

    public Collection<Player> getPlayers() { return this.players.values(); }

    /**
     * Add the player to the room.
     * @param player The player object.
     */
    public void addPlayer(Player player) {
        player.setStatus(Player.ROOM_WAITING);
        player.setRoom(this);
        this.players.put(player.getId(), player);

        // notify other players
        broadcastToOthers(player, new DataPack(DataPack.E_ROOM_ENTER, DataPackUtil.getPlayerInfoMessage(player)));

        logger.info(player.toString() + " has entered the room " + this.toString());
        parent.roomListChanged(this);
    }

    public void setHost(Player host){
        if(this.players.containsValue(host))
            host.setHost(true);
    }

    public Player getHost(){
        for(Player player : players.values()){
            if(player.isHost())
                return player;
        }
        return null;
    }

    public int getPlayerPosition(Player player){
        for(int i = 0;i < 4;i ++){
            if(player.equals(readyPlayers[i]))
                return i;
        }
        return -1;
    }

    public boolean playerSelectPosition(Player player, int position){
        if(position < -1 || position >= 4 || (!player.isRobot() && !this.players.containsKey(player.getId())))
            return false;

        // remove the player from the current position
        for(int i = 0;i < 4;i ++){
            Player readyPlayer = readyPlayers[i];
            if(player.equals(readyPlayer)){
                readyPlayers[i] = null;
                if(player.isRobot()){
                    players.remove(player.getId());
                    parent.roomListChanged(this);
                }
                break;
            }
        }

        // if the player wants to pick another position
        if(position != -1){
            if(readyPlayers[position] != null)
                return false;

            readyPlayers[position] = player;
            if(player.isRobot()){
                players.put(player.getId(), player);
                parent.roomListChanged(this);
            }
        }
        // notify other players
        broadcastToOthers(player, new DataPack(DataPack.E_ROOM_POSITION_SELECT, true, DataPackUtil.getPlayerInfoMessage(player)));
        return true;
    }

    public void broadcastToAll(DataPack dataPack){
       for(Player roomPlayer : players.values()){
           if(!roomPlayer.isRobot()){
               try{
                   roomPlayer.getSocket().send(dataPack);
               } catch(Exception e){

               }
           }
       }
    }

    public void broadcastToOthers(Player broadcaster, DataPack dataPack){
        if(players.containsValue(broadcaster)){
            for(Player roomPlayer : players.values()){
                if(!roomPlayer.equals(broadcaster) &&!roomPlayer.isRobot()){
                    try{
                        roomPlayer.getSocket().send(dataPack);
                    } catch(Exception e){

                    }
                }
            }
        }
    }

    public boolean containsPlayer(Player player){ return players.containsValue(player); }

    public void removePlayer(Player player){
        // remove the player from ready players' array.
        for(int i = 0;i < 4;i ++){
            Player readyPlayer = readyPlayers[i];
            if(player.equals(readyPlayer))
                readyPlayers[i] = null;
        }
        player.setStatus(Player.ROOM_SELECTING);
        player.setRoom(null);
        this.players.remove(player.getId());
        // notify other players
        broadcastToOthers(player, new DataPack(DataPack.E_ROOM_EXIT, DataPackUtil.getPlayerInfoMessage(player)));

        logger.info(player.toString() + " has left the room " + this.toString());
        parent.roomListChanged(this);
    }

    public void startGame() {
        this.isPlaying = true;
        for(Player player : players.values())
            player.setStatus(Player.PLAYING);

        // send out game start signal to the players
        broadcastToAll(new DataPack(DataPack.E_GAME_START, true));

        logger.info(this.toString() + " has started the game.");
        parent.roomListChanged(this);
    }

    public void finishGame(){
        this.isPlaying = false;
        for(Player player : players.values()){
            player.setStatus(Player.ROOM_WAITING);
        }
        logger.info(this.toString() + " has finished the game.");
        parent.roomListChanged(this);
    }

    @Override
    public String toString(){
        return name + '(' + String.valueOf(id) + ')';
    }
}
