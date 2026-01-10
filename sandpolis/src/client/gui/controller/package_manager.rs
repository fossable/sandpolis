use bevy_egui::egui;
use sandpolis_core::InstanceId;
use crate::InstanceState;
use crate::client::gui::queries;

#[derive(Default, Clone, serde::Serialize, serde::Deserialize)]
pub struct PackageManagerState {
    pub search_term: String,
    pub selected_package: Option<String>,
}

pub fn render(ui: &mut egui::Ui, state: &InstanceState, instance_id: InstanceId) {
    let state_id = egui::Id::new(format!("pm_{}", instance_id));
    let mut pm_state = ui.data_mut(|d| d.get_persisted::<PackageManagerState>(state_id).unwrap_or_default());

    ui.horizontal(|ui| {
        ui.label("Search:");
        ui.text_edit_singleline(&mut pm_state.search_term);
        if ui.button("🔍").clicked() {}
    });

    ui.separator();
    ui.label("Installed Packages:");
    egui::ScrollArea::vertical().max_height(250.0).show(ui, |ui| {
        match queries::query_packages(state, instance_id) {
            Ok(packages) => {
                if packages.is_empty() {
                    ui.label("No packages found");
                } else {
                    for package in packages {
                        let is_selected = pm_state.selected_package.as_ref() == Some(&package.name);
                        if ui.selectable_label(is_selected, format!("{} v{}", package.name, package.version)).clicked() {
                            pm_state.selected_package = Some(package.name.clone());
                        }
                    }
                }
            }
            Err(_) => { ui.label("Error loading packages"); }
        }
    });

    ui.separator();
    ui.horizontal(|ui| {
        if ui.button("Install").clicked() {}
        let has_selection = pm_state.selected_package.is_some();
        if ui.add_enabled(has_selection, egui::Button::new("Update")).clicked() {}
        if ui.add_enabled(has_selection, egui::Button::new("Remove")).clicked() {}
    });

    ui.data_mut(|d| d.insert_persisted(state_id, pm_state));
}
