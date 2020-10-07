# I Do 4 U
Final project for Post PC course at the Hebrew University in Jerusalem

# Team Members
Ido Porat 307905166 iporat08@gmail.com

Ido Lazer 204635783 idolazer8@gmail.com

# App Version
1.2

# Additional Comments
This application allows you to create custom tasks on your phone. Each task is made of one condition and a few actions - when the condition is met, the device will perform the actions for you. At the moment, the app supports these conditions: Connection to a specific Wi-Fi network, connection to a specific Bluetooth device and entering a geographical area. It also supports the following actions: Changing the phone's volume settings (ring volume and media volume), changing screen brightness and launching a specific app.

Technical details:
The heart of this app is the BroadcastRecieverService class, which is in charge of a foreground service that listens to all relevant broadcasts (bluetooth, Wi-Fi and location) and performs the right actions whenever some condition is met. The tasks themselves are managed inside the TaskManager class which is in charge of handling all information regarding the tasks and their states. We tried to keep the design as modular as possible, in order to allow more conditions and actions in the future. In order to do that, all specific information regarding some condition or some action is saved in a data class, so the TaskManager itself doesn't need to know any of the specifics of any task.

