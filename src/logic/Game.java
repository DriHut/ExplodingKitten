package logic;

import logic.utils.Card;
import logic.utils.Deck;
import logic.utils.players.Player;
import networking.protocol.Command;
import networking.protocol.Error;
import networking.server.ClientHandler;
import networking.server.ServerGame;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Game {

    // Constants
    public static final int DEFAULT_HAND_SIZE = 7;
    public static final int DEFUSES_COUNT = 6;
    public static final int NOPE_DELAY = 10;
    public static final ScheduledExecutorService DELAYED_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    // Game data
    protected final List<ClientHandler> clientHandlers;
    protected final Deck deck;

    // Players for turn logic
    protected ClientHandler currentClient;
    private ClientHandler previousClient;

    // Game turns utils
    private final Stack<Card> actionStack;
    private final Stack<Card> skippedStack;
    private Card nopedCard;
    private Card lastCard;
    private ClientHandler favorTarget;
    private ScheduledFuture<?> delayedAction;
    private Thread delayedThread;
    private boolean awaitUserInteraction;
    private boolean canNope;

    public Game(List<ClientHandler> clientHandlers) {
        this.clientHandlers = clientHandlers;
        this.deck = new Deck();
        this.actionStack = new Stack<>();
        this.skippedStack = new Stack<>();
        this.awaitUserInteraction = false;
    }

    /**
     * Sends a command to all connected client handlers.
     *
     * @param command The command to be sent.
     * @param args Additional arguments for the command, if any.
     */
    private void broadcast(Command command, String... args) {
        clientHandlers.forEach((client) -> client.sendCommand(command, args));
    }

    /**
     * Starts the game.
     *
     */
    public void startGame() {
        dealInitialCards();
        deck.insertExplosionsAndDefuses(clientHandlers.size());

        pickStartingPlayer();
        broadcast(Command.NOTIFY, "The game has been started!");
        nextTurn();
        System.out.println("A game has started!");
    }

    /**
     * Deals the initial cards to each client handler's player.
     * Adds cards from the deck to the player's hand, including a DEFUSE card.
     * Sends a command to each client handler to update their player's cards and the number of cards left in the deck.
     */
    private void dealInitialCards() {
        for (ClientHandler clientHandler : clientHandlers) {
            Player player = clientHandler.getPlayer();
            for (int i = 1; i < DEFAULT_HAND_SIZE; i++) {
                Card card = deck.drawCard();
                player.addCard(card);
            }
            player.addCard(Card.DEFUSE);
        }
    }


    /**
     * Randomly select the starting player
     */
    private void pickStartingPlayer() {
        Random random = new Random();
        int startingPlayerIndex = random.nextInt(clientHandlers.size());
        currentClient = clientHandlers.get(startingPlayerIndex);
    }


    /**
     * Handles the move made by a client in the game.
     *
     * @param clientHandler The client handler making the move.
     * @param card          The card played by the client.
     */
    public void doMove(ClientHandler clientHandler, Card card) {
        // Handle unknown card and when the user has to send a specific action or if it doesn't have a card
        if (card == null || awaitUserInteraction || !clientHandler.getPlayer().hasCard(card)) {
            clientHandler.sendError(Error.E3);
            return;
        }

        if (card.equals(Card.NOPE)) { // let player play nope card in any circumstances
            playNope(clientHandler);
            return;
        }

        if (clientHandler != currentClient) { // handle not player turn
            clientHandler.sendError(Error.E6);
            return;
        }

        playCard(clientHandler, card);
    }

    /**
     * Sends updates to all connected client handlers.
     * Updates include sending the player's cards and the number of cards left in the deck.
     */
    private void sendAllUpdates() {
        clientHandlers.forEach(this::sendPlayerUpdate);
    }

    /**
     * Sends a command to a specific client handler to update the player's cards and the number of cards left in the deck.
     *
     * @param clientHandler The client handler to send the command to.
     */
    private void sendPlayerUpdate(ClientHandler clientHandler) {
        clientHandler.sendCommand(
                Command.PLAYERS,
                clientHandler.getPlayer().getCards(),
                lastCard != null ? lastCard.name(): "",
                deck.size() + ""
        );
    }

    /**
     * Updates the game state after playing the "NOPE" card.
     *
     * @param clientHandler The client handler of the player who played the "NOPE" card.
     */
    private void playNope(ClientHandler clientHandler) {
        clientHandler.getPlayer().playCard(Card.NOPE);
        // prevent current player receiving double updates
        if (!clientHandler.equals(currentClient) || !canNope) clientHandler.sendCommand(Command.EXECUTEDMOVE);
        if (!canNope) return;

        if (nopedCard != null) {
            actionStack.push(nopedCard);
            lastCard = nopedCard;
            nopedCard = null;
        } else if (!actionStack.isEmpty() && !actionStack.peek().equals(Card.DEFUSE)) {
            // nope previous person turn
            if (actionStack.peek().equals(Card.DRAW)) {
                actionStack.pop();
                broadcast(Command.NEXT, previousClient.getPlayer().getName(), previousClient.getPlayer().getName());
                currentClient.sendCommand(Command.EXECUTEDMOVE);
                currentClient = previousClient;
                previousClient = null;

                // reverse first attack action
                if (!actionStack.isEmpty() && actionStack.peek().equals(Card.ATTACK) && actionStack.size() == 1) {
                    actionStack.pop();
                    actionStack.push(Card.DRAW);
                    actionStack.push(Card.ATTACK);
                }
            }

            if (!skippedStack.isEmpty() && Card.SKIP.equals(lastCard)) {
                actionStack.push(skippedStack.pop());
                actionStack.push(Card.SKIP);
            }

            nopedCard = actionStack.pop();
            lastCard = Card.NOPE;
        }

        doEffects();
    }

    /**
     * Discards the current "NOPE" card, if any.
     */
    private void discardNoped() {
        if (nopedCard == null) return;
        nopedCard = null;
    }

    /**
     * Cancels the delayed task if it exists.
     *
     * @param doCard Flag indicating whether to execute the effects of the top card.
     */
    private void cancelDelayedTask(boolean doCard) {
        if (delayedAction != null) {
            // wait for termination if its running
            if (delayedAction.getDelay(TimeUnit.MILLISECONDS) <= 0) {
                broadcast(Command.NOTIFY, "Too late...");
                try {
                    if (!Thread.currentThread().equals(delayedThread)) delayedAction.get();
                } catch (InterruptedException | ExecutionException ignore) {}
                delayedAction = null;
                return;
            }

            delayedAction.cancel(false);

            if (doCard) doEffects();
            else broadcast(Command.NOTIFY, "The card has been cancelled");
            delayedAction = null;
        }
    }

    /**
     * Plays a card in the game.
     *
     * @param clientHandler The client handler making the move.
     * @param card The card to be played.
     */
    private void playCard(ClientHandler clientHandler, Card card) {
        if ( // make sure the player doesn't try to play a card when the last card ends their turn
                delayedAction != null
                        && !actionStack.isEmpty()
                        && (actionStack.peek().equals(Card.ATTACK)
                        || (actionStack.isEmpty()
                        && !skippedStack.isEmpty()
                )
                )
        ) clientHandler.sendError(Error.E3);

        cancelDelayedTask(true);

        lastCard = card;
        actionStack.push(card);
        clientHandler.getPlayer().playCard(card);
        canNope = true;
        discardNoped();

        doEffects();
    }

    /**
     * Executes the effects of the top card on the action stack.
     * This method is private and should only be called from within the {@code Game} class.
     */
    private void doEffects() {
        if (actionStack.isEmpty()) return;
        Card topCard = actionStack.peek();
        ClientHandler playingClient = currentClient;

        // creates a delayed task to let users nope it if they want
        if (delayedAction == null && Card.DELAYED_CARD.contains(topCard)) {
            delayedAction = DELAYED_EXECUTOR
                    .schedule(() -> {
                        delayedThread = Thread.currentThread();
                        canNope = false;
                        this.doEffects();
                    }, NOPE_DELAY, TimeUnit.SECONDS);
            broadcast(
                    Command.NOTIFY,
                    playingClient.getPlayer().getName()
                            + " is placing the card "
                            + topCard.name()
                            + " hurry if you want to nope it!"
            );
            return;
        }

        cancelDelayedTask(false); // cancel card delay as it necessarily has been played

        switch (topCard) {
            case SKIP: // skip
                actionStack.pop();
                if (!actionStack.isEmpty()) skippedStack.push(actionStack.pop());

                // process separately from empty stack because it needs confirmation
                if (!actionStack.isEmpty()) {
                    doEffects();
                    return;
                }

                break;
            case DEFUSE: // Defuse exploding kitten or pre-defuse in case an exploding kitten is drawn
                actionStack.pop();
                if (!actionStack.peek().equals(Card.EXPLODING_KITTEN)) {
                    actionStack.push(Card.DEFUSE); // Pre-Defuse
                    break;
                }
                actionStack.pop();
                playingClient.getPlayer().addCard(Card.EXPLODING_KITTEN);
                playingClient.sendCommand(Command.EXPLODINGKITTEN);
                return;
            case SHUFFLE: // shuffle the deck
                deck.shuffle();
                actionStack.pop();
                break;
            case SEE_THE_FUTURE: // send the 3 three cards of the deck to the current player
                actionStack.pop();
                playingClient.sendCommand(
                        Command.NOTIFY,
                        "Here are the 3 top cards on the deck: \\n"
                                + deck.peekTopCards(3)
                                .stream()
                                .map(Card::name)
                                .collect(Collectors.joining(", "))
                );
                break;
            case FAVOR: // make another player give the current player a card
                if (favorTarget != null) break;

                String name = playingClient.getPlayer().getName();
                playingClient.sendCommand( // ask player for a target
                        Command.HAND,
                        clientHandlers
                                .stream()
                                .filter(clientHandler -> !clientHandler.getPlayer().getName().equals(name))
                                .map(clientHandler -> clientHandler.getPlayer().getName())
                                .collect(Collectors.joining(", "))
                );
                awaitUserInteraction = true;
                break;
            case ATTACK: // make the next player draw two time per attack card on the pile/actionStack
                // remove draw card if it's the first attack card in the strike
                if (actionStack.size() == 2 && actionStack.get(0).equals(Card.DRAW)) actionStack.remove(Card.DRAW);
                // only move to the next player if the attack was done by the current player
                if (lastCard.equals(Card.ATTACK)) nextTurn();
                break;
            case EXPLODING_KITTEN: // Kill the player if no defuse
                if (!actionStack.contains(Card.DEFUSE)) {
                    if (currentClient.getPlayer().hasCard(Card.DEFUSE)) {
                        actionStack.push(Card.DEFUSE);
                        playingClient.getPlayer().playCard(Card.DEFUSE);
                        sendPlayerUpdate(playingClient);
                        doEffects();
                        return;
                    } else {
                        playingClient.sendCommand(Command.NOTIFY, "You drew an EXPLODING_KITTEN but you don't have a DEFUSE!");
                        gameOver(playingClient);
                        actionStack.pop();
                    }
                } else {
                    broadcast(Command.NOTIFY, playingClient.getPlayer().getName() + " has preemptively defused a kitten that was just drawn");
                    actionStack.remove(Card.DEFUSE);
                    actionStack.push(Card.DEFUSE);
                    doEffects();
                    return;
                }
                break;
        }

        if (actionStack.isEmpty()) nextTurn();
        else sendAllUpdates();

        if (!List.of(Card.FAVOR, Card.EXPLODING_KITTEN).contains(topCard)) {
            if (previousClient == null) {
                previousClient = currentClient;
                return;
            }

            playingClient.sendCommand(Command.EXECUTEDMOVE); // confirm move
        }
    }

    /**
     * Method for handling the game over event.
     *
     * @param clientHandler The client handler that triggered the game over event.
     */
    public void gameOver(ClientHandler clientHandler) {
        broadcast(Command.NOTIFY, clientHandler.getPlayer().getName() + " has lost!");

        clientHandlers.remove(clientHandler);
        clientHandler.sendCommand(Command.GAMEOVER);
        clientHandler.getPlayer().reset();

        if (clientHandlers.size() == 1) {
            ClientHandler winner = clientHandlers.get(0);
            winner.sendCommand(Command.NOTIFY, "You won the game well done");
            winner.sendCommand(Command.GAMEOVER);
            ServerGame.EndGame();
        }
    }

    /**
     * Allows a client to choose a target player for the current action.
     *
     * @param clientHandler The client handler making the choice.
     * @param target        The target player's name.
     */
    public void chooseTarget(ClientHandler clientHandler, String target) {
        if ( // check for target empty, correct action, and different player
                favorTarget != null
                        || !actionStack.peek().equals(Card.FAVOR)
                        || !clientHandler.equals(currentClient)
                        || target.equals(currentClient.getPlayer().getName())
        ) {
            clientHandler.sendError(Error.E3);
            return;
        }

        // get targeted client if any
        ClientHandler targetedClient = clientHandlers
                .stream()
                .filter(client -> client.getPlayer().getName().equals(target))
                .findFirst()
                .orElse(null);
        if (targetedClient == null) { // no client targeted
            clientHandler.sendError(Error.E4);
            return;
        }

        if (targetedClient.getPlayer().getHand().isEmpty()) { // when the target is out of cards
            clientHandler.sendCommand(Command.NOTIFY, target + " is out of cards");
            clientHandler.sendCommand(Command.EXECUTEDMOVE);
            actionStack.pop();
            awaitUserInteraction = false;
            return;
        }


        clientHandler.sendCommand(Command.NOTIFY, "Waiting for " + target + " to choose a card");
        favorTarget = targetedClient;
        favorTarget.sendCommand(Command.DEMAND, clientHandler.getPlayer().getName()); // send demand to other player
    }

    /**
     * Gives a card from one player's hand to another player's hand.
     *
     * @param clientHandler The client handler initiating the card transfer.
     * @param card The card to be given.
     */
    public void giveCard(ClientHandler clientHandler, Card card) {
        if ( // check for target empty, correct action, and if he has card
                (!actionStack.isEmpty() && !actionStack.peek().equals(Card.FAVOR))
                        || !clientHandler.equals(favorTarget)
                        || !clientHandler.getPlayer().hasCard(card)
        ) {
            clientHandler.sendError(Error.E3);
            return;
        }

        favorTarget.getPlayer().getHand().remove(card);
        currentClient.getPlayer().getHand().add(card);

        broadcast(
                Command.NOTIFY,
                favorTarget.getPlayer().getName()
                        + " has given a card to "
                        + currentClient.getPlayer().getName()
        );

        sendPlayerUpdate(favorTarget);
        sendPlayerUpdate(currentClient);
        clientHandler.sendCommand(Command.EXECUTEDMOVE);
        currentClient.sendCommand(Command.EXECUTEDMOVE);

        actionStack.pop();
        awaitUserInteraction = false;
        favorTarget = null;
    }

    /**
     * Updates the game state to the next turn.
     */
    public void nextTurn() {
        previousClient = currentClient;
        currentClient = clientHandlers.get((clientHandlers.indexOf(currentClient) + 1) % clientHandlers.size());

        actionStack.push(Card.DRAW);
        sendAllUpdates();
        broadcast(Command.NEXT, previousClient.getPlayer().getName(), currentClient.getPlayer().getName());
    }

    /**
     * Draws a card for the specified client handler.
     *
     * @param clientHandler The client handler for whom to draw the card.
     */
    public void drawCard(ClientHandler clientHandler) {
        if (!currentClient.equals(clientHandler)) {
            clientHandler.sendError(Error.E6);
        }
        discardNoped();

        // Draw a card
        Card card = deck.drawCard();
        if (card.equals(Card.EXPLODING_KITTEN)) { // kills the player if no defuse
            actionStack.push(Card.EXPLODING_KITTEN);
            doEffects();
        } else {
            clientHandler.getPlayer().addCard(card);
        }

        // Do that after drawing a card to make sure preemptive defuses work
        actionStack.removeAll(Collections.singleton(Card.DEFUSE)); // remove all preemptive defuses
        if (!actionStack.isEmpty()) actionStack.pop(); // consume action shouldn't have anything else than attacks or draw cards on the top

        sendPlayerUpdate(clientHandler);

        if (card.equals(Card.EXPLODING_KITTEN)) return;
        if (actionStack.empty()) nextTurn();
        clientHandler.sendCommand(Command.EXECUTEDMOVE); // give client feedback
    }

    /**
     * Places a card in the game deck at the specified index.
     *
     * @param clientHandler The client handler making the move.
     * @param card The card to be placed.
     * @param index The index at which to place the card in the deck.
     */
    public void place(ClientHandler clientHandler, Card card, int index) {
        if (!currentClient.equals(clientHandler)
                || !clientHandler.getPlayer().hasCard(card)
                || index < 0
                || index >= deck.size()
        ) {
            clientHandler.sendError(Error.E3);
        }
        clientHandler.getPlayer().playCard(card);

        deck.insertCardAtPosition(card, index);

        sendAllUpdates();
        if (actionStack.isEmpty()) nextTurn();
        clientHandler.sendCommand(Command.EXECUTEDMOVE); // give client feedback
    }
}