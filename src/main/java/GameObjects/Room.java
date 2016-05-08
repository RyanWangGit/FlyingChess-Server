package GameObjects;

import java.util.*;

/**
 * Created by Ryan on 16/4/21.
 */
public class Room {
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
        parent.roomListChanged(this);
    }

    public void setHost(Player host){
        if(this.players.containsValue(host))
            host.setHost(true);
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
        return true;
    }

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
        parent.roomListChanged(this);
    }

    public void startGame() {
        this.isPlaying = true;
        for(Player player : players.values()){
            player.setStatus(Player.PLAYING);
        }
        parent.roomListChanged(this);
    }

    public void finishGame(){
        this.isPlaying = false;
        for(Player player : players.values()){
            player.setStatus(Player.ROOM_WAITING);
        }
        parent.roomListChanged(this);
    }
}
