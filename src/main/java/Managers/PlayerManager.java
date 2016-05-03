package Managers;

import DataPack.DataPack;
import GameObjects.Player.Player;
import GameObjects.User;
import edu.emory.mathcs.backport.java.util.Collections;
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
    private Map<Integer, Player> allPlayers = null;

    public PlayerManager(){
        this.allPlayers = new ConcurrentHashMap<>(100, 0.75f);
    }

    public Player getPlayer(int id) {
        // if it is a robot
        if(id < 0 && id >= -4)
            return new Player(new User(id, "Robot", null, 0), null);
        else
            return this.allPlayers.get(id);

    }

    public void addPlayer(Player player) { this.allPlayers.put(player.getId(), player); }

    public void removePlayer(Player player){
        try{
            player.getSocket().send(new DataPack(DataPack.TERMINATE));
            player.getSocket().close();
            this.allPlayers.remove(player.getId());
        } catch(IOException e){
            logger.catching(e);
        }
    }

    public void removePlayer(int playerId){
        removePlayer(this.allPlayers.get(playerId));
    }

    public Collection<Player> getAllPlayers(){
        return Collections.unmodifiableCollection(this.allPlayers.values());
    }

}
