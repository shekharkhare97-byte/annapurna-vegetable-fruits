# Annapurna Customer App — Locked Redesign Requirements

This document is the source of truth for the separate Customer App redesign.
Do not ask the customer to repeat these requirements or silently change them.

## Design lock
- Reproduce the latest customer-approved Annapurna green-and-white reference design as closely as possible.
- The approved reference is authoritative. Do not substitute a generic grocery template, an older reference, or an assistant-invented layout.
- Preserve the exact brand wording: **Annapurna Vegetables and Fruits**.
- Keep the same visual hierarchy, screen composition, colors, product/vegetable/fruit imagery, logo placement, and feature placement shown in the approved reference.
- If the reference asset is not available to the implementer, stop before making visual design decisions and request that exact asset; do not guess.

## Required customer features
- Home screen opens first and follows the approved reference.
- Categories, product details, product photos/prices, search, cart, and checkout.
- Offers / Live Offers and New Arrivals.
- Annapurna AI, voice search, and photo/image-based product search.
- Marathi, Hindi, and English across all visible UI, statuses, dialogs, errors, and notifications.
- Live order tracking, Wishlist, Notifications, Profile, and Refer & Earn.

## Live delivery tracking identity
- Show the delivery partner's live/moving location when the existing order lifecycle permits it.
- Delivery partner wears a **green T-shirt**.
- Delivery vehicle / delivery box carries the **Annapurna Vegetables and Fruits** logo/branding.
- Never show a different retailer brand or a red-shirt delivery partner.

## Data and integration safety
- Build this as a separate new Customer App; do not replace or modify the old installed app.
- Preserve existing Firebase project data, customer orders, products, and Admin–Delivery workflows.
- Do not change Firestore production data/rules, payment behavior, or live integrations without explicit approval.
- Customer OTP must not be introduced.
- Keep GPS permission/order and tracking behavior consistent with the already agreed requirements.
- Prefer free-tier compatible services; do not introduce paid dependencies without approval.

## Delivery / quality gates
- Do not claim implementation, build, APK signing, installation, or testing until each has actually succeeded.
- Do not distribute a debug-signed APK as a release.
- A release APK requires the replacement upload key and GitHub Actions secrets to be configured securely by the repository owner.
- The new Android application ID requires a genuine Firebase Android app registration and matching `google-services.json`; never edit or fabricate that file.
- Keep work on `customer-app-redesign-safe` and its existing review PR. Do not merge to `main` without explicit approval.
