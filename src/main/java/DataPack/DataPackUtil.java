package DataPack;

import GameObjects.Player.Player;
import GameObjects.Room;
import Managers.RoomManager;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Ryan on 16/5/2.
 */
public class DataPackUtil {
    public static List<String> getRoomsMessage(RoomManager roomManager){
        List<String> msgList = new LinkedList<>();
        for(Room room : roomManager.getAllRooms()){
            msgList.addAll(DataPackUtil.getRoomInfoMessage(room));
        }
        return msgList;
    }

    public static List<String> getRoomPlayerInfoMessage(Room room){
        List<String> msgList = new LinkedList<>();
        List<String> otherPlayerMsgList = new LinkedList<>();
        for(Player player : room.getPlayers()){
            // put the host player in the front
            if(player.isHost())
                msgList.addAll(getPlayerInfoMessage(player, room));
            else
                otherPlayerMsgList.addAll(getPlayerInfoMessage(player, room));
        }
        msgList.addAll(otherPlayerMsgList);
        return msgList;
    }

    public static List<String> getPlayerInfoMessage(Player player, Room room){
        List<String> msgList = new LinkedList<>();
        msgList.add(String.valueOf(player.getId()));
        msgList.add(player.getName());
        msgList.add(String.valueOf(player.getPoints()));
        msgList.add(String.valueOf(room.getPlayerPosition(player)));
        return msgList;
    }

    public static List<String> getRoomInfoMessage(Room room){
        List<String> msgList = new LinkedList<>();
        // add room id
        msgList.add(String.valueOf(room.getId()));
        // add room name
        msgList.add(room.getName());
        // add number of players in the room
        msgList.add(String.valueOf(room.getPlayers().size()));
        // add room status
        if(room.isPlaying())
            msgList.add("1");
        else
            msgList.add("0");
        return msgList;
    }
}
