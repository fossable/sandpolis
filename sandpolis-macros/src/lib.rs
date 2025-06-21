use proc_macro::TokenStream;
use proc_macro2::Span;
use quote::quote;
use std::hash::{DefaultHasher, Hash, Hasher};
use syn::{
    self, DeriveInput, Field, Fields, Ident, ItemStruct, LitInt, Path, Token,
    meta::ParseNestedMeta, parse::Parser, parse_macro_input,
};

#[proc_macro_derive(StreamEvent)]
pub fn derive_event(input: TokenStream) -> TokenStream {
    let input = parse_macro_input!(input as DeriveInput);
    let name = input.ident;

    // TODO assert type name ends with 'Event'?

    let expanded = quote! {
        #[cfg(any(feature = "server", feature = "agent"))]
        impl Into<axum::extract::ws::Message> for #name {
            fn into(self) -> axum::extract::ws::Message {
                sandpolis_network::stream::event_to_message(&self)
            }
        }
    };

    TokenStream::from(expanded)
}

#[proc_macro_derive(Data)]
pub fn derive_data(input: TokenStream) -> TokenStream {
    let input = parse_macro_input!(input as DeriveInput);

    let struct_name = &input.ident;
    let expanded = quote! {
        impl sandpolis_database::Data for #struct_name {
            fn id(&self) -> sandpolis_database::DataIdentifier {
                self._id
            }

            // fn set_id(&mut self, id: DataIdentifier) {
            //     self._id = id;
            // }
        }
    };

    TokenStream::from(expanded)
}

#[proc_macro_derive(InstanceData)]
pub fn derive_instance_data(input: TokenStream) -> TokenStream {
    let input = parse_macro_input!(input as DeriveInput);

    let struct_name = &input.ident;
    let expanded = quote! {
        impl sandpolis_database::InstanceData for #struct_name {
            fn instance_id(&self) -> sandpolis_core::InstanceId {
                self._instance_id
            }
        }
    };

    TokenStream::from(expanded)
}

#[proc_macro_derive(HistoricalData)]
pub fn derive_historical_data(input: TokenStream) -> TokenStream {
    let input = parse_macro_input!(input as DeriveInput);

    let struct_name = &input.ident;
    let expanded = quote! {
        impl sandpolis_database::HistoricalData for #struct_name {
            fn timestamp(&self) -> sandpolis_database::DbTimestamp {
                self._timestamp
            }
        }
    };

    TokenStream::from(expanded)
}

#[derive(Default)]
struct DataAttributes {
    // Our attributes
    expire: bool,
    history: bool,
    instance: bool,

    // Wrapper for: https://github.com/vincent-herlemont/native_model/blob/084a81809d3d82bba731ae930eafb56aae3537bc/native_model_macro/src/lib.rs#L19
    pub(crate) id: Option<LitInt>,
    pub(crate) version: Option<LitInt>,
    pub(crate) with: Option<Path>,
    pub(crate) from: Option<Path>,
    pub(crate) try_from: Option<(Path, Path)>,
}

impl DataAttributes {
    fn parse(&mut self, meta: ParseNestedMeta) -> syn::parse::Result<()> {
        if meta.path.is_ident("expire") {
            self.expire = true;
        } else if meta.path.is_ident("history") {
            self.history = true;
        } else if meta.path.is_ident("instance") {
            self.instance = true;
        } else if meta.path.is_ident("id") {
            self.id = Some(meta.value()?.parse()?);
        } else if meta.path.is_ident("version") {
            self.version = Some(meta.value()?.parse()?);
        } else if meta.path.is_ident("with") {
            self.with = Some(meta.value()?.parse()?);
        } else if meta.path.is_ident("from") {
            self.from = Some(meta.value()?.parse()?);
        } else if meta.path.is_ident("try_from") {
            // let tuple_try_from: TupleTryFrom = meta.value()?.parse()?;
            // let mut fields = tuple_try_from.fields.into_iter();
            // self.try_from.replace((
            //     fields.next().unwrap().clone(),
            //     fields.next().unwrap().clone(),
            // ));
        } else {
            panic!(
                "Unknown attribute: {}",
                meta.path.get_ident().unwrap().to_string()
            );
        }
        Ok(())
    }
}

/// Automates some of the boilerplate needed when defining `Data` structs. Model
/// ids will be generated according to the struct name.
#[proc_macro_attribute]
pub fn data(args: TokenStream, input: TokenStream) -> TokenStream {
    let mut attrs = DataAttributes::default();
    let args_parser = syn::meta::parser(|meta| attrs.parse(meta));
    parse_macro_input!(args with args_parser);

    let mut item_struct = parse_macro_input!(input as ItemStruct);
    let struct_name = item_struct.ident.to_string();

    // Get id or compute from struct name
    let id: u32 = attrs
        .id
        .map(|v| v.base10_parse().expect("Failed to parse model version"))
        .unwrap_or(struct_name_to_id(&struct_name));

    // Get version that was passed in or default to 1
    let version = attrs.version.unwrap_or(LitInt::new("1", Span::call_site()));

    if let Fields::Named(ref mut fields) = item_struct.fields {
        // Add id field
        fields.named.push(
            Field::parse_named
                .parse2(quote! {
                    /// Primary key
                    #[primary_key]
                    pub _id: sandpolis_database::DataIdentifier
                })
                .expect("Failed to parse _id field"),
        );

        // Add timestamp field
        if attrs.history {
            fields.named.push(
                Field::parse_named
                    .parse2(quote! {
                        /// Creation timestamp
                        #[secondary_key]
                        pub _timestamp: sandpolis_database::DbTimestamp
                    })
                    .expect("Failed to parse _timestamp field"),
            );
        }

        // Add instance id field
        if attrs.expire {
            fields.named.push(
                Field::parse_named
                    .parse2(quote! {
                        /// ID of instance associated with this data
                        #[secondary_key]
                        pub _instance_id: sandpolis_core::InstanceId
                    })
                    .expect("Failed to parse _instance_id field"),
            );
        }

        // Add expiration field
        if attrs.expire {
            fields.named.push(
                Field::parse_named
                    .parse2(quote! {
                        /// Expiration timestamp
                        #[secondary_key]
                        pub _expiration: DbTimestamp
                    })
                    .expect("Failed to parse _expiration field"),
            );
        }
    }

    // At minimum, the Data trait is required
    let mut data_derive_macros = quote!(sandpolis_macros::Data);

    if attrs.history {
        data_derive_macros.extend(quote! { , sandpolis_macros::HistoricalData });
    }

    if attrs.instance {
        data_derive_macros.extend(quote! { , sandpolis_macros::InstanceData });
    }

    // TODO pass remaining if they are present
    let tokens = quote! {
        #[derive(Serialize, Deserialize, Clone, PartialEq, Debug, Default, #data_derive_macros)]
        #[native_model::native_model(id = #id, version = #version)]
        #[native_db::native_db]
        #item_struct
    };

    tokens.into()
}

/// Hash a struct name to obtain the unique id
fn struct_name_to_id(name: &str) -> u32 {
    let mut hasher = DefaultHasher::new();
    std::env::var("CARGO_PKG_NAME")
        .expect("Crate name not found")
        .hash(&mut hasher);
    name.hash(&mut hasher);
    (hasher.finish() & 0xFFFF_FFFF) as u32
}
