use proc_macro::TokenStream;
use quote::quote;
use syn::{self, parse_macro_input, Data, DeriveInput, Fields, Ident};

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

#[proc_macro_derive(Delta)]
pub fn derive_delta(input: TokenStream) -> TokenStream {
    let input = parse_macro_input!(input as DeriveInput);

    let struct_name = &input.ident;
    let enum_name = Ident::new(&format!("{}Delta", struct_name), struct_name.span());

    let enum_variants_with_type = match &input.data {
        Data::Struct(data_struct) => {
            if let Fields::Named(fields) = &data_struct.fields {
                fields
                    .named
                    .iter()
                    .map(|field| {
                        let field_name = &field.ident;
                        let field_type = &field.ty;
                        quote! {
                            #field_name(#field_type),
                        }
                    })
                    .collect::<Vec<_>>()
            } else {
                panic!("Only structs with named fields are supported.");
            }
        }
        _ => panic!("Only structs are supported."),
    };

    let enum_variants_with_match = match &input.data {
        Data::Struct(data_struct) => {
            if let Fields::Named(fields) = &data_struct.fields {
                fields
                    .named
                    .iter()
                    .map(|field| {
                        let field_name = &field.ident;
                        quote! {
                            #enum_name::#field_name(data) => self.#field_name = data,
                        }
                    })
                    .collect::<Vec<_>>()
            } else {
                panic!("Only structs with named fields are supported.");
            }
        }
        _ => panic!("Only structs are supported."),
    };

    let expanded = quote! {
        pub enum #enum_name {
            #(#enum_variants_with_type)*
        }

        impl std::ops::AddAssign<#enum_name> for #struct_name {
            fn add_assign(&mut self, other: #enum_name) {
                match other {
                    #(#enum_variants_with_match)*
                }
            }
        }
    };

    TokenStream::from(expanded)
}

// TODO annotate a Data struct with attribute that identifies key so it can be automatically obtained
