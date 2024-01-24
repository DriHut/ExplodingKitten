package networking.server;

import logic.Game;
import logic.utils.players.Player;
import networking.protocol.Command;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerGame {

    public static final int PORT = 25500;
    private static final int PLAYER_COUNT = 2;
    private static final List<ClientHandler> clientList = Collections.synchronizedList(new ArrayList<>());
    private static Game game;



    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server started at port " + PORT);

            while (true) {
                Thread.sleep(100);
                if (clientList.size() < PLAYER_COUNT) register(serverSocket.accept());
            }
        } catch (IOException | InterruptedException ignored) {}
    }

    /**
     * Registers a new client connection.
     *
     * @param socket The socket representing the client connection.
     */
    public static void register(Socket socket) {
        System.out.println("New client connection");
        ClientHandler clientHandler = new ClientHandler(socket);
        clientList.add(clientHandler);
        new Thread(clientHandler).start();
    }

    /**
     * Start Game if conditions are met
     */
    public static void addPlayer() {
        int playerCount = (int) clientList.stream().filter(clientHandler -> clientHandler.getPlayer() != null).count();
        if (playerCount >= PLAYER_COUNT && game == null) {
            game = new Game(clientList);
            System.out.println("game is starting");
            game.startGame();
        }
    }

    /**
     * Sends a command to all connected clients.
     *
     * @param command The command to be sent.
     * @param args Additional arguments for the command, if any.
     */
    public static void broadcast(ClientHandler ignoreClient, Command command, String... args) {
        clientList.stream()
            .filter(clientHandler -> !clientHandler.equals(ignoreClient))
            .forEach(clientHandler -> clientHandler.sendCommand(command, args));
    }

    /**
     * Checks if a name is valid by verifying if it is unique in the client list.
     *
     * @param name The name to be checked.
     * @return true if the name is valid (i.e., unique), false otherwise.
     */
    public static boolean isNameValid(String name) {
        for (ClientHandler clientHandler: clientList) {
            if (clientHandler.getPlayer() != null && clientHandler.getPlayer().getName().equals(name)) return false;
        }
        return true;
    }

    /**
     * Removes the specified client handler from the client list.
     *
     * @param clientHandler The client handler to be removed.
     */
    public static void removeClient(ClientHandler clientHandler) {
        clientList.remove(clientHandler);
        if (game != null) game.gameOver(clientHandler);
        Player player = clientHandler.getPlayer();
        clientList.forEach((client) ->
            client.sendCommand(Command.NOTIFY, (player == null ? "A client" : player.getName()) + " has left the server and game")
        );
    }

    /**
     * Retrieves the instance of the current game.
     *
     * @return The instance of the current game.
     */
    public static Game getGame() {
        return game;
    }

    /**
     * Determines if the game has started.
     *
     * @return true if the game has started, false otherwise.
     */
    public static boolean hasStarted() {
        return game != null;
    }

    /**
     * Ends the current game by resetting the game instance and printing a message.
     */
    public static void EndGame() {
        game = null;
        clientList.forEach(clientHandler -> clientHandler.getPlayer().reset());
        System.out.println("A game has ended");
    }
}
