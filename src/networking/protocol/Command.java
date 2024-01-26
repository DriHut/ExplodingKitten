package networking.protocol;

import networking.client.ServerHandler;
import networking.server.ClientHandler;

import java.util.function.BiConsumer;

public enum Command {
    /* ----------------------------------------------- CLIENT => SERVER -----------------------------------------------*/
    HELLO("Hello", 1, ClientHandler::handleHello),
    EXIT("Exit", 0, ClientHandler::handleExit),
    MOVE("Move", 1, ClientHandler::handleMove),
    TARGET("Choose target", 1, ClientHandler::handleTarget),
    CARD("Draw card", 0, ClientHandler::handleDraw),
    // NEW send message to server telling which card it wants to give specifying the card sent
    GIVE("Give card", 1, ClientHandler::handleGive),
    // NEW send message to server telling where to put the exploding kitten at
    PLACE("Place kitten", 1, ClientHandler::handlePlace),
    /* ----------------------------------------------- SERVER => CLIENT -----------------------------------------------*/
    WELCOME("Welcome", 1, ServerHandler::handleWelcome),
    HAND("Current hand", 1, ServerHandler::handleHand),
    // NEW send message to targeted player for a favor to choose a card specifying the player to give it to
    DEMAND("Choose card", 1, ServerHandler::handleDemand),
    EXECUTEDMOVE("Executed move", 0, ServerHandler::handleExecuted),
    EXPLODINGKITTEN("Exploding Kitten", 0, ServerHandler::handleExplode),
    GAMEOVER("Game over", 0, ServerHandler::handleGameOver),
    NEXT("Next turn", 2, ServerHandler::handleNext),
    PLAYERS("Players", 3, ServerHandler::handlePlayer),
    NOTIFY("Notify", 1, ServerHandler::handleNotify),
    /* ------------------------------------------------- BIDIRECTIONAL ------------------------------------------------*/
    CHAT("Chat", 2, Handler::handleChat);

    public static final String SEPARATOR = "|";

    private final String value;
    private final int args;
    private final BiConsumer<Handler, String[]> executor;

    Command(String value, int args, BiConsumer<Handler, String[]> executor) {
        this.value = value;
        this.args = args;
        this.executor = executor;
    }

    /**
     * Converts a String value to a Command enum value.
     *
     * @param value The String value to convert.
     * @return The Command enum value corresponding to the given String value.
     * @throws IllegalArgumentException If the given String value does not match any Command enum value.
     */
    public static Command fromString(String value) throws IllegalArgumentException {
        for (Command command : Command.values()) {
            if (value.equals(command.value)) return command;
        }
        throw new IllegalArgumentException("unknown command: " + value);
    }

    /**
     * Retrieves the number of arguments expected for the command.
     *
     * @return The number of arguments expected for the command.
     */
    public int getArgs() {
        return args;
    }

    /**
     * Executes the given command with the provided arguments using the specified handler.
     *
     * @param handler The handler to be used for executing the command.
     * @param args The arguments to be passed to the command.
     */
    public void executeWith(Handler handler, String... args) {
        executor.accept(handler, args);
    }

    @Override
    public String toString() {
        return value;
    }
}