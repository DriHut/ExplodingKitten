# How to run:
1) Use Java 11.
2) You can run the [ServerGame](./src/networking/server/ServerGame.java). 
3) You can run the [ClientGame](./src/networking/client/ClientGame.java) specifying '-h' for [HumanPlayer](./src/logic/utils/players/HumanPlayer.java) or '-ai' for [ComputerPlayer](./src/logic/utils/players/ComputerPlayer.java).
4) Then follow the instructions on the client terminals to set the connection.

# Tweaking the game:
- Adding player can be done by changing the constant PLAYER_COUNT in [ServerGame](./src/networking/server/ServerGame.java).
- Changing the port can be done in the same place.
- You can change the hand size, defuse count in the deck and the delay before a NOPE can be played in [Game](./src/logic/Game.java).
- If you want feedback from networking messages, you can add logging in [Handler](./src/networking/protocol/Handler.java). 

# About the structure:
- All server networking handling is in the part [server](./src/networking/server).
- All client networking handling is in the part [client](./src/networking/client).
- All the game logic is in [logic](./src/logic).
- The player is used for both client and server but the [ClientPlayer](./src/logic/utils/players/ClientPlayer.java) is specifically is for the client side. 
- Both [HumanPlayer](./src/logic/utils/players/HumanPlayer.java) and [ComputerPlayer](./src/logic/utils/players/ComputerPlayer.java) are extensions of [ClientPlayer](./src/logic/utils/players/ClientPlayer.java).
- The [protocol](./src/networking/protocol) package contains all the utils for networking.

# About the game:
- The game implements the cards ATTACK, SKIP, FAVOR, SHUFFLE, SEE_THE_FUTURE, NOPE, DEFUSE, and EXPLODING_KITTEN.
- The cat cards don't do anything.
- ATTACK cards can be stacked.
- You can skip any draw actions.
- Cards like FAVOR, SHUFFLE, and SEE_THE_FUTURE can only be noped in a given set of time after being placed. 
- NOPE can be noped becoming a yes, meaning the card will be executed again.
- The player has a choice on where to place the defused kitten in the deck.
- The player can choose who the favor is from. 
- A player can play the DEFUSE card preemptively. 

# About Computer player:
none implemented have fun
