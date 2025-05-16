package cz.cvut.fel.pjv.gameengine3000.multiplayer;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientNetworkHandler implements NetworkHandler {

    private final MultiplayerCoordinator coordinator;
    private final String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private Thread receiveThread;

    public ClientNetworkHandler(MultiplayerCoordinator coordinator, String host, int port) {
        this.coordinator = coordinator;
        this.host = host;
        this.port = port;
    }

    @Override
    public void start() throws IOException {
        try {
            System.out.println("ClientNetworkHandler: Attempting to connect to " + host + ":" + port);
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            connected.set(true);

            receiveThread = new Thread(this, "Client-Receive-Thread");
            receiveThread.start();
            System.out.println("ClientNetworkHandler: Connected successfully.");

        } catch (IOException e) {
            System.err.println("ClientNetworkHandler: Failed to connect or open streams: " + e.getMessage());
            connected.set(false);
            cleanupResources(); // Clean up any partially opened resources
            throw e; // Propagate exception to inform caller
        }
    }

    @Override
    public void run() { // This is the client's receive loop
        while (connected.get() && !socket.isClosed()) {
            try {
                Object message = in.readObject();
                if (message != null) {
                    processServerMessage(message);
                } else {
                    // Stream might be closed orderly by server
                    System.out.println("ClientNetworkHandler: Server closed connection (read null).");
                    break;
                }
            } catch (ClassNotFoundException e) {
                System.err.println("ClientNetworkHandler: Received unknown class type: " + e.getMessage());
                // Potentially ignore or log, but don't necessarily disconnect
            } catch (EOFException | SocketException e) {
                // EOFException: Server closed connection abruptly or stream ended.
                // SocketException: Connection reset, broken pipe etc.
                if (connected.get()) { // Only log if we weren't expecting it
                    System.out.println("ClientNetworkHandler: Connection closed by server or network error: " + e.getMessage());
                }
                break; // Exit loop on these critical errors
            } catch (IOException e) {
                if (connected.get()) {
                    System.err.println("ClientNetworkHandler: IOException reading message: " + e.getMessage());
                    e.printStackTrace();
                }
                break; // Assume connection lost on other IO errors
            }
        }

        // If loop exits, handle disconnection
        if (connected.get()) { // Check if we broke out unexpectedly
            handleDisconnection();
        }
        System.out.println("ClientNetworkHandler: Receive loop finished.");
    }

    private void processServerMessage(Object message) {
        if (message instanceof AssignPlayerIdMessage) {
            // System.out.println("ClientNetworkHandler: Received AssignPlayerIdMessage"); // DEBUG
            coordinator.onAssignPlayerIdReceived((AssignPlayerIdMessage) message);
        } else if (message instanceof ServerUpdateMessage) {
            // System.out.println("ClientNetworkHandler: Received ServerUpdateMessage"); // DEBUG
            coordinator.onServerUpdateReceived((ServerUpdateMessage) message);
        } else {
            System.out.println("ClientNetworkHandler: Received unknown message type from server: " + message.getClass().getSimpleName());
        }
    }

    @Override
    public void sendClientUpdate(ClientUpdateMessage msg) {
        if (!connected.get() || out == null || socket.isClosed()) {
            System.err.println("ClientNetworkHandler: Cannot send update, not connected.");
            return;
        }
        try {
            out.writeObject(msg);
            out.flush(); // Ensure data is sent
            // System.out.println("ClientNetworkHandler: Sent ClientUpdateMessage"); // DEBUG
        } catch (SocketException e) {
            System.err.println("ClientNetworkHandler: Failed to send ClientUpdate (Socket closed?): " + e.getMessage());
            handleDisconnection(); // Connection is likely dead
        } catch (IOException e) {
            System.err.println("ClientNetworkHandler: IOException sending ClientUpdate: " + e.getMessage());
            // Potentially handle disconnection if sending fails
        }
    }

    @Override
    public void sendServerUpdate(ServerUpdateMessage msg) {
        // Client does not send ServerUpdateMessages
        System.err.println("ClientNetworkHandler: Incorrectly tried to call sendServerUpdate.");
    }


    private void handleDisconnection() {
        // Use compareAndSet to ensure this logic runs only once
        if (connected.compareAndSet(true, false)) {
            System.out.println("ClientNetworkHandler: Handling disconnection...");
            cleanupResources();
            coordinator.onNetworkDisconnected(); // Notify the coordinator
        }
    }

    private void cleanupResources() {
        System.out.println("ClientNetworkHandler: Cleaning up network resources...");
        try {
            if (in != null) in.close();
        } catch (IOException e) { /* ignore */ }
        try {
            if (out != null) out.close();
        } catch (IOException e) { /* ignore */ }
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) { /* ignore */ }
        in = null;
        out = null;
        socket = null;
        // Interrupt the receive thread if it's still alive (e.g., stuck)
        if (receiveThread != null && receiveThread.isAlive()) {
            receiveThread.interrupt();
        }
    }

    @Override
    public void stop() {
        System.out.println("ClientNetworkHandler: Stop requested.");
        // Set connected to false first to signal loops to stop
        connected.set(false);
        cleanupResources(); // Close socket etc., which should interrupt blocking IO
        // Coordinator notification happens either in handleDisconnection or called externally
    }

    @Override
    public boolean isConnected() {
        return connected.get() && socket != null && !socket.isClosed();
    }
}
