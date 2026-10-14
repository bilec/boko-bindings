use jni::JNIEnv;
use jni::objects::{JClass, JString};
use std::panic::{AssertUnwindSafe, catch_unwind};
use std::path::Path;

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_github_bilec_boko_Boko_convert(
    mut env: JNIEnv,
    _class: JClass,
    input: JString,
    output: JString,
) {
    let result = catch_unwind(AssertUnwindSafe(|| {
        let input: String = env.get_string(&input).map_err(|error| error.to_string())?.into();
        let output: String = env.get_string(&output).map_err(|error| error.to_string())?.into();
        boko_bindings_core::convert(Path::new(&input), Path::new(&output))
    }));
    let error = match result {
        Ok(Ok(())) => return,
        Ok(Err(error)) => error,
        Err(_) => "boko conversion panicked".to_string(),
    };
    let _ = env.throw_new("java/lang/IllegalStateException", error);
}