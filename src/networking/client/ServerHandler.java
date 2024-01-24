package networking.client;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import static networking.server.ServerGame.PORT;

import logic.utils.players.ClientPlayer;
import networking.protocol.Command;
import networking.protocol.Error;
import networking.protocol.Handler;

public class ServerHandler extends Handler {

    @Override
    public void run() {
        try {
            InetAddress address = null;
            while (address == null) {
                try {
                    String input = ClientGame.getUserInput("enter a valid ip for the server or \".\" for localhost");
                    if (input.equals(".")) {
                        input = "127.0.0.1";
                        ClientGame.print("127.0.0.1");
                    }
                    address = InetAddress.getByName(input);
                } catch (UnknownHostException e) {
                    System.out.println("unknown host please retry");
                }
            }

            init(address);

            while (ClientGame.getPlayer() == null) {
                sendCommand(Command.HELLO, ClientGame.getUserInput("enter your pseudo"));
                String text;
                while (true) {
                    text = inputReader.readLine();
                    handleCommand(text);
                    if (text.startsWith(Command.WELCOME.toString()) || Error.fromString(text) != null) break;
                }
            }
            ClientGame.doneInit();

            String text;
            while ((text = inputReader.readLine()) != null) {
                try {
                    handleCommand(text);
                } catch (IllegalArgumentException e) {
                    ClientGame.print(e.getMessage());
                    ClientGame.print("message was: " + text);
                    sendError(Error.E7);
                }
            }
        } catch (IOException e) {
            System.out.println("The connection was closed: " + e.getCause());
            System.exit(0);
        }
    }

    /**
     * Initializes a socket connection to the given address.
     *
     * @param address The InetAddress of the server to connect to.
     */
    protected void init(InetAddress address) {
        Socket socket = null;
        while (socket == null) {
            try {
                System.out.println("Attempting to connect to " + address + ":"  + PORT + "...");
                socket = new Socket(address, PORT);
                inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outputWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                System.out.println("Connection established");
            } catch (IOException e) {
                System.out.println("could not create a socket on " + address + ":" + PORT);
            }
        }
    }

    /**
     * Handles the "Welcome" command received from the server.
     * Sets the name of the client game.
     *
     * @param handler The handler for the command.
     * @param args    The authorized player name.
     */
    public static void handleWelcome(Handler handler, String... args) {
        ClientGame.setName(args[0]);
        System.out.println("Welcome: " + args[0]);
    }

    /**
     * Handles a "notify" command received from the server.
     *
     * @param handler The handler for the command.
     * @param args    The message sent
     */
    public static void handleNotify(Handler handler, String... args) {
        ClientGame.print(args[0]);
    }

    /**
     * Handles the next turn in the game. If it's the player's turn, it takes the turn and notifies the player
     * of their options. Otherwise, it prints a message indicating whose turn it is.
     *
     * @param handler The handler for the command.
     * @param args    Firstly the last player, secondly the current player.
     */
    public static void handleNext(Handler handler, String... args) {
        ClientPlayer player = ClientGame.getPlayer();
        ClientGame.displayGame();
        if (player.getName().equals(args[1])) {
            player
                .takeTurn()
                .thenAcceptAsync(card -> {
                    if (card == null) handler.sendCommand(Command.CARD);
                    else handler.sendCommand(Command.MOVE, card.name());
                });
        } else {
            player.endTurn();
            ClientGame.print("It is now " + args[1] + "'s turn");
        }
    }

    /**
     * Handles the "player's" information received from the server.
     *
     * @param handler The handler for the command.
     * @param args    Firstly the cards, secondly the top card, thirdly pile size.
     */
    public static void handlePlayer(Handler handler, String... args) {
        ClientGame.getPlayer().setCards(args[0]);
        ClientGame.setLastCardPlayed(args[1]);
        ClientGame.setPileSize(args[2]);
    }

    /**
     * Handles a "hand" command received from the server.
     *
     * @param handler The handler for the command.
     * @param args    The list of targets.
     */
    public static void handleHand(Handler handler, String... args) {
        ClientGame.getPlayer()
            .choosePlayer(Arrays.asList(args[0].split(", ")))
            .thenAcceptAsync( player -> handler.sendCommand(Command.TARGET, player) );
    }

    /**
     * Handles "demand" command received from the server. It prompts the user to choose a card to give to another player.
     *
     * @param handler The Handler instance for the command.
     * @param args    The name of the player to give the card to.
     */
    public static void handleDemand(Handler handler, String... args) {
        ClientGame.displayGame();
        ClientGame.getPlayer()
            .chooseCard()
            .thenAcceptAsync( card -> handler.sendCommand(Command.GIVE, card.name()) );
    }

    /**
     * Handles the execution of a move. Prints a success message.
     *
     * @param handler The handler for the move.
     * @param args    None.
     */
    public static void handleExecuted(Handler handler, String... args) {
        ClientGame.print("Your move has been executed");
        ClientPlayer player = ClientGame.getPlayer();
        if (player.canPlay()) {
            ClientGame.displayGame();
            player
                .takeTurn()
                .thenAcceptAsync(card -> {
                    if (card == null) handler.sendCommand(Command.CARD);
                    else handler.sendCommand(Command.MOVE, card.name());
                });
        } else player.confirmMove();
    }

    /**
     * Handles the game over event. This method is called when the game is over and the player has lost.
     *
     * @param handler The handler for the command. It is not used in this method.
     * @param args    None.
     */
    public static void handleGameOver(Handler handler, String... args) {
        ClientGame.getPlayer().stop();
    }

    /**
     * Handles an "explode" command received from the server.
     *
     * @param handler The handler for the command.
     * @param args    None.
     */
    public static void handleExplode(Handler handler, String... args) {
        ClientGame.displayGame();
        ClientGame.getPlayer()
            .choosePosition()
            .thenAcceptAsync(position -> {
                System.out.println(position);
                handler.sendCommand(Command.PLACE, position + "");
            });
    }
}