ModpackUploader
===============

A utility for synchronising Minecraft Forge mods between client and server.

TODO:
-Configuration file for selecting a file server, and storing user preferences

-Improve file indexing, add hashfile data to ensure certain filename is present while
ignoring its checksum

-Improve file indexing, add hashfile data to ignore certain directories when searching
for excess files (VoxelMap waypoints in mods folder for example)

-Improve file indexing, add feature to allow user to prevent certain files from being
deleted (user added client-side mods)

-Improve file indexing, add hashfile data to allow optional files that user can
choose to download. Some of these may be in sets, IE a set of mini-map mods.
