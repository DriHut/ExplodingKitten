package networking.client;

import logic.utils.Card;
import logic.utils.players.*;
import networking.protocol.Command;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class ClientGame {
    private static boolean isHuman = true;
    private static ClientPlayer player;
    private static final CountDownLatch isInit = new CountDownLatch(1);
    private static final Scanner scanner = new Scanner(System.in);

    // Informative data
    public static Card lastCardPlayed;
    public static int pileSize;

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 1) throw new RuntimeException("missing argument for player type.");
        if (args[0].equals("ai")) isHuman = false;

        ServerHandler handler = new ServerHandler();
        new Thread(handler).start();

        isInit.await();
        if (!(player instanceof HumanPlayer)) return;
        HumanPlayer humanPlayer = (HumanPlayer) player;
        String input;
        while ((input = scanner.nextLine()) != null) {
            if (input.startsWith("!"))
                handler.sendCommand(
                    Command.CHAT,
                    player.getName(),
                    input.replaceFirst("!", "")
                );
            humanPlayer.handleInput(input, handler);
        }
    }

    /**
     * Gets user input from the console. ONly useful to initialize
     *
     * @param prompt The prompt message displayed to the user.
     * @return The user input as a String.
     */
    public static String getUserInput(String prompt) {
        System.out.print(prompt + "\n>> ");
        return scanner.next();
    }

    /**
     * This method is used to indicate that initialization of the game has been completed.
     */
    public static void doneInit() {
        isInit.countDown();
    }

    /**
     * Sets the last card played
     *
     * @param rawCard The type of the last card played.
     */
    public static void setLastCardPlayed(String rawCard) {
        if (rawCard.isEmpty()) {
            lastCardPlayed = null;
            return;
        }
        lastCardPlayed = Card.valueOf(rawCard);
    }

    /**
     * Sets the size of the pile.
     *
     * @param num The size of the pile as a String.
     */
    public static void setPileSize(String num) {
        pileSize = Integer.parseInt(num);
    }

    /**
     * Sets the name of the player in the game.
     *
     * @param name the name of the player
     */
    public static void setName(String name) {
        if (isHuman)
            try {
                player = new HumanPlayer(name);
            } catch (IOException ignored) {}
        else player = new ComputerPlayer(name);
    }

    /**
     * Retrieves the ClientPlayer object representing the player in the game.
     *
     * @return The ClientPlayer object representing the player.
     */
    public static ClientPlayer getPlayer() {
        return player;
    }

    /**
     * Prints a message to the console, if the player is not an instance of ComputerPlayer.
     *
     * @param message The message to be printed.
     */
    public static void print(String message) {
        if (player instanceof ComputerPlayer) return;
        System.out.println(message);
    }


    /**
     * Displays the current state of the game.
     * Prints a message to the console with information such as the size of the pile and the last card played.
     * Only prints the message if the player is not an instance of ComputerPlayer.
     */
    public static void displayGame() {
        if (player instanceof ComputerPlayer) return;
        print(((HumanPlayer) player).getHandDisplay());
        print("The top card: " + (lastCardPlayed != null ? lastCardPlayed.name():"None") + " Pile size: " + pileSize);
    }
}
