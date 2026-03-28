# Resources Directory

This directory holds all static assets used by the frontend.

## Structure

```
src/main/resources/
├── css/
│   └── theme.css          ← Global Catppuccin Mocha dark theme
├── images/
│   ├── logo.png           ← App logo (512×512 recommended)
│   ├── splash-bg.png      ← Optional splash background
│   └── icons/
│       ├── task.png
│       ├── calendar.png
│       ├── progress.png
│       └── leetcode.png
└── fonts/
    └── JetBrainsMono.ttf  ← Optional: monospace font for code snippets
```

## Loading Resources in Code

```java
// CSS
scene.getStylesheets().add(
    getClass().getResource("/css/theme.css").toExternalForm()
);

// Images
Image logo = new Image(getClass().getResourceAsStream("/images/logo.png"));
ImageView logoView = new ImageView(logo);

// Custom font
Font codeFont = Font.loadFont(
    getClass().getResourceAsStream("/fonts/JetBrainsMono.ttf"), 14
);
```

## Naming Conventions
- All file names: `kebab-case` (e.g. `splash-bg.png`)
- Icons: square PNGs, 64×64 px minimum
- Recommended formats: PNG for icons/logos, JPEG for photos
