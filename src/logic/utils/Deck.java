package logic.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import static logic.Game.DEFUSES_COUNT;

public class Deck extends Stack<Card> {

    public Deck() {
        initialize();
        shuffle();
    }

    private void initialize() {
        addCards(Card.ATTACK, 4);
        addCards(Card.FAVOR, 4);
        addCards(Card.NOPE, 5);
        addCards(Card.SHUFFLE, 4);
        addCards(Card.SKIP, 4);
        addCards(Card.SEE_THE_FUTURE, 5);
        addCards(Card.BEARD_CAT, 4);
        addCards(Card.CATTERMELON, 4);
        addCards(Card.HAIRY_POTATO_CAT, 4);
        addCards(Card.RAINBOW_RALPHING_CAT, 4);
        addCards(Card.TACOCAT, 4);
    }
    public void insertExplosionsAndDefuses(int amountOfPlayers){
        addCards(Card.DEFUSE, DEFUSES_COUNT - amountOfPlayers);
        addCards(Card.EXPLODING_KITTEN, amountOfPlayers - 1);
        shuffle();
    }

    private void addCards(Card card, int quantity) {
        for (int i = 0; i < quantity; i++) {
            this.add(card);
        }
    }
    public void shuffle() {
        Collections.shuffle(this);
    }

    public Card drawCard() {
        return pop();
    }

    public List<Card> drawCards(int numCards) {
        List<Card> drawnCards = new ArrayList<>();
        for (int i = 0; i < Math.min(numCards, size()); i++) {
            drawnCards.add(drawCard());
        }
        return drawnCards;
    }


    public List<Card> peekTopCards(int numCards) {
        List<Card> topCards = new ArrayList<>();

        for (int i = 1; i <= Math.min(numCards, size()); i++) {
            topCards.add(this.get(size()-i));
        }
        return topCards;
    }

    public void insertCardAtPosition(Card card, int position) {
        add(position, card);
    }
}