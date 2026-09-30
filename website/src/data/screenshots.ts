export interface Shot {
  src: string;
  alt: string;
  title: string;
  caption: string;
  /** Landscape shots span the full grid row instead of one column. */
  wide?: boolean;
}

/**
 * Curated set shipped with the site. The full collection lives in
 * `docs/screenshots/` in the repo — copy more files into
 * `website/public/screenshots/` and extend this list to grow the gallery.
 */
export const shots: Shot[] = [
  {
    src: "/screenshots/kwideo-player.jpg",
    alt: "kwideo player",
    title: "Player",
    caption: "mpv power, touch-friendly controls.",
    wide: true,
  },
  {
    src: "/screenshots/kwideo-dark-default-home.png",
    alt: "kwideo home library",
    title: "Home",
    caption: "Continue watching & browse.",
  },
  {
    src: "/screenshots/kwideo-dark-default-movies.png",
    alt: "kwideo movies grid",
    title: "Movies",
    caption: "Poster grid with durations.",
  },
  {
    src: "/screenshots/kwideo-dark-default-library.png",
    alt: "kwideo library",
    title: "Library",
    caption: "Shows, movies & folders.",
  },
  {
    src: "/screenshots/kwideo-movie-showcase.png",
    alt: "kwideo movie details",
    title: "Details",
    caption: "Movie showcase view.",
  },
  {
    src: "/screenshots/kwideo-welcome.png",
    alt: "kwideo welcome tour",
    title: "Welcome",
    caption: "First-run onboarding tour.",
  },
];
