use boko::{Book, Format};
use std::fs::File;
use std::path::Path;

pub fn convert(input: &Path, output: &Path) -> Result<(), String> {
    let format = match output.extension().and_then(|extension| extension.to_str()) {
        Some("epub") => Format::Epub,
        Some("azw3") => Format::Azw3,
        Some("kfx") => Format::Kfx,
        Some("md") => Format::Markdown,
        _ => return Err("Unsupported boko output format".to_string()),
    };
    let source = input.canonicalize().map_err(|error| error.to_string())?;
    if output.canonicalize().ok().as_ref() == Some(&source) {
        return Err("Input and output must be different files".to_string());
    }
    let book = Book::open(&source).map_err(|error| error.to_string())?;
    let mut file = File::create(output).map_err(|error| error.to_string())?;
    book.export(format, &mut file).map_err(|error| error.to_string())
}

#[cfg(test)]
mod tests {
    use super::*;
    use boko::Book;
    use std::fs::File;
    use std::io::Write;
    use std::path::Path;
    use zip::write::SimpleFileOptions;

    #[test]
    fn converts_all_outputs_and_rejects_unsafe_paths() {
        let directory = std::env::temp_dir().join(format!("boko-core-test-{}", std::process::id()));
        std::fs::create_dir_all(&directory).unwrap();
        let input = directory.join("book.epub");
        let mut archive = zip::ZipWriter::new(File::create(&input).unwrap());
        for (name, text) in [
            ("mimetype", include_str!("../../tests/fixtures/mimetype")),
            ("META-INF/container.xml", include_str!("../../tests/fixtures/META-INF/container.xml")),
            ("content.opf", include_str!("../../tests/fixtures/content.opf")),
            ("chapter.xhtml", include_str!("../../tests/fixtures/chapter.xhtml")),
        ] {
            archive.start_file(name, SimpleFileOptions::default()).unwrap();
            archive.write_all(text.trim().as_bytes()).unwrap();
        }
        archive.finish().unwrap();
        let original = std::fs::read(&input).unwrap();
        assert!(convert(&input, &input).is_err());
        assert_eq!(std::fs::read(&input).unwrap(), original);

        for extension in ["epub", "azw3", "kfx", "md"] {
            let output = directory.join(format!("converted.{extension}"));
            convert(&input, &output).unwrap();
            assert!(output.metadata().unwrap().len() > 0);
            if extension == "md" {
                assert!(std::fs::read_to_string(&output).unwrap().contains("A readable book."));
            } else {
                assert_eq!(Book::open(&output).unwrap().metadata().title, "Test Book");
            }
        }
        let unsupported = directory.join("unsupported.mobi");
        assert!(convert(&input, &unsupported).is_err());
        assert!(!unsupported.exists());
        let missing_output = directory.join("missing.kfx");
        assert!(convert(Path::new("/nonexistent/book.epub"), &missing_output).is_err());
        assert!(!missing_output.exists());
        std::fs::remove_dir_all(directory).unwrap();
    }
}