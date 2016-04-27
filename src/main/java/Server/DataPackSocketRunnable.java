package Server;

import DataPack.DataPack;
import Database.Database;
import GameObjects.Player;
import GameObjects.Room;
import GameObjects.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Ryan on 16/4/12.
 */
class DataPackSocketRunnable extends DataPackSocket implements Runnable {
    private static Logger logger = LogManager.getLogger(DataPackSocketRunnable.class.getName());
    private SSLServer parent = null;

    public DataPackSocketRunnable(SSLServer parent, Socket socket) throws IOException{
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
        } catch(SocketException e){
            logger.info("Socket shutdown due to external request.");
        } catch (Exception e){
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

                    User user = Database.getUser(username);
                    if(user == null){
                        send(new DataPack(DataPack.LOGIN, false));
                    }
                    else{
                        // login successful
                        if(user.getPasswordMD5().equals(passwordMD5)){

                            List<String> msgList = new ArrayList<>();
                            msgList.add(String.valueOf(user.getId()));
                            msgList.add(String.valueOf(user.getPoints()));
                            send(new DataPack(DataPack.LOGIN, true, msgList));

                            Player player = new Player(user, this);
                            parent.addPlayer(player);
                        }
                    }
                    return;
                }
                case DataPack.REGISTER:{
                    String username = dataPack.getMessage(0);
                    String passwordMD5 = dataPack.getMessage(1).toUpperCase();

                    User user = Database.getUser(username);
                    if(user == null){
                        List<String> msgList = new ArrayList<>();

                        int userIndex = Database.addUser(username, passwordMD5);
                        msgList.add(String.valueOf(userIndex));
                        send(new DataPack(DataPack.REGISTER, true, msgList));
                    }
                    else{
                        send(new DataPack(DataPack.REGISTER, false));
                    }
                    return;
                }
                case DataPack.LOGOUT:{
                    int playerIndex = Integer.valueOf(dataPack.getMessage(0));

                    parent.removePlayer(parent.getPlayer(playerIndex));
                    send(new DataPack(DataPack.LOGOUT, true));
                    return;
                }
                case DataPack.ROOM_CREATE:{
                    Integer playerId = Integer.valueOf(dataPack.getMessage(0));
                    String roomName = dataPack.getMessage(1);

                    int roomId = parent.addRoom(roomName, parent.getPlayer(playerId));
                    List<String> msgList = new ArrayList<>();
                    msgList.add(String.valueOf(roomId));
                    logger.info("Room created: " + roomId + " " + roomName);
                    send(new DataPack(DataPack.ROOM_CREATE, true, msgList));
                    return;
                }
                case DataPack.ROOM_ENTER:{
                    Player player = parent.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    Room room = parent.getRoom(Integer.valueOf(dataPack.getMessage(1)));
                    if(room == null || player == null){
                        send(new DataPack(DataPack.ROOM_ENTER, false));
                    }
                    else{
                        // if room has reached its limit
                        if(room.getPlayers().size() == 4){
                            send(new DataPack(DataPack.ROOM_ENTER, false));
                        }
                        else{
                            room.addPlayer(player);
                            // send room user info back
                            List<String> msgList = new LinkedList<>();
                            List<String> otherPlayerMsgList = new LinkedList<>();
                            for(Player roomPlayer : room.getPlayers()){
                                // put the host player in the front
                                if(roomPlayer.isHost()){
                                    msgList.add(String.valueOf(roomPlayer.getId()));
                                    msgList.add(roomPlayer.getName());
                                    msgList.add(String.valueOf(roomPlayer.getPoints()));
                                }
                                else{
                                    // add user id
                                    otherPlayerMsgList.add(String.valueOf(roomPlayer.getId()));
                                    // add user name
                                    otherPlayerMsgList.add(roomPlayer.getName());
                                    // add user points
                                    otherPlayerMsgList.add(String.valueOf(roomPlayer.getPoints()));
                                }
                            }
                            msgList.addAll(otherPlayerMsgList);
                            send(new DataPack(DataPack.ROOM_ENTER, true, msgList));

                            // send new player info to other players
                            msgList.clear();
                            msgList.add(String.valueOf(player.getId()));
                            msgList.add(player.getName());
                            msgList.add(String.valueOf(player.getPoints()));
                            for(Player roomPlayer : room.getPlayers()){
                                if(!roomPlayer.equals(player)){
                                    roomPlayer.getSocket().send(new DataPack(DataPack.ROOM_USER_ENTERED, true, msgList));
                                }
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
                        msgList.add(String.valueOf(room.getPlayers().size()));
                        // add room status
                        if(room.isPlaying())
                            msgList.add("1");
                        else
                            msgList.add("0");
                    }
                    send(new DataPack(DataPack.ROOM_LOOKUP, true, msgList));
                    return;
                }
                case DataPack.ROOM_SELECT_POSITION:{
                    Player player = parent.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    Room room = parent.getRoom(Integer.valueOf(dataPack.getMessage(1)));
                    int position = Integer.valueOf(dataPack.getMessage(2));

                    boolean isSuccessful = room.playerSelectPosition(player, position);
                    if(isSuccessful){
                        List<String> msgList = new ArrayList<>();
                        msgList.add(String.valueOf(player.getId()));
                        msgList.add(String.valueOf(position));
                        player.getSocket().send(new DataPack(DataPack.ROOM_SELECT_POSITION, true));
                        for(Player roomPlayer : room.getPlayers()){
                            if(!roomPlayer.equals(player)){
                                roomPlayer.getSocket().send(new DataPack(DataPack.ROOM_USER_PICK_POSITION, msgList));
                            }
                        }
                    }
                    else{
                        player.getSocket().send(new DataPack(DataPack.ROOM_SELECT_POSITION, false));
                    }
                    return;
                }
                case DataPack.ROOM_EXIT:{
                    Player player = parent.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    Room room = parent.getRoom(Integer.valueOf(dataPack.getMessage(1)));

                    // remove the room if host exits
                    if(player.isHost()){
                        for(Player roomPlayer : room.getPlayers()){
                            send(new DataPack(DataPack.ROOM_EXIT, true));
                        }
                    }
                    else{
                        room.removePlayer(player);
                        send(new DataPack(DataPack.ROOM_EXIT, true));

                        List<String> msgList = new ArrayList<>();
                        msgList.add(String.valueOf(player.getId()));
                        // send player left message to other users
                        for(Player roomPlayer : room.getPlayers()){
                            roomPlayer.getSocket().send(new DataPack(DataPack.ROOM_USER_LEFT, true, msgList));
                        }
                    }
                    return;
                }
                case DataPack.GAME_START:{
                    Player player = parent.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    Room room = parent.getRoom(Integer.valueOf(dataPack.getMessage(1)));

                    if(player.isHost()){
                        room.setPlaying(true);
                        // send out game start signal and positions info to the users
                        for(Player roomPlayer : room.getPlayers()) {
                            roomPlayer.getSocket().send(new DataPack(DataPack.GAME_START, true));
                        }
                    }
                    else {
                        player.getSocket().send(new DataPack(DataPack.GAME_START, false));
                    }
                    return;
                }
                // the following 2 commands' logic is basically the same(simply forward the datapack)
                case DataPack.GAME_FINISHED:
                case DataPack.GAME_PROCEED:{
                    Player player = parent.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    Room room = parent.getRoom(Integer.valueOf(dataPack.getMessage(1)));

                    // update datapack's time
                    dataPack.setDate(new Date());

                    // simply forward the datapack to the users in the same room
                    for(Player roomPlayer : room.getPlayers()) {
                        if(!roomPlayer.equals(player)){
                            roomPlayer.getSocket().send(dataPack);
                        }
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

        if(!(obj instanceof DataPackSocketRunnable))
            return false;

        DataPackSocketRunnable socketRunnable = (DataPackSocketRunnable) obj;

        if(socketRunnable.socket == null)
            return false;

        if(socketRunnable.socket.getInetAddress().equals(this.socket.getInetAddress())
                && socketRunnable.socket.getPort() == this.socket.getPort()){
            return true;
        }

        return false;
    }
}
