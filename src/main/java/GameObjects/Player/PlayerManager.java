package GameObjects.Player;

import GameObjects.ObjectManager;
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
public class PlayerManager {
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