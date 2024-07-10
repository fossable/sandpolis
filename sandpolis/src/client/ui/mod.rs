use crate::client::ui::physics::GraphLayoutEngine;
use bevy::{
    color::palettes::basic::*,
    input::{gestures::RotationGesture, touch::TouchPhase},
    prelude::*,
    sprite::Mesh2dHandle,
    window::{AppLifecycle, WindowMode},
};
use rapier2d::dynamics::RigidBodyHandle;

mod physics;

/// Initialize and start rendering the UI.
pub fn run() {
    let mut app = App::new();
    app.add_plugins(DefaultPlugins.set(WindowPlugin {
        primary_window: Some(Window {
            resizable: false,
            mode: if cfg!(target_os = "android") {
                WindowMode::BorderlessFullscreen
            } else {
                WindowMode::Windowed
            },
            // on iOS, gestures must be enabled.
            // This doesn't work on Android
            recognize_rotation_gesture: true,
            ..default()
        }),
        ..default()
    }))
    .insert_resource(GraphLayoutEngine::new())
    .add_systems(Startup, setup)
    .add_systems(
        Update,
        (
            touch_camera,
            button_handler,
            handle_lifetime,
            update_node_positions,
        ),
    );

    // MSAA makes some Android devices panic, this is under investigation
    // https://github.com/bevyengine/bevy/issues/8229
    #[cfg(target_os = "android")]
    app.insert_resource(Msaa::Off);

    app.run();
}

fn touch_camera(
    windows: Query<&Window>,
    mut touches: EventReader<TouchInput>,
    mut camera: Query<&mut Transform, With<Camera3d>>,
    mut last_position: Local<Option<Vec2>>,
    mut rotations: EventReader<RotationGesture>,
) {
    let window = windows.single();

    for touch in touches.read() {
        if touch.phase == TouchPhase::Started {
            *last_position = None;
        }
        if let Some(last_position) = *last_position {
            let mut transform = camera.single_mut();
            *transform = Transform::from_xyz(
                transform.translation.x
                    + (touch.position.x - last_position.x) / window.width() * 5.0,
                transform.translation.y,
                transform.translation.z
                    + (touch.position.y - last_position.y) / window.height() * 5.0,
            )
            .looking_at(Vec3::ZERO, Vec3::Y);
        }
        *last_position = Some(touch.position);
    }
    // Rotation gestures only work on iOS
    for rotation in rotations.read() {
        let mut transform = camera.single_mut();
        let forward = transform.forward();
        transform.rotate_axis(forward, rotation.0 / 10.0);
    }
}

#[derive(Bundle, Clone)]
pub struct Node {
    pub id: NodeId,
    pub mesh: Mesh2dHandle,
    pub material: Handle<ColorMaterial>,
    pub transform: Transform,
    // pub global_transform: GlobalTransform,
    // /// User indication of whether an entity is visible
    pub visibility: Visibility,
    // // Inherited visibility of an entity.
    // pub inherited_visibility: InheritedVisibility,
    // // Indication of whether an entity is visible in any view.
    // pub view_visibility: ViewVisibility,
}

fn setup(
    mut commands: Commands,
    mut layout_engine: ResMut<GraphLayoutEngine>,
    mut meshes: ResMut<Assets<Mesh>>,
    mut materials: ResMut<Assets<ColorMaterial>>,
) {
    commands.spawn(Camera2dBundle::default());
    commands.spawn(Node {
        id: NodeId(layout_engine.add()),
        mesh: meshes.add(Rectangle::default()).into(),
        transform: Transform::default().with_scale(Vec3::splat(128.)),
        material: materials.add(Color::from(PURPLE)),
        visibility: Visibility::Visible,
    });

    // Test ui
    commands
        .spawn(ButtonBundle {
            style: Style {
                justify_content: JustifyContent::Center,
                align_items: AlignItems::Center,
                position_type: PositionType::Absolute,
                left: Val::Px(50.0),
                right: Val::Px(50.0),
                bottom: Val::Px(50.0),
                ..default()
            },
            ..default()
        })
        .with_children(|b| {
            b.spawn(
                TextBundle::from_section(
                    "Test Button",
                    TextStyle {
                        font_size: 30.0,
                        color: Color::BLACK,
                        ..default()
                    },
                )
                .with_text_justify(JustifyText::Center),
            );
        });
}

fn button_handler(
    mut interaction_query: Query<
        (&Interaction, &mut BackgroundColor),
        (Changed<Interaction>, With<Button>),
    >,
) {
    for (interaction, mut color) in &mut interaction_query {
        match *interaction {
            Interaction::Pressed => {
                *color = BLUE.into();
            }
            Interaction::Hovered => {
                *color = GRAY.into();
            }
            Interaction::None => {
                *color = WHITE.into();
            }
        }
    }
}

// Pause audio when app goes into background and resume when it returns.
// This is handled by the OS on iOS, but not on Android.
fn handle_lifetime(
    mut lifecycle_events: EventReader<AppLifecycle>,
    music_controller: Query<&AudioSink>,
) {
    let Ok(music_controller) = music_controller.get_single() else {
        return;
    };

    for event in lifecycle_events.read() {
        match event {
            AppLifecycle::Idle | AppLifecycle::WillSuspend | AppLifecycle::WillResume => {}
            AppLifecycle::Suspended => music_controller.pause(),
            AppLifecycle::Running => music_controller.play(),
        }
    }
}

#[derive(Clone, Component, Debug)]
struct NodeId(RigidBodyHandle);

fn update_node_positions(
    time: Res<Time>,
    mut layout_engine: ResMut<GraphLayoutEngine>,
    mut nodes: Query<(&NodeId, &mut Transform)>,
) {
    layout_engine.step();

    for (id, mut node) in &mut nodes {
        if let Some((y, x)) = layout_engine.get_position(id.0) {
            if node.translation.y != y || node.translation.x != x {
                trace!(y = y, x = x, "Translating node");
                node.translation.y = y;
                node.translation.x = x;
            }
        } else {
            trace!("Node not found");
        }
    }
}
