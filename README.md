# [JobSounds](https://github.com/birgersp/df-jobsounds)

A Dwarf Fortress sound utility. [Download here](https://github.com/birgersp/df-jobsounds/releases).

This application reads the current action and position of your dwarves, and plays sounds accordingly. Checkout version 0.3 [demo](https://youtu.be/EpGBG0oPhmQ).

Requires [DFHack](http://www.bay12forums.com/smf/index.php?topic=139553) and Java.

**How to use**
* Extract the contents of the .zip to your Dwarf Fortress installation folder
* Start Dwarf Fortress
* Run the .jar file (on Windows, you can use the .bat script to run it)
* NOTE: If you don't want to copy this utility to your Dwarf Fortress folder, extract it somewhere else and enter the Dwarf Fortress directory when you launch the utility.
For Windows users, the easiest way is to modify the .bat file by adding the directory at the end of the launch command. For example:

```
@echo off
java -jar df-laboursounds.jar "C:\Dwarf Fortress 0.43.03"
pause
```

**Current features**
* Digging sounds
* Woodchopping sounds
* Building sounds

**Project plan**
1. Create a DFHack .lua script which reads the current jobs of the dwarves
2. Make the script send TCP messages to a standalone application, which plays the sounds accordingly
3. Add appropriate sounds

**Credits**
* [People making sounds](https://github.com/birgersp/df-jobsounds-bin)
* The really awesome [DFHack project](https://github.com/DFHack)
