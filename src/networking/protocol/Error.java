package networking.protocol;

public enum Error {
    E1("Invalid handshake"),
    E2("Invalid name"),
    E3("Invalid move"),
    E4("Invalid target"),
    E5("Not enough cards"),
    E6("Not your turn"),
    E7("Non-protocol messages"),
    E8("Missing information"),
    E9("Unknown error");

    private final String value;

    Error (String value) {
        this.value = value;
    }

    /**
     * Converts a String value to an Error enum value.
     *
     * @param value The String value to convert.
     * @return The Error enum value corresponding to the given String value, or null if it doesn't match any Error enum value.
     */
    public static Error fromString(String value) {
        for (Error error : Error.values()) {
            if (value.equals(error.value)) return error;
        }
        return null;
    }

    @Override
    public String toString() {
        return value;
    }
}