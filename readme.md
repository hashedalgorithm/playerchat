# Player Chat Application

A simple multi-client chat application implemented in Java using sockets.  
The project consists of a **server** that manages multiple client connections and **clients** that can send chat requests, confirm requests, and exchange messages.

https://github.com/user-attachments/assets/423f23bc-a824-4ab4-909a-2ad5eb1a60cf

---
## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Setup](#setup)
- [How Handshake Works](#how-handshake-works)
- [Message Request Flow](#message-request-flow)
- [Messaging Flow](#messaging-flow)
- [Payloads](#payloads)
- [Limitations](#limitations)

---

## Overview

The application allows multiple clients to connect to a central server.  
Clients can:
1. Register with the server via a handshake.
2. Send a chat request to other clients.
3. Exchange messages with connected clients.
4. Limit messages to a configurable maximum per session.

---

## Architecture

- **Server** (`Server.java`):
    - Listens on a specific port for incoming client connections.
    - Creates a `ClientInstance` for each connected client.
    - Maintains a map of connected clients for routing messages and requests.

- **Client** (`Client.java`):
    - Connects to the server via a TCP socket.
    - Performs handshake to register instance ID.
    - Sends/receives chat requests and messages.

- **ClientInstance** (`ClientInstance.java`):
    - Represents a client on the server.
    - Handles incoming messages, forwards requests, and sends confirmations.

- **MessageParser** (`MessageParser.java`):
    - Utility to serialize and parse message payloads between clients and server.
    - 
<img width="1023" height="1141" alt="Playerchat-360T-Architecture drawio" src="https://github.com/user-attachments/assets/6bbec23b-c22b-48fd-bea4-1c2537b59af0" />


---
## Setup

1. **Start the Server**:
   ```bash
   java -cp target/classescom.hashedalgorithm.playerchat.server.App
	```

Server listens on port 12345.

2. **Start Clients**:
```
java -cp target/classes com.hashedalgorithm.playerchat.client.App
```

2. Enter your unique chat name when prompted.

---

## **How Handshake Works**

Handshake ensures each client has a unique ID and is registered with the server.
### **Steps:**

1. Client connects to server.
2. Client sends a handshake request:

```
req:handshake|from:<instanceId>
```

3. Server verifies that the instanceId is unique.
4. Server responds:
    - **Success** or  **Failed** (duplicate or invalid)

```
req:handshake|id:<instanceId>|stat:success/failed
```     

4. Client proceeds only if handshake succeeds.

---

## **Message Request Flow**
Message requests allow a client to initiate a chat with another client.

### **Steps:**

1. Client A sends a message request to Client B:

```
from:<clientA>|to:<clientB>|req:msg
```

2. Server forwards the request to Client B:
    - If Client B exists, Client B can accept:

```
req:msg|from:<clientB>|to:<clientA>|stat:success
```

	- If Client B does not exist, Client A is notified:

```
req:msg|from:<clientA>|to:<clientB>|stat:failed
```

2. Upon receiving SUCCESS, Client A marks the connection as active.

---

## **Messaging Flow**
Once a chat request is accepted, clients can exchange messages.
### **Steps:**

1. Client sends a message:

```
from:<clientA>|to:<clientB>|msg:<message>
```

2. Server receives the message and forwards it to the recipient.
3. Recipient processes the message:

```
from:<clientA>|msg:{<counter>} - <message>
```

4. - counter tracks message sequence number per session.
5. Each client limits the number of messages per session (configurable via MAX_MESSAGES).

---

## **Payloads**

|**Field**|**Description**|**Example**|
|---|---|---|
|req|Type of request|handshake, msg|
|from|Sender’s instance ID|hashed|
|to|Recipient’s instance ID|dee|
|msg|Actual chat message|Hello there!|
|stat|Status of a request/response|success, failed|
|id|Instance ID (used in handshake)|hashed|

---

## **Limitations**

- Maximum clients supported: 10
- Maximum messages per session: Configured using MAX_MESSAGES in Client and ClientInstance.
- No encryption; plain text communication.
- Simple sequential processing; does not scale for large numbers of clients.

---

## **Example Logs**
**Client 1**:

```
Enter chat name: hashed
Connected to server.
Handshake completed.
Send chat request to: dee
Connected with dee!
[hashed]: {1} - Hello
```

**Client 2**:

```
Enter chat name: dee
Connected to server.
Handshake completed.
Received message request from hashed.
[hashed]: {1} - Hello
```

**Server**:

```
Handshake with client - hashed completed.
Handshake with client - dee completed.
Message request from hashed to dee
Message request confirmation from dee to hashed is success
```

---

## **Notes**
- All communication follows a structured payload format for easier parsing.
- Clients handle reconnections, timeouts, and failed requests gracefully.
- The project can be extended to include message persistence, encryption, or GUI integration.

## **References**
- ChatGPT - Contents of this Readme are written using AI.
