# Flux Gaming

**The keyboard as a controller.** In a game, the Unihertz Titan 2 Elite's keys become controller
buttons or the game's own keys, its trackpad two sticks or a mouse, and keys can tap where a game
draws its buttons. The Games app holds every game, from your launchers and emulators, your game
folders and Xbox Cloud Gaming, each with its own art and settings. No root needed: Flux Gaming sends
its input through its built-in shell, paired once with wireless debugging (or another Flux app's).

Part of the Flux apps: [Flux Keyboard](https://github.com/Flux-Sniffer-Mods/Flux-Keyboard),
Flux Gaming, [Flux Streaming](https://github.com/Flux-Sniffer-Mods/Flux-Streaming) and
[Flux Desktop](https://github.com/Flux-Sniffer-Mods/Flux-Desktop). Each is an app of its own;
installed together they work as one: Flux Keyboard says when you're typing in a game, Flux Streaming
streams your games to Moonlight with their art.

## What it does

- **Gaming mode**: a profile per game or app, picked as it comes to the front; controller, keys or touch, as the game takes them.
- **The Games app**: every game in one grid (GameNative, GameHub, the emulators, ES-DE's ROM folders, your own), moved through with the keys, on the phone or a TV.
- **Xbox Cloud Gaming** in its own player, the keys as a controller.
- **Emulators set up for you**: Dolphin, PPSSPP, Azahar, Eden and others given the layout, with dual screens where the game has two.
- **Keys on the game's buttons**: placed on the screen for games that only take touch.
- **Quick Settings tile** for gaming mode, and the games on the home screen.

## Get it

Download the APK from [Releases](https://github.com/Flux-Sniffer-Mods/Flux-Gaming/releases).
Switch on its accessibility service when it asks (Android first asks to allow restricted settings
for apps installed from a file). It updates itself from there.

## Building

```
git clone --recurse-submodules https://github.com/Flux-Sniffer-Mods/Flux-Gaming
./gradlew :app:assembleDebug
```

`core/` is [Flux Core](https://github.com/Flux-Sniffer-Mods/Flux-Core), what the Flux apps share.

## License

GPL-3.0. What changed: [CHANGES.md](CHANGES.md).
