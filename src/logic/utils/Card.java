package logic.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum Card {
    EXPLODING_KITTEN,
    DEFUSE,
    ATTACK,
    FAVOR,
    NOPE,
    SHUFFLE,
    SKIP,
    SEE_THE_FUTURE,
    BEARD_CAT,
    TACOCAT,
    RAINBOW_RALPHING_CAT,
    HAIRY_POTATO_CAT,
    CATTERMELON,
    DRAW; // Util card to add to the stacks of actions to do

    /**
     * Cards with immediate actions that can be noped
     */
    public static final List<Card> DELAYED_CARD = List.of(FAVOR, SHUFFLE, SEE_THE_FUTURE);
}