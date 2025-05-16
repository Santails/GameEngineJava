package cz.cvut.fel.pjv.gameengine3000.multiplayer;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerNetworkHandler implements NetworkHandler {

    private final MultiplayerCoordinator coordinator;
    private final int port;
    private ServerSocket serverSocket;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ConcurrentHashMap<Integer, ClientConnectionHandler> clientHandlers = new ConcurrentHashMap<>();
    private final AtomicInteger nextClientId = new AtomicInteger(1); // Start client IDs from 1 (0 is host)
    private Thread acceptThread;
    private final ExecutorService clientExecutor = Executors.newCachedThreadPool(); // Handles client threads

    public ServerNetworkHandler(MultiplayerCoordinator coordinator, int port) {
        this.coordinator = coordinator;
        this.port = port;
    }

    @Override
    public void start() throws IOException {
        try {
            serverSocket = new ServerSocket(port);
            running.set(true);
            acceptThread = new Thread(this, "Server-Accept-Thread");
            acceptThread.start();
            System.out.println("ServerNetworkHandler: Server started on port " + port);
        } catch (IOException e) {
            System.err.println("ServerNetworkHandler: Could not start server on port " + port);
            running.set(false);
            throw e; // Re-throw to signal failure
        }
    }

    @Override
    public void run() { // This is the main server accept loop
        while (running.get() && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("ServerNetworkHandler: Client connected from " + clientSocket.getInetAddress());

                if (clientHandlers.size() >= 1) { // Simple limit: Host + 1 Client
                    System.out.println("ServerNetworkHandler: Max clients reached. Connection rejected.");
                    try { clientSocket.close(); } catch (IOException ignored) {}
                    continue;
                }

                int clientId = nextClientId.getAndIncrement();
                ClientConnectionHandler handler = new ClientConnectionHandler(clientSocket, clientId, this);
                clientHandlers.put(clientId, handler);
                clientExecutor.submit(handler); // Start handling client communication in a new thread

                // Inform the coordinator about the new client
                coordinator.onClientConnected(clientId);

            } catch (SocketException e) {
                if (running.get()) { // Ignore if we are stopping
                    System.err.println("ServerNetworkHandler: SocketException in accept loop: " + e.getMessage());
                }
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("ServerNetworkHandler: IOException in accept loop: " + e.getMessage());
                    // Consider stopping the server on critical accept errors?
                }
            }
        }
        System.out.println("ServerNetworkHandler: Accept loop finished.");
    }

    // Called by MultiplayerCoordinator to send ID to a specific client
    public void sendPlayerIdAssignment(int clientId, int assignedId) {
        ClientConnectionHandler handler = clientHandlers.get(clientId);
        if (handler != null) {
            handler.sendMessage(new AssignPlayerIdMessage(assignedId));
            System.out.println("ServerNetworkHandler: Sent ID " + assignedId + " to client " + clientId);
        } else {
            System.err.println("ServerNetworkHandler: Attempted to send ID to non-existent client " + clientId);
        }
    }

    // Send update to ALL connected clients
    @Override
    public void sendServerUpdate(ServerUpdateMessage msg) {
        if (!running.get()) return;
        // System.out.println("ServerNetworkHandler: Sending ServerUpdate to " + clientHandlers.size() + " clients."); // DEBUG
        for (ClientConnectionHandler handler : clientHandlers.values()) {
            handler.sendMessage(msg);
        }
    }

    @Override
    public void sendClientUpdate(ClientUpdateMessage msg) {
        // Server does not send ClientUpdateMessages
        System.err.println("ServerNetworkHandler: Incorrectly tried to call sendClientUpdate.");
    }


    // Called by ClientConnectionHandler when a client disconnects
    void handleClientDisconnect(int clientId) {
        ClientConnectionHandler removed = clientHandlers.remove(clientId);
        if (removed != null) {
            System.out.println("ServerNetworkHandler: Client " + clientId + " disconnected.");
            coordinator.onClientDisconnected(clientId); // Inform coordinator
        }
    }

    // Called by ClientConnectionHandler when a message is received
    void processClientMessage(int clientId, Object message) {
        if (message instanceof ClientUpdateMessage) {
            // System.out.println("ServerNetworkHandler: Received CUM from " + clientId); // DEBUG
            coordinator.onClientUpdateReceived((ClientUpdateMessage) message);
        } else {
            System.out.println("ServerNetworkHandler: Received unknown message type from client " + clientId + ": " + message.getClass().getSimpleName());
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            System.out.println("ServerNetworkHandler: Stopping server...");
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close(); // Close the server socket to interrupt accept()
                }
            } catch (IOException e) {
                System.err.println("ServerNetworkHandler: Error closing server socket: " + e.getMessage());
            }

            // Shut down client handlers gracefully
            clientExecutor.shutdown(); // Disable new tasks from being submitted
            for (ClientConnectionHandler handler : clientHandlers.values()) {
                handler.stopClient();
            }
            clientHandlers.clear();

            // Interrupt the accept thread if it's stuck
            if (acceptThread != null && acceptThread.isAlive()) {
                acceptThread.interrupt();
            }

            System.out.println("ServerNetworkHandler: Server stopped.");
            // Optionally inform coordinator of shutdown if it wasn't initiated by it
            // coordinator.onNetworkDisconnected();
        }
    }

    @Override
    public boolean isConnected() {
        return running.get(); // Server is "connected" if it's running
    }

    // --- Inner Class for Handling Individual Clients ---
    private static class ClientConnectionHandler implements Runnable {
        private final Socket socket;
        private final int clientId;
        private final ServerNetworkHandler server; // Reference back to parent server
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private final AtomicBoolean clientRunning = new AtomicBoolean(false);

        ClientConnectionHandler(Socket socket, int clientId, ServerNetworkHandler server) {
            this.socket = socket;
            this.clientId = clientId;
            this.server = server;
            this.clientRunning.set(true);
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                System.out.println("ClientHandler " + clientId + ": Streams opened.");

                while (clientRunning.get() && !socket.isClosed()) {
                    try {
                        Object message = in.readObject();
                        if (message != null) {
                            server.processClientMessage(clientId, message);
                        } else {
                            // Stream might be closed orderly
                            break;
                        }
                    } catch (ClassNotFoundException e) {
                        System.err.println("ClientHandler " + clientId + ": Received unknown class type: " + e.getMessage());
                    } catch (EOFException e) {
                        System.out.println("ClientHandler " + clientId + ": Connection closed by client (EOF).");
                        break; // End of stream reached, client likely disconnected
                    } catch (SocketException e) {
                        System.out.println("ClientHandler " + clientId + ": SocketException (client likely disconnected): " + e.getMessage());
                        break;
                    } catch (IOException e) {
                        if(clientRunning.get()) { // Only log if we weren't expecting to stop
                            System.err.println("ClientHandler " + clientId + ": IOException reading message: " + e.getMessage());
                            e.printStackTrace();
                        }
                        break; // Assume connection lost on other IO errors
                    }
                }
            } catch (IOException e) {
                System.err.println("ClientHandler " + clientId + ": Failed to open streams: " + e.getMessage());
            } finally {
                stopClient(); // Ensure cleanup happens
                server.handleClientDisconnect(clientId); // Notify server
            }
            System.out.println("ClientHandler " + clientId + ": Thread finished.");
        }

        // Send a message TO this specific client
        public void sendMessage(Object msg) {
            if (clientRunning.get() && out != null && !socket.isClosed()) {
                try {
                    out.writeObject(msg);
                    out.flush(); // Ensure it's sent immediately
                    // System.out.println("ClientHandler " + clientId + ": Sent " + msg.getClass().getSimpleName()); // DEBUG
                } catch (SocketException e) {
                    System.err.println("ClientHandler " + clientId + ": Failed to send message (Socket closed?): " + e.getMessage());
                    stopClient(); // Connection is likely dead
                }
                catch (IOException e) {
                    System.err.println("ClientHandler " + clientId + ": IOException sending message: " + e.getMessage());
                    // Consider stopping if sending fails persistently
                }
            }
        }

        // Gracefully stop this client handler
        public void stopClient() {
            if (clientRunning.compareAndSet(true, false)) {
                System.out.println("ClientHandler " + clientId + ": Stopping...");
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close(); // Close socket, interrupts blocking reads/writes
                    }
                } catch (IOException e) {
                    System.err.println("ClientHandler " + clientId + ": Error closing socket: " + e.getMessage());
                }
                // Streams are closed implicitly by socket.close() or explicitly in finally block
            }
        }
    }
}