# Limitations

This document records anything that cannot be implemented as originally
specified, together with the reason and the closest workable alternative. It is
maintained per the project's golden rules (do not fake features — document them).

---

## Build verification (Phase 1) — RESOLVED

**Status: resolved. Phase 1 is build- and runtime-verified.**

The Phase 1 code was originally authored in a sandbox whose network policy
blocked `maven.neoforged.net`, so the NeoForge glue layer
(`core/DindijariClient`, `module/ModuleManager`, `modules/SprintModule`)
shipped compile-unverified while the MC-independent core was verified with
`javac` plus a round-trip harness.

That gap has since been closed in an environment with full network access:

- `gradle build` (Gradle 8.14.3, NeoForge 21.1.193, Minecraft 1.21.1,
  official Mojang mappings) **passes with no changes** to the glue layer.
- `gradle runClient` boots to the title screen; the log shows
  `Module framework initialized with 1 modules` and `Dindijari Client ready`.
- Phase 1 acceptance was exercised end-to-end in the running client (driven
  via Xvfb + xdotool): pressing `J` in-world toggled Sprint, the debounced
  autosave wrote `enabled: true` to
  `config/dindijariclient/profiles/default.json`, the value survived a clean
  quit, and after a full client restart the restored in-memory state was
  confirmed by toggling again.

One caveat only applies to the wrapper: `gradlew` downloads its Gradle
distribution from GitHub Releases, which some egress policies block. A locally
installed Gradle 8.x works identically.

---

## Phase 2 (render library + themed UI) notes

### GUI background blur

No custom post-process shader was needed: Minecraft 1.21.1 ships a Gaussian
blur post-chain (`shaders/post/blur.json`) that vanilla screens already use.
Client screens call the vanilla `renderBlurredBackground` and lay the theme's
charcoal tint over it, so blur strength follows the user's vanilla
"Menu Background Blurriness" accessibility slider — which is also what the
Theme Editor's "Blur Intensity" slider reads and writes. Setting it to 0
disables blur entirely (the darkened overlay remains), which doubles as the
spec's darkened fallback. Verified working under Mesa/llvmpipe software GL.

### Loading/transition screens

- **Replaced** (via `ScreenEvent.Opening` swaps; two private fields exposed
  through a NeoForge access transformer, the supported mechanism):
  world generation progress (`LevelLoadingScreen`), "Loading terrain"
  (`ReceivingLevelScreen`, login case only) and every `GenericMessageScreen`
  interstitial ("Saving world", "Reading world data...", etc.).
- **Nether/End portal transitions** keep the vanilla portal visuals on
  purpose — they are in-world effects, not menus.
- **`ConnectScreen` ("Connecting to server...") cannot be safely replaced**:
  the screen object owns the live connection handshake (vanilla code calls
  back into the exact instance), so swapping it would break joining servers.
  Supported fallback: its backdrop is branded via
  `ScreenEvent.BackgroundRendered` (charcoal fill + wordmark); the vanilla
  status text and Cancel button render on top. Same treatment for
  `ProgressScreen`.
- The **early loading window** (before the title screen, while mods load) is
  drawn by NeoForge's early-display module outside the Screen system; theming
  it is out of scope for a Screen-level mod and it is not replaced.

### Create New World scope

The themed create-world screen exposes name, game mode (Survival / Creative /
Hardcore), difficulty, seed, Generate Structures and Bonus Chest, and creates
worlds through the vanilla `WorldOpenFlows.createFreshLevel` pipeline with the
normal world preset. Vanilla's deeper options (world-type presets like
Superflat, gamerule editor, experiments, data-pack selection) are intentionally
not in the design reference and are not exposed; they may return in a later
phase behind the Advanced section.

### Texture and 4K policy

The client ships **no raster UI textures at all**: every shape is generated
geometry (vertex colours) and every piece of text — including the DINDIJARI
wordmark — is live TTF text, never baked into an image. Body fonts are
rasterised at 6x oversample (60 px glyphs for 10-unit text ⇒ at or above
physical size up to GUI scale 4 / 4K) and the wordmark uses a dedicated
display-size Inter face (30 units at 4x = 120 px rasters) drawn at scale 1.
All layout is computed in float GUI units from the design tokens, so panels
and text stay sharp and aligned at any window size and GUI scale 1–4. With no
shipped textures there is nothing to mipmap or size-check.

### Design-reference deviations (intentional)

- **Vanilla sub-screens**: Edit Server info, Direct Connect and the Mods
  list are still the vanilla screens. The Options screen and its sub-screens
  are themed (see below).
- **World icons**: world cards draw the reference's generic icon square with
  the world's initial, not `icon.png`. Server cards *do* render real favicons
  since the icon bugfix.
- **Singleplayer card actions**: the reference shows only Play / Create / Back,
  so Edit/Delete/Re-Create world management is not reachable from the themed
  screen in this phase.
- **Realms**: the themed menus have no Realms entry (not in the reference);
  disconnecting from a Realm lands on the Multiplayer screen.
