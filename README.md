# Ultras_settings v1

A premium Settings GUI front-end by **UC_Hussein**. It owns **no TPA logic at all** — every TPA-related
button in these GUIs simply runs one of **Ultras_TPA**'s own commands. If you don't have Ultras_TPA
installed, Ultras_settings still loads and its GUIs still open; TPA buttons just show a
"not installed" message instead of doing anything.

## Compatibility

- **Built for:** modern Paper (Adventure/MiniMessage), Java 25 toolchain, `paper-api` pinned in
  `build.gradle.kts` at `1.21.4-R0.1-SNAPSHOT`.
- **The spec asked for Paper 1.21 → 26.2.** Being straightforward about this: there is no Paper/Minecraft
  build called "26.2" as of this writing, and nothing unreleased can honestly be tested against. What
  was actually done: zero NMS, zero deprecated-for-removal API, only public Paper/Bukkit + Adventure —
  so bumping the single `paper-api` version line and rebuilding should keep working as new Paper
  releases ship, but that's an engineering expectation, not something that was tested against a version
  that doesn't exist yet. Verify against your real server version yourself.
- One flagged exception: `ItemMeta#setEnchantmentGlintOverride` (used for the `glow: true` button
  option) exists since Paper 1.20.5. If you're genuinely running something predating that, glowing
  buttons would need a different implementation — everything else in this plugin has no such floor.

## ⚠️ Building the jar

This project was written in a sandboxed environment **with no internet access** — `gradle build`
could not actually be run here (no way to download the Paper API / shadow plugin), and no Gradle
wrapper is included for the same reason. You need to run the real build yourself:

- **Easiest:** push this folder to a GitHub repo — `.github/workflows/build.yml` builds on every push
  (via `gradle/actions/setup-gradle`, which provisions Gradle itself) and uploads
  `build/libs/Ultras_settings-v1.jar` as a workflow artifact.
- **Locally:** install Gradle + JDK 25, run `gradle build` in this folder. Output:
  `build/libs/Ultras_settings-v1.jar` (exactly the artifact name the spec asked for).
- If the Paper dependency fails to resolve, check
  https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/ for the version string
  available for your target Minecraft version and update `build.gradle.kts`.

## Install

1. Drop the built jar into `plugins/` (alongside your existing Ultras_TPA jar — order doesn't matter,
   Ultras_settings is a `softdepend`, not a hard dependency).
2. Start the server once to generate `config.yml`, `lang/en.yml`, `lang/ar.yml`, and `gui/*.yml`.
3. Edit `config.yml` (Arabic comments throughout) if needed, then `/setting_admin reload`.

## Commands

| Command | Description |
|---|---|
| `/setting`, `/settings` | Opens your own Settings GUI |
| `/setting rest` | Resets **your own** settings to the configured defaults |
| `/setting_admin reload` | Reloads config, both language files, and every GUI file — no restart |
| `/setting_admin open <gui>` | Opens any discovered `gui/*.yml` file for yourself, by its file name (no `.yml`) |
| `/setting_admin <player>` | Opens **that player's** Settings GUI (their real state, not yours) |
| `/setting_admin rest <player>` | Resets that player's settings to defaults (requires them online — see below) |

## Permissions

| Permission | Default | Covers |
|---|---|---|
| `ultras.settings.use` | true | `/setting`, `/settings`, `/setting rest` |
| `ultras.settings.admin` | op | base gate for every `/setting_admin` use |
| `ultras.settings.admin.reload` | op | `/setting_admin reload` |
| `ultras.settings.admin.open` | op | `/setting_admin open <gui>` |
| `ultras.settings.admin.player` | op | `/setting_admin <player>` |
| `ultras.settings.admin.reset` | op | `/setting_admin rest <player>` |

Individual buttons can also require their own `permission:` — see "Editing GUIs" below.

## ⚠️ The one real architectural limitation — please read

