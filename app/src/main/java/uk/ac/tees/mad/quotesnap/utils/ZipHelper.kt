package uk.ac.tees.mad.quotesnap.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipHelper {

    // create zip file from the list of files
    fun createZipFile(context: Context, files: List<File>, zipFileName: String): File {
        val zipFile = File(context.cacheDir, zipFileName)

        ZipOutputStream(FileOutputStream(zipFile)).use { zipOutputStream ->
            files.forEach { file ->
                try {
                    FileInputStream(file).use { inputStream ->
                        val entry = ZipEntry(file.name)
                        zipOutputStream.putNextEntry(entry)

                        val buffer = ByteArray(1024)
                        var length: Int
                        while (inputStream.read(buffer).also { length = it } > 0) {
                            zipOutputStream.write(buffer, 0, length)
                        }

                        zipOutputStream.closeEntry()
                    }
                } catch (e: Exception) {
                    Log.e("ZipHelper", "Error zipping file ${file.name}: ${e.message}")
                }
            }
        }
        return zipFile
    }

    // clean up tempeorary file
    fun cleanUpFiles(files: List<File>) {
        files.forEach {
            try {
                if (it.exists()) {
                    it.delete()
                }
            } catch (e: Exception) {
                Log.e("ZipHelper", "Error deleting file: ${e.message}")
            }
        }
    }

}