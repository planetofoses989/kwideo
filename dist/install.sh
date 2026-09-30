#!/usr/bin/env bash
# Kwideo AppImage installer (Linux; macOS partially — .desktop is Linux-only)
# Installs a Kwideo *.AppImage to a user, hidden, or system location and
# creates a .desktop launcher. No downloads, no root unless --system.
#
# Usage:
#   ./install.sh [APPIMAGE] [--user|--hidden|--system] [--dest DIR] [--yes] [--uninstall]
#
# Modes:
#   --user    (default) ~/.local/bin/Kwideo.AppImage + ~/.local/share/applications/kwideo.desktop
#   --hidden  ~/.kwideo/Kwideo.AppImage (hidden folder) + user .desktop
#   --system  /opt/kwideo/Kwideo.AppImage + /usr/share/applications/kwideo.desktop (requires sudo)
#   --dest DIR  override install dir (useful for testing / custom layouts)
#
# Other flags:
#   --yes        non-interactive (overwrite without prompting)
#   --uninstall  remove installed AppImage + .desktop and exit
#   -h, --help   show help
#
# Notes:
# - This repo currently builds Android APKs, not a Linux AppImage. This script
#   installs any AppImage you point it at (e.g. a future Kwideo Linux build).
# - Windows does not use .sh/.desktop; use WSL/MSYS2 or a native installer there.
set -euo pipefail

APP_NAME="Kwideo"
APP_ID="kwideo"
APPIMAGE_NAME="Kwideo.AppImage"
ICON_NAME="kwideo"

MODE="user"
DEST_OVERRIDE=""
ASSUME_YES=0
DO_UNINSTALL=0
SRC=""

die() { printf 'error: %s\n' "$*" >&2; exit 1; }
info() { printf '%s\n' "$*"; }

usage() {
  sed -n '2,30p' "$0"
}

while [ $# -gt 0 ]; do
  case "$1" in
    --user) MODE="user"; shift ;;
    --hidden) MODE="hidden"; shift ;;
    --system) MODE="system"; shift ;;
    --dest) DEST_OVERRIDE="${2:-}"; [ -n "$DEST_OVERRIDE" ] || die "--dest needs a directory"; shift 2 ;;
    --dest=*) DEST_OVERRIDE="${1#--dest=}"; shift ;;
    --yes|-y) ASSUME_YES=1; shift ;;
    --uninstall) DO_UNINSTALL=1; shift ;;
    -h|--help) usage; exit 0 ;;
    --) shift; [ $# -gt 0 ] && SRC="$1" && shift; break ;;
    -*) die "unknown flag: $1 (see --help)" ;;
    *) [ -z "$SRC" ] && SRC="$1" || die "only one APPIMAGE argument allowed"; shift ;;
  esac
done

# Resolve locations per mode.
case "$MODE" in
  user)   INSTALL_DIR="${HOME}/.local/bin"; DESKTOP_FILE="${HOME}/.local/share/applications/${APP_ID}.desktop" ;;
  hidden) INSTALL_DIR="${HOME}/.${APP_ID}"; DESKTOP_FILE="${HOME}/.local/share/applications/${APP_ID}.desktop" ;;
  system) INSTALL_DIR="/opt/${APP_ID}"; DESKTOP_FILE="/usr/share/applications/${APP_ID}.desktop" ;;
esac
[ -n "$DEST_OVERRIDE" ] && INSTALL_DIR="$DEST_OVERRIDE"
TARGET="${INSTALL_DIR}/${APPIMAGE_NAME}"

# Writable without sudo? Walk up to the first existing ancestor (targets may
# not exist yet on fresh installs).
can_write() {
  local p="$1"
  while [ ! -e "$p" ] && [ "$p" != "/" ] && [ "$p" != "." ] && [ "$p" != "$HOME" ]; do
    p="$(dirname "$p")"
  done
  [ -e "$p" ] && [ -w "$p" ] && return 0
  # Parent chain missing entirely (e.g. fresh fake HOME): check HOME itself.
  [ -w "$HOME" ] && return 0
  return 1
}

uninstall() {
  info "Uninstalling ${APP_NAME} (${MODE})..."
  for f in "$TARGET" "$DESKTOP_FILE"; do
    if [ -e "$f" ]; then
      if can_write "$f"; then rm -f "$f"; else sudo rm -f "$f"; fi
      info "  removed $f"
    else
      info "  not present: $f"
    fi
  done
  if command -v update-desktop-database >/dev/null 2>&1; then
    if can_write "$(dirname "$DESKTOP_FILE")"; then update-desktop-database "$(dirname "$DESKTOP_FILE")" >/dev/null 2>&1 || true
    else sudo update-desktop-database "$(dirname "$DESKTOP_FILE")" >/dev/null 2>&1 || true; fi
  fi
  info "Done."
}

[ "$DO_UNINSTALL" -eq 1 ] && uninstall && exit 0

