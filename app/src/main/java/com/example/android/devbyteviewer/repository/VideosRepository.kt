package com.example.android.devbyteviewer.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.example.android.devbyteviewer.database.VideosDatabase
import com.example.android.devbyteviewer.database.asDomainModel
import com.example.android.devbyteviewer.domain.DevByteVideo
import com.example.android.devbyteviewer.network.DevByteNetwork
import com.example.android.devbyteviewer.network.asDatabaseModel
import com.example.android.devbyteviewer.network.asDomainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for fetching devbyte videos from the network and storing them on disk
 */
class VideosRepository(private val database: VideosDatabase) {

    /**
     * LiveData object to read the video playlist from the database.
     * This LiveData object is automatically updated when the database is updated.
     *
     *  Convert the list of database objects to a list of domain objects.
     *  Use the asDomainModel() conversion function.
     */
    val videos: LiveData<List<DevByteVideo>> = Transformations.map(database.videoDao.getVideos()){
        it.asDomainModel()
    }

    /**
     * Refresh the videos stored in the offline cache.
     * Databases on Android are stored on the file system, or disk, and in order to save they
     * must perform a disk I/O. The disk I/O, or reading and writing to disk, is slow and
     * always blocks the current thread until the operation is complete. Because of this,
     * you have to run the disk I/O in the I/O dispatcher. This dispatcher is designed to
     * offload blocking I/O tasks to a shared pool of threads using withContext(Dispatchers.IO){}.
     *
     * This function uses the IO dispatcher to ensure the database insert database operation
     * happens on the IO dispatcher. By switching to the IO dispatcher using `withContext` this
     * function is now safe to call from any thread including the Main thread.
     * Use the await() function to suspend the coroutine until the playlist is available
     */
    suspend fun refreshVideos() {
        withContext(Dispatchers.IO) {
            val playlist = DevByteNetwork.devbytes.getPlaylistAsync().await()

            //Use the asDatabaseModel() extension function to map the playlist to the database object.
            database.videoDao.insertAll(playlist.asDatabaseModel())
        }
    }
}