Ultras_TPA's commands are **self-referential**: whoever runs `/tpa toggle` is whose setting flips.
There's no "run this command as if you were Steve" built into Ultras_TPA, and per your instructions
this project never touches Ultras_TPA's code to add one. Bukkit does give one clean, 100%-standard way
to still make `/setting_admin <player>` actually work: **`Server#dispatchCommand(CommandSender, String)`
accepts any `CommandSender` — including another online player's own `Player` object.** So when an admin
clicks a toggle button while viewing `Steve`'s settings, Ultras_settings runs the exact same command
Steve would've typed, *with Steve as the sender* (`Bukkit.dispatchCommand(steve, "tpa toggle")`). This
is not reflection, not NMS, not a hack — it's the documented purpose of that API's signature — but it
only works **if Steve is online right now**. If he's offline, the button shows a
"player must be online" message instead of silently doing nothing. One side-effect worth knowing:
Ultras_TPA's own feedback message for that command goes to **Steve's** chat (since as far as Ultras_TPA
is concerned, Steve ran it) — the admin sees the GUI refresh with the new state instead.

`/setting_admin rest <player>` has the same requirement and the same reason.

## GUI files (`gui/` folder)

| File | What it is |
|---|---|
| `setting.gui.yml` | Main menu — 4 rows × 7 buttons (TPA/TPAHere/TPA-All/Auto/GUI/Blocklist + placeholders) |
| `setting.gui1.yml`, `setting.gui2.yml` | Example secondary pages, purely to prove a new page needs zero Java |
| `blocklist.gui.yml` | Paginated online-player list |
| `block.gui.yml` | Per-player Block/Unblock screen |

Drop any new `<name>.gui.yml` file in this folder and run `/setting_admin reload` — it's immediately
usable via `/setting_admin open <name>.gui` and shows up in that command's tab-completion automatically,
with **no Java changes and no restart**.

### Editing GUIs

Every GUI file supports: `title`, `size` (rounded to a multiple of 9, 9–54), `close-on-action`,
`allow-player-items`, `sound-open`, `decoration` (a filler material/name + list of `slots`), and a
`buttons:` map. Each button has `slot`, `enabled`, an optional `permission`, and a `type`:

- **`TOGGLE`** — has `toggle-source` (`tpa` / `tpahere` / `tpaall` / `auto`) plus `on:`/`off:` blocks.
  Each block: `material`, `name`, `lore`, `sound`, `volume`, `pitch`, `glow`, `custom-model-data`,
  `command`, `message`. The real ON/OFF state is read fresh from Ultras_TPA's own data file every time
  the GUI opens or a button is clicked — never guessed from "the last button you clicked."
- **`ACTION`** — flat fields (same list as above) plus `action:` (`OPEN_GUI`, `CLOSE`, `PLAYER_COMMAND`,
  `CONSOLE_COMMAND`, `MESSAGE`, `BACK`, `NEXT_PAGE`, `PREVIOUS_PAGE`, `TOGGLE`, `BLOCK`, `UNBLOCK`,
  `NONE`) and, for `OPEN_GUI`/`BACK`, `target-gui: "<file-id>"`.
- **`COMING_SOON`** — flat fields only; clicking plays its sound and shows its `message` (or the
  `buttons.coming-soon` language key if none is set) and does nothing else.
- **`BLOCK_STATUS` / `UNBLOCK_STATUS`** (`block.gui.yml` only) — `active:`/`inactive:` blocks instead of
  `on:`/`off:`. The "active" one is shown and clickable exactly when that action is currently valid
  (Block when *not* blocked, Unblock when *already* blocked); the other renders as `inactive` and does
  nothing on click — this is what makes double-clicks and stale GUIs safe (spec sections 15 & 79).

