package shroudserver.chat;

import shroudserver.net.ClientHandler;

public class User {
    private Room room; // the room this user is in
    private String username; // the username of the user
    private ClientHandler handler; // the client handler the user is bound to

    /**
     * Creates a user on the server
     * @param username the username of the client
     * @param handler the client handler this user is bound to
     */
    public User(String username, ClientHandler handler) {
        this.username = username;
        this.handler = handler;
        this.room = null;
    }

    /**
     * Sets the room of this client
     * @param newRoom The new room for the client to join
     */
    public void setRoom(Room newRoom) {
        this.room = newRoom;
    }

    /**
     * Kicks the client from their current room
     */
    public void kick() {
        this.room = null;
    }

    /**
     * Gets the name of this user
     * @return The name of this user
     */
    public String getName() {
        return this.username;
    }

    /**
     * Sends a message to this user
     * @param message The message to send
     */
    public void send(String message) {
        this.handler.addToInbox(message);
    }

    /**
     * Sends a message to the user's room as the user
     * @param message The message to send
     */
    public void sendToRoom(String message) {
        message = message.replace("\n", "");
        this.room.send(this, message);
    }

    /**
     * Leaves the room calmly
     */
    public void close() {
        this.room.leave(this);
    }

    /**
     * Returns the room of this user
     * @return The room of this user
     */
    public Room getRoom() {
        return this.room;
    }
}
