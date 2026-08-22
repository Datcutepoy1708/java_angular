---
name: Precision Tech
colors:
  surface: '#f8f9fa'
  surface-dim: '#d9dadb'
  surface-bright: '#f8f9fa'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f4f5'
  surface-container: '#edeeef'
  surface-container-high: '#e7e8e9'
  surface-container-highest: '#e1e3e4'
  on-surface: '#191c1d'
  on-surface-variant: '#424754'
  inverse-surface: '#2e3132'
  inverse-on-surface: '#f0f1f2'
  outline: '#727785'
  outline-variant: '#c2c6d6'
  surface-tint: '#005ac2'
  primary: '#0058be'
  on-primary: '#ffffff'
  primary-container: '#2170e4'
  on-primary-container: '#fefcff'
  inverse-primary: '#adc6ff'
  secondary: '#545f73'
  on-secondary: '#ffffff'
  secondary-container: '#d5e0f8'
  on-secondary-container: '#586377'
  tertiary: '#924700'
  on-tertiary: '#ffffff'
  tertiary-container: '#b75b00'
  on-tertiary-container: '#fffbff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d8e2ff'
  primary-fixed-dim: '#adc6ff'
  on-primary-fixed: '#001a42'
  on-primary-fixed-variant: '#004395'
  secondary-fixed: '#d8e3fb'
  secondary-fixed-dim: '#bcc7de'
  on-secondary-fixed: '#111c2d'
  on-secondary-fixed-variant: '#3c475a'
  tertiary-fixed: '#ffdcc6'
  tertiary-fixed-dim: '#ffb786'
  on-tertiary-fixed: '#311400'
  on-tertiary-fixed-variant: '#723600'
  background: '#f8f9fa'
  on-background: '#191c1d'
  surface-variant: '#e1e3e4'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.05em
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  container-max: 1280px
  gutter: 24px
  margin-desktop: 48px
  margin-mobile: 16px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 32px
---

## Brand & Style

The design system is built for a high-end PC component and electronics marketplace. The brand personality is technical yet accessible, focusing on clarity, precision, and a "clean room" aesthetic that mirrors the high-tech nature of the hardware. 

The design style is **Minimalist** with a focus on functional elegance. It utilizes heavy whitespace to reduce cognitive load when browsing complex technical specifications. While the structure is systematic and corporate, it avoids being cold by using soft radius values and subtle depth cues. The goal is to evoke a sense of organized efficiency and reliability, ensuring that the product photography remains the focal point.

## Colors

The palette is intentionally restrained to prioritize product visibility. 

- **Primary (#3B82F6):** A soft, modern blue used exclusively for primary calls to action, active states, and critical highlights.
- **Secondary (#1E293B):** A deep slate used for primary text and high-contrast UI elements like headers or dark-mode toggles.
- **Neutral (#F8F9FA):** The foundation for background sections, providing a subtle contrast against white surfaces.
- **Surface (#FFFFFF):** Used for cards, containers, and inputs to create a "layered" feel against the light gray background.
- **Accents:** High-contrast badges (e.g., Sale, New) should use a refined "Success" green or "Alert" red, but these are kept desaturated to match the professional tone.

## Typography

This design system utilizes **Inter** across all levels to maintain a systematic and utilitarian feel. The hierarchy relies on weight and subtle letter-spacing adjustments rather than excessive size variance.

- **Headlines:** Use SemiBold (600) or Bold (700) weights. High-level displays use negative letter-spacing to appear more cohesive.
- **Body:** Regular (400) weight is used for all descriptions and specifications for maximum legibility.
- **Labels:** Used for categories, tags, and small metadata. Uppercase styling with increased tracking is applied to `label-md` to differentiate it from body text.

## Layout & Spacing

The layout follows a **Fluid Grid** model with a maximum container width to ensure readability on ultra-wide monitors.

- **Desktop:** 12-column grid with 24px gutters. Page margins are generous (48px) to reinforce the minimalist aesthetic.
- **Mobile:** 4-column grid with 16px margins.
- **Spacing Rhythm:** Based on an 8px scale. Use `stack-md` (16px) for internal card padding and `stack-lg` (32px) for vertical section spacing. 
- **Product Grids:** Product listings should use a responsive grid that moves from 1 column (mobile) to 2 columns (tablet) to 4 columns (desktop).

## Elevation & Depth

Visual hierarchy is achieved through **Tonal Layers** supplemented by **Ambient Shadows**.

- **Level 0 (Background):** #F8F9FA.
- **Level 1 (Cards/Surface):** #FFFFFF with a very soft, diffused shadow (0px 4px 20px rgba(0, 0, 0, 0.04)). This makes product cards appear to lift slightly off the gray background.
- **Level 2 (Hover States/Modals):** A more pronounced shadow (0px 10px 30px rgba(0, 0, 0, 0.08)) to indicate interactivity or focus.
- **Outlines:** Use 1px borders in #E5E7EB for secondary buttons and input fields to maintain a crisp, technical look without adding heavy shadows.

## Shapes

The shape language is consistently **Rounded** to soften the industrial nature of the products.

- **Standard Elements:** Buttons, input fields, and product cards use a 0.5rem (8px) radius.
- **Large Elements:** Banners and large containers use 1rem (16px) radius.
- **Circular Elements:** Category icons, cart count badges, and radio buttons use full rounding (9999px) to provide visual variety and emphasize "touchpoints."

## Components

- **Buttons:** Primary buttons are solid #3B82F6 with white text. Secondary buttons use a #E5E7EB border with #1E293B text. Both use 8px rounding and 12px x 24px padding.
- **Product Cards:** White background, 8px radius, subtle shadow. Images should have a light gray (#F8F9FA) padding area or be perfectly isolated on white.
- **Search Bar:** A sleek, full-width or large-center input with a 1px border. The search icon is placed inside the leading edge, using a subtle neutral-400 color.
- **Category Icons:** Circular containers (#F8F9FA) with centered glyphs or product silhouettes. Text labels are placed below, using `label-sm`.
- **Badges:** Small, high-contrast pills (e.g., -20% in #EF4444 or New in #10B981) placed in the top-right corner of product cards.
- **Input Fields:** 8px radius, 1px #E5E7EB border. On focus, the border transitions to #3B82F6 with a subtle blue glow.
- **Cart/Account Icons:** Clean line-art icons with a primary-colored dot for active notifications or cart counts.