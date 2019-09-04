package shroudserver.net;

import shroudserver.chat.*;

import java.io.IOException;
import java.net.Socket;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.util.ArrayList;

public class ClientHandler extends Thread {
    private Socket sock;
    private DataInputStream dis;
    private DataOutputStream dos;
    private User user;
    private ArrayList<String> inbox;
    private boolean isClosed;
    private boolean inRoom;

    public ClientHandler(Socket sock) throws IOException {
        this.sock = sock;
        this.dis = new DataInputStream(this.sock.getInputStream());
        this.dos = new DataOutputStream(this.sock.getOutputStream());
        this.user = null;
        this.inbox = new ArrayList();
        this.isClosed = false;
        this.inRoom = false;
    }

    public synchronized void addToInbox(String message) {
        this.inbox.add(message);
    }

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

    private void send(String data) {
        data = data.replace("\n", "") + "\n";
        try {
            this.dos.writeChars(data);
        }
        catch(IOException e) {
            this.close();
        }
    }

    private synchronized void createRoom(String roomName, String roomPassword) throws RoomInUseException {
        Room newRoom = new Room(roomName, roomPassword, this.user);
        newRoom.start();
        this.inRoom = true;
    }

    private synchronized void createUser() {
        String username = this.read();
        this.user =  new User(username, this);
    }

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
        while(!this.isClosed) {
            while(!this.inRoom) {
                roomRequestString = this.read();

                try {
                    roomRequest = new Request(roomRequestString);
                }

                catch(InvalidRequestException e) {
                    this.send("invalid request");
                }

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

            switch(currentRequest.getMethod()) {
                case GET:
                case SEND:
                case LEAVE:
                case EXIT:
                    this.executeRequest(roomRequest);
                    break;
                default:
                    this.send("already in a room");
            }
        }
    }

    // methods to execute Requests

    private synchronized void executeRequest(Request request) {
        String roomName;
        switch(request.getMethod()) {
            case GET:
                String message = this.getMessage();
                this.send(message);
                break;
            case EXIT:
                this.user.close();
                this.close();
                break;
            case JOIN:
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
                break;
            case SEND:
                this.user.sendToRoom(request.getArgument());
                break;
            case CREATE:
                roomName = request.getArgument().split(" ")[0];
                String password = request.getArgument().split(" ")[1];
                try {
                    this.createRoom(roomName, password);
                }

                catch(RoomInUseException e) {
                    this.send("room in use");
                    return;
                }
                break;
            case LEAVE:
                this.user.close();
                this.inRoom = false;
        }
    }
}
