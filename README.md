# shroudserver
Server for the Shroud protocol.

The Shroud protocol isn't implemented on the client-side yet, however, I will release an application to implement it clientside shortly.


The client connects to the server, and sends a username for that client to use.

Clients chat in rooms, and each room has a pre-shared key specified by the client that created the room.

The client starts in a state of belonging to no room. Whenever the client is without a room, the only requests accepted are join, create, and exit.

After a client joins a room, the only accepted requests are get, exit, leave, and send

# requests

join \<room name\> \<validation\>

joins the room specified by room name. The validation should be encrypted using the crypto module(haven't decided what algorithm it will use yet), and when decrypted it should yield "join " followed by the username of the client.

create \<room name\> \<password\>

creates the room specified by room name. The password is sent in plaintext, which poses an attack vector if the room creator's traffic, or the server's traffic, is being sniffed. This will be fixed in an update before official launch.

exit

disconnects the client from the server, as well as any rooms

send \<message\>

sends message to the server

get

tells the server to send the next message the client hasn't seen. the server sends "empty" if none are found

leave

exits the current room, but leaves the user online, able to join another room
