package cz.cvut.fel.pjv.gameengine3000.multiplayer;

import java.io.Serializable;

public class AssignPlayerIdMessage implements Serializable {
    private static final long serialVersionUID = 1L; // Good practice for Serializable

    public final int assignedPlayerId;

    public AssignPlayerIdMessage(int assignedPlayerId) {
        this.assignedPlayerId = assignedPlayerId;
    }
}