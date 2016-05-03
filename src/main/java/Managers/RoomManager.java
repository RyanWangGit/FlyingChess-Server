package Managers;

import GameObjects.Player.Player;
import GameObjects.Room;
import edu.emory.mathcs.backport.java.util.Collections;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Ryan on 16/5/1.
 */
public class RoomManager {
    private Map<Integer, Room> rooms = null;
    private int nextId = 0;

    public RoomManager(){
        this.rooms = new ConcurrentHashMap<>(100, 0.75f);
    }

    public Collection<Room> getAllRooms(){
        return Collections.unmodifiableCollection(this.rooms.values());
    }

    public Room getRoom(int roomId){
        return this.rooms.get(roomId);
    }

    public synchronized int addRoom(String roomName, Player host){
        Room room = new Room(nextId, roomName, host);
        host.setHost(true);
        room.addPlayer(host);
        this.rooms.put(nextId, room);
        nextId++;
        return nextId - 1;
    }

    public void removeRoom(Room room) { this.rooms.remove(room.getId()); }
}
