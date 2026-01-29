use proc_macro::TokenStream;
use proc_macro2::TokenStream as TokenStream2;
use quote::quote;
use std::hash::{DefaultHasher, Hash, Hasher};
use syn::{
    self, DeriveInput, Field, Fields, ItemStruct, LitInt, Path, meta::ParseNestedMeta,
    parse::Parser, parse_macro_input,
};

/// Returns the token stream for the `sandpolis_instance` crate root.
/// When compiling from within `sandpolis-instance` itself, this returns `crate`;
/// otherwise it returns `sandpolis_instance`.
fn instance_crate() -> TokenStream2 {
    if std::env::var("CARGO_PKG_NAME").as_deref() == Ok("sandpolis-instance") {
        quote! { crate }
    } else {
        quote! { sandpolis_instance }
    }
}

#[proc_macro_derive(Data)]
pub fn derive_data(input: TokenStream) -> TokenStream {
    let input = parse_macro_input!(input as ItemStruct);
    let krate = instance_crate();

    let expiration = if input
        .fields
        .iter()
        .find(|field| {
            field
                .ident
                .as_ref()
                .map(|i| i.to_string())
                .unwrap_or_default()
                == "_expiration"
        })
        .is_some()
    {
        quote! {
            fn expiration(&self) -> Option<#krate::database::DataExpiration> {
                Some(self._expiration)
            }
        }
    } else {
        quote! {
            fn expiration(&self) -> Option<#krate::database::DataExpiration> {
                None
            }
        }
    };

    let struct_name = &input.ident;
    let expanded = quote! {
        impl #krate::database::Data for #struct_name {
            fn id(&self) -> #krate::database::DataIdentifier {
                self._id
            }

            fn set_id(&mut self, id: #krate::database::DataIdentifier) {
                self._id = id;
            }

            fn revision(&self) -> #krate::database::DataRevision {
                self._revision
            }

            fn set_revision(&mut self, revision: #krate::database::DataRevision) {
                self._revision = revision;
            }

            fn creation(&self) -> #krate::database::DataCreation {
                self._creation
            }

            #expiration
        }
    };

    TokenStream::from(expanded)
}

#[derive(Default)]
struct DataAttributes {
    // Our attributes
    temporal: bool,
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
        if meta.path.is_ident("temporal") {
            self.temporal = true;
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
                meta.path
                    .get_ident()
                    .map(|i| i.to_string())
                    .unwrap_or_default()
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
    let krate = instance_crate();

    if let Fields::Named(ref mut fields) = item_struct.fields {
        // Add id field
        fields.named.push(
            Field::parse_named
                .parse2(quote! {
                    /// Primary key
                    #[primary_key]
                    pub _id: #krate::database::DataIdentifier
                })
                .expect("Failed to parse _id field"),
        );

        // Add revision field
        fields.named.push(
            Field::parse_named
                .parse2(quote! {
                    /// Revision
                    #[secondary_key]
                    pub _revision: #krate::database::DataRevision
                })
                .expect("Failed to parse _revision field"),
        );

        // Add creation field
        fields.named.push(
            Field::parse_named
                .parse2(quote! {
                    /// Creation timestamp
                    #[secondary_key]
                    pub _creation: #krate::database::DataCreation
                })
                .expect("Failed to parse _creation field"),
        );

        // Add expiration field
        if attrs.temporal {
            fields.named.push(
                Field::parse_named
                    .parse2(quote! {
                        /// Expiration timestamp
                        #[secondary_key]
                        pub _expiration: #krate::database::DataExpiration
                    })
                    .expect("Failed to parse _expiration field"),
            );
        }

        // Add instance id field
        if attrs.instance {
            fields.named.push(
                Field::parse_named
                    .parse2(quote! {
                        /// ID of instance associated with this data
                        #[secondary_key]
                        pub _instance_id: #krate::InstanceId
                    })
                    .expect("Failed to parse _instance_id field"),
            );
        }
    }

    // Process args for native_model
    let mut model_args = quote!();

    if let Some(id) = attrs.id.as_ref() {
        // Pass through
        model_args.extend(quote! { id = #id });
    } else {
        // Default
        let id = struct_name_to_id(&struct_name);
        model_args.extend(quote! { id = #id });
    }

    if let Some(version) = attrs.version.as_ref() {
        // Pass through
        model_args.extend(quote! { , version = #version });
    } else {
        // Default
        model_args.extend(quote! { , version = 1 });
    }

    if let Some(with) = attrs.with.as_ref() {
        // Pass through
        model_args.extend(quote! { , with = #with });
    }

    if let Some(from) = attrs.from.as_ref() {
        // Pass through
        model_args.extend(quote! { , from = #from });
    }

    // if let Some(try_from) = attrs.try_from.as_ref() {
    //     // Pass through
    //     model_args.extend(quote! { , try_from = #try_from });
    // }

    let tokens = quote! {
        #[derive(serde::Serialize, serde::Deserialize, Clone, PartialEq, Debug, sandpolis_macros::Data)]
        #[native_model::native_model(#model_args)]
        #[native_db::native_db]
        #item_struct
    };

    tokens.into()
}

/// Hash a struct name to obtain the unique id
fn struct_name_to_id(name: &str) -> u32 {
    let mut hasher = DefaultHasher::new();

    // Include crate name to allow structs with the same name in different layers
    std::env::var("CARGO_PKG_NAME")
        .expect("Crate name not found")
        .hash(&mut hasher);
    name.hash(&mut hasher);
    (hasher.finish() & 0xFFFF_FFFF) as u32
}

#[proc_macro_derive(Stream)]
pub fn derive_stream_requester(input: TokenStream) -> TokenStream {
    let input = parse_macro_input!(input as DeriveInput);
    let name = &input.ident;
    let krate = instance_crate();
    // TODO validate base name ends with one of these.
    let base_name = &name
        .to_string()
        .trim_end_matches("Requester")
        .trim_end_matches("Responder")
        .to_string();
    let type_tag = struct_name_to_id(&base_name);

    let expanded = quote! {
        impl #krate::network::stream::Stream for #name {
            fn tag() -> u32 {
                #type_tag
            }
        }
    };

    TokenStream::from(expanded)
}
