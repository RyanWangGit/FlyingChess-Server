package PlayerFilter;

import GameObjects.Player;

/**
 * Created by Ryan on 16/5/2.
 */
public class RoomSelectingFilter implements PlayerFilter{

    public boolean isBlocked(Player player){
        return !(player.getStatus() == Player.ROOM_SELECTING);
    }
}
