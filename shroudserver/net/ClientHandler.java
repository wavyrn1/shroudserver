package shroudserver.net;

import shroudserver.chat.*;

import java.io.IOException;
import java.net.Socket;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.util.ArrayList;

/**
 * Represents the client handler for each client
 */
public class ClientHandler extends Thread {
    private Socket sock; // the socket to communicate with the client
    private DataInputStream dis; // the input stream to read from the client
    private DataOutputStream dos; // the output stream to write to the client
    private User user; // the user this client is acting as
    private ArrayList<String> inbox; // messages to send the user
    private boolean isClosed; // if this handler is closed
    private boolean inRoom; // if the user is currently in a room

    /**
     * Creates a new Client handler
     * @param sock A socket returned from java.net.ServerSocket.accept()
     * @throws IOException If there's an issue establishing communications
     */
    public ClientHandler(Socket sock) throws IOException {
        this.sock = sock;
        this.dis = new DataInputStream(this.sock.getInputStream());
        this.dos = new DataOutputStream(this.sock.getOutputStream());
        this.user = null;
        this.inbox = new ArrayList();
        this.isClosed = false;
        this.inRoom = false;
    }

    /**
     * Adds a message to this handler's inbox
     * @param message The message to add to the handler
     */
    public synchronized void addToInbox(String message) {
        this.inbox.add(message);
    }

    /**
     * Grabs the oldest message in the inbox and removes it from the inbox
     * @return The oldest message in the inbox
     */
    private synchronized String getMessage() {
        try {
            String message = this.inbox.get(0);
            this.inbox.remove(0);
            return message;
        }
        catch(Exception e) {
            return "empty";
        }
    }

    /**
     * Closes this handler
     */
    private synchronized void close() {
        this.isClosed = true;
        try {
            this.dos.close();
            this.dis.close();
            this.sock.close();
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads a line from the input stream
     * @return A line of text from the client. Closes the handler if this fails
     */
    private String read() {
        String buffer = "";
        char current = ' ';
        try {
            current = (char) this.dis.readByte();
            while(current != '\n') {
                current = (char) this.dis.readByte();
                buffer += current;
            }
        }
        catch(IOException e) {
            this.close();
        }
        return buffer;
    }

    /**
     * Sends a line of data to the client. Closes the handler if this fails
     * @param data Data to send to the client
     */
    private void send(String data) {
        data = data.replace("\n", "") + "\n";
        try {
            this.dos.writeChars(data);
        }
        catch(IOException e) {
            this.close();
        }
    }

    /**
     * Creates a new room and runs its thread.
     * @param roomName The name of the room
     * @param roomPassword The password to encrypt this room with. Currently sent in cleartext, but that will be fixed before launch
     * @throws RoomInUseException If there's already a room with that name.
     */
    private synchronized void createRoom(String roomName, String roomPassword) throws RoomInUseException {
        Room newRoom = new Room(roomName, roomPassword, this.user);
        newRoom.start();
        this.inRoom = true;
    }

    /**
     * Starts the process of listening for the client's username.
     */
    private synchronized void createUser() {
        String username = this.read();
        this.user =  new User(username, this);
    }

    /**
     * Joins a room
     * @param roomName name of the room to join
     * @param validation "join " + this client's username, encrypted with the shroudserver.crypto package
     * @throws InvalidPasswordException If the validation fails
     * @throws NoRoomFoundException If no room with that name is found
     */
    private synchronized void joinRoom(String roomName, String validation) throws InvalidPasswordException, NoRoomFoundException {
        Room roomToJoin = Room.getRoomByName(roomName);
        roomToJoin.addUser(this.user, validation);
        this.inRoom = true;
    }

    @Override
    public void run() {
        this.createUser();
        String currentRequestString;
        Request currentRequest = null;
        String roomRequestString;
        Request roomRequest = null;
        // only accept requests to join or create rooms while not in a room
        // only accept room commands while in a room
        while(!this.isClosed) {
            while(!this.inRoom) {
                roomRequestString = this.read();

                try {
                    roomRequest = new Request(roomRequestString);
                }

                catch(InvalidRequestException e) {
                    this.send("invalid request");
                }
                // whitelist the acceptable requests while not in a room
                switch(roomRequest.getMethod()) {
                    case EXIT:
                    case JOIN:
                    case CREATE:
                        this.executeRequest(roomRequest);
                        break;
                    default:
                        this.send("not in a room");
                }
            }

            currentRequestString = this.read();
            try {
                currentRequest = new Request(currentRequestString);
            }

            catch(InvalidRequestException e) {
                this.send("invalid request");
            }
            // whitelist the acceptable requests while in a room
            switch(currentRequest.getMethod()) {
                case GET:
                case SEND:
                case LEAVE:
                case EXIT:
                case USERS:
                    this.executeRequest(roomRequest);
                    break;
                default:
                    this.send("already in a room");
            }
        }
    }

    /**
     * Executes a request.
     * @param request The request to execute
     */
    private synchronized void executeRequest(Request request) {
        String roomName;
        switch(request.getMethod()) {
            case GET: // "get" -- returns the oldest stored message
                String message = this.getMessage();
                this.send(message);
                break;
            case EXIT: // "exit" -- closes this handler, presumably exiting the program
                this.user.close();
                this.close();
                break;
            case JOIN: // "join <room name> <validation>" joins the room listed, if available
                roomName = request.getArgument().split(" ")[0];
                String validation = request.getArgument().split(" ")[1];
                try {
                    this.joinRoom(roomName, validation);
                }

                catch(NoRoomFoundException e) {
                    this.send("room not found");
                    return;
                }

                catch(InvalidPasswordException e) {
                    this.send("invalid password");
                    return;
                }
                this.inRoom = true;
                break;
            case SEND: // "send <message"> -- sends message to room
                this.user.sendToRoom(request.getArgument());
                break;
            case CREATE: // "create <room name> <password> -- creates a room with name, using password. Sends password through cleartext -- will be fixed by release
                roomName = request.getArgument().split(" ")[0];
                String password = request.getArgument().split(" ")[1];
                try {
                    this.createRoom(roomName, password);
                    this.inRoom = true;
                }

                catch(RoomInUseException e) {
                    this.send("room in use");
                    return;
                }
                break;
            case LEAVE: // "leave" -- leaves the current room
                this.user.close();
                this.inRoom = false;
                break;
            case USERS: // "users" -- sends a list of users in the same channel to the client
                this.send(this.user.getRoom().getUserList());
                break;
        }
    }
}
