package logic.utils.players;

import logic.utils.Card;

import java.util.ArrayList;
import java.util.List;

public class Player {
    protected String name;        // Player name
    protected List<Card> hand;    // Player's hand of cards

    public Player(String name) {
        this.name = name;
        this.hand = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    /**
     * Retrieves the cards in the player's hand as a string.
     * Each card is represented by its name, separated by commas.
     *
     * @return A string representation of the cards in the player's hand.
     */
    public String getCards() {
        StringBuilder cards = new StringBuilder();
        hand.forEach((card) -> cards.append(card.name()).append(","));

        return cards.toString();
    }

    /**
     * Retrieves the player's hand of cards.
     *
     * @return The list of cards in the player's hand.
     */
    public List<Card> getHand() {
        return hand;
    }

    /**
     * Adds a card to the player's hand.
     *
     * @param card The card to be added.
     */
    public void addCard(Card card) {
        hand.add(card);
    }


    /**
     * Removes the specified card from the player's hand.
     *
     * @param card The card to be removed from the player's hand.
     */
    public void playCard(Card card) {
        hand.remove(card);
    }

    /**
     * Checks if the specified card is present in the player's hand.
     *
     * @param card The card to check.
     * @return {@code true} if the player has the card in hand, {@code false} otherwise.
     */
    public boolean hasCard(Card card) {
        return hand.contains(card);
    }

    /**
     * Resets the player's hand by clearing all the cards.
     */
    public void reset() {
        hand.clear();
    }
}
