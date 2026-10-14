use pyo3::exceptions::PyRuntimeError;
use pyo3::prelude::*;
use std::panic::{AssertUnwindSafe, catch_unwind};
use std::path::PathBuf;

#[pyfunction]
fn convert(py: Python<'_>, input: PathBuf, output: PathBuf) -> PyResult<()> {
    py.detach(move || {
        match catch_unwind(AssertUnwindSafe(|| boko_bindings_core::convert(&input, &output))) {
            Ok(result) => result.map_err(PyRuntimeError::new_err),
            Err(_) => Err(PyRuntimeError::new_err("boko conversion panicked")),
        }
    })
}

#[pymodule]
fn boko_python(module: &Bound<'_, PyModule>) -> PyResult<()> {
    module.add("__version__", env!("CARGO_PKG_VERSION"))?;
    module.add_function(wrap_pyfunction!(convert, module)?)?;
    Ok(())
}