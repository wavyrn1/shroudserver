package shroudserver.chat;

import shroudserver.net.ClientHandler;

public class User {
    private Room room;
    private String username;
    private ClientHandler handler;


    public User(String username, ClientHandler handler) {
        this.username = username;
        this.handler = handler;
        this.room = null;
    }

    public void setRoom(Room newRoom) {
        this.room = newRoom;
    }

    public void kick() {
        this.room = null;
    }

    public String getName() {
        return this.username;
    }

    public void send(String message) {
        this.handler.addToInbox(message);
    }

    public void sendToRoom(String message) {
        message = message.replace("\n", "");
        this.room.send(this, message);
    }

    public void close() {
        this.room.leave(this);
    }
}
