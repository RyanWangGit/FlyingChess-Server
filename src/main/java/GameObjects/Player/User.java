package GameObjects.Player;

/**
 * Created by Ryan on 16/4/27.
 */
public class User {
    protected int id = -1;
    protected String userName = null;
    protected String passwordMD5 = null;
    protected int points = -1;

    public User(int id, String userName, String passwordMD5, int points){
        this.id = id;
        this.userName = userName;
        this.passwordMD5 = passwordMD5;
        this.points = points;
    }

    public User(User user){
        this.id = user.id;
        this.userName = user.userName;
        this.passwordMD5 = user.passwordMD5;
        this.points = user.points;
    }

    public int getId() { return this.id; }

    public String getName() { return this.userName; }

    public String getPasswordMD5() { return this.passwordMD5; }

    public int getPoints() { return this.points; }

    public void setPoints(int points) { this.points = points; }
}
