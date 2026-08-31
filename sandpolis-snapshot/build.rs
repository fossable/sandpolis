fn main() {
    #[cfg(feature = "uki")]
    slint_build::compile("ui/boot_snapshot.slint").expect("failed to compile boot_snapshot.slint");
}
