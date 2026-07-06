// The imported screen-capture code (from rustdesk's `scrap`) selects its
// backend with the custom cfgs `x11`, `quartz` and `dxgi` rather than plain
// `target_os`, because on unix the choice between X11 and DXGI/Quartz is not a
// simple 1:1 mapping. scrap set these from its own build script; we replicate
// that here.
fn main() {
    println!("cargo::rustc-check-cfg=cfg(dxgi,quartz,x11)");

    let target_os = std::env::var("CARGO_CFG_TARGET_OS").unwrap();

    match target_os.as_str() {
        "ios" | "android" => {}
        "windows" => println!("cargo::rustc-cfg=dxgi"),
        "macos" => println!("cargo::rustc-cfg=quartz"),
        // Every other unix: assume X11 (with a Wayland fallback at runtime).
        _ => println!("cargo::rustc-cfg=x11"),
    }
}
