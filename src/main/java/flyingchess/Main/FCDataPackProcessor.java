package flyingchess.Main;

import core.DataPack.DataPack;
import core.DataPack.DataPackProcessor;
import core.DataPack.DataPackTcpSocket;
import flyingchess.FCDataPack.FCDataPackUtil;
import core.Database.Database;
import flyingchess.FCDataPack.FCDataPack;
import flyingchess.GameObjects.ObjectManager;
import flyingchess.GameObjects.Player;
import flyingchess.GameObjects.Room;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Ryan on 16/6/13.
 */
public class FCDataPackProcessor implements DataPackProcessor {
    private Logger logger = LogManager.getLogger(FCDataPackProcessor.class.getName());
    private ObjectManager objectManager = null;
    private Player selfPlayer = null;
    private DataPackTcpSocket socket = null;

    public FCDataPackProcessor(ObjectManager objectManager){
        this.objectManager = objectManager;
        this.selfPlayer = null;
    }

    /**
     * Defines the behavior when connection has started.
     * @param socket The socket of the connection.
     */
    @Override
    public void started(DataPackTcpSocket socket) {
        this.socket = socket;
    }

    /**
     * Defines the behavior when received a datapack.
     *
     * @param dataPack
     */
    @Override
    public void process(DataPack dataPack) throws IOException{
        try{
            switch(dataPack.getCommand()){
                case FCDataPack.INVALID:{
                    logger.warn("DataPack Invalid.");
                    return;
                }
                case FCDataPack.R_LOGIN:{
                    String playerName = dataPack.getMessage(0);
                    String password = dataPack.getMessage(1).toUpperCase();

                    Player player = objectManager.createPlayer(playerName, password);

                    // login successful
                    if(player != null){
                        player.setSocket(socket);
                        this.selfPlayer = player;
                        List<String> msgList = new ArrayList<>();
                        msgList.add(String.valueOf(player.getId()));
                        msgList.add(String.valueOf(player.getPoints()));
                        socket.send(new FCDataPack(FCDataPack.A_LOGIN, true, msgList));
                    }
                    // login failed
                    else{
                        socket.send(new FCDataPack(FCDataPack.A_LOGIN, false));
                    }
                    return;
                }
                case FCDataPack.R_REGISTER:{
                    String userName = dataPack.getMessage(0);
                    String password = dataPack.getMessage(1).toUpperCase();
                    boolean isSuccessful = objectManager.registerUser(userName, password);
                    socket.send(new FCDataPack(FCDataPack.A_REGISTER, isSuccessful));
                    if(isSuccessful)
                        logger.info("New user registered : " + userName);
                    return;
                }
                case FCDataPack.R_LOGOUT:{
                    Player player = objectManager.getPlayer(Integer.valueOf(dataPack.getMessage(0)));

                    objectManager.removePlayer(player);
                    return;
                }
                case FCDataPack.R_ROOM_CREATE:{
                    Player player = objectManager.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    String roomName = dataPack.getMessage(1);

                    if(player.getRoom() != null){
                        socket.send(new FCDataPack(FCDataPack.A_ROOM_CREATE, false));
                        return;
                    }

                    Room room = objectManager.createRoom(roomName, player);

                    List<String> msgList = new ArrayList<>();
                    msgList.add(String.valueOf(room.getId()));
                    socket.send(new FCDataPack(FCDataPack.A_ROOM_CREATE, true, msgList));
                    return;
                }
                case FCDataPack.R_ROOM_LOOKUP:{
                    List<String> msgList = FCDataPackUtil.getRoomsMessage(objectManager);
                    dataPack.setCommand(FCDataPack.A_ROOM_LOOKUP);
                    dataPack.setSuccessful(true);
                    dataPack.setMessageList(msgList);
                    socket.send(dataPack);
                    return;
                }
                case FCDataPack.R_ROOM_ENTER:{
                    Player player = objectManager.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    Room room = objectManager.getRoom(Integer.valueOf(dataPack.getMessage(1)));
                    if(room == null || player == null){
                        socket.send(new FCDataPack(FCDataPack.A_ROOM_ENTER, false));
                    }
                    else{
                        // if room has reached its limit
                        if(room.getPlayers().size() >= 4){
                            socket.send(new FCDataPack(FCDataPack.A_ROOM_ENTER, false));
                        }
                        else{
                            room.addPlayer(player);

                            // send room player info back
                            socket.send(new FCDataPack(FCDataPack.A_ROOM_ENTER, true, FCDataPackUtil.getRoomPlayerInfoMessage(room)));
                        }
                    }
                    return;
                }
                case FCDataPack.R_ROOM_POSITION_SELECT:{
                    Player player = objectManager.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    Room room = objectManager.getRoom(Integer.valueOf(dataPack.getMessage(1)));

                    int position = Integer.valueOf(dataPack.getMessage(4));

                    boolean isSuccessful = room.playerSelectPosition(player, position);

                    if(isSuccessful)
                        socket.send(new FCDataPack(FCDataPack.A_ROOM_POSITION_SELECT, true, FCDataPackUtil.getPlayerInfoMessage(player)));
                    else
                        socket.send(new FCDataPack(FCDataPack.E_ROOM_POSITION_SELECT, false));

                    return;
                }
                case FCDataPack.R_ROOM_EXIT:{
                    Player player = objectManager.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    Room room = objectManager.getRoom(Integer.valueOf(dataPack.getMessage(1)));

                    // remove the room if host exits
                    if(player.isHost())
                        objectManager.removeRoom(room);
                    else
                        room.removePlayer(player);

                    return;
                }
                case FCDataPack.R_GAME_START:{
                    Player player = objectManager.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    Room room = objectManager.getRoom(Integer.valueOf(dataPack.getMessage(1)));

                    if(player.isHost() && room.containsPlayer(player)){
                        room.startGame();
                    }
                    else{
                        logger.warn("Player " + player.toString() + " attempted to start a game which he/she " +
                                "is not part of or he/she is not host in " + room.toString());
                    }
                    return;
                }
                // the following 2 commands' logic is basically the same(simply forward the datapack)
                case FCDataPack.R_GAME_FINISHED:{
                    Player player = objectManager.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    Room room = objectManager.getRoom(Integer.valueOf(dataPack.getMessage(1)));
                    room.finishGame();

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

                    room.broadcastToOthers(selfPlayer, new FCDataPack(FCDataPack.E_GAME_FINISHED, FCDataPackUtil.getRoomPlayerInfoMessage(room)));
                    return;
                }
                case FCDataPack.R_GAME_PROCEED_DICE:{
                    Player player = objectManager.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    Room room = objectManager.getRoom(Integer.valueOf(dataPack.getMessage(1)));

                    logger.info(player.toString() + " rolled " + dataPack.getMessage(2) + " in " + room.toString());
                    // set the command
                    dataPack.setCommand(FCDataPack.E_GAME_PROCEED_DICE);
                    // update datapack's time
                    dataPack.setDate(new Date());

                    // simply forward the datapack to the users in the same room
                    room.broadcastToOthers(selfPlayer, dataPack);
                    return;
                }
                case FCDataPack.R_GAME_PROCEED_PLANE:{
                    Player player = objectManager.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    Room room = objectManager.getRoom(Integer.valueOf(dataPack.getMessage(1)));

                    logger.info(player.toString() + " selected " + dataPack.getMessage(2) + " plane in " + room.toString());
                    // set the command
                    dataPack.setCommand(FCDataPack.E_GAME_PROCEED_PLANE);
                    // update datapack's time
                    dataPack.setDate(new Date());

                    // simply forward the datapack to the users in the same room
                    room.broadcastToOthers(selfPlayer, dataPack);
                    return;
                }
                case FCDataPack.R_GAME_EXIT:{
                    Player player = objectManager.getPlayer(Integer.valueOf(dataPack.getMessage(0)));
                    Room room = objectManager.getRoom(Integer.valueOf(dataPack.getMessage(1)));

                    // remove those have disconnected
                    for(Player roomPlayer : room.getPlayers()){
                        if(roomPlayer.isInStatus(Player.DISCONNECTED)){
                            objectManager.removePlayer(roomPlayer);
                        }
                    }
                    // generate datapack
                    List<String> msgList = new ArrayList<>();
                    msgList.add(String.valueOf(player.getId()));
                    if(player.isHost()){
                        msgList.add("1");
                        objectManager.removeRoom(room);
                    }
                    else{
                        msgList.add("0");
                    }
                    dataPack.setCommand(FCDataPack.E_GAME_PLAYER_DISCONNECTED);
                    dataPack.setDate(new Date());
                    dataPack.setMessageList(msgList);

                    // broadcast disconnected info
                    room.broadcastToOthers(selfPlayer, dataPack);

                    socket.send(new FCDataPack(FCDataPack.A_GAME_EXIT, true));
                    return;
                }
                default:
                    return;
            }
        } catch(IndexOutOfBoundsException e){
            logger.warn("core.DataPack missing necessary parameters.");
        }
    }

    /**
     * Defines the behavior when the connection has stopped.
     */
    @Override
    public void stopped() {
        objectManager.removePlayer(selfPlayer);
    }

    @Override
    public boolean equals(Object obj){
        if(obj == null)
            return false;

        if(!(obj instanceof FCDataPackProcessor))
            return false;

        FCDataPackProcessor processor = (FCDataPackProcessor) obj;

        if(processor.socket == null)
            return false;

        if(processor.socket.getInetSocketAddress().equals(this.socket.getInetSocketAddress())){
            return true;
        }

        return false;
    }
}
