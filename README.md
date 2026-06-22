# LD-ToyDroid-Cemu

LD-ToyDroid-Cemu is an Android companion application for [Cemu](https://github.com/cemu-project/Cemu) that allows you to wirelessly control the emulated LEGO Dimensions Toy Pad using your smartphone or tablet over your local network.

## Features

*   **Wireless Control**: Place, remove, and move figures on the virtual Toy Pad directly from your Android device.
*   **Live Pad State**: See a real-time representation of the 7 slots on the Toy Pad and what figures are currently on them.
*   **Complete Catalogue**: Includes a searchable built-in database of all 83 LEGO Dimensions minifigures and hundreds of vehicle tokens. No need to memorize ID numbers.
*   **Physical Layout Mapping**: The UI explicitly names the slots according to their physical position on the pad (e.g., "Center Main Hero", "Left Adventure", "Right Ability").

## Prerequisites

This app requires a custom build of the Cemu emulator that includes the **Dimensions HTTP Server**. 
The server runs on an embedded background thread (port `8031`) and exposes the internal `DimensionsUSB` capabilities to your local network.

### Setting up Cemu
1. Ensure your Cemu build includes the HTTP server modifications (using `cpp-httplib`).
2. Open Cemu.
3. Navigate to **Tools → Emulated USB Devices**.
4. In the Dimensions tab, ensure **Emulate Dimensions Toypad** is checked.
5. Launch LEGO Dimensions.

## Building the App

This is a standard Android Studio / Gradle project.

1. Open the `LD-ToyDroid-Cemu` folder in **Android Studio**.
2. Let Gradle sync the dependencies (OkHttp, Gson, Material Components).
3. Build and run the app on an emulator or a physical Android device (API 26+).

## Usage

1. Find the local IP address of the PC running Cemu (e.g., `192.168.1.50`). Both your PC and your Android device must be on the same local network.
2. Open **LD-ToyDroid-Cemu** on your Android device.
3. Enter your PC's IP address in the top text field and tap **Connect**.
4. The screen will populate with the 7 virtual Toy Pad slots.
5. **To place a figure**: Tap "Place Figure" on an empty slot, search for your desired character or vehicle, and select it. It will instantly appear in the game.
6. **To remove a figure**: Tap the red "Remove" button next to an occupied slot.

## Architecture & API

The app uses `OkHttp3` and Kotlin Coroutines to communicate with the Cemu host.

The Cemu embedded server listens on `0.0.0.0:8031` and exposes the following REST endpoints:
*   `GET /list` - Returns a JSON array of all 7 slots and their occupancy.
*   `POST /place` - Expects `{"slot": [0-6], "figureId": [uint]}`. Creates the NFC `.bin` file and loads it into the emulator.
*   `POST /remove` - Expects `{"slot": [0-6]}`. Lifts the figure off the pad.
*   `POST /move` - Expects `{"fromSlot": [0-6], "toSlot": [0-6]}`. Lifts and relocates a figure to a new pad section.
