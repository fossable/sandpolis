//! Shared, rate-limited HTTP access for scraping services.

use crate::config::ScrapeConfig;
use anyhow::{Result, bail};
use std::sync::Arc;
use std::time::Duration;
use tokio::sync::Semaphore;
use url::Url;

/// A response body, already checked against the configured size limit.
#[derive(Debug, Clone)]
pub struct Fetched {
    /// The URL the body actually came from, after any redirects.
    pub url: Url,
    pub content_type: Option<String>,
    pub body: Vec<u8>,
}

impl Fetched {
    /// The body as UTF-8, lossily decoded.
    pub fn text(&self) -> std::borrow::Cow<'_, str> {
        String::from_utf8_lossy(&self.body)
    }
}

/// Shared HTTP access for scraping services: one connection pool, one request
/// budget, one response size limit.
#[derive(Clone)]
pub struct HttpFetcher {
    client: reqwest::Client,
    /// Caps outbound requests in flight across every service that holds a clone
    /// of this fetcher, so adding scrapers doesn't multiply the load we put on
    /// the network (or on any one site).
    budget: Arc<Semaphore>,
    max_bytes: usize,
}

impl HttpFetcher {
    pub fn new(config: &ScrapeConfig) -> Result<Self> {
        let client = reqwest::Client::builder()
            .user_agent(config.user_agent.clone())
            .timeout(Duration::from_secs(config.request_timeout))
            // Scraping follows links into untrusted territory; a redirect chain
            // is fine but shouldn't be unbounded.
            .redirect(reqwest::redirect::Policy::limited(5))
            .build()?;

        Ok(Self {
            client,
            budget: Arc::new(Semaphore::new(config.max_concurrent_requests.max(1))),
            max_bytes: config.max_response_bytes,
        })
    }

    /// GET `url`, reading at most the configured number of bytes.
    ///
    /// The body is read incrementally and abandoned as soon as it exceeds the
    /// limit, so an endless response can't exhaust memory even when it lies
    /// about (or omits) its content length.
    pub async fn get(&self, url: &Url) -> Result<Fetched> {
        if !matches!(url.scheme(), "http" | "https") {
            bail!("Refusing to fetch non-HTTP URL: {url}");
        }

        let _permit = self.budget.acquire().await?;

        let response = self.client.get(url.clone()).send().await?;
        let status = response.status();
        if !status.is_success() {
            bail!("{url} returned {status}");
        }

        let final_url = response.url().clone();
        let content_type = response
            .headers()
            .get(reqwest::header::CONTENT_TYPE)
            .and_then(|v| v.to_str().ok())
            .map(|v| v.to_string());

        let mut response = response;
        let mut body = Vec::new();
        while let Some(chunk) = response.chunk().await? {
            if body.len() + chunk.len() > self.max_bytes {
                bail!("{url} exceeds the {} byte response limit", self.max_bytes);
            }
            body.extend_from_slice(&chunk);
        }

        Ok(Fetched {
            url: final_url,
            content_type,
            body,
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn fetcher_rejects_non_http_urls() -> Result<()> {
        let http = HttpFetcher::new(&ScrapeConfig::default())?;
        let error = http
            .get(&Url::parse("file:///etc/passwd")?)
            .await
            .expect_err("file URLs are refused");
        assert!(error.to_string().contains("non-HTTP"));
        Ok(())
    }
}
