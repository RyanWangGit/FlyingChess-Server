package flyingchess.Main;

import core.DataPack.DataPack;
import flyingchess.GameObjects.Player;
import flyingchess.PlayerFilter.PlayerFilter;

import java.util.Collection;

/**
 * Created by Ryan on 16/5/2.
 */
public class BroadcastRunnable implements Runnable {
    private Collection<Player> allPlayers = null;
    private PlayerFilter filter = null;
    private DataPack dataPack = null;
    private boolean isRunning = true;

    public BroadcastRunnable(Collection<Player> allPlayers, PlayerFilter filter, DataPack dataPack){
        this.allPlayers = allPlayers;
        this.dataPack = dataPack;
        this.filter = filter;
    }

    @Override
    public void run(){
        for(Player player : allPlayers){
            try{
                if(!filter.isBlocked(player)){
                    if(isRunning)
                        player.getSocket().send(dataPack);
                    else
                        return;
                }
            } catch(Exception e){

            }
        }
    }

    public void shutdown(){
        this.isRunning = false;
    }
}