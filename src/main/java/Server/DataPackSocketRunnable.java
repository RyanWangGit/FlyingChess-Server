package Server;

import DataPack.DataPack;
import DataPack.DataPackUtil;
import Database.Database;
import GameObjects.Player.Player;
import GameObjects.Player.RoomSelectingFilter;
import GameObjects.Room;
import GameObjects.User;
import Managers.PlayerManager;
import Managers.RoomManager;
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
    private RoomManager roomManager = null;
    private PlayerManager playerManager = null;
    private BroadcastThread broadcastThread = null;

    public DataPackSocketRunnable(RoomManager roomManager, PlayerManager playerManager, Socket socket) throws IOException{
        super(socket);
        this.roomManager = roomManager;
        this.playerManager = playerManager;
    }

    public DataPackSocketRunnable(PlayerManager playerManager, RoomManager roomManager, Socket socket) throws IOException{
        super(socket);
        this.playerManager = playerManager;
        this.roomManager = roomManager;
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

    private Thread newRoomInfoBroadcastThread(){
        if(broadcastThread != null){
           broadcastThread.shutdown();
        }
        DataPack dataPack = new DataPack(DataPack.A_ROOM_LOOKUP, DataPackUtil.getRoomsMessage(roomManager));
        return new BroadcastThread(playerManager.getAllPlayers(), new RoomSelectingFilter(), dataPack);
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
                case DataPack.R_LOGIN:{
                    String username = dataPack.getMessage(0);
                    String passwordMD5 = dataPack.getMessage(1).toUpperCase();

                    User user = Database.getUser(username);
                    if(user == null || !user.getPasswordMD5().equals(passwordMD5)){
                        send(new DataPack(DataPack.A_LOGIN, false));
                    }
                    else{
                        // login successful
                        List<String> msgList = new ArrayList<>();
                        msgList.add(String.valueOf(user.getId()));
                        msgList.add(String.valueOf(user.getPoints()));
                        send(new DataPack(DataPack.A_LOGIN, true, msgList));

                        Player currentPlayer = playerManager.getPlayer(user.getId());
                        if(currentPlayer == null){
                            Player player = new Player(user, this);
                            playerManager.addPlayer(player);
                        }
                        else {
                            // close the former client connection
                            if(!currentPlayer.getSocket().equals(this)){
                                currentPlayer.getSocket().close();
                                currentPlayer.setSocket(this);
                            }
                        }
                    }
                    return;
                }
                case DataPack.R_REGISTER:{
                    String username = dataPack.getMessage(0);
                    String passwordMD5 = dataPack.getMessage(1).toUpperCase();

                    User user = Database.getUser(username);
                    if(user == null){
                        List<String> msgList = new ArrayList<>();

                        int userIndex = Database.addUser(username, passwordMD5);
                        msgList.add(String.valueOf(userIndex));
                        send(new DataPack(DataPack.A_REGISTER, true, msgList));
                    }
                    else{
                        send(new DataPack(DataPack.A_REGISTER, false));
                    }
                    return;
                }
                case DataPack.R_LOGOUT:{
                    int playerId = Integer.valueOf(dataPack.getMessage(0));

                    playerManager.removePlayer(playerId);
                    send(new DataPack(DataPack.A_LOGOUT, true));
                    return;
                }
                case DataPack.R_ROOM_CREATE:{
                    Integer playerId = Integer.valueOf(dataPack.getMessage(0));
                    String roomName = dataPack.getMessage(1);

                    int roomId = roomManager.addRoom(roomName, playerManager.getPlayer(playerId));
                    List<String> msgList = new ArrayList<>();
                    msgList.add(String.valueOf(roomId));
                    logger.info("Room created: " + roomId + " " + roomName);
                    send(new DataPack(DataPack.A_ROOM_CREATE, true, msgList));
                    return;
                }
                case DataPack.R_ROOM_LOOKUP:{
                    List<String> msgList = DataPackUtil.getRoomsMessage(roomManager);
                    dataPack.setCommand(DataPack.A_ROOM_LOOKUP);
                    dataPack.setSuccessful(true);
                    dataPack.setMessageList(msgList);
                    send(dataPack);
                    return;
                }
                case DataPack.R_ROOM_ENTER:{
                    Player player = playerManager.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    Room room = roomManager.getRoom(Integer.valueOf(dataPack.getMessage(1)));
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

                    newRoomInfoBroadcastThread().start();

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

                    return;
                }
                case DataPack.R_GAME_START:{
                    Player player = playerManager.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    Room room = roomManager.getRoom(Integer.valueOf(dataPack.getMessage(1)));

                    if(player.isHost()){
                        newRoomInfoBroadcastThread().start();

                        room.setPlaying(true);
                        // send out game start signal to the players
                        for(Player roomPlayer : room.getPlayers()) {
                            if(!roomPlayer.isRobot())
                                roomPlayer.getSocket().send(new DataPack(DataPack.E_GAME_START, true));
                        }
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
                        if(!roomPlayer.equals(player)){
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
