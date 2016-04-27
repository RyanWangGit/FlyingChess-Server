package Server;

import DataPack.DataPack;
import Database.Database;
import GameObjects.Room;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Ryan on 16/4/12.
 */
class UserSocketRunnable extends UserSocket implements Runnable {
    private static Logger logger = LogManager.getLogger(UserSocketRunnable.class.getName());
    private SSLServer parent = null;


    public UserSocketRunnable(SSLServer parent, Socket socket) throws IOException{
        super(socket);
        this.parent = parent;
    }

    public void run(){
        try{
            logger.info("Connection established with " + socket.getInetAddress().toString());

            // enter process loop
            while(true){
                processDataPack(receive());
            }

        } catch(EOFException e){
            logger.warn("Found EOF when reading from " + socket.getInetAddress().toString() + ".Drop the connection.");
        }
        catch(SocketException e){
            logger.info("Socket shutdown due to external request.");
        }
        catch (Exception e){
            logger.catching(e);
        }
    }

    /**
     * Process the incoming data packs.
     *
     * @param dataPack The data pack to be processed.
     */
    private void processDataPack(DataPack dataPack) {
        try{
            switch(dataPack.getCommand()){
                case DataPack.INVALID:{
                    logger.warn("DataPack Invalid.");
                    return;
                }
                case DataPack.LOGIN:{
                    String username = dataPack.getMessage(0);
                    String passwordMD5 = dataPack.getMessage(1).toUpperCase();

                    List<String> userInfo = Database.getUser(username);
                    if(userInfo == null){
                        send(new DataPack(DataPack.LOGIN, false));
                    }
                    else{
                        // login successful
                        if(userInfo.get(2).equals(passwordMD5)){
                            List<String> msgList = new ArrayList<>();
                            msgList.add(userInfo.get(0));
                            msgList.add(userInfo.get(3));
                            UserSocket currentSocket = parent.getUserSocket(Integer.valueOf(userInfo.get(0)));
                            send(new DataPack(DataPack.LOGIN, true, msgList));

                            // process the user's former connections
                            if(currentSocket != null && !currentSocket.equals(this)){
                                currentSocket.send(new DataPack(DataPack.TERMINATE));
                                currentSocket.close();
                            }
                            parent.addUserSocket(Integer.valueOf(userInfo.get(0)), this);
                        }
                    }
                    return;
                }
                case DataPack.REGISTER:{
                    String username = dataPack.getMessage(0);
                    String passwordMD5 = dataPack.getMessage(1).toUpperCase();

                    List<String> userInfo = Database.getUser(username);
                    if(userInfo == null){
                        int userIndex = Database.addUser(username, passwordMD5);
                        List<String> msgList = new ArrayList<>();
                        msgList.add(String.valueOf(userIndex));
                        send(new DataPack(DataPack.REGISTER, true, msgList));
                    }
                    else{
                        send(new DataPack(DataPack.REGISTER, false));
                    }
                    return;
                }
                case DataPack.LOGOUT:{
                    int userIndex = Integer.valueOf(dataPack.getMessage(0));
                    parent.removeUserSocket(userIndex);
                    send(new DataPack(DataPack.LOGOUT, true));
                    return;
                }
                case DataPack.ROOM_CREATE:{
                    Integer userId = Integer.valueOf(dataPack.getMessage(0));
                    String roomName = dataPack.getMessage(1);
                    int roomId = parent.addRoom(roomName, userId);
                    List<String> msgList = new ArrayList<>();
                    msgList.add(String.valueOf(roomId));
                    logger.info("Room created: " + roomId + " " + roomName);
                    send(new DataPack(DataPack.ROOM_CREATE, true, msgList));
                    return;
                }
                case DataPack.ROOM_ENTER:{
                    Integer userId = Integer.valueOf(dataPack.getMessage(0));
                    Integer roomId = Integer.valueOf(dataPack.getMessage(1));
                    Room room = parent.getRoom(roomId);
                    if(room == null){
                        send(new DataPack(DataPack.ROOM_ENTER, false));
                    }
                    else{
                        // if room has reached its limit
                        if(room.getUsers().size() == 4){
                            send(new DataPack(DataPack.ROOM_ENTER, false));
                        }
                        else{
                            // add user into room
                            int position = room.addUser(userId);

                            // send room user info back
                            List<String> msgList = new ArrayList<>();
                            msgList.add(String.valueOf(position));
                            for(Integer roomUserId : room.getUsers()){
                                if(roomUserId != userId){
                                    // add user id
                                    msgList.add(String.valueOf(roomUserId));

                                    List<String> userInfo = Database.getUser(roomUserId);
                                    // add user name
                                    msgList.add(userInfo.get(1));
                                    // add user points
                                    msgList.add(userInfo.get(3));
                                    // add user position
                                    msgList.add(String.valueOf(room.getUserPosition(roomUserId)));
                                }
                            }
                            send(new DataPack(DataPack.ROOM_ENTER, true, msgList));

                            // send new user info to other users
                            msgList.clear();
                            msgList.add(String.valueOf(userId));
                            List<String> userInfo = Database.getUser(userId);
                            msgList.add(userInfo.get(1));
                            msgList.add(userInfo.get(3));
                            msgList.add(String.valueOf(position));
                            for(Integer roomUserId : room.getUsers()){
                                parent.getUserSocket(roomUserId)
                                        .send(new DataPack(DataPack.ROOM_USER_ENTERED, true, msgList));
                            }
                        }
                    }
                    return;
                }
                case DataPack.ROOM_LOOKUP:{
                    List<String> msgList = new ArrayList<>();
                    for(Room room : parent.getRooms()){
                        // add room id
                        msgList.add(String.valueOf(room.getId()));
                        // add room name
                        msgList.add(room.getName());
                        // add number of players in the room
                        msgList.add(String.valueOf(room.getUsers().size()));
                        // add room status
                        if(room.isPlaying())
                            msgList.add("1");
                        else
                            msgList.add("0");
                    }
                    send(new DataPack(DataPack.ROOM_LOOKUP, true, msgList));
                    return;
                }
                case DataPack.ROOM_EXIT:{
                    Integer userId = Integer.valueOf(dataPack.getMessage(0));
                    Integer roomId = Integer.valueOf(dataPack.getMessage(1));
                    Room room = parent.getRoom(roomId);
                    room.removeUser(userId);
                    send(new DataPack(DataPack.ROOM_EXIT, true));

                    List<String> msgList = new ArrayList<>();
                    msgList.add(String.valueOf(userId));
                    // send user left message to other users
                    for(Integer roomUserId : room.getUsers()){
                        parent.getUserSocket(roomUserId)
                                .send(new DataPack(DataPack.ROOM_USER_LEFT, true, msgList));
                    }
                    return;
                }
                case DataPack.GAME_START:{
                    Integer userId = Integer.valueOf(dataPack.getMessage(0));
                    Integer roomId = Integer.valueOf(dataPack.getMessage(1));
                    Room room = parent.getRoom(roomId);

                    // send out game start signal and positions info to the users
                    for(Integer roomUserId : room.getUsers()) {
                        parent.getUserSocket(roomUserId)
                                .send(new DataPack(DataPack.GAME_START, true));
                    }
                    return;
                }
                // the following 2 commands' logic is basically the same(simply forward the datapack)
                case DataPack.GAME_FINISHED:
                case DataPack.GAME_PROCEED:{
                    Integer roomId = Integer.valueOf(dataPack.getMessage(1));
                    Room room = parent.getRoom(roomId);

                    // update datapack's time
                    dataPack.setDate(new Date());

                    // simply forward the datapack to the users in the same room
                    for(Integer roomUserId : room.getUsers()) {
                        parent.getUserSocket(roomUserId)
                                .send(dataPack);
                    }
                    return;
                }
                default:
                    return;
            }
        } catch(IndexOutOfBoundsException e){
            logger.warn("DataPack missing necessary parameters.");
        }
        catch(Exception e){
            logger.catching(e);
        }
    }

    @Override
    public boolean equals(Object obj){
        if(obj == null)
            return false;

        if(!(obj instanceof UserSocketRunnable))
            return false;

        UserSocketRunnable socketRunnable = (UserSocketRunnable) obj;

        if(socketRunnable.socket == null)
            return false;

        if(socketRunnable.socket.getInetAddress().equals(this.socket.getInetAddress())
                && socketRunnable.socket.getPort() == this.socket.getPort()){
            return true;
        }

        return false;
    }
}
