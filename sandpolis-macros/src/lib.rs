use proc_macro::TokenStream;
use proc_macro2::Span;
use quote::quote;
use std::hash::{DefaultHasher, Hash, Hasher};
use syn::{
    self, DeriveInput, Field, Fields, Ident, ItemStruct, LitInt, Path, Token,
    meta::ParseNestedMeta, parse_macro_input,
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
        impl Data for #struct_name {
            fn id(&self) -> DataIdentifier {
                self._id
            }

            fn set_id(&mut self, id: DataIdentifier) {
                self._id = id;
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

#[proc_macro_attribute]
pub fn data(args: TokenStream, input: TokenStream) -> TokenStream {
    let mut attrs = DataAttributes::default();
    let args_parser = syn::meta::parser(|meta| attrs.parse(meta));
    parse_macro_input!(args with args_parser);

    let mut item_struct = parse_macro_input!(input as ItemStruct);

    // Get id or compute from struct name
    let id: u32 = attrs
        .id
        .map(|v| v.base10_parse().unwrap())
        .unwrap_or(struct_name_to_id(&item_struct.ident.to_string()));

    // Get version that was passed in or default to 1
    let version = attrs.version.unwrap_or(LitInt::new("1", Span::call_site()));

    if let Fields::Named(ref mut fields) = item_struct.fields {
        // Add id field
        fields.named.push(Field {
            attrs: vec![],
            vis: syn::Visibility::Inherited,
            mutability: syn::FieldMutability::None,
            ident: Some(Ident::new("_id", Span::call_site())),
            colon_token: Some(Token![:]),
            ty: todo!(),
        });

        // Add timestamp field
        if attrs.history {}

        // Add expiration field
        if attrs.expire {}
    }

    return quote! {
        #[derive(Clone, Send)]
        #[native_model(id = $id, version = $version)]
        #[native_db]
        #item_struct
    }
    .into();
}

/// Hash a struct name to obtain the unique id
fn struct_name_to_id(name: &str) -> u32 {
    let mut hasher = DefaultHasher::new();
    name.hash(&mut hasher);
    (hasher.finish() & 0xFFFF_FFFF) as u32
}
