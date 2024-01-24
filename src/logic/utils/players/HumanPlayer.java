package logic.utils.players;

import logic.utils.Card;
import networking.client.ClientGame;
import networking.protocol.Command;
import networking.protocol.Handler;

import java.io.*;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class HumanPlayer extends ClientPlayer {

    private final PipedInputStream readingInput;
    private final PrintWriter readingOutput;
    private final AtomicBoolean needInput;
    private CompletableFuture<?> inputRequest;

    public HumanPlayer(String name) throws IOException {
        super(name);
        PipedOutputStream pipedOutputStream = new PipedOutputStream();
        readingInput = new PipedInputStream(pipedOutputStream);
        readingOutput = new PrintWriter(pipedOutputStream);
        needInput = new AtomicBoolean(false);
    }

    /**
     * Handles the input provided by the user.
     *
     * @param input The input provided by the user.
     */
    public void handleInput(String input, Handler handler) {
        if (needInput.get()) {
            readingOutput.println(input);
            readingOutput.flush();
            return;
        }

        if (input.equalsIgnoreCase("nope") && hand.contains(Card.NOPE)) {
            hand.remove(Card.NOPE);
            handler.sendCommand(Command.MOVE, Card.NOPE.name());
        }
    }

    /**
     * Frees any ongoing input request by canceling it and setting the needInput flag to false.
     * This method does nothing if there is no ongoing input request.
     */
    private void freeRequest() {
        if (inputRequest == null || inputRequest.isDone()) return;
        inputRequest.cancel(true);

        // free the lock created by the scanner
        InputStream in = System.in;
        System.setIn(new ByteArrayInputStream("\n".getBytes()));
        System.setIn(in);

        System.out.println();
        needInput.set(false);
    }

    /**
     * Retrieves the string representation of the player's hand of cards.
     *
     * @return The string representation of the player's hand of cards.
     */
    public String getHandDisplay() {
        StringBuilder message = new StringBuilder("Your hand:");

        for (int i = 0; i < hand.size(); i++) {
            message.append("\n").append(i+1).append(": ").append(hand.get(i));
        }

        return message.toString();
    }

    @Override
    public CompletableFuture<Card> takeTurn() {
        canPlay = true;
        freeRequest();
        System.out.println("Choose a card to play by number/name or \"draw\" a card");
        inputRequest = CompletableFuture.supplyAsync(() -> {
            needInput.set(true);
            Scanner scanner = new Scanner(readingInput);
            Card card = null;
            while (card == null) {
                System.out.print(">> ");
                String input = scanner.nextLine();
                if(input.equalsIgnoreCase("draw")) break;
                card = getCardFromHand(input);
            }
            needInput.set(false);

            return card;
        });
        return (CompletableFuture<Card>) inputRequest;
    }

    @Override
    public CompletableFuture<Card> chooseCard() {
        freeRequest();
        System.out.println("Choose a card to give");
        inputRequest = CompletableFuture.supplyAsync(() -> {
            needInput.set(true);
            Scanner scanner = new Scanner(readingInput);
            Card card = null;
            while (card == null) {
                System.out.print(">> ");
                String input = scanner.nextLine();
                card = getCardFromHand(input);
            }
            needInput.set(false);

            return card;
        });
        return (CompletableFuture<Card>) inputRequest;
    }

    /**
     * Get a card from player hand either namely or by index
     *
     * @param input The input provided by the user, either the name of the card or the index of the card in the hand.
     * @return The card if any matches null otherwise.
     */
    private Card getCardFromHand(String input) {
        // get card namely
        String modifiedInput = input.toUpperCase().replaceAll(" ", "_");
        if (
            Stream.of(Card.values())
                .map(Card::name)
                .anyMatch(s -> s.equals(modifiedInput))
        ) {
            Card card = Card.valueOf(modifiedInput);
            if (!hand.contains(card)) return null;
            return card;
        }

        // get card by index
        try {
            int index = Integer.parseInt(input) - 1;
            if (hand.size() <= index || index < 0) return null;
            return hand.get(index);
        } catch (NumberFormatException ignore) {}

        return null;
    }

    @Override
    public CompletableFuture<String> choosePlayer(List<String> players) {
        freeRequest();
        System.out.println("Please choose a player that needs to make you a favor: " + String.join(", ", players));
        inputRequest = CompletableFuture.supplyAsync(() -> {
            needInput.set(true);
            Scanner scanner = new Scanner(readingInput);
            String player = null;
            while (player == null) {
                System.out.print(">> ");
                String input = scanner.nextLine();
                if (players.stream().noneMatch(p -> p.equalsIgnoreCase(input))) continue;
                player = input;
            }
            needInput.set(false);

            return player;
        });
        return (CompletableFuture<String>) inputRequest;
    }

    @Override
    public CompletableFuture<Integer> choosePosition() {
        freeRequest();
        System.out.println("You draw an exploding kitten but you automatically defused it");
        System.out.println(
            "Choose a number to place the "
                + Card.EXPLODING_KITTEN.name()
                + " back at; (bottom is 0 top is "
                + ClientGame.pileSize
                + ")"
        );
        inputRequest = CompletableFuture.supplyAsync(() -> {
            needInput.set(true);
            Scanner scanner = new Scanner(readingInput);
            int index = -1;
            while (index == -1) {
                System.out.print(">> ");
                String input = scanner.nextLine();

                try {
                    index = Integer.parseInt(input);
                    if (index < 0 || index > ClientGame.pileSize) index = -1;
                } catch (NumberFormatException ignore) {}
            }
            needInput.set(false);

            return index;
        });
        return (CompletableFuture<Integer>) inputRequest;
    }

    @Override
    public void endTurn() {
        freeRequest();
        super.endTurn();
    }

    @Override
    public void confirmMove() {
        freeRequest();
    }

    @Override
    public void stop() {
        System.out.println("The game is over for you at least");
    }

}
