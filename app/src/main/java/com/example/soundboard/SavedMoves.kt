package com.example.soundboard

import android.content.Context
import android.net.Uri
import android.media.*
import java.io.File
import java.nio.ByteBuffer

object SavedMoves {
    fun load(context: Context): List<Move> = context.getSharedPreferences("imports", Context.MODE_PRIVATE)
        .all.mapNotNull { (id, name) ->
            val file = File(context.filesDir, "$id.mp4")
            if (file.exists() && name is String) Move(name, 0, Uri.fromFile(file).toString()) else null
        }

    // Stream-copy at sync boundaries: no codec dependency or loss of quality.
    fun save(context: Context, uri: Uri, name: String, startMs: Int, endMs: Int) {
        val id = java.util.UUID.randomUUID().toString()
        val file = File(context.filesDir, "$id.mp4")
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(context, uri, null)
            val output = MediaMuxer(file.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = output
            val tracks = mutableMapOf<Int, Int>()
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    tracks[i] = output.addTrack(format)
                    extractor.selectTrack(i)
                }
            }
            require(tracks.isNotEmpty()) { "No supported video tracks" }
            val metadata = MediaMetadataRetriever()
            try {
                metadata.setDataSource(context, uri)
                metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull()?.let { output.setOrientationHint(it) }
            } finally { metadata.release() }
            extractor.seekTo(startMs * 1000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val base = extractor.sampleTime.coerceAtLeast(0)
            output.start()
            val buffer = ByteBuffer.allocate(32 * 1024 * 1024)
            val info = MediaCodec.BufferInfo()
            var samples = 0
            while (extractor.sampleTrackIndex >= 0 && extractor.sampleTime < endMs * 1000L) {
                buffer.clear()
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                info.set(0, size, (extractor.sampleTime - base).coerceAtLeast(0),
                    if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
                output.writeSampleData(tracks.getValue(extractor.sampleTrackIndex), buffer, info)
                samples++
                extractor.advance()
            }
            require(samples > 0) { "Empty selection" }
            output.stop()
            check(context.getSharedPreferences("imports", Context.MODE_PRIVATE).edit().putString(id, name).commit())
        } catch (e: Exception) {
            file.delete()
            throw e
        } finally {
            extractor.release()
            muxer?.release()
        }
    }
}
