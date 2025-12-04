# Product Detail Video Support (Local Storage)

## 1. Setup Storage & Permissions

- Add `READ_EXTERNAL_STORAGE` (and `READ_MEDIA_VIDEO` for Android 13+) to `AndroidManifest.xml`.
- Create a robust helper function to check/request runtime permissions in `ProductDetailActivity`.

## 2. Data Model Updates

- Update `Product` class to include a `videoFileName` field (e.g., `"zyn_citrus_promo.mp4"`).
- Update `MainActivity` to populate this field for each product.

## 3. UI Implementation (`activity_product_detail.xml`)

- Replace the static `ImageView` with a `VideoView` (or `ExoPlayer` if available, but `VideoView` is simpler for local files).
- Add a placeholder/loading state or fallback image.

## 4. Logic Implementation (`ProductDetailActivity.kt`)

- Receive `videoFileName` from Intent.
- Construct the full path: `Environment.getExternalStorageDirectory() + "/Movies/ZootBox/" + videoFileName`.
- Check if file exists.
- If exists: Prepare and loop video.
- If not exists: Fallback to the static `imageRes`.

## 5. Testing Strategy

- Verify permissions flow.
- Test playback of a sample video file placed in the device folder.