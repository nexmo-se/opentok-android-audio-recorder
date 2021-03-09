package com.nexmo.audiorecorder

import android.Manifest
import android.content.Intent
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.opentok.android.*
import com.opentok.android.Session.SessionListener
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.io.File

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
        const val PERMISSIONS_REQUEST_CODE = 124
    }

    private var session: Session? = null
    private var publisher: Publisher? = null
    private var subscriber: Subscriber? = null

    private lateinit var publisherViewContainer: FrameLayout
    private lateinit var subscriberViewContainer: FrameLayout
    private lateinit var recordedAudioButton: Button

    private val sessionListener: SessionListener = object: SessionListener {
        override fun onStreamDropped(session: Session?, stream: Stream?) {
            Log.d(TAG, "onStreamDropped: Stream Dropped ${stream?.getStreamId() ?: "null id"} in session ${session?.sessionId ?: "null id"}")

            if (subscriber != null) {
                subscriber = null
                subscriberViewContainer.removeAllViews()
            }
        }

        override fun onStreamReceived(session: Session?, stream: Stream?) {
            Log.d(TAG, "onStreamReceived: Stream Received: ${stream?.getStreamId() ?: "null id"} in session ${session?.sessionId ?: "null id"}")

            if (subscriber == null) {
                subscriber = Subscriber.Builder(this@MainActivity, stream).build()
                subscriber?.renderer?.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
                subscriber?.setSubscriberListener(subscriberListener)

                session?.subscribe(subscriber)
                subscriberViewContainer.addView(subscriber?.view)
            }
        }

        override fun onConnected(session: Session?) {
            Log.d(TAG, "onConnected: Connected to session ${session?.sessionId ?: "null id"}")

            publisher = Publisher.Builder(this@MainActivity).build()
            publisher?.setPublisherListener(publisherListener)
            publisher?.renderer?.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)

            publisherViewContainer.addView(publisher?.view)
            if (publisher?.view is GLSurfaceView) {
                (publisher?.view as GLSurfaceView).setZOrderOnTop(true)
            }

            session?.publish(publisher)
        }

        override fun onDisconnected(session: Session?) {
            Log.d(TAG, "onDisconnected: Disconnected from session ${session?.sessionId ?: "null id"}")
        }

        override fun onError(session: Session?, opentokError: OpentokError?) {
            Log.e(TAG, "Session Error: ${opentokError?.message ?: "null message"}")
        }
    }

    private val publisherListener = object: PublisherKit.PublisherListener {
        override fun onStreamCreated(publisher: PublisherKit?, stream: Stream?) {
            Log.d(TAG, "onStreamCreated: Publisher Stream Created. Own stream ${stream?.getStreamId() ?: "null id"}")
        }

        override fun onStreamDestroyed(publisher: PublisherKit?, stream: Stream?) {
            Log.d(TAG, "onStreamDestroyed: Publisher Stream Destroyed. Own stream ${stream?.getStreamId() ?: "null id"}")
        }

        override fun onError(publisher: PublisherKit?, opentokError: OpentokError?) {
            Log.e(TAG, "PublisherKit onError: ${opentokError?.message ?: "null message"}")
        }
    }

    private val subscriberListener = object: SubscriberKit.SubscriberListener {
        override fun onConnected(subscriber: SubscriberKit?) {
            Log.d(TAG, "onConnected: Subscriber connected. Stream ${subscriber?.stream?.getStreamId() ?: "null id"}")
        }

        override fun onDisconnected(subscriber: SubscriberKit?) {
            Log.d(TAG, "onDisconnected: Subscriber disconnected. Stream ${subscriber?.stream?.getStreamId() ?: "null id"}")
        }

        override fun onError(subscriber: SubscriberKit?, opentokError: OpentokError?) {
            Log.e(TAG, "SubscriberKit onError: ${opentokError?.message ?: "null message"}")
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        publisherViewContainer = findViewById(R.id.publisher_container)
        subscriberViewContainer = findViewById(R.id.subscriber_container)
        recordedAudioButton = findViewById(R.id.recorded_audio_button)

        recordedAudioButton.setOnClickListener {
            // Go to RecordedAudioActivity
            val intent = Intent(this, RecordedAudioActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onPause() {
        super.onPause()
        session?.disconnect()
        session = null

        publisherViewContainer.removeAllViews()
        publisher = null

        subscriberViewContainer.removeAllViews()
        subscriber = null
    }

    override fun onResume() {
        super.onResume()
        session?.disconnect()

        publisherViewContainer.removeAllViews()
        publisher = null

        subscriberViewContainer.removeAllViews()
        subscriber = null

        requestPermissions()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    @AfterPermissionGranted(PERMISSIONS_REQUEST_CODE)
    private fun requestPermissions() {
        val perms = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        if (EasyPermissions.hasPermissions(this, *perms)) {
            initializeSession()
        } else {
            EasyPermissions.requestPermissions(this, "This app needs access to your camera and mic to make video calls", PERMISSIONS_REQUEST_CODE, *perms)
        }
    }

    private fun initializeSession() {

        if (AudioDeviceManager.getAudioDevice() == null || AudioDeviceManager.getAudioDevice()::class.simpleName == "DefaultAudioDevice") {
            // Set to custom audio device
            Log.i(TAG, "Setting up new Audio Device (recorder)")

            val timestampText: Long = System.currentTimeMillis() / 1000
            val fileName = "$timestampText-${OpentokConfig.SESSION_ID}.raw"
            val externalRoot = this.getExternalFilesDir(null)
            val filePath = File(externalRoot, fileName)
            Log.i(TAG, "File Path: ${filePath.absolutePath}")

            val audioDevice = RecorderAudioDevice(this, filePath)
            AudioDeviceManager.setAudioDevice(audioDevice)

            Log.i(TAG, "Audio Device (recorder) setup completed")
        } else {
            // Already existed
            Log.i(TAG, "AudioDevice existed - ${AudioDeviceManager.getAudioDevice()::class.simpleName ?: "Unknown audio device"}")
        }


        Log.i(TAG, "API Key: ${OpentokConfig.API_KEY}")
        Log.i(TAG, "Session ID: ${OpentokConfig.SESSION_ID}")
        Log.i(TAG, "Token: ${OpentokConfig.TOKEN}")

        session = Session.Builder(this, OpentokConfig.API_KEY, OpentokConfig.SESSION_ID).build()
        session?.setSessionListener(sessionListener)
        session?.connect(OpentokConfig.TOKEN)
    }
}
