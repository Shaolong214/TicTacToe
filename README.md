# TicTacToe
*Author: Shaolong Xu*

**Usage:**
To get started with my application, please follow these straightforward steps:

**1. Starting the Server**

 - Open your terminal application.
 - Navigate to the directory where the application files (Server.jar and Client.jar) are located.
 - To initiate the server, execute the following command, replacing <ip> with your IP address and <port> with the desired port number:

`java -jar Server.jar <ip> <port>`

**2. Launching the Client**

 - In the same or a new terminal window, navigate to the application's directory.
 - Start the client by running the following command, where you replace <username> with your desired username, <server_ip> with the server's IP address, and <server_port> with the server's port number:

`java -jar Client.jar <username> <server_ip> <server_port>`

To enhance your gaming experience, you can launch multiple clients. Simply repeat the client launch step on additional terminal windows, providing a unique <username> for each.




**Introduction:**

In the modern realm of distributed systems, RMI (Remote Method Invocation) has emerged as a paragon of seamless remote interactions, particularly within Java environments. Capitalizing on the potency of RMI, this report delves into the meticulous design of a distributed Tic-Tac-Toe gaming framework. Designed using Java and harnessed by RMI, the system is tailored to accommodate an array of players concurrently. Beyond the game's core mechanics, the system pioneers an integrated chat facility, allowing players to engage in real-time conversations. This is further complemented by an intuitive graphical user interface (GUI) for the client, while the server, strategically devoid of a GUI, maintains its focus on adeptly orchestrating the gaming sessions.

**Component:**

Diving deeper into the system's architectural anatomy, we discern four pivotal components: Server, Client, Interface, and GUI. The Server's backbone is fortified by the `TicTacToeServer` class, entrusted with client connectivity and game session orchestration. Under its aegis lie the `GameSession` and `Player` classes. The former encapsulates individual gaming encounters between two players, maintaining the dynamic game state and interfacing with two `Player` objects, symbolizing the participants. The latter, the `Player` class, delineates each player, having an intrinsic bond with a `ClientInterface` and always finding itself nestled within a `GameSession`.

The Interface facet is crystallized by the `ServerInterface` and `ClientInterface` classes. They meticulously chalk out the remote methods, with the former focusing on server offerings to clients and the latter reciprocating this paradigm. Their collective role is pivotal in forging a robust communication conduit between the server and client components.

Lastly, the Client aspect is manifested by the `Client` class, an epitome of client-side game interactions. It seamlessly intertwines with the `ClientGUI`, which, as the GUI's embodiment, renders an immersive visual experience for users, making their TicTacToe journey both engaging and intuitive.

**Class design:**

<img width="1000" alt="image" src="https://github.com/Shaolong214/TicTacToe/assets/103941617/2552d301-ead2-4e00-a7ff-768d7a807f54">
 
*Figure 1: Class Overview of the project*

The Distributed Tic Tac Toe system elegantly intertwines several classes to facilitate a robust, interactive gaming experience via a client-server architecture. At its core, the `TicTacToeServer` class acts as the epicentre, orchestrating game logic, player sessions, and overseeing interactions with the clients, while maintaining and managing individual games through `GameSession` class, ensuring the orderly progression and state maintenance of each ongoing game. Players, represented within the system via the `Player` class, store crucial data like username, score, and game-related information and interact through `ClientInterface` to communicate updates and outcomes, thereby contributing to maintaining an individualized and continuous gaming experience for each user.

In the realm of client-side operations, the `Client` class emerges as a pivotal component, offering a bridge for user interactions with the server and managing the graphical user interface events, while navigating through the server's exposed methods via the `ServerInterface` class, thus ensuring a seamless and consistent communicative channel between the user and the server. Moreover, the `ClientGUI` class is entrusted with the vital role of rendering the game state graphically, offering users a visually intuitive platform to interact with the ongoing game and chat functionalities, seamlessly working in tandem with the `Client` class to visually reflect the real-time status and evolution of the game.

