# Screensaver Loop

A single-file web page (`index.html`, no build step) that plays videos picked from your phone gallery as a full-screen looping screensaver, and shows whether the phone is online and whether a TV is connected.

## Use it
1. Put the phone and the TV on the same Wi-Fi.
2. Open the page, tap **Choose videos from gallery**, then **Play full screen**.
3. On the phone open quick settings, tap **Cast** or **Smart View** and pick the TV. The TV mirrors the loop.

Options: shuffle, crossfade (off / 1 / 2 / 4 s), moving clock (avoids burn-in), sound, dim. Videos stay on the device (kept in the browser's IndexedDB) and are never uploaded.

## Connection status
- **Wi-Fi / Online / Offline** comes from the browser's network state and updates live.
- **TV connected** is shown only when the browser's Remote Playback (Cast button) reports a real connection. Mirroring from the phone's quick settings cannot be seen by a web page, so in that case the status stays "not connected" even though the TV is showing the video. The page says so under the status.

## Limits
- A web page cannot send a gallery file straight to the TV over the LAN. Use Cast / Smart View mirroring (the phone must stay on), or open this page in the TV's own browser if it has one.
- The **Connect TV / Cast to TV** button depends on the browser (Chrome on Android) and may refuse local files. It then points you to Cast / Smart View.
- Not tested on a real TV yet.

## Host it
Open `index.html` locally, or enable GitHub Pages for this repo (Settings > Pages > Deploy from a branch). The page is then served at `/screensaver-loop/`.
