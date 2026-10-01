# StreamCheck

IPTV stream health checker for Android. Load an M3U playlist (URL or file),
probe every stream, and export a cleaned playlist containing only working streams.

- Concurrent stream probing with live alive/dead counters
- Dead / Alive / All filters
- Export alive-only M3U to Downloads
- Settings: probe timeout, parallel checks, UI style (3D Glossy / Calm)

Built with a manual `aapt2` + `d8` pipeline (no Gradle). See `build.sh`.

Created by ZyneLabs.
