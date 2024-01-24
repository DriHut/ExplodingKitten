package networking.server;

import logic.utils.Card;
import logic.utils.players.Player;
import networking.protocol.Command;
import networking.protocol.Error;
import networking.protocol.Handler;

import java.io.*;
import java.net.Socket;
import java.util.regex.Pattern;

public class ClientHandler extends Handler {

    private final Socket socket;
    private Player player;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        init(socket);
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = inputReader.readLine()) != null) {
                handleCommand(message);
            }
        } catch (IOException ignored) {}
        shutdown();
    }

    private void init(Socket socket) {
        try {
            inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outputWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            shutdown();
        }
    }

    private void shutdown() {
        try {
            inputReader.close();
            outputWriter.close();
            socket.close();
        } catch (IOException e){
            e.printStackTrace();
        }
        ServerGame.removeClient(this);
    }

    public Player getPlayer() {
        return player;
    }

    /**
     * Handles the "Hello" command received from the client.
     *
     * @param handler The handler object.
     * @param args    The name the client wishes to have.
     */
    public static void handleHello(Handler handler, String... args) {
        if (!(handler instanceof ClientHandler)) return;
        ClientHandler client = (ClientHandler) handler;

        if (client.player != null) {
            client.sendError(Error.E1);
            return;
        }

        String name = args[0];
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9]{1,12}$"); // check length and validity of characters
        if (!pattern.asMatchPredicate().test(name) || !ServerGame.isNameValid(name)) {
            client.sendError(Error.E2);
            return;
        }

        client.player = new Player(name);
        client.sendCommand(Command.WELCOME, name);
        System.out.println("A new player has been created: " + name);
        ServerGame.addPlayer();
    }

    /**
     * Handles the "Exit" command by shutting down the client connection.
     *
     * @param handler The handler object.
     * @param args    None.
     */
    public static void handleExit(Handler handler, String... args) {
        if (!(handler instanceof ClientHandler)) return;
        ClientHandler client = (ClientHandler) handler;

        client.shutdown();
    }

    /**
     * Handles the "Move" command received from the client.
     *
     * @param handler The handler object.
     * @param args    The card to play;
     */
    public static void handleMove(Handler handler, String... args) {
        if (!(handler instanceof ClientHandler)) return;
        ClientHandler client = (ClientHandler) handler;

        if (ServerGame.hasEnded()) {
            client.sendError(Error.E8);
            return;
        }

        Card card = Card.valueOf(args[0]);
        ServerGame.getGame().doMove(client, card);
    }

    /**
     * Handles the "Target" command received from the client.
     *
     * @param handler The handler object.
     * @param args    The target name;
     */
    public static void handleTarget(Handler handler, String... args) {
        if (!(handler instanceof ClientHandler)) return;
        ClientHandler client = (ClientHandler) handler;

        if (ServerGame.hasEnded()) {
            client.sendError(Error.E8);
            return;
        }

        ServerGame.getGame().chooseTarget(client, args[0]);
    }

    /**
     * Handles the "Give" command received from the client.
     *
     * @param handler The handler object.
     * @param args    The card to give.
     */
    public static void handleGive(Handler handler, String... args) {
        if (!(handler instanceof ClientHandler)) return;
        ClientHandler client = (ClientHandler) handler;

        if (ServerGame.hasEnded()) {
            client.sendError(Error.E8);
            return;
        }

        Card card = Card.valueOf(args[0]);
        ServerGame.getGame().giveCard(client, card);
    }

    /**
     *
     * Handles the "Place" command received from the client.
     *
     * @param handler The handler object.
     * @param args    The index to put the card at
     */
    public static void handlePlace(Handler handler, String... args) {
        if (!(handler instanceof ClientHandler)) return;
        ClientHandler client = (ClientHandler) handler;

        System.out.println("place");
        if (ServerGame.hasEnded()) {
            client.sendError(Error.E8);
            return;
        }

        try {
            ServerGame.getGame().place(client, Card.EXPLODING_KITTEN, Integer.parseInt(args[0]));
        } catch (NumberFormatException ignore) {
            client.sendError(Error.E3);
        }
    }

    /**
     * Handles the "Draw" command received from the client.
     *
     * @param handler The handler object.
     * @param args    None.
     */
    public static void handleDraw(Handler handler, String... args) {
        if (!(handler instanceof ClientHandler)) return;
        ClientHandler client = (ClientHandler) handler;

        if (ServerGame.hasEnded()) {
            client.sendError(Error.E8);
            return;
        }

        ServerGame.getGame().drawCard(client);
    }
}
