package Server;

import DataPack.DataPack;
import DataPack.DataPackUtil;
import Database.Database;
import GameObjects.Player.Player;
import GameObjects.Player.RoomSelectingFilter;
import GameObjects.Room;
import Managers.ObjectManager;
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
    private ObjectManager objectManager = null;
    private BroadcastThread broadcastThread = null;

    public DataPackSocketRunnable(ObjectManager objectManager, Socket socket) throws IOException{
        super(socket);
        this.objectManager = objectManager;
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
        finally {
            objectManager.removePlayer(objectManager.getPlayer(this));
        }
    }





    /**
     * Process the incoming data packs.
     *
     * @param dataPack The data pack to be processed.
     */
    private void processDataPack(DataPack dataPack) throws SocketException, IOException{
        try{
            switch(dataPack.getCommand()){
                case DataPack.INVALID:{
                    logger.warn("DataPack Invalid.");
                    return;
                }
                case DataPack.R_LOGIN:{
                    String playerName = dataPack.getMessage(0);
                    String password = dataPack.getMessage(1).toUpperCase();

                    Player player = objectManager.addPlayer(playerName, password);

                    // login successful
                    if(player != null){
                        player.setSocket(this);
                        List<String> msgList = new ArrayList<>();
                        msgList.add(String.valueOf(player.getId()));
                        msgList.add(String.valueOf(player.getPoints()));
                        send(new DataPack(DataPack.A_LOGIN, true, msgList));
                    }
                    // login failed
                    else{
                        send(new DataPack(DataPack.A_LOGIN, false));
                    }
                    return;
                }
                case DataPack.R_REGISTER:{
                    String userName = dataPack.getMessage(0);
                    String password = dataPack.getMessage(1).toUpperCase();
                    boolean isSuccessful = objectManager.registerUser(userName, password);
                    send(new DataPack(DataPack.A_REGISTER, isSuccessful));
                    return;
                }
                case DataPack.R_LOGOUT:{
                    Player player = objectManager.getPlayer(Integer.valueOf(dataPack.getMessage(0));

                    objectManager.removePlayer(player);

                    send(new DataPack(DataPack.A_LOGOUT, true));
                    return;
                }
                case DataPack.R_ROOM_CREATE:{
                    Player player = objectManager.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    String roomName = dataPack.getMessage(1);

                    Room room = objectManager.addRoom(roomName, player);

                    List<String> msgList = new ArrayList<>();
                    msgList.add(String.valueOf(room.getId()));
                    send(new DataPack(DataPack.A_ROOM_CREATE, true, msgList));

                    logger.info("Room created: " + room.getId() + " " + room.getName());
                    return;
                }
                case DataPack.R_ROOM_LOOKUP:{
                    List<String> msgList = DataPackUtil.getRoomsMessage(objectManager);
                    dataPack.setCommand(DataPack.A_ROOM_LOOKUP);
                    dataPack.setSuccessful(true);
                    dataPack.setMessageList(msgList);
                    send(dataPack);
                    return;
                }
                case DataPack.R_ROOM_ENTER:{
                    Player player = objectManager.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    Room room = objectManager.getRoom(Integer.valueOf(dataPack.getMessage(1)));
                    if(room == null || player == null){
                        send(new DataPack(DataPack.A_ROOM_ENTER, false));
                    }
                    else{
                        // if room has reached its limit
                        if(room.getPlayers().size() >= 4){
                            send(new DataPack(DataPack.A_ROOM_ENTER, false));
                        }
                        else{
                            room.addPlayer(player);

                            // broadcast new room info
                            newRoomInfoBroadcastThread().start();

                            // send room player info back
                            send(new DataPack(DataPack.A_ROOM_ENTER, true, DataPackUtil.getRoomPlayerInfoMessage(room)));

                            // send new player info to other players
                            dataPack.setCommand(DataPack.E_ROOM_ENTER);
                            dataPack.setDate(new Date());
                            dataPack.setSuccessful(true);
                            dataPack.setMessageList(DataPackUtil.getPlayerInfoMessage(player, room));
                            for(Player roomPlayer : room.getPlayers()){
                                if(!roomPlayer.equals(player) && !roomPlayer.isRobot()){
                                    roomPlayer.getSocket().send(dataPack);
                                }
                            }
                        }
                    }
                    return;
                }
                case DataPack.R_ROOM_POSITION_SELECT:{
                    Player player = playerManager.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    Room room = roomManager.getRoom(Integer.valueOf(dataPack.getMessage(1)));
                    int position = Integer.valueOf(dataPack.getMessage(4));

                    boolean isSuccessful = room.playerSelectPosition(player, position);
                    if(isSuccessful){
                        List<String> msgList = DataPackUtil.getPlayerInfoMessage(player, room);
                        for(Player roomPlayer : room.getPlayers()){
                            if(!roomPlayer.isRobot()){
                                roomPlayer.getSocket().send(new DataPack(DataPack.E_ROOM_POSITION_SELECT, true, msgList));
                            }
                        }
                    }
                    else{
                        send(new DataPack(DataPack.E_ROOM_POSITION_SELECT, false));
                    }
                    return;
                }
                case DataPack.R_ROOM_EXIT:{
                    Player player = playerManager.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    Room room = roomManager.getRoom(Integer.valueOf(dataPack.getMessage(1)));

                    List<String> msgList = DataPackUtil.getPlayerInfoMessage(player, room);
                    dataPack.setCommand(DataPack.E_ROOM_EXIT);
                    dataPack.setDate(new Date());
                    dataPack.setSuccessful(true);
                    dataPack.setMessageList(msgList);
                    // send player left message to other players
                    for(Player roomPlayer : room.getPlayers()){
                        if(!roomPlayer.equals(player)){
                            roomPlayer.getSocket().send(dataPack);
                        }
                    }

                    // remove the room if host exits
                    if(player.isHost()) {
                        roomManager.removeRoom(room);
                        player.setHost(false);
                    }
                    else{
                        room.removePlayer(player);
                    }

                    // send back operation result message
                    send(new DataPack(DataPack.A_ROOM_EXIT, true));

                    newRoomInfoBroadcastThread().start();

                    return;
                }
                case DataPack.R_GAME_START:{
                    Player player = playerManager.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    Room room = roomManager.getRoom(Integer.valueOf(dataPack.getMessage(1)));

                    if(player.isHost()){
                        room.setPlaying(true);
                        // send out game start signal to the players
                        for(Player roomPlayer : room.getPlayers()) {
                            if(!roomPlayer.isRobot())
                                roomPlayer.getSocket().send(new DataPack(DataPack.E_GAME_START, true));
                        }

                        newRoomInfoBroadcastThread().start();
                    }
                    else {
                        send(new DataPack(DataPack.E_GAME_START, false));
                    }
                    return;
                }
                // the following 2 commands' logic is basically the same(simply forward the datapack)
                case DataPack.R_GAME_FINISHED:{
                    Player player = playerManager.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    Room room = roomManager.getRoom(Integer.valueOf(dataPack.getMessage(1)));
                    room.setPlaying(false);
                    newRoomInfoBroadcastThread().start();

                    for(Player roomPlayer : room.getPlayers()){
                        if(!roomPlayer.isRobot()){
                            if(roomPlayer.equals(player)){
                                player.setPoints(player.getPoints() + 10);
                                Database.updateUser(player);
                            }
                            else{
                                roomPlayer.setPoints(roomPlayer.getPoints() - 5);
                                Database.updateUser(roomPlayer);
                            }
                        }
                    }

                    // add room player info to message list
                    List<String> msgList = new LinkedList<>();
                    msgList.add(String.valueOf(player.getId()));
                    msgList.addAll(DataPackUtil.getRoomPlayerInfoMessage(room));
                    dataPack.setMessageList(msgList);

                    // set datapack command
                    dataPack.setCommand(DataPack.E_GAME_FINISHED);
                    // update datapack's time
                    dataPack.setDate(new Date());

                    // simply forward the datapack to the users in the same room
                    for(Player roomPlayer : room.getPlayers()) {
                        if(!roomPlayer.equals(player) && !roomPlayer.isRobot()){
                            roomPlayer.getSocket().send(dataPack);
                        }
                    }
                    return;
                }
                case DataPack.R_GAME_PROCEED_DICE:{
                    Player player = playerManager.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    Room room = roomManager.getRoom(Integer.valueOf(dataPack.getMessage(1)));

                    // set the command
                    dataPack.setCommand(DataPack.E_GAME_PROCEED_DICE);
                    // update datapack's time
                    dataPack.setDate(new Date());

                    // simply forward the datapack to the users in the same room
                    for(Player roomPlayer : room.getPlayers()) {
                        if(!roomPlayer.equals(player) && !roomPlayer.isRobot()){
                            roomPlayer.getSocket().send(dataPack);
                        }
                    }
                    return;
                }
                case DataPack.R_GAME_PROCEED_PLANE:{
                    Player player = playerManager.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    Room room = roomManager.getRoom(Integer.valueOf(dataPack.getMessage(1)));

                    // set the command
                    dataPack.setCommand(DataPack.E_GAME_PROCEED_PLANE);
                    // update datapack's time
                    dataPack.setDate(new Date());

                    // simply forward the datapack to the users in the same room
                    for(Player roomPlayer : room.getPlayers()) {
                        if(!roomPlayer.equals(player) && !roomPlayer.isRobot()){
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
