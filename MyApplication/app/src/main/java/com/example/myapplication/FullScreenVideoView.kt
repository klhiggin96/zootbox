package com.example.myapplication

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.TextureView
import java.io.IOException

/**
 * A TextureView-based video player that scales to CenterCrop.
 * Supports alpha blending for cross-fades.
 */
class FullScreenVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextureView(context, attrs, defStyleAttr), TextureView.SurfaceTextureListener {

    private var mediaPlayer: MediaPlayer? = null
    private var videoUri: Uri? = null
    private var isPrepared = false
    private var onPreparedListener: ((MediaPlayer) -> Unit)? = null
    private var onCompletionListener: (() -> Unit)? = null
    private var onErrorListener: ((MediaPlayer, Int, Int) -> Boolean)? = null
    private var videoWidth = 0
    private var videoHeight = 0

    init {
        surfaceTextureListener = this
    }

    fun setVideoURI(uri: Uri) {
        videoUri = uri
        Log.d("FullScreenVideoView", "setVideoURI called: $uri")
        if (surfaceTexture != null) {
            openVideo()
        } else {
            Log.d("FullScreenVideoView", "Surface not ready yet, will open when available")
        }
    }

    fun setOnPreparedListener(listener: (MediaPlayer) -> Unit) {
        onPreparedListener = listener
    }
    
    fun setOnCompletionListener(listener: () -> Unit) {
        onCompletionListener = listener
    }
    
    fun setOnErrorListener(listener: (MediaPlayer, Int, Int) -> Boolean) {
        onErrorListener = listener
    }

    fun start() {
        if (isPrepared) {
            mediaPlayer?.start()
        }
    }

    fun pause() {
        if (isPrepared && mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    fun seekTo(msec: Int) {
        if (isPrepared) {
            mediaPlayer?.seekTo(msec)
        }
    }
    
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }

    fun stopPlayback() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isPrepared = false
    }

    private fun openVideo() {
        Log.d("FullScreenVideoView", "openVideo() called")
        if (videoUri == null) {
            Log.e("FullScreenVideoView", "videoUri is null, aborting")
            return
        }
        if (surfaceTexture == null) {
            Log.e("FullScreenVideoView", "surfaceTexture is null, aborting")
            return
        }

        try {
            Log.d("FullScreenVideoView", "Creating MediaPlayer for: $videoUri")
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, videoUri!!)
                setSurface(Surface(surfaceTexture))
                isLooping = false // We handle looping manually for cross-fade
                setOnPreparedListener { mp ->
                    isPrepared = true
                    this@FullScreenVideoView.videoWidth = mp.videoWidth
                    this@FullScreenVideoView.videoHeight = mp.videoHeight
                    Log.d("FullScreenVideoView", "MediaPlayer prepared successfully, video size: ${this@FullScreenVideoView.videoWidth}x${this@FullScreenVideoView.videoHeight}")
                    applyCenterCropScaling()
                    onPreparedListener?.invoke(mp)
                }
                setOnCompletionListener {
                    Log.d("FullScreenVideoView", "MediaPlayer completed")
                    onCompletionListener?.invoke()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e("FullScreenVideoView", "MediaPlayer error: what=$what, extra=$extra")
                    onErrorListener?.invoke(mp, what, extra) ?: true
                }
                prepareAsync()
            }
            Log.d("FullScreenVideoView", "MediaPlayer created successfully, preparing async")
        } catch (e: IOException) {
            Log.e("FullScreenVideoView", "Failed to create MediaPlayer", e)
            e.printStackTrace()
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        Log.d("FullScreenVideoView", "Surface texture available: ${width}x${height}")
        if (videoUri != null) {
            openVideo()
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        // handled by layout
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        stopPlayback()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // no-op
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Use proper defaults instead of 0 to prevent potential 0x0 dimensions
        val width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val height = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        Log.d("FullScreenVideoView", "onMeasure: ${width}x${height}")
        setMeasuredDimension(width, height)

        // Apply CenterCrop scaling if video is loaded
        if (videoWidth > 0 && videoHeight > 0) {
            applyCenterCropScaling()
        }
    }

    private fun applyCenterCropScaling() {
        if (videoWidth == 0 || videoHeight == 0 || width == 0 || height == 0) {
            return
        }

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val videoAspectRatio = videoWidth.toFloat() / videoHeight.toFloat()
        val viewAspectRatio = viewWidth / viewHeight

        val scaleX: Float
        val scaleY: Float

        // CenterCrop scaling - fill entire screen, crop as needed
        if (videoAspectRatio > viewAspectRatio) {
            // Video is wider than view - scale to fit height and crop sides
            scaleY = viewHeight / videoHeight
            scaleX = scaleY
        } else {
            // Video is taller than view - scale to fit width and crop top/bottom
            scaleX = viewWidth / videoWidth
            scaleY = scaleX
        }

        // Calculate the pivot point (center of the view)
        val pivotX = viewWidth / 2
        val pivotY = viewHeight / 2

        // Create transformation matrix for CenterCrop
        val matrix = android.graphics.Matrix()

        // Translate to origin
        matrix.postTranslate(-videoWidth / 2f, -videoHeight / 2f)

        // Scale up to fill the screen
        matrix.postScale(scaleX, scaleY)

        // Translate back to center of view
        matrix.postTranslate(pivotX, pivotY)

        Log.d("FullScreenVideoView", "Applying CenterCrop: viewSize=${viewWidth}x${viewHeight}, videoSize=${videoWidth}x${videoHeight}, scale=${scaleX}x${scaleY}")
        setTransform(matrix)
    }
}