# Velqor v2.2.0 - Transparent Navigation Bar & Liquid Glass Refinement

- MainActivity.java:
  - Enabled true edge-to-edge transparent navigation bar via Window.setNavigationBarContrastEnforced(false) and Window.setStatusBarContrastEnforced(false) on API 29+.
  - Enabled Window.setDecorFitsSystemWindows(false) on API 30+ and light status/navigation bar icons for light mode.
  - Dynamically resolved WindowInsets (navigationBars & ime) to position floating tab bar and hide automatically when keyboard is active.
  - Eliminated opaque 0xE6000000 bottom gradient scrim so content flows directly through the transparent navigation bar.

- ThemeTokens.java:
  - Adjusted COLOR_SURFACE_GLASS_DARK to 0x80181920 (~50% alpha smoked glass) and COLOR_SURFACE_GLASS_LIGHT to 0x80FFFFFF for translucent liquid glass look.

- BlurBackdropView.java:
  - Reduced RenderNode blur tint overlay to 0x5516171E (33% tint) and added upper specular reflection.
  - Upgraded fallback rendering to translucent surface glass with specular dome highlight.

- FloatingTabBar.java:
  - Added soft 2-layer ambient and key shadows via Canvas drawRoundRect.
  - Disabled default Android view elevation to remove hard border edges.
