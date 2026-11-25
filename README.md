Diabolical Default Gamerules
=======

I was looking for a default gamerule mod that would allow me to set gamerules with one command, and I couldn't find
what I wanted, so I asked ChatGPT (AI) to create this abomination.

What this mod does is self explanetory. You can change gamerule defaults when you make a new world,
or turn on a config value to resync your gamerules in an already existing world!

I created this repo to keep track of all the changes made throughout the life of this mod, whatever that may be.

Install Instructions
=======

If you want to download this mod, follow these steps:

1. Click the green Code button and press Download ZIP
2. Extract the contents of the ZIP file somewhere you can access
3. Open a powershell window inside your extracted folder by holding down Ctrl+Shift while right clicking the window
4. Type out the command ./gradlew build
5. Once the gradle build finishes, close out the window and locate the builds folder
6. Open the builds folder and then open the libs folder
7. Drag the mod out the folder and place where you need it

Extra Info
=======

Commands:

/diabolical_gamerules - Main Command Syntax
 - save_defaults - Saves any gamerule changes you made to the config file
 - sync_gamerules - Applies gamerule changes to the current world you're in
 - test_prompt - Shows a debug pop up asking to sync the world's gamerules to your config defaults. If you set the argument to true, it will show you the real prompt instead.

When using this mod, all you have to do is change your gamerules like normal.
When you're satisfied with what you have set, simply run the save command and every change you made will be written to the config file.

If you join an old or already existing world, you also have the option of using the sync command.
What this will do is read what changes you added to the config, and apply them to the world you've joined.

If you don't want to use a command, you can turn on a config option to show a pop up prompt ingame asking if you wish to
sync the world's gamerules to your config defaults.

Lastly, this mod's license is MIT.

I had this made because I couldn't find anything to my liking,
but now that this exists, people can fork and make this into something better.
