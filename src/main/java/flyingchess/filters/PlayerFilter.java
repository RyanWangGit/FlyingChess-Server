package flyingchess.filters;

import flyingchess.game.Player;

/**
 * Created by Ryan on 16/5/2.
 */
public interface PlayerFilter {
    boolean isBlocked(Player player);
}
