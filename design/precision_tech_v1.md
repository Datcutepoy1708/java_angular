---
name: Precision Tech
colors:
  surface: '#f8f9ff'
  surface-dim: '#d0dbed'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e6eeff'
  surface-container-high: '#dee9fc'
  surface-container-highest: '#d9e3f6'
  on-surface: '#121c2a'
  on-surface-variant: '#424654'
  inverse-surface: '#27313f'
  inverse-on-surface: '#eaf1ff'
  outline: '#737786'
  outline-variant: '#c2c6d7'
  surface-tint: '#0056d0'
  primary: '#0055ce'
  on-primary: '#ffffff'
  primary-container: '#2f6fed'
  on-primary-container: '#ffffff'
  inverse-primary: '#b1c5ff'
  secondary: '#585f6c'
  on-secondary: '#ffffff'
  secondary-container: '#dce2f3'
  on-secondary-container: '#5e6572'
  tertiary: '#9d4200'
  on-tertiary: '#ffffff'
  tertiary-container: '#c45400'
  on-tertiary-container: '#ffffff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dae2ff'
  primary-fixed-dim: '#b1c5ff'
  on-primary-fixed: '#001847'
  on-primary-fixed-variant: '#0040a0'
  secondary-fixed: '#dce2f3'
  secondary-fixed-dim: '#c0c7d6'
  on-secondary-fixed: '#151c27'
  on-secondary-fixed-variant: '#404754'
  tertiary-fixed: '#ffdbcb'
  tertiary-fixed-dim: '#ffb691'
  on-tertiary-fixed: '#341100'
  on-tertiary-fixed-variant: '#793100'
  background: '#f8f9ff'
  on-background: '#121c2a'
  surface-variant: '#d9e3f6'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
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
  label-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.01em
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
  margin-mobile: 16px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 32px
  section-gap: 80px
---

## Brand & Style
The design system is built on a foundation of **Minimalism** and **Modern Corporate** aesthetics. It prioritizes clarity, performance, and trust, reflecting the high-spec nature of the technology it showcases. The emotional response should be one of "effortless sophistication"—the UI recedes into the background to let product photography take center stage. 

The visual language uses heavy whitespace to reduce cognitive load and a limited color palette to maintain focus on calls to action. Every element is intentional, avoiding decorative flourishes in favor of functional, high-quality execution.

## Colors
This design system utilizes a high-clarity light mode palette.
- **Primary:** Soft Blue (#2F6FED) is used exclusively for primary actions, progress indicators, and active states to guide user flow.
- **Neutral/Text:** Dark Gray (#1F2937) provides high contrast for legibility in headings. Medium Gray (#6B7280) is used for metadata, captions, and secondary information.
- **Backgrounds:** Pure White (#FFFFFF) is the primary canvas. Very Light Gray (#F7F8FA) is used for section differentiation and background fills for product cards.

## Typography
The design system relies on **Inter** for its systematic and utilitarian qualities. 
- **Headings:** Use tighter letter spacing and bold weights to create a strong visual anchor for product titles.
- **Body Text:** Employs generous line heights (1.5x minimum) to ensure long product descriptions remain readable.
- **Labels:** Used for navigation items and small UI details, utilizing a medium weight to maintain legibility at smaller scales.

## Layout & Spacing
The design system follows a **Fixed Grid** model for desktop and a **Fluid** model for mobile.
- **Desktop:** 12-column grid within a 1280px container. Gutters are fixed at 24px to provide "air" between product cards.
- **Mobile:** 4-column fluid grid with 16px side margins.
- **Vertical Rhythm:** A strict 8px base unit drives all spacing. Section gaps are intentionally large (80px+) to distinguish between product categories and marketing hero blocks, reinforcing the "light and airy" feel.

## Elevation & Depth
Depth is conveyed through **Ambient Shadows** and **Tonal Layers**. 
- **Surfaces:** Use `#FFFFFF` for elevated cards against a `#F7F8FA` background to create natural separation without heavy lines.
- **Shadows:** Avoid harsh black shadows. Use a soft, multi-layered blur: `0px 4px 20px rgba(31, 41, 55, 0.05)`. 
- **Interactions:** On hover, cards should subtly lift by increasing shadow spread and shifting 2px upward. Do not use borders for containers; allow the shadow to define the boundary.

## Shapes
The shape language is consistently **Rounded**, striking a balance between professional precision and approachability.
- **Standard (8px):** Applied to input fields, small buttons, and thumbnails.
- **Large (12px):** Applied to product cards and main containers.
- **Extra Large (16px+):** Reserved for promotional banners and hero imagery.
- **Pill:** Used exclusively for status badges (e.g., "In Stock") and category chips.

## Components
- **Buttons:** Primary buttons use a solid `#2F6FED` fill with white text. Secondary buttons use a light gray ghost style (no border, `#F7F8FA` background) to maintain hierarchy.
- **Cards:** Product cards feature a white background, 12px corner radius, and a soft shadow. Imagery should have a light gray padding fill to ensure white products don't bleed into the card background.
- **Input Fields:** Use a subtle `#F7F8FA` fill with a 1px border that only becomes Primary Blue on focus.
- **Chips/Badges:** Small, pill-shaped elements with low-opacity fills of the primary color for categories or stock status.
- **Lists:** Clean, horizontal rules in `#F7F8FA` between items, utilizing generous 16px padding to avoid a cramped "data-heavy" look.
- **Navigation:** A sticky top bar with a glassmorphism effect (backdrop blur) to maintain context while scrolling through long product lists.