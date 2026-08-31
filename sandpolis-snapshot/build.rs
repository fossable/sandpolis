fn main() {
    #[cfg(feature = "agent")]
    slint_build::compile("ui/boot_display.slint").expect("failed to compile boot_display.slint");
}
