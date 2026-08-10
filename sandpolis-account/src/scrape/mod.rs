//! Scraping data from the Internet into the database.
//!
//! Scrapers are ordinary [`sandpolis_instance::service::Service`]s that reach out
//! over the network, so scheduling, cancellation, and run bookkeeping all come
//! from the service runner. What lives here is the part that's specific to
//! scraping: a shared [`HttpFetcher`] enforcing one connection pool, one request
//! budget, and one response size limit across every scraper.
//!
//! Only servers scrape. Agents have no reason to reach out to third parties on
//! the estate's behalf, and having every client do it independently would
//! multiply the traffic for no benefit.

#[cfg(feature = "server")]
mod http;

#[cfg(feature = "server")]
pub use http::{Fetched, HttpFetcher};
