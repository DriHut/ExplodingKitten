package logic.utils.players;

import logic.utils.Card;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ClientPlayer extends Player {
    protected boolean canPlay;

    public ClientPlayer(String name) {
        super(name);
    }

    /**
     * Checks if the player is able to play in the game.
     *
     * @return {@code true} if the player can play, {@code false} otherwise.
     */
    public boolean canPlay() {
        return canPlay;
    }

    /**
     * Sets the player's hand of cards based on the given string representation of cards.
     * The string should contain the names of the cards separated by commas.
     *
     * @param cards A string representation of the cards in the player's hand.
     *              Each card should be represented by its name, separated by commas.
     */
    public void setCards(String cards) {
        String[] rawCards;
        if (cards.isEmpty()) rawCards = new String[0];
        else rawCards = cards.split(",");
        hand = Stream.of(rawCards).map(Card::valueOf).collect(Collectors.toList());
    }

    /**
     * Retrieves the string representation of the player's hand of cards.
     * The hand is displayed as a numbered list of cards.
     * Each card is represented by its index in the hand.
     *
     * @return The string representation of the player's hand of cards.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("'s cards:\n");

        int i = 1;
        for (Card card : hand) {
            sb.append(i).append(". ").append(card).append("\n");
            i++;
        }

        return sb.toString();
    }

    /**
     * Represents the end of a player's turn.
     * This method should be implemented by the player to handle any necessary actions at the end of their turn.
     */
    public void endTurn() {
        canPlay = false;
    }

    /**
     * Represents a player taking their turn.
     *
     * @return A CompletableFuture that will eventually complete with the card chosen by the player.
     */
    public abstract CompletableFuture<Card> takeTurn();

    /**
     * Represents a method for a player to choose a card.
     *
     * @return A CompletableFuture that will eventually complete with the chosen card.
     */
    public abstract CompletableFuture<Card> chooseCard();

    /**
     * Represents a method for a player to choose a target player.
     *
     * @param players A list of player names from which the target player will be chosen.
     * @return A CompletableFuture that will eventually complete with the chosen target player as a String.
     */
    public abstract CompletableFuture<String> choosePlayer(List<String> players);

    /**
     * Player chooses a position to insert an exploding kitten back at
     *
     * @return A CompletableFuture that will eventually complete with the chosen position as an Integer.
     */
    public abstract CompletableFuture<Integer> choosePosition();

    /**
     * Confirms the player's move, indicating that they are unable to play in the game.
     * For example, printing a message or taking a specific action.
     */
    public abstract void confirmMove();

    /**
     * Stops the player from continuing to play in the game.
     */
    public abstract void stop();
}
