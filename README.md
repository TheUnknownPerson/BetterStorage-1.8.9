BetterStorage
=============

.. is a [Minecraft](http://minecraft.net/) mod which adds more storage options. It aims
for fun, interesting and balanced features which would fit well into vanilla Minecraft,
but also work nicely together with other mods. Information about all its features can be
found in the [Minecraft Forums thread](http://www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/wip-mods/2735004-betterstorage).

If you're planning to contribute features to the master repository,
I'd appreciate if you talk to me first. You can reach me on the forums.

Setting up the Development Environment
--------------------------------------
Thanks to the magic of gradle, setting up the development environment is easy.

In the windows or linux/unix command prompt, run ```./gradlew setupDecompWorkspace```,
then run either ```./gradlew idea``` or ```./gradlew eclipse``` depending on what IDE you use.

Localizations
-------------

If you want to contribute localizations, make sure to keep the structure intact. The
```#```'s in the tooltip section are used as new lines. Keep the tooltips short, try to
use the same amount of ```#```'s as the en_US language file or less.

You can test your translation easily by moving it into the mod's .jar file in the
folder ```assets/betterstorage/lang/```. Use this to make sure the tooltips look good
with different window sizes and GUI scales.
