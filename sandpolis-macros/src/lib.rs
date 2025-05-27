use proc_macro::TokenStream;
use quote::quote;
use syn::{self, Data, DeriveInput, Fields, Ident, parse_macro_input};

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
pub fn derive_delta(input: TokenStream) -> TokenStream {
    let input = parse_macro_input!(input as DeriveInput);

    let struct_name = &input.ident;
    let expanded = quote! {
        impl Data for #struct_name {
            fn id(&self) -> DataIdentifier {
                self._id
            }
        }
    };

    TokenStream::from(expanded)
}
