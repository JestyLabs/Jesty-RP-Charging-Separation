# Application artwork

The maintainer supplied the final 1920x1080 charging-state backgrounds and the
transparent Jesty header wordmark on 2026-09-26.

- `res/drawable-nodpi/jesty_rp_background.png`: charging separation confirmed.
- `res/drawable-nodpi/jesty_rp_background_charging.png`: normal battery charging.
- `res/drawable-nodpi/jesty_rp_wordmark.png`: exact transparent header wordmark.
- `assets/branding/jesty_wordmark_header.png`: preserved branding source copy.

The Activity chooses between the two backgrounds from confirmed runtime state;
the artwork itself does not determine or claim that separation succeeded.
