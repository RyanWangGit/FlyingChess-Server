package DataPackTest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import core.datapack.DataPack;

/**
 * Created by Ryan on 16/4/10.
 */
public class DataPackTest {
    public static void main(String[] args){
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create();
        String json = "{\n" +
                "  \"command\": 1,\n" +
                "  \"date\": \"2016-01-01 00:00:00\",\n" +
                "  \"msgList\": [\"11\", \"22\"]\n" +
                "}";
        DataPack dataPack = gson.fromJson(json,DataPack.class);
        System.out.println(gson.toJson(dataPack));
    }
}
