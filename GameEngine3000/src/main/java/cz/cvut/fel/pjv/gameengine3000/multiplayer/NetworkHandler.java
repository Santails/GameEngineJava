package cz.cvut.fel.pjv.gameengine3000.multiplayer;

import java.io.IOException;

/**
 * Common interface for network communication handlers (Server and Client).
 */
public interface NetworkHandler extends Runnable {

    /**
     * Starts the network handler thread (listening for connections or connecting).
     * @throws IOException If the initial setup (binding port, connecting) fails.
     */
    void start() throws IOException;

    /**
     * Stops the network handler, closes connections, and cleans up resources.
     */
    void stop();

    /**
     * Checks if the handler is currently connected (for client) or running (for server).
     * @return true if active, false otherwise.
     */
    boolean isConnected(); // For client, this means connected to server. For server, means it's running.


    // Specific send methods for type safety, implementations will handle routing.

    /**
     * Sends a client state update. Only applicable for ClientNetworkHandler.
     * @param msg The message to send.
     */
    void sendClientUpdate(ClientUpdateMessage msg); // Implemented meaningfully only by Client

    /**
     * Sends a server state update. Only applicable for ServerNetworkHandler.
     * @param msg The message to send.
     */
    void sendServerUpdate(ServerUpdateMessage msg); // Implemented meaningfully only by Server

    // Consider adding sendPlayerIdAssignment if needed directly on handler,
    // but it's better handled via coordinator logic for now.
}