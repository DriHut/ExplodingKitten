package logic.utils.players;

import logic.utils.Card;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ComputerPlayer extends ClientPlayer {

    public ComputerPlayer(String name) {
        super(name);
    }

    @Override
    public CompletableFuture<Card> takeTurn() {
        canPlay = true;
        return null; // TODO
    }

    @Override
    public CompletableFuture<Card> chooseCard() {
        return null; // TODO
    }

    @Override
    public CompletableFuture<String> choosePlayer(List<String> players) {
        return null; // TODO
    }

    @Override
    public CompletableFuture<Integer> choosePosition() {
        return null; // TODO
    }

    @Override
    public void confirmMove() {
        // TODO
    }

    @Override
    public void stop() {

    }
}
