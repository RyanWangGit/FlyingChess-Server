package GameObjects;

import java.util.*;

/**
 * Created by Ryan on 16/4/21.
 */
public class Room {
    private int id = -1;
    private String name = null;
    private Player[] readyPlayers = null;
    private boolean isPlaying = false;
    private Collection<Player>  players = null;

    public Room(int id, String name, Player host){
        this.id = id;
        this.name = name;
        this.readyPlayers = new Player[4];
        this.isPlaying = false;
        this.players = new HashSet<>();
        this.players.add(host);
    }

    public void setPlaying(boolean isPlaying) { this.isPlaying = isPlaying; }

    public boolean isPlaying() { return this.isPlaying; }

    public int getId(){
        return this.id;
    }

    public String getName(){
        return this.name;
    }

    public Collection<Player> getPlayers() { return this.players; }

    /**
     * Add the player to the room.
     * @param player The player object.
     */
    public void addPlayer(Player player) { this.players.add(player); }

    public void removePlayer(Player player){
        // remove the player from ready players' array.
        for(int i = 0;i < 4;i ++){
            Player readyPlayer = readyPlayers[i];
            if(player.equals(readyPlayer))
                readyPlayers[i] = null;
        }

        this.players.remove(player);
    }

    public boolean playerSelectPosition(Player player, int position){
        if(position < -1 || position >= 4 || !this.players.contains(player))
            return false;

        // if the player wants to unready
        if(position == -1){
            for(int i = 0;i < 4;i ++){
                Player readyPlayer = readyPlayers[i];
                if(readyPlayer.equals(player)){
                    readyPlayers[i] = null;
                    return true;
                }
            }
        }
        else{
            if(readyPlayers[position] != null)
                return false;

            readyPlayers[position] = player;
            return true;
        }
        return false;
    }

    public int getPlayerPosition(Player player){
        for(int i = 0;i < 4;i ++){
            if(player.equals(readyPlayers[i]))
                return i;
        }
        return -1;
    }
}
