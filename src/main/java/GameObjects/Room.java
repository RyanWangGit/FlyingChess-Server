package GameObjects;

import GameObjects.Player.Player;

import java.util.*;

/**
 * Created by Ryan on 16/4/21.
 */
public class Room {
    private int id = -1;
    private String name = null;
    private Player[] readyPlayers = null;
    private boolean isPlaying = false;
    private Map<Integer, Player>  players = null;

    public Room(int id, String name, Player host){
        this.id = id;
        this.name = name;
        this.readyPlayers = new Player[4];
        this.isPlaying = false;
        this.players = new HashMap<>();
        this.players.put(host.getId(), host);
    }

    public void setPlaying(boolean isPlaying) {
        this.isPlaying = isPlaying;
        if(isPlaying){
            for(Player player : players.values()){
                player.setStatus(Player.PLAYING);
            }
        }
        else{
            for(Player player : players.values()){
                player.setStatus(Player.ROOM_WAITING);
            }
        }
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
        this.players.put(player.getId(), player);
    }

    public void removePlayer(Player player){
        // remove the player from ready players' array.
        for(int i = 0;i < 4;i ++){
            Player readyPlayer = readyPlayers[i];
            if(player.equals(readyPlayer))
                readyPlayers[i] = null;
        }
        player.setStatus(Player.ROOM_SELECTING);
        this.players.remove(player.getId());
    }

    public boolean playerSelectPosition(Player player, int position){
        if(position < -1 || position >= 4 || (!player.isRobot() && !this.players.containsKey(player.getId())))
            return false;

        // remove the player from the current position
        for(int i = 0;i < 4;i ++){
            Player readyPlayer = readyPlayers[i];
            if(player.equals(readyPlayer)){
                readyPlayers[i] = null;
                if(player.isRobot())
                    players.remove(player.getId());
                break;
            }
        }

        // if the player wants to pick another position
        if(position != -1){
            if(readyPlayers[position] != null)
                return false;

            readyPlayers[position] = player;
            if(player.isRobot())
                players.put(player.getId(), player);
        }
        return true;
    }

    public int getPlayerPosition(Player player){
        for(int i = 0;i < 4;i ++){
            if(player.equals(readyPlayers[i]))
                return i;
        }
        return -1;
    }
}
