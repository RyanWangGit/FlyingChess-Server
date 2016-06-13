package flyingchess.PlayerFilter;

import flyingchess.GameObjects.Player;

/**
 * Created by Ryan on 16/5/2.
 */
public class RoomSelectingFilter implements PlayerFilter{

    public boolean isBlocked(Player player){
        return !player.isInStatus(Player.ROOM_SELECTING);
    }
}