The synergy between the `ServerInterface` and `ClientInterface` classes forms the backbone of the client-server communication, ensuring methodical and systematic interactions that adhere to predefined contracts, furnishing a reliable and stable communication framework. Notably, the design capitalizes on a meticulous delineation of responsibilities amongst classes, yielding a system that not only presents a cohesive user experience but also underpins potential scalability and maintainability, pivotal for future adaptations and expansions of the distributed gaming environment. Thus, the architecture successfully intertwines functionality and user experience, encapsulating complex interactions within a user-friendly interface and ensuring an engaging and stable gaming platform for users to enjoy.

<img width="1500" alt="image" src="https://github.com/Shaolong214/TicTacToe/assets/103941617/438d98fe-93eb-4813-a3d7-aa23d96d8f89">

*Figure 2: Sequence Diagram Illustrating Player Interaction and Game Flow*

The sequence diagram above serves as a pivotal tool to elucidate the intricate interactions among various classes, ensuring a coherent understanding of the flow and control within the system. The diagram under discussion meticulously delineates the interactions among seven pivotal classes, namely `ClientInterface`, `Client`, `ClientGUI`, `ServerInterface`, `TicTacToeServer`, `GameSession`, and `Player`, each represented by a distinct vertical line, or lifeline, and interconnected through horizontal arrows symbolizing method calls or message exchanges.

The initiation of the process is spearheaded by the `Client` class, which not only instantiates `ClientGUI` but also establishes a connection to the server through `ServerInterface`, subsequently sending a message to register the player, a task adeptly managed by `TicTacToeServer`. This registration acts as a catalyst for the `TicTacToeServer` to conjure a new `GameSession`, assigning a `Player` instance to it, thereby ensuring the meticulous management of game logic, which encompasses checking win conditions and orchestrating player turns.

As the gameplay unfolds, the `Client`, through the medium of `ClientGUI`, makes a move, a message that permeates through the `ServerInterface`. This move is then seamlessly forwarded to `TicTacToeServer`, which, in turn, updates the `GameSession`. The latter validates the move, refreshes the game state, and communicates back to the `TicTacToeServer`, ensuring both `Client` instances are updated regarding the move.

For chat interactions, the `Client` dispatches a chat message via `ClientGUI`, which traverses through `ServerInterface` and is subsequently forwarded to `TicTacToeServer`. This message is then broadcasted to all `Client` instances tethered to the specific `GameSession`, ensuring the message is displayed on their respective `ClientGUI`.

Upon the realization of a win condition or a declaration of a draw, the `GameSession` notifies the `TicTacToeServer`, which then communicates the outcome to both `Client` instances through the `ServerInterface`. The `Client`, in response, displays the outcome on the `ClientGUI`, potentially prompting the user for subsequent actions, such as initiating a new game or exiting the application.

In instances of `Client` disconnection, the `TicTacToeServer`, notified via the `ServerInterface`, manages the disconnection with grace, potentially informing the other player and ensuring the `GameSession` is terminated and resources are meticulously cleaned up, thereby ensuring a seamless user experience and maintaining the integrity of the game state.

**Analysis:**

The design chosen for the Tic-Tac-Toe game leveraging Java's Remote Method Invocation (RMI) for its implementation. This discourse offers a comprehensive appraisal of the architectural decisions, user interface considerations, and pivotal determinants underpinning the project.

Choice of RMI over Sockets:
Opting for RMI instead of traditional sockets reflects a strategic emphasis on development efficiency. RMI naturally abstracts the intricate layers of network communication, allowing for remote methods in a Java Virtual Machine to be invoked with the semblance of them being local. This abstraction not only accelerates the development pace but also ensures cleaner code. Conversely, with sockets, developers might find themselves enmeshed in the detailed management of data streams, making the development trajectory more intricate and susceptible to errors.

System Architecture:
The system's compartmentalization into distinct Server, Client, and Interface components is a nod to modularity and clean design. Centralized management is entrusted to the `TicTacToeServer` class, which streamlines game sessions and player interactions. Meanwhile, the encapsulated game logic within `GameSession` and participant data in the `Player` classes ensures that each component serves a singular, clear-cut function, paving the way for easier maintenance and potential scalability.

