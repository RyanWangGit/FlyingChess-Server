package Managers;

import GameObjects.Player;
import GameObjects.Room;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Ryan on 16/5/1.
 */
public class RoomManager {
    private Map<Integer, Room> rooms = null;
    private int nextRoomId = 0;

    public RoomManager(){
        this.rooms = new ConcurrentHashMap<>(100, 0.75f);
    }

    public Collection<Room> getRooms(){
        return this.rooms.values();
    }

    public Room getRoom(int roomId){
        return this.rooms.get(roomId);
    }

    public synchronized int addRoom(String roomName, Player host){
        Room room = new Room(nextRoomId, roomName, host);
        host.setHost(true);
        room.addPlayer(host);
        this.rooms.put(nextRoomId, room);
        nextRoomId++;
        return nextRoomId - 1;
    }

    public void removeRoom(Room room) { this.rooms.remove(room.getId()); }
}
