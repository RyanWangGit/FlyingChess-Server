package Server;

import DataPack.DataPack;
import Database.Database;
import Room.Room;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Ryan on 16/4/12.
 */
class UserCommunicationJob implements Runnable {
    private static Logger logger = LogManager.getLogger(UserCommunicationJob.class.getName());
    private Socket sock = null;
    private DataInputStream is = null;
    private DataOutputStream os = null;
    private SSLServer parent = null;
    private Gson dataPackGson = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create();
    private int blockSize = 0;

    public UserCommunicationJob(SSLServer parent, Socket sock) throws IOException{
        this.sock = sock;
        this.parent = parent;
        this.os = new DataOutputStream(sock.getOutputStream());
        this.is = new DataInputStream(sock.getInputStream());
    }

    public void run(){
        try{
            logger.info("Connection established with " + sock.getInetAddress().toString());
            // segment data into json strings

            while(true){
                processDataPack(receive());
            }

        } catch(EOFException e){
            logger.warn("Found EOF when reading from " + sock.getInetAddress().toString() + ".Drop the connection.");
        }
        catch(SocketException e){
            logger.info("Socket shutdown due to external request.");
        }
        catch (Exception e){
            logger.catching(e);
        }
    }

    private void processDataPack(DataPack dataPack){
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
                        send(new DataPack(DataPack.LOGIN, new Date(), false, null));
                    }
                    else{
                        // login successful
                        if(userInfo.get(2).equals(passwordMD5)){
                            List<String> msgList = new ArrayList<>();
                            msgList.add(userInfo.get(0));
                            msgList.add(userInfo.get(3));
                            for(Room room : parent.getRooms()){
                                // add room id
                                msgList.add(String.valueOf(room.getId()));
                                // add room name
                                msgList.add(room.getName());
                                // add number of players in the room
                                msgList.add(String.valueOf(room.getUsers().size()));
                            }
                            send(new DataPack(DataPack.LOGIN, new Date(), true, msgList));
                            parent.addUserCommunicationJob(Integer.valueOf(userInfo.get(0)), this);
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
                        send(new DataPack(DataPack.REGISTER, new Date(), true, msgList));
                    }
                    else{
                        send(new DataPack(DataPack.REGISTER, new Date(), false, null));
                    }
                    return;
                }
                case DataPack.LOGOUT:{
                    int userIndex = Integer.valueOf(dataPack.getMessage(0));
                    parent.removeUserCommunicationJob(userIndex);
                    send(new DataPack(DataPack.LOGOUT, new Date(), true, null));
                    return;
                }
                case DataPack.ROOM_CREATE:{
                    Integer userId = Integer.valueOf(dataPack.getMessage(0));
                    String roomName = dataPack.getMessage(1);
                    int roomId = parent.addRoom(roomName, userId);
                    List<String> msgList = new ArrayList<>();
                    msgList.add(String.valueOf(roomId));
                    logger.info("Room created: " + roomId + " " + roomName);
                    send(new DataPack(DataPack.ROOM_CREATE, new Date(), true, msgList));
                    return;
                }
                case DataPack.ROOM_ENTER:{
                    Integer userId = Integer.valueOf(dataPack.getMessage(0));
                    Integer roomId = Integer.valueOf(dataPack.getMessage(1));
                    Room room = parent.getRoom(roomId);
                    if(room == null){
                        send(new DataPack(DataPack.ROOM_ENTER, new Date(), false, null));
                    }
                    else{
                        // if room has reached its limit
                        if(room.getUsers().size() == 4){
                            send(new DataPack(DataPack.ROOM_ENTER, new Date(), false, null));
                        }
                        else{
                            // add user into room
                            int position = room.addUser(userId);

                            // send room user info back
                            List<String> msgList = new ArrayList<>();
                            msgList.add(String.valueOf(position));
                            for(Integer roomUserId : room.getUsers()){
                                // add user id
                                msgList.add(String.valueOf(roomUserId));
                                // add user name
                                msgList.add(Database.getUser(roomUserId).get(1));
                            }
                            send(new DataPack(DataPack.ROOM_ENTER, new Date(), true, msgList));

                            // send new user info to other users
                            msgList.clear();
                            msgList.add(String.valueOf(userId));
                            List<String> userInfo = Database.getUser(userId);
                            msgList.add(userInfo.get(1));
                            msgList.add(userInfo.get(3));
                            msgList.add(String.valueOf(position));
                            for(Integer roomUserId : room.getUsers()){
                                parent.getUserCommunicationJob(roomUserId)
                                        .send(new DataPack(DataPack.ROOM_USER_ENTERED, new Date(), true, msgList));
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
                    }
                    send(new DataPack(DataPack.LOGIN, new Date(), true, msgList));
                    return;
                }
                case DataPack.ROOM_EXIT:{
                    Integer userId = Integer.valueOf(dataPack.getMessage(0));
                    Integer roomId = Integer.valueOf(dataPack.getMessage(1));
                    Room room = parent.getRoom(roomId);
                    room.removeUser(userId);
                    send(new DataPack(DataPack.ROOM_EXIT, new Date(), true, null));

                    List<String> msgList = new ArrayList<>();
                    msgList.add(String.valueOf(userId));
                    // send user left message to other users
                    for(Integer roomUserId : room.getUsers()){
                        parent.getUserCommunicationJob(roomUserId)
                                .send(new DataPack(DataPack.ROOM_USER_LEFT, new Date(), true, msgList));
                    }
                    return;
                }
                case DataPack.GAME_START:{
                    Integer userId = Integer.valueOf(dataPack.getMessage(0));
                    Integer roomId = Integer.valueOf(dataPack.getMessage(1));
                    Room room = parent.getRoom(roomId);

                    // send out game start signal and positions info to the users
                    for(Integer roomUserId : room.getUsers()) {
                        parent.getUserCommunicationJob(roomUserId)
                                .send(new DataPack(DataPack.GAME_START, new Date(), true, null));
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
                        parent.getUserCommunicationJob(roomUserId)
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

    private DataPack receive() throws IOException{
        // get the block size integer
        if(blockSize == 0){
            this.blockSize = this.is.readInt();
            logger.debug("Message size: " + String.valueOf(this.blockSize));
        }

        byte[] bytes = new byte[blockSize];
        this.is.readFully(bytes);
        this.blockSize = 0;

        String json = new String(bytes, "UTF-8");
        logger.debug(json);

        // parse the datapack and return
        return dataPackGson.fromJson(json, DataPack.class);
    }

    public synchronized void send(DataPack dataPack) {
        try{
            byte[] sendBytes = dataPackGson.toJson(dataPack, DataPack.class).getBytes(Charset.forName("UTF-8"));
            int bytesSize = sendBytes.length;
            logger.debug(bytesSize);
            this.os.writeInt(bytesSize);
            this.os.write(sendBytes);
            logger.debug(new String(sendBytes));
        } catch(IOException e){
            logger.catching(e);
        }
    }

    public void shutdown(){
        try{
            this.sock.close();
        } catch(IOException e){
            logger.catching(e);
        }
    }
}
