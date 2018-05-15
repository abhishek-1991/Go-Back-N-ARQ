# Go-Back-N-ARQ
Go Back N ARQ Implementation
Installation at Server:

Copy Files Packet.java and GoBackNServer.java at the same path
Compilation => javac *.java
Execution	=> java GoBackNServer <port_number> <filename> <loss_probability> 

Installation at Client:

Copy Files Packet.java and Client.java at the same path
Compilation => javac Client.java
Execution	=> java Client <Server_Host_Address> <port_number> <file_name> <N> <MSS>
