package flyingchess.FCDataPack;

import core.DataPack.DataPack;

import java.util.List;

/**
 * Created by Ryan on 16/6/13.
 */
public class FCDataPack extends DataPack {
    /**
     * Commands in login process.
     */
    public final static int R_LOGIN = 1000;
    public final static int A_LOGIN = 1010;
    public final static int R_LOGOUT = 1002;
    public final static int R_REGISTER = 1003;
    public final static int A_REGISTER = 1013;

    /**
     * Commands in room selecting process.
     */
    public final static int R_ROOM_ENTER = 2000;
    public final static int A_ROOM_ENTER = 2010;
    public final static int E_ROOM_ENTER = 2100;
    public final static int R_ROOM_CREATE = 2001;
    public final static int A_ROOM_CREATE = 2011;
    public final static int R_ROOM_LOOKUP = 2002;
    public final static int A_ROOM_LOOKUP = 2012;

    /**
     * Commands in room process.
     */
    public final static int R_ROOM_EXIT = 3000;
    public final static int E_ROOM_EXIT = 3100;
    public final static int R_ROOM_POSITION_SELECT = 3001;
    public final static int A_ROOM_POSITION_SELECT = 3101;
    public final static int E_ROOM_POSITION_SELECT = 3101;
    public final static int R_GAME_START = 3002;
    public final static int E_GAME_START = 3102;

    /**
     * Commands in gaming process.
     */
    public final static int R_GAME_PROCEED_DICE = 4000;
    public final static int E_GAME_PROCEED_DICE = 4100;
    public final static int R_GAME_PROCEED_PLANE = 4001;
    public final static int E_GAME_PROCEED_PLANE = 4101;
    public final static int E_GAME_PLAYER_DISCONNECTED = 4102;
    public final static int E_GAME_PLAYER_CONNECTED = 4103;
    public final static int R_GAME_EXIT = 4008;
    public final static int A_GAME_EXIT = 4018;
    public final static int R_GAME_FINISHED = 4009;
    public final static int E_GAME_FINISHED = 4109;

    public FCDataPack(int command, boolean isSuccessful, List<String> msgList){
        super(command, isSuccessful, msgList);
    }

    public FCDataPack(int command, List<String> msgList){
        super(command, msgList);
    }

    public FCDataPack(int command, boolean isSuccessful){
        super(command, isSuccessful);
    }

    public FCDataPack(int command){
        super(command);
    }
}