Every command string supports `%target%`, substituted with the player being acted on. Sounds accept
either a literal Bukkit `Sound` name (e.g. `BLOCK_NOTE_BLOCK_PLING`) or a key from `config.yml`'s
`sounds:` section (e.g. `back`, `next-page`) — both work in either place; whichever resolves first wins.

### Adding a TPA button

Copy an existing `TOGGLE` button, change `toggle-source` and `command` to match one of
`config.yml`'s `integration.ultras-tpa.commands` entries (or add a new command there), pick a slot,
done — no rebuild needed for a *config* change, just `/setting_admin reload`.

## Language

`lang/en.yml` (default) and `lang/ar.yml` hold every system/feedback message — button *appearance*
(names/lore) lives in the GUI files themselves, matching the spec's own split between "GUI file"
fields and "language file" messages. Switch with `language: en` / `language: ar` in `config.yml`, or
drop `plugins/Ultras_settings/lang/<code>.yml` for a custom language (falls back to English for any
missing key).

## Storage

- **TPA-related state** (TPA/TPAHere/TPA-All/Auto/Blocked) is **not stored by Ultras_settings at
  all** — it's read live, read-only, from Ultras_TPA's own `player-data.yml`
  (`integration.ultras-tpa.data-file` in config.yml), and every write happens by dispatching
  Ultras_TPA's own commands, never by editing that file directly. This is what keeps the two plugins'
  data honestly in sync with zero risk of one silently overwriting the other.
- **Ultras_settings' own data** (currently unused — reserved for a future non-TPA setting replacing a
  "COMING SOON" button) lives at `plugins/Ultras_settings/data/players/<uuid>.yml`, one file per
  player, genuinely lazy-loaded (nothing read from disk until that specific player's data is asked
  for) and evicted from memory after being saved when they leave (`storage.unload-on-quit`), so RAM use
  never scales with your player *history*, only with who's online right now.

## Project tree

```
Ultras_settings/
├── build.gradle.kts, settings.gradle.kts, gradle.properties
├── README.md
├── .github/workflows/build.yml
└── src/main/
    ├── resources/
    │   ├── plugin.yml, config.yml
    │   ├── lang/en.yml, lang/ar.yml
    │   └── gui/setting.gui.yml, setting.gui1.yml, setting.gui2.yml, blocklist.gui.yml, block.gui.yml
    └── java/me/uc_hussein/ultrassettings/
        ├── UltrasSettingsPlugin.java
        ├── config/ConfigManager.java
        ├── language/LanguageManager.java
        ├── integration/TpaIntegration.java
        ├── models/ (ButtonType, ActionType, VisualState, ButtonDefinition, GuiDefinition)
        ├── gui/ (ClickableGui, GuiRegistry, SettingsGui, BlocklistGui, BlockGui)
        ├── services/ (SoundService, GuiOpener, SettingsService)
        ├── storage/PlayerDataStorage.java
        ├── listeners/ (InventoryListener, PlayerQuitListener)
        └── commands/ (SettingCommand, SettingAdminCommand, + tab completers)
```

## Known simplifications (flagged, not hidden)

- The "GUI" toggle button (`/tpa gui`) is an `ACTION` button, not a `TOGGLE`. Ultras_TPA's `/tpa gui`
  command only *opens* the TPA player-picker GUI — it doesn't persist an on/off preference — so there's
  no real state for Ultras_settings to honestly display as ON/OFF. If Ultras_TPA gains that toggle later,
  swap this one button's `type` to `TOGGLE` with `toggle-source: gui` in `setting.gui.yml`; everything
  else (data-key mapping, defaults) is already wired for it in `config.yml`.
- Admin editing of another player's settings requires that player to be **online** (see the
  architecture note above) — this is a hard limitation of "don't modify Ultras_TPA," not an oversight.
- `defaults.gui` exists in `config.yml` for forward-compatibility with the point above, but
  `/setting rest` currently only resets `tpa`/`tpahere`/`tpaall`/`auto` (the settings that actually
  have real persisted state to reset).
