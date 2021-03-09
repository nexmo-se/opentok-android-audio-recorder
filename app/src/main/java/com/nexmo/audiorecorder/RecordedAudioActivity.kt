package com.nexmo.audiorecorder

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class RecordedAudioActivity : AppCompatActivity() {
    companion object {
        const val TAG = "RecordedAudioActivity"
    }

    private val audioFileList: MutableList<File> = mutableListOf()

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>

    private var sampleRate: Int = 44100

    private var currentFile: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recorded_audio)

        listView = findViewById(R.id.recorded_audio_listview)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        listView.adapter = adapter

        listView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val audioFile = audioFileList[position]
                Log.d(TAG, "Position: $position [${audioFile.name}]")

                playFile(audioFile)
            }


        loadRecordedAudioList()
    }

    private fun loadRecordedAudioList() {
        val externalRoot = this.getExternalFilesDir(null)
        val newAudioFileList = (externalRoot?.listFiles() ?: arrayOf()).sortedWith(Comparator {
            a, b -> a.name.compareTo(b.name)
        })


        audioFileList.clear()
        adapter.clear()
        Log.d(TAG, "Clearing File")

        for (audioFile in newAudioFileList) {
            Log.d(TAG, "File: ${audioFile.name}")

            audioFileList.add(audioFile)
            adapter.add(audioFile.name)
        }

        adapter.notifyDataSetChanged()
    }

    private fun playFile(file: File) {
        val runnable = Runnable {
            val currentTimestamp = System.currentTimeMillis()
            val currentExpectedFile = "$currentTimestamp-${file.name}"

            currentFile = currentExpectedFile

            Log.d(TAG, "Playback initializing")
            // get the minimum buffer size that can be used
            val minPlayBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                if (minPlayBufSize >= 6000) minPlayBufSize else minPlayBufSize * 2,
                AudioTrack.MODE_STREAM
            )

            // Check that the audioRecord is ready to be used.
            if (audioTrack.state != AudioTrack.STATE_INITIALIZED) {
                throw RuntimeException("AudioTrack not initialized $sampleRate")
            }

            Log.d(TAG, "Playback starting")
            audioTrack.play()
            Log.d(TAG, "Playback started")

            val inputStream: InputStream = FileInputStream(file)
            val buffer = ByteArray(16000)
            var readSize = inputStream.read(buffer, 0, 16000)
            while (readSize > 0 && currentFile != null && currentFile.equals(currentExpectedFile)) {
                audioTrack.write(buffer, 0, readSize)
                readSize = inputStream.read(buffer, 0, 16000)
            }

            Log.d(TAG, "Playback ending")
            audioTrack.pause()
            audioTrack.stop()
            audioTrack.flush()
            audioTrack.release()
            Log.d(TAG, "Playback ended")
        }
        val thread = Thread(runnable)
        thread.start()
    }
}
