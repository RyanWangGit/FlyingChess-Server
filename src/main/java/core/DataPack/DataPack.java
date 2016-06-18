package core.DataPack;

import java.util.Date;
import java.util.List;

/**
 * Basic class data pack object, which should be extended with application-specific commands.
 * @author Ryan Wang
 */
public class DataPack {
    /**
     * Server does nothing upon receiving INVALID datapack.
     */
    public final static int INVALID = 0;

    /**
     * Client should quit upon receiving TERMINATE datapack.
     */
    public final static int TERMINATE = 9000;

    protected int command = 0;
    protected Date date = null;
    protected boolean isSuccessful = false;
    protected List<String> msgList = null;

    public DataPack(int command, boolean isSuccessful, List<String> msgList, Date date){
        this.command = command;
        this.date = date;
        this.isSuccessful = isSuccessful;
        this.msgList = msgList;
    }

    public DataPack(int command, boolean isSuccessful, List<String> msgList){
        this.command = command;
        this.date = new Date();
        this.isSuccessful = isSuccessful;
        this.msgList = msgList;
    }

    public DataPack(int command, List<String> msgList){
        this.command = command;
        this.date = new Date();
        this.msgList = msgList;
        this.isSuccessful = false;
    }

    public DataPack(int command, boolean isSuccessful){
        this.command = command;
        this.date = new Date();
        this.msgList = null;
        this.isSuccessful = isSuccessful;
    }

    public DataPack(int command){
        this.command = command;
        this.date = new Date();
        this.msgList = null;
        this.isSuccessful = false;
    }

    public boolean isValid(){
        return command != INVALID;
    }

    public boolean isSuccessful() { return this.isSuccessful; }

    public void setSuccessful(boolean isSuccessful){ this.isSuccessful = isSuccessful; }

    public List<String> getMessageList(){
        return msgList;
    }

    public void setMessageList(List<String> msgList){
        this.msgList = msgList;
    }

    public Date getDate(){
        return date;
    }

    public void setDate(Date date){
        this.date = date;
    }

    public int getCommand(){
        return command;
    }

    public void setCommand(int command){
        this.command = command;
    }

    public String getMessage(int index){ return this.msgList.get(index); }
}
