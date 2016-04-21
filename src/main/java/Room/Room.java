package Room;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Ryan on 16/4/21.
 */
public class Room {
    private int id = -1;
    private String name = null;
    private int[] positions = null;
    public Room(int id, String name, Integer hostId){
        this.id = id;
        this.name = name;
        this.positions = new int[4];
        this.positions[0] = hostId;
    }

    public int getId(){
        return this.id;
    }
    public String getName(){
        return this.name;
    }

    public Collection<Integer> getUsers(){
        Set<Integer> users = new HashSet<>(4);
        for(int userId : positions){
            if(userId > 0)
                users.add(userId);
        }
        return users;
    }

    public int[] getPositions(){
        return this.positions;
    }

    /**
     * Add the user to the room.
     * @param userId The id of the user
     * @return The position this user takes, or -1 if there's no available position.
     */
    public int addUser(Integer userId){
        for(int i = 0; i < 4; i++){
            if(positions[i] == 0){
                positions[i] = userId;
                return i;
            }
        }
        return -1;
    }

    public boolean removeUser(Integer userId){
        int index = getUserPosition(userId);
        if(index == -1)
            return false;

        positions[index] = 0;
        return true;
    }

    public int getUserPosition(Integer userId){
        for(int i = 0; i < 4; i++){
            if(positions[i] == userId){
                return i;
            }
        }
        return -1;
    }
}
