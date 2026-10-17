# PingBoard

An admin debugging tool for Paper 26.3. `/ping` toggles a sidebar showing every online player's ping, lowest at the top. The sidebar appears only for the player who ran the command.

```
▮▮▮▮ (head) | Steve 87ms
```

The bars are the vanilla tab-list ping icons and the head is the player's skin. Both are drawn with Adventure object components, so no resource pack is needed. The ping text colours match the coloured bars of the VanillaTweaks ping icon pack.

## Requirements

- Paper 26.3 and Java 25.
- Minecraft 1.21.9 or newer clients, so they can render object components.

## Build

```sh
./gradlew build        # Windows: gradlew.bat build
```

The plugin jar is written to `build/libs/PingBoard-1.0.2.jar`.

## Install

1. Copy the jar into the server's `plugins/` folder.
2. Start or restart the server. This creates `plugins/PingBoard/config.yml`.
3. Edit the config if you need to, then restart. There is no reload command.

## Usage

| | |
|---|---|
| Command | `/ping` toggles the sidebar on or off for you only. It is off by default and resets when you log out or the server restarts. |
| Permission | `pingboard.admin` (default: op) |

While your sidebar is open you are moved to a private scoreboard. Turning it off restores the scoreboard you had before. The main scoreboard is never modified.

## Config

| Key | Default | Meaning |
|---|---|---|
| `refresh-interval` | `10` | Refresh period in ticks. 10–20 is recommended. |
| `title` | `<yellow><bold>Ping</bold></yellow>` | Sidebar title, in MiniMessage format. |
| `max-lines` | `15` | Maximum number of lines (1–15). The highest pings are kept. |
| `ping-source` | `keepalive` | Where ping values come from: `keepalive` or `probe` (see below). |

### `ping-source`

- **`keepalive`** (default) uses `Player#getPing()`. On Paper 26.3 the server sends a keep-alive every second, and `getPing()` is the average round trip over the last 5 seconds. Because it is an average, spikes are spread out. It also stays at its old value while a client isn't responding, until the late replies arrive.
- **`probe`** sends one cookie request to each player per refresh (at most one waiting at a time) with `Player#retrieveCookie`, and times the reply. The sidebar shows the latest single round trip. If a reply is overdue, it shows how long it has been waiting, so a frozen client is visible right away. Players who never reply fall back to `getPing()`.
  - It measures the round trip to whatever answers cookie requests. Behind ViaBackwards (pre-1.20.5 clients) or Geyser, a translation layer may answer instead of the client, which makes values read too low. Use `keepalive` on servers that rely on those.
  - The cost is one tiny packet pair per online player per refresh, and only while someone has the sidebar open.

## Testing

1. Start the server as an op player and run `/ping`. You should see the sidebar and the message `Ping sidebar enabled.`
2. Run `/ping` again. You should see `Ping sidebar disabled.`, and any sidebar you had before (from another plugin or `/scoreboard objectives setdisplay sidebar …`) should come back.
3. Running `ping` from the console should print the "players only" message.
4. Deop a second account (or `lp user <name> permission set pingboard.admin false`) and run `/ping` with it. It should get the no-permission message, and its screen should not change.
5. To see ordering and colours, add latency with a tool such as [clumsy](https://jagt.github.io/clumsy/) on Windows or `tc qdisc add dev lo root netem delay 200ms` on Linux.
6. Log out and back in. The sidebar should be off again. Players who leave should disappear from everyone's sidebar.