Enhanced Fault Tolerance with `GameSession` and `Player` Classes:
A standout feature is the game's resilience to disruptions. By housing crucial game data within the `GameSession` and `Player` classes, the system demonstrates foresight for potential interruptions. In the event a player gets disconnected mid-game, this architecture ensures no data loss. The system provides a grace period of 30 seconds for reconnection, post which the game state is seamlessly restored, allowing for uninterrupted gameplay. Such a feature is indispensable in today's digital age, where connectivity issues can be sporadic yet impactful.

Remote Interfaces:
The introduction of `ServerInterface` and `ClientInterface` components is pivotal for the RMI architecture. These interfaces crystallize the communication blueprint, ensuring both the server and client march to the beat of a predefined rhythm. However, their presence, while fostering structured communication, also introduces a level of rigidity. Any alterations to these interfaces could mandate cascading changes across the entire system.

Improvement for the Future:
Expandability: Augmenting the design with player profiles, leader boards, and game replays can offer a more enriched user experience.
Security Enhancements: Fortifying the system with data encryption and secure RMI connections can preempt potential security vulnerabilities.

**Conclusion:**

The Distributed Tic-Tac-Toe System, meticulously crafted with Java and RMI, stands out as a testament to the seamless integration of distributed systems and interactive gaming, providing a robust and interactive platform that not only ensures engaging gameplay but also fosters real-time communication among players through an integrated chat facility. The architecture, which is thoughtfully divided into distinct components - Server, Client, Interface, and GUI, demonstrates a commendable application of modularity and clean design principles, ensuring each component serves a singular, clear-cut function, thereby enhancing maintainability and scalability.

The system's resilience to disruptions, particularly through the `GameSession` and `Player` classes, ensures an uninterrupted and consistent gaming experience, even in the face of sporadic connectivity issues. This, coupled with the strategic use of `ServerInterface` and `ClientInterface`, establishes a structured communication blueprint between the server and client, albeit with a level of rigidity that might necessitate cascading changes for any alterations.

While the current implementation provides a stable and engaging gaming platform, future enhancements could potentially explore avenues like player profiles, leader boards, and game replays to enrich user experience, alongside bolstering security through data encryption and secure RMI connections. In essence, the project not only serves as a viable model for distributed gaming systems but also as a foundation upon which further advancements and features can be seamlessly integrated, paving the way for a more enriched and secure gaming environment in future iterations.

**User intrerfaces**

User interfaces of the game:

![3021709648177_ pic](https://github.com/Shaolong214/TicTacToe/assets/103941617/8ce4c840-0029-4fd3-8de8-1ce4f4de61da)
![3031709648177_ pic](https://github.com/Shaolong214/TicTacToe/assets/103941617/adb688cd-bc30-42e0-8363-2db7a050e360)
![3041709648177_ pic](https://github.com/Shaolong214/TicTacToe/assets/103941617/f161f594-b602-4285-b2be-bb9d8783e0aa)
![3061709648177_ pic](https://github.com/Shaolong214/TicTacToe/assets/103941617/d9d88f6b-bb14-4a29-b98c-742841b76d85)
![3071709648177_ pic](https://github.com/Shaolong214/TicTacToe/assets/103941617/822d40c0-9ce9-4ca9-bb7c-6c0747f74082)
![3081709648177_ pic](https://github.com/Shaolong214/TicTacToe/assets/103941617/0c07dcfb-d4df-448d-ae6e-45d6e2e6a7b6)
![3091709648177_ pic](https://github.com/Shaolong214/TicTacToe/assets/103941617/9d78220f-217e-4f1f-97ee-b4aa45b628f9)
![3111709648315_ pic](https://github.com/Shaolong214/TicTacToe/assets/103941617/24b54eed-c009-4c9d-98e9-72c8638a8a79)
![3121709648315_ pic](https://github.com/Shaolong214/TicTacToe/assets/103941617/583c4c61-936d-4706-b233-ad53b9911a3e)
![3131709648361_ pic](https://github.com/Shaolong214/TicTacToe/assets/103941617/1b3f5454-ef70-4141-9d23-31bde61f9178)
![3101709648177_ pic](https://github.com/Shaolong214/TicTacToe/assets/103941617/a2fbd010-17d7-4106-a8cd-f944a46189a3)
