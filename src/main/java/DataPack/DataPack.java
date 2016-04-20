package DataPack;

import java.util.Date;
import java.util.List;

/**
 * Created by Ryan on 16/4/8.
 */
public class DataPack {
    // Common command
    public final static int INVALID = 0;
    public final static int LOGIN = 1000;
    public final static int LOGOUT = 1002;
    public final static int REGISTER = 1003;
    public final static int TERMINATE = 2000;

    private int command = 0;
    private Date date = null;
    private boolean isSuccessful = false;
    private List<String> msgList = null;

    public DataPack(int command, Date date, boolean isSuccessful, List<String> msgList){
        this.command = command;
        this.date = date;
        this.msgList = msgList;
    }

    public boolean isValid(){
        return command != INVALID;
    }

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
