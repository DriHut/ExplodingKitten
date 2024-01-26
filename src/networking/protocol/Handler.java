package networking.protocol;

import networking.client.ClientGame;
import networking.client.ServerHandler;
import networking.server.ServerGame;
import networking.server.ClientHandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import static networking.protocol.Command.SEPARATOR;

public abstract class Handler implements Runnable {

    protected BufferedReader inputReader;
    protected BufferedWriter outputWriter;

    /**
     * Sends a command to the server.
     *
     * @param command The command to be sent.
     * @param args Additional arguments for the command, if any.
     */
    public void sendCommand(Command command, String... args) {
        send(command + SEPARATOR + String.join(SEPARATOR, args));
    }

    /**
     * Sends an error message to the client.
     *
     * @param error The error to be sent.
     */
    public void sendError(Error error) {
        send(error.toString());
    }

    /**
     * Sends a message to the server.
     *
     * @param message The message to be sent.
     */
    protected void send(String message) {
        // Uncomment for debugging sent messages
        // if (this instanceof ClientHandler) System.out.print(((ClientHandler) this).getPlayer().getName() + "-> ");
        // System.out.println(message);
        try {
            outputWriter.write(message);
            outputWriter.newLine();
            outputWriter.flush();
        } catch (Exception ignored) {}
    }

    /**
     * Handles a command received as a message.
     *
     * @param message The message containing the command.
     * @throws IllegalArgumentException If the command does not match any supported command or if the number of arguments is incorrect.
     */
    protected void handleCommand(String message) throws IllegalArgumentException {
        String[] parts = message.split("\\" + SEPARATOR, 2);
        Error error = Error.fromString(parts[0]);
        if (error != null) {
            System.out.println(error.name() + ": " + error);
            return;
        }

        Command command = Command.fromString(parts[0]);

        String[] args = parts.length == 2 ? parts[1].replaceAll("\\\\n", "\n").split("\\" + SEPARATOR) : new String[0];

        if (args.length == 1 && args[0].isEmpty()) args = new String[0];
        if (command.getArgs() != args.length)
            throw new IllegalArgumentException(command + " expects " + command.getArgs() + " arguments and got: " + args.length);

        command.executeWith(this, args);
    }

    /**
     * Handles chat messages sent by clients or servers.
     * If this handler is an instance of ServerHandler, it prints the chat message to the ClientGame.
     * If this handler is an instance of ClientHandler, it broadcasts the chat message to all connected clients in the ServerGame.
     *
     * @param args Firstly the player name, Secondly the message;
     */
    public void handleChat(String... args) {
        if (this instanceof ServerHandler) ClientGame.print(args[0] + "> " + args[1]);
        else if (this instanceof ClientHandler) ServerGame.broadcast((ClientHandler) this, Command.CHAT, args);
    }
}