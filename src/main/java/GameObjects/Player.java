package GameObjects;

import Server.DataPackSocket;

/**
 * Created by Ryan on 16/4/27.
 */
public class Player extends User {
    private boolean isHost = false;
    private DataPackSocket socket = null;
    private int status = 0;
    private PlayerManager parent = null;
    private Room room = null;
    public static final int ROOM_SELECTING = 0;
    public static final int ROOM_WAITING = 1;
    public static final int PLAYING = 2;

    Player(User user, PlayerManager parent){
        super(user);
        this.socket = null;
        this.status = ROOM_SELECTING;
        this.parent = parent;
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

    @Override
    public int hashCode(){
        return this.id;
    }

    public int getStatus() { return this.status; }

    public void setStatus(int status) { this.status = status; }

    public Room getRoom(){ return room; }

    protected void setRoom(Room room) { this.room = room; }
}