# Auto-detect AppImage if not given.
if [ -z "$SRC" ]; then
  SRC="$(ls -1 ./*.AppImage 2>/dev/null | head -n 1 || true)"
  [ -z "$SRC" ] && SRC="$(ls -1 ./dist/*.AppImage 2>/dev/null | head -n 1 || true)"
fi
[ -n "$SRC" ] || die "no AppImage given and none found in ./ or ./dist/. Usage: ./install.sh Kwideo-*.AppImage [--user|--hidden|--system]"
[ -f "$SRC" ] || die "AppImage not found: $SRC"

mkdir_cmd() { if can_write "$1"; then mkdir -p "$1"; else sudo mkdir -p "$1"; fi; }
copy_cmd() { if can_write "$2"; then cp -f "$1" "$2" && chmod +x "$2"; else sudo cp -f "$1" "$2" && sudo chmod +x "$2"; fi; }

if [ -e "$TARGET" ] && [ "$ASSUME_YES" -eq 0 ]; then
  printf '%s exists. Overwrite? [y/N] ' "$TARGET"
  read -r ans || ans=""
  case "$ans" in [yY][eE][sS]|[yY]) ;; *) die "aborted" ;; esac
fi

info "Installing ${APP_NAME}:"
info "  source: $SRC"
info "  target: $TARGET"
mkdir_cmd "$INSTALL_DIR"
copy_cmd "$SRC" "$TARGET"

# Resolve icon if shipped next to the script (dist/kwideo.png|svg), else fall back.
ICON_DIR_USER="${HOME}/.local/share/icons/hicolor/256x256/apps"
ICON_DIR_SYS="/usr/share/icons/hicolor/256x256/apps"
ICON_SRC=""
for cand in "$(dirname "$0")/kwideo.png" "$(dirname "$0")/kwideo.svg" "./dist/kwideo.png" "./dist/kwideo.svg"; do
  if [ -f "$cand" ]; then ICON_SRC="$cand"; break; fi
done
ICON_ENTRY="$ICON_NAME"
if [ -n "$ICON_SRC" ]; then
  case "$MODE" in
    system) ICON_DIR="$ICON_DIR_SYS" ;;
    *) ICON_DIR="$ICON_DIR_USER" ;;
  esac
  ICON_EXT="${ICON_SRC##*.}"
  if can_write "$ICON_DIR"; then
    mkdir -p "$ICON_DIR" && cp -f "$ICON_SRC" "${ICON_DIR}/${ICON_NAME}.${ICON_EXT}"
  else
    sudo mkdir -p "$ICON_DIR" && sudo cp -f "$ICON_SRC" "${ICON_DIR}/${ICON_NAME}.${ICON_EXT}"
  fi
  if command -v gtk-update-icon-cache >/dev/null 2>&1; then
    if can_write "$ICON_DIR"; then gtk-update-icon-cache -f -t "$(dirname "$(dirname "$ICON_DIR")")" >/dev/null 2>&1 || true
    else sudo gtk-update-icon-cache -f -t "$(dirname "$(dirname "$ICON_DIR")")" >/dev/null 2>&1 || true; fi
  fi
fi

# Write .desktop launcher.
DESKTOP_TMP="$(mktemp)"
cat > "$DESKTOP_TMP" <<EOF
[Desktop Entry]
Type=Application
Name=${APP_NAME}
Comment=Modern open-source video player (mpv-powered Android fork; Linux AppImage)
Exec="${TARGET}" %U
TryExec=${TARGET}
Icon=${ICON_ENTRY}
Categories=AudioVideo;Video;Player;
MimeType=video/mp4;video/x-matroska;video/webm;video/avi;video/mpeg;video/ogg;video/quicktime;video/x-flv;video/x-ms-wmv;audio/mpeg;audio/flac;audio/ogg;audio/mp4;
Terminal=false
StartupWMClass=${APP_NAME}
EOF
if can_write "$DESKTOP_FILE"; then
  mkdir -p "$(dirname "$DESKTOP_FILE")" && cp -f "$DESKTOP_TMP" "$DESKTOP_FILE" && chmod 644 "$DESKTOP_FILE"
else
  sudo mkdir -p "$(dirname "$DESKTOP_FILE")" && sudo cp -f "$DESKTOP_TMP" "$DESKTOP_FILE" && sudo chmod 644 "$DESKTOP_FILE"
fi
rm -f "$DESKTOP_TMP"

if command -v update-desktop-database >/dev/null 2>&1; then
  if can_write "$(dirname "$DESKTOP_FILE")"; then update-desktop-database "$(dirname "$DESKTOP_FILE")" >/dev/null 2>&1 || true
  else sudo update-desktop-database "$(dirname "$DESKTOP_FILE")" >/dev/null 2>&1 || true; fi
fi
if command -v desktop-file-validate >/dev/null 2>&1; then
  desktop-file-validate "$DESKTOP_FILE" 2>&1 || info "(warning: desktop-file-validate reported issues above)"
fi

info "Done. Launch '${APP_NAME}' from your app menu, or run: ${TARGET}"
