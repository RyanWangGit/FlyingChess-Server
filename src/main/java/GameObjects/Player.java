package GameObjects;

import Server.DataPackSocket;

/**
 * Created by Ryan on 16/4/27.
 */
public class Player extends User {
    private boolean isHost = false;
    private DataPackSocket socket = null;

    public Player(User user, DataPackSocket socket, boolean isHost){
        super(user);
        this.socket = socket;
        this.isHost = isHost;
    }

    public Player(User user, DataPackSocket socket){
        super(user);
        this.socket = socket;
    }

    public boolean isRobot() { return this.id <= -1 && this.id >= -4; }

    public boolean isHost() { return this.isHost; }

    public void setHost(boolean isHost) { this.isHost = isHost; }

    public void setSocket(DataPackSocket socket) { this.socket = socket; }

    public DataPackSocket getSocket() { return this.socket; }

    @Override
    public boolean equals(Object obj){
        if(obj == null)
            return false;

        if(obj instanceof Player){
            Player player = (Player) obj;
            return this.id == player.id;
        }

        return false;
    }
}
