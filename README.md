# Trial Selector

A client-side Fabric mod for Minecraft 1.21+ that lets you select your desired rewards from Trial Vaults and Ominous Trial Vaults, automatically unlocking them the **exact frame** your chosen reward appears.

---

## Features

- **Exact-Frame Timing**: Injects directly into the client render pipeline (`VaultBlockEntityRenderer`) and network packet handler (`ClientPlayNetworkHandler`) to detect and click at the exact millisecond/frame the target item appears—ensuring you never roll the wrong item due to tick delay.
- **Customizable Loot Picker**:
  - Full drop table support for both Standard Trial Vaults and Ominous Trial Vaults (Heavy Core, Mace, Wind Burst, Flow Armor Trim, Enchanted Golden Apple, etc.).
  - Search filter and category tabs ("All Drops", "Ominous Vault", "Standard Vault").
  - Click any reward to toggle target selection (marked with a distinct red cross `✖`).
  - Quick action buttons: "Select Rares Only", "Select All", "Deselect All".
- **Real-Time Vault HUD**:
  - Displays currently targeted Vault type (Standard or Ominous).
  - Displays cycling item name and texture.
  - Highlights target matches in vibrant green with `[MATCH!]`.
  - Shows key inventory status, lock state, and distance.
- **Proactive Key Management**:
  - Automatically holds the required key (Trial Key or Ominous Trial Key) when in range of an active vault so there is zero latency when the reward shows up.
  - Automatically switches hotbar slots or swaps from main inventory.
- **Target Locking**:
  - Press `B` to lock onto a specific vault so camera movement doesn't lose focus.

---

## Default Keybinds

| Key | Action |
|:---:|:---|
| **V** | Open Trial Selector Configuration & Rewards Menu |
| **H** | Toggle Auto-Unlock ON / OFF |
| **B** | Lock / Unlock Target Vault |

*(All keybinds can be reconfigured in `Options` -> `Controls` -> `Key Binds`)*

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/) for Minecraft 1.21+.
2. Download and place the **Fabric API** jar in your `.minecraft/mods` directory.
3. Download the latest **Trial Selector** release jar and place it in `.minecraft/mods`.

---

## Building from Source

Requirements: Java 21+

```bash
# Clone the repository
git clone https://github.com/L30LH/TrialSelector.git
cd TrialSelector

# Build with Gradle
./gradlew build
```

The compiled mod jar will be in `build/libs/`.

---

## License

This project is licensed under the **GNU General Public License v3.0** (GPL-3.0). See the [LICENSE](LICENSE) file for details.
