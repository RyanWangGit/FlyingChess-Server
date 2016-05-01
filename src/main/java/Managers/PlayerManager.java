package Managers;

import DataPack.DataPack;
import GameObjects.Player;
import GameObjects.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Ryan on 16/5/1.
 */
public class PlayerManager {
    private static Logger logger = LogManager.getLogger(PlayerManager.class.getName());
    private Map<Integer, Player> players = null;

    public PlayerManager(){
        this.players = new ConcurrentHashMap<>(1000, 0.75f);
    }
    public Player getPlayer(int id) {
        // if it is a robot
        if(id < 0 && id >= -4){
            return new Player(new User(id, "Robot", null, 0), null);
        }
        else
            return this.players.get(id);
    }

    public void addPlayer(Player player){
        Player currentPlayer = this.players.get(player.getId());
        if(currentPlayer != null){
            if(currentPlayer.getSocket().equals(player.getSocket()))
                return;
            // remove current
            removePlayer(currentPlayer);
        }

        this.players.put(player.getId(), player);
        return;
    }

    public void removePlayer(Player player){
        try{
            player.getSocket().send(new DataPack(DataPack.TERMINATE));
            player.getSocket().close();
            this.players.remove(player.getId());
        } catch(IOException e){
            logger.catching(e);
        }
    }

    public void removePlayer(int playerId){
        removePlayer(this.players.get(playerId));
    }

    public Collection<Player> getPlayers(){
        return this.players.values();
    }
}