- **Font dropdown**: rendered as a cycle chip (click to switch Inter ↔
  JetBrains Mono) rather than a dropdown list; with two bundled fonts a list
  adds nothing. A font change applies to screens (re)opened afterwards —
  already-built labels on the current screen keep their style until re-init.
- **Click GUI panel positions** persist for the session (dragging is kept
  while the game runs) but are not yet written to the config profile; that is
  planned alongside the Phase 3 HUD editor layout persistence.
- **Reference modules**: the mock shows future modules (FPS Display, CPS
  Counter, Zoom, Fullbright, …) that belong to later phases; category panels
  list only modules that actually exist (Sprint, Theme) and honestly label
  empty categories "No modules yet" instead of showing non-functional rows.

### Performance measurement environment

The 60+ FPS acceptance target cannot be measured meaningfully in the authoring
container: rendering runs on Mesa **llvmpipe software rasterisation** (no GPU),
which caps the whole game — vanilla screens included — well below what any real
GPU does. The render library itself introduces no per-frame allocations and
batches through the vanilla GUI render types; the F6 debug screen shows a live
FPS readout for verification on real hardware.


## Performance & QoL modules (honesty notes)

Every module documents exactly what it changes; none promises FPS numbers.

- **Performance Mode** affects only the client's own UI rendering (skips the
  blur pass, drop shadows and RGB line/accent animation). It does not touch
  world rendering — that is what the other Performance modules and presets do.
- **Low Particles / No Entity Shadows / FPS Limiter / FPS Presets** write the
  corresponding *vanilla* options — nothing else. Presets and the FPS Limiter
  apply only on user interaction in the Click GUI; loading the saved config at
  startup never overwrites vanilla settings. If Low Particles / No Entity
  Shadows stay enabled across sessions, vanilla persists the forced value, so
  a later disable restores the value seen when the module was last enabled.
- **No Vignette is not implemented.** In 1.21.1 the vignette has no dedicated
  render layer or event: it draws inside the `CAMERA_OVERLAYS` layer together
  with the pumpkin, powder-snow, spyglass and portal overlays, and only when
  Graphics is not Fast. Cancelling that whole layer would silently remove
  gameplay-critical overlays, which fails this project's honesty rule. (The
  FPS Presets' Fast graphics preset removes the vignette as a side effect.)
- **Screenshot-to-clipboard is not implemented.** GLFW's clipboard is
  text-only; image clipboard requires AWT, which is unsafe to touch on macOS
  under `-XstartOnFirstThread` and unreliable headless. No supported
  cross-platform path exists in 1.21.1.
- **Fullbright** writes the gamma value directly through an access-transformed
  field because `OptionInstance.set` clamps to the 0–1 slider range; this is
  the classic fullbright mechanism, restored on disable.
- **Auto Reconnect** only intercepts the vanilla "Disconnected" screen — a
  user-chosen disconnect never triggers it.
- **Server favicons**: decode/upload/release mirrors the vanilla server list
  (`FaviconTexture`); it could not be exercised against a live server in the
  authoring environment (raw game-port egress is blocked), so it is verified
  by inspection plus the offline placeholder path.


## Themed Options screens (wrapping, not reimplementing)

The Options root and the Video / Music & Sounds / Controls / Mouse / Chat /
Skin Customization / Language / Accessibility screens are themed. Every row
wraps the live vanilla `OptionInstance` — the client only calls `get()`/`set()`,
so validation, callbacks (GUI scale resize, vsync, fullscreen) and persistence
are exactly vanilla; options save on screen close like vanilla. Sliders map
through the option's own `SliderableValueSet` (interface widened by access
transformer), covering int ranges, unit doubles and xmapped wrappers such as
Max Framerate. Video Settings reproduces vanilla's mipmap-change texture
reload on close.

Kept vanilla deliberately:
- **Key Binds** — the capture list with conflict highlighting is deeply
  coupled to `KeyBindsList`; re-skinning it would mean reimplementing capture
  logic. Reachable from the themed Controls screen.
- **Resource Packs** — drag-and-drop between two live pack lists plus folder
  watching; kept vanilla and reachable from the themed root.
- **Online Options** — small vanilla screen with Realms-specific rows.
- The vanilla **GPU warn-list confirmation** for Fabulous graphics is not
  reproduced; the graphics cycler switches modes directly.
- Any option whose value set the wrapper does not recognise embeds its
  vanilla widget so functionality is never lost (none of the mirrored lists
  currently need this fallback except none — verified visually).
- The **Language** list renders names with the vanilla font stack: language
  names span scripts (Arabic, CJK, Cyrillic) that the bundled Inter face does
  not cover.


## Mods screen, tablist and icon set status

- The **Mods screen** is themed (searchable cards, detail pane with version /
  id / authors / license / description, Config button via
  `IConfigScreenFactory`). Mod **logo images** are not loaded — cards show an
  initial-glyph icon; vanilla logo loading reaches into each mod jar's
  resources and is deferred. Mod names/descriptions use the vanilla font
  stack for full script coverage.
- The **custom tablist module** and the **flat icon atlas** are NOT yet
  implemented — deferred to the next round rather than shipped unverified.
  All current screens function without icon sprites (text and generated
  geometry only), so no vanilla sprites leak into themed screens.
