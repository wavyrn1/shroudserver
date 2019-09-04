package shroudserver.chat;

import java.util.ArrayList;
import shroudserver.crypto.*;

/**
 * Represents each chat room
 */
public class Room extends Thread {
    public static ArrayList<Room> usedRooms = new ArrayList(); // represents all open rooms
    private String roomName; // name of the room, used to connect
    private String password; // password used to encrypt messages, ensures each client is authorized to join
    private ArrayList<User> users; // the users in this room
    private ArrayList<String> inbox; // inbox for messages sent to the server
    private boolean isClosed;

    /**
     * Creates a new room.
     * @param roomName The name of the room you wish to create.
     * @param password The password used to encrypt messages in the room.
     * @throws RoomInUseException When the roomName is already in use.
     */
    public Room(String roomName, String password, User founder) throws RoomInUseException {
        // first we ensure that there's not another room with this name
        if(!this.isValidRoomName(roomName)) {
            throw new RoomInUseException();
        }
        // then we initialize our fields
        this.roomName = roomName;
        this.password = password;
        this.users = new ArrayList();
        this.inbox = new ArrayList();
        this.isClosed = false;
        // and add the founder to the room
        try {
            this.addUser(founder, "");
        }
        catch(InvalidPasswordException e) {};
    }

    /**
     * Gets the name of this room.
     * @return The name of this room.
     */
    public String getRoomName() {
        return this.roomName;
    }

    /**
     * Adds the user to this room, and validates their password. Founders do not need validation.
     * @param newUser The new user to add to the room
     * @throws InvalidPasswordException When the password given is invalid
     */
    public synchronized void addUser(User newUser, String message) throws InvalidPasswordException {
        if(this.users.size() == 0) {
            this.users.add(newUser);
            return;
        }

        // we take the verification message the user sends to the server, encrypted with this server's password
        Ciphertext cipher = new Ciphertext(message);
        // then we decrypt the password
        Plaintext plain = cipher.toPlaintext();
        // and get the message in string format
        message = plain.toString();

        // the user should send "join <their name>" encrypted using the shroudserver.crypto package
        String verification = String.format("join %s", newUser.getName());
        // if the verification fails, we throw an exception
        if(!verification.equals(message)) {
            throw new InvalidPasswordException();
        }
        this.users.add(newUser);
        newUser.setRoom(this);
    }

    /**
     * Closes this room.
     */
    public void close() {
        for(User eachUser : this.users) {
            eachUser.kick();
        }
        Room.usedRooms.remove(this);
        this.isClosed = true;
    }

    /**
     * Sends a message to this server.
     * @param sender The user sending the message.
     * @param message The message that is sent.
     */
    public void send(User sender, String message) {
        this.inbox.add(String.format("%s: %s\n", sender.getName(), message));
    }

    /**
     * Sends all the inboxed messages to all the users, then clears the inbox.
     */
    private synchronized void updateUsers() {
        for(User eachUser : this.users) {
            for(String eachMessage : this.inbox) {
                eachUser.send(eachMessage);
            }
        }
        this.inbox.clear();
    }

    /**
     * Starts running the Room's thread.
     */
    @Override
    public void run() {
        while(!this.isClosed) {
            if(this.inbox.size() != 0) {
                this.updateUsers();
            }
            else {
                this.close();
            }
        }
    }

    /**
     * Sent by a user to calmly leave the room.
     * @param user The user to leave the room.
     */
    public synchronized void leave(User user) {
        this.users.remove(user);
        user.kick();
    }

    /**
     * Ensures that the room name is not in use
     * @param roomName The room name to check
     * @return False if the room name is in use.
     */
    private synchronized boolean isValidRoomName(String roomName) {
        for(Room eachRoom : Room.usedRooms) {
            if(eachRoom.getRoomName().equals(roomName)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Takes a room's name, and returns that room.
     * @param roomName The name of the room to search for
     * @return The room using that name.
     * @throws NoRoomFoundException If no room is found with that name.
     */
    public static synchronized Room getRoomByName(String roomName) throws NoRoomFoundException {
        for(Room eachRoom : Room.usedRooms) {
            if(eachRoom.getRoomName().equals(roomName)) {
                return eachRoom;
            }
        }
        throw new NoRoomFoundException();
    }
}
