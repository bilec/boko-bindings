package io.github.bilec.boko

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class BokoTest {
    @Test
    fun publicApiConvertsFormatsAndPreservesSourceOnErrors() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.cacheDir, "bindings-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val source = File(directory, "book.epub")
            ZipOutputStream(source.outputStream()).use { zip ->
                for (name in listOf("mimetype", "META-INF/container.xml", "content.opf", "chapter.xhtml")) {
                    zip.putNextEntry(ZipEntry(name))
                    instrumentation.context.assets.open(name).use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            val original = source.readBytes()
            assertThrows(IllegalStateException::class.java) { Boko.convert(source.path, source.path) }
            assertArrayEquals(original, source.readBytes())
            for (extension in listOf("epub", "azw3", "kfx", "md")) {
                val output = File(directory, "output.$extension")
                Boko.convert(source.path, output.path)
                assertTrue(output.length() > 0)
                if (extension == "md") assertTrue(output.readText().contains("A readable book."))
            }
            val restored = File(directory, "restored.epub")
            Boko.convert(File(directory, "output.kfx").path, restored.path)
            ZipFile(restored).use { zip ->
                val text = zip.entries().asSequence().filter { it.name.endsWith(".xhtml") || it.name.endsWith(".html") }
                    .joinToString { zip.getInputStream(it).bufferedReader().use { reader -> reader.readText() } }
                assertTrue(text.contains("A readable book."))
            }
            val unsupported = File(directory, "unsupported.mobi")
            assertThrows(IllegalStateException::class.java) { Boko.convert(source.path, unsupported.path) }
            assertFalse(unsupported.exists())
        } finally {
            directory.deleteRecursively()
        }
    }
}