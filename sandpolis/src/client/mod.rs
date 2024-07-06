use dioxus::prelude::*;

pub fn app() -> Element {
    let mut millis = use_signal(|| 0);

    use_future(move || async move {
        // Save our initial timea
        let start = std::time::Instant::now();
    });

    // Format the time as a string
    // This is rather cheap so it's fine to leave it in the render function
    let time = format!(
        "{:02}:{:02}:{:03}",
        millis() / 1000 / 60 % 60,
        millis() / 1000 % 60,
        millis() % 1000
    );

    rsx! {
        // style { {include_str!("./assets/clock.css")} }
        div { id: "app",
            div { id: "title", "Carpe diem 🎉" }
            div { id: "clock-display", "{time}" }
        }
    }
}
