import tempfile
import unittest
import zipfile
from pathlib import Path

import boko_python


class ConversionTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.source = self.root / "book.epub"
        fixtures = Path(__file__).parent / "fixtures"
        with zipfile.ZipFile(self.source, "w") as archive:
            for path in fixtures.rglob("*"):
                if path.is_file():
                    archive.writestr(path.relative_to(fixtures).as_posix(), path.read_bytes().strip())

    def test_all_outputs_and_kfx_round_trip(self):
        for extension in ("epub", "azw3", "kfx", "md"):
            with self.subTest(extension=extension):
                output = self.root / f"output.{extension}"
                self.assertIsNone(boko_python.convert(self.source, output))
                self.assertGreater(output.stat().st_size, 0)
                if extension == "md":
                    self.assertIn("A readable book.", output.read_text())
        restored = self.root / "restored.epub"
        boko_python.convert(self.root / "output.kfx", restored)
        with zipfile.ZipFile(restored) as archive:
            text = b"".join(archive.read(name) for name in archive.namelist() if name.endswith((".xhtml", ".html")))
        self.assertIn(b"A readable book.", text)

    def test_invalid_requests_do_not_change_source_or_create_output(self):
        original = self.source.read_bytes()
        with self.assertRaises(RuntimeError):
            boko_python.convert(self.source, self.source)
        self.assertEqual(original, self.source.read_bytes())
        for source, name in ((self.source, "unsupported.mobi"), (self.root / "missing.epub", "missing.kfx")):
            output = self.root / name
            with self.assertRaises(RuntimeError):
                boko_python.convert(str(source), str(output))
            self.assertFalse(output.exists())


if __name__ == "__main__":
    unittest.main